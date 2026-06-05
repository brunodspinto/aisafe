/*
 * main.c - Flight Simulation entry point (US105: hybrid environment)
 *
 * US105 refactoring: pipes substituídos por shared memory + named semaphores.
 * O processo pai é agora multi-threaded:
 *   - coordinator_thread: loop de sincronização passo-a-passo (lê posições,
 *     verifica segurança, envia GO/STOP via shared memory + semáforos).
 *   - report_thread:      aguarda variável de condição no fim da simulação
 *                         e gera o relatório final (US109).
 *
 * Padrões SCOMP usados:
 *   shm_open + ftruncate + mmap  →  ex1-7.c, ex2-6.c
 *   sem_open / sem_post / sem_wait → ex1-7.c, ex2-5.c
 *   pthread_create / pthread_join  → ex1-9.c
 *   mutex + cond var               → T7/T8 slides
 *   SIGUSR1                        → ex1-3.c, ex1-4.c (em flight_process.c)
 */
#define _POSIX_C_SOURCE 200809L
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <semaphore.h>
#include "types.h"
#include "config.h"
#include "ipc.h"
#include "aca_filter.h"
#include "flight_parser.h"
#include "flight_process.h"
#include "safety_monitor.h"
#include "report.h"
#include "shared_memory.h"

#define FLIGHT_PLANS_FILE "flight_plans.json"
#define CONFIG_FILE       "simulation.conf"
#define N_FLIGHTS_COLLISION 2

/* ------------------------------------------------------------------ */
/* Contextos passados às threads do processo pai                        */
/* ------------------------------------------------------------------ */

typedef struct {
    sim_shm_t           *shm;
    sem_t               *pos_sems[MAX_FLIGHTS];
    sem_t               *ctrl_sems[MAX_FLIGHTS];
    flight_history_t    *histories;
    geo_boundary_t       ACA;
    simulation_params_t  params;
    int                  n_flights;
    pid_t               *pids;
    flight_plan_t       *plans;
    pthread_mutex_t     *g_mutex;
    pthread_cond_t      *g_done_cond;
    int                 *g_sim_done;
} coordinator_ctx_t;

typedef struct {
    sim_shm_t        *shm;
    flight_history_t *histories;
    int               n_flights;
    pthread_mutex_t  *g_mutex;
    pthread_cond_t   *g_done_cond;
    int              *g_sim_done;
} report_ctx_t;

/* ------------------------------------------------------------------ */
/* coordinator_thread: loop de sincronização passo-a-passo (US105/108)  */
/* ------------------------------------------------------------------ */
static void *coordinator_thread(void *arg) {
    coordinator_ctx_t *ctx = (coordinator_ctx_t *)arg;
    sim_shm_t *shm   = ctx->shm;
    int n_flights     = ctx->n_flights;

    aircraft_position_t prev_positions[MAX_FLIGHTS];
    aircraft_position_t current_positions[MAX_FLIGHTS];
    int has_position[MAX_FLIGHTS];
    memset(has_position, 0, sizeof(has_position));

    int local_active[MAX_FLIGHTS];
    int n_active = n_flights;
    for (int i = 0; i < n_flights; i++) local_active[i] = 1;

    int prediction_done[MAX_FLIGHTS][MAX_FLIGHTS];
    memset(prediction_done, 0, sizeof(prediction_done));

    int total_violations = 0;
    int sim_aborted      = 0;

    /* Padrão ex1-9.c: write() em vez de printf() em threads POSIX */
    char buf[512];
    int  n;

    write(STDOUT_FILENO, "\n=== Air Traffic Control Live Feed ===\n", 39);

    while (n_active > 0) {
        /* Fase 1: recolher posição de cada voo activo (bloqueia por semáforo) */
        for (int i = 0; i < n_flights; i++) {
            if (!local_active[i]) continue;

            sem_wait(ctx->pos_sems[i]);

            /* O voo sinalizou que terminou (active[i]=0)? */
            if (!shm->active[i]) {
                local_active[i] = 0;
                n_active--;
                n = snprintf(buf, sizeof(buf), "[%s] flight completed.\n",
                             ctx->plans[i].identifier);
                write(STDOUT_FILENO, buf, n);
                continue;
            }

            aircraft_position_t pos = shm->positions[i];

            /* US101: filtro ACA, detecção entrada/saída, histórico */
            int in_aca = is_in_aca(&pos, &ctx->ACA);
            int idx    = find_or_create_flight(ctx->histories, MAX_FLIGHTS,
                                               pos.flight_id);
            if (idx >= 0) {
                aca_state_t prev_state = ctx->histories[idx].aca_state;
                if (in_aca) {
                    if (prev_state == ACA_BEFORE) {
                        n = snprintf(buf, sizeof(buf), "[%s] >>> ENTERING ACA\n",
                                     pos.flight_id);
                        write(STDOUT_FILENO, buf, n);
                    }
                    ctx->histories[idx].aca_state = ACA_INSIDE;
                    if (ctx->histories[idx].count < MAX_POSITIONS)
                        ctx->histories[idx].positions[ctx->histories[idx].count++] = pos;
                    n = snprintf(buf, sizeof(buf),
                                 "[%s] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt"
                                 " hdg=%.1f vz=%.1fm/s\n",
                                 pos.flight_id, pos.latitude, pos.longitude,
                                 pos.altitude_meters, pos.speed_knots,
                                 pos.heading_deg, pos.vz_mps);
                    write(STDOUT_FILENO, buf, n);
                } else {
                    if (prev_state == ACA_INSIDE) {
                        n = snprintf(buf, sizeof(buf), "[%s] <<< EXITING ACA\n",
                                     pos.flight_id);
                        write(STDOUT_FILENO, buf, n);
                        ctx->histories[idx].aca_state = ACA_AFTER;
                    }
                }
            }

            /* US102: deslizar janela do vector de movimento */
            if (has_position[i] > 0)
                prev_positions[i] = current_positions[i];
            else
                prev_positions[i] = pos;
            current_positions[i] = pos;
            has_position[i]++;
        }

        if (n_active == 0) break;

        /* Previsão de colisões futuras (aviso único por par) */
        flight_plan_t *plan_ptrs[MAX_FLIGHTS];
        for (int k = 0; k < n_flights; k++) plan_ptrs[k] = &ctx->plans[k];

        for (int i = 0; i < n_flights; i++) {
            if (!local_active[i] || has_position[i] < 1) continue;
            for (int j = i + 1; j < n_flights; j++) {
                if (!local_active[j] || has_position[j] < 1) continue;
                if (!prediction_done[i][j]) {
                    prediction_done[i][j] = 1;
                    predict_future_collisions(
                        (flight_plan_t *const *)plan_ptrs, i, 0, j, 0,
                        ctx->params.safe_dist_horiz_m,
                        ctx->params.safe_dist_vert_m);
                }
            }
        }

        /* US102: verificação de segurança */
        int abort_sim = 0;
        for (int i = 0; i < n_flights; i++) {
            if (!local_active[i] || has_position[i] < 2) continue;
            if (monitor_safety_violations(
                    i, prev_positions, current_positions, has_position,
                    local_active, ctx->pids, n_flights, &total_violations,
                    ctx->params.safe_dist_horiz_m, ctx->params.safe_dist_vert_m,
                    ctx->params.max_violations)) {
                abort_sim = 1;
                break;
            }
        }

        if (abort_sim) {
            sim_aborted = 1;
            for (int i = 0; i < n_flights; i++) {
                if (local_active[i]) {
                    shm->ctrl[i] = 0;           /* STOP */
                    sem_post(ctx->ctrl_sems[i]);
                    local_active[i] = 0;
                }
            }
            n_active = 0;
            break;
        }

        /* Enviar GO a todos os voos activos */
        for (int i = 0; i < n_flights; i++) {
            if (local_active[i]) {
                shm->ctrl[i] = 1;               /* GO */
                sem_post(ctx->ctrl_sems[i]);
            }
        }
    }

    print_history(ctx->histories, MAX_FLIGHTS);

    /*
     * US105 — Condition-variable producer side (mutex + cond var pattern).
     *
     * The coordinator thread acts as *producer*: it writes the final
     * simulation results into shared memory and into the g_sim_done flag,
     * then wakes the report_thread via pthread_cond_signal().
     *
     * Protocol (textbook producer/consumer with a predicate):
     *   1. Acquire the mutex so the flag update and the signal are atomic
     *      from the consumer's point of view.
     *   2. Set the predicate (g_sim_done = 1) while holding the lock.
     *   3. Signal the condition variable.
     *   4. Release the mutex.
     *
     * Using a condition variable here (rather than a plain pthread_join) is
     * deliberate: report_thread must start setting up the report as soon as
     * the simulation logic is finished, not after the coordinator's thread
     * stack is cleaned up — the two phases overlap intentionally.
     */
    pthread_mutex_lock(ctx->g_mutex);
    shm->total_violations = total_violations;
    shm->sim_aborted      = sim_aborted;
    *ctx->g_sim_done      = 1;
    pthread_cond_signal(ctx->g_done_cond);   /* wake report_thread */
    pthread_mutex_unlock(ctx->g_mutex);

    pthread_exit(NULL);
}

/* ------------------------------------------------------------------ */
/* report_thread: aguarda fim da simulação e gera relatório (US109)     */
/* ------------------------------------------------------------------ */
static void *report_thread(void *arg) {
    report_ctx_t *ctx = (report_ctx_t *)arg;

    /*
     * US105 — Condition-variable consumer side (mutex + cond var pattern).
     *
     * The report_thread acts as *consumer*: it blocks here until the
     * coordinator_thread signals that the simulation has finished.
     *
     * Protocol (textbook producer/consumer with a predicate):
     *   1. Acquire the mutex before inspecting the predicate.
     *   2. Loop on pthread_cond_wait() — handles spurious wake-ups; the
     *      loop re-checks g_sim_done each time it returns.
     *   3. pthread_cond_wait() atomically releases the mutex and suspends
     *      the thread; on wake-up it reacquires the mutex before returning.
     *   4. Once g_sim_done == 1, read the results and release the mutex.
     *
     * This is the canonical POSIX condition-variable usage from the SCOMP
     * T7/T8 slides: always pair cond_wait with a mutex and a while-loop
     * predicate to avoid lost-wake and spurious-wake bugs.
     */
    pthread_mutex_lock(ctx->g_mutex);
    while (!*ctx->g_sim_done)
        pthread_cond_wait(ctx->g_done_cond, ctx->g_mutex);
    int total_violations = ctx->shm->total_violations;
    int sim_aborted      = ctx->shm->sim_aborted;
    pthread_mutex_unlock(ctx->g_mutex);

    /* Padrão ex1-9.c: write() em vez de printf() em threads POSIX */
    write(STDOUT_FILENO,
          "\n[SYSTEM] Simulation concluded. Spawning report generation process...\n",
          71);
    generate_final_report(ctx->histories, ctx->n_flights,
                          total_violations, sim_aborted);

    pthread_exit(NULL);
}

/* ------------------------------------------------------------------ */
/* main                                                                  */
/* ------------------------------------------------------------------ */
int main(int argc, char *argv[]) {
    simulation_params_t params;
    int cfg_result = load_config(CONFIG_FILE, &params);
    if (cfg_result == -1) {
        fprintf(stderr, "Error: cannot open config file '%s'\n", CONFIG_FILE);
        return 1;
    }
    if (cfg_result > 0) {
        fprintf(stderr, "Error: parse error in '%s' at line %d\n",
                CONFIG_FILE, cfg_result);
        return 1;
    }
    if (validate_config(&params) != 0)
        return 1;

    int n_flights_from_json = 0;
    flight_plan_t *plans = NULL;
    if (parse_flight_plans_from_json(FLIGHT_PLANS_FILE, &plans,
                                     &n_flights_from_json) != 0) {
        fprintf(stderr, "Error: could not load or parse flight plans from '%s'\n",
                FLIGHT_PLANS_FILE);
        return 1;
    }

    int n_flights = n_flights_from_json;
    for (int a = 1; a < argc; a++) {
        if (strcmp(argv[a], "--collision") == 0) {
            n_flights = N_FLIGHTS_COLLISION;
            printf("Collision test mode enabled, using first %d flight plans.\n",
                   n_flights);
        }
    }
    if (n_flights > n_flights_from_json) {
        fprintf(stderr,
                "Warning: requested %d flights, but only %d are available.\n",
                n_flights, n_flights_from_json);
        n_flights = n_flights_from_json;
    }
    if (n_flights > MAX_FLIGHTS) {
        fprintf(stderr, "Warning: capping simulation at %d flights.\n", MAX_FLIGHTS);
        n_flights = MAX_FLIGHTS;
    }

    signal(SIGPIPE, SIG_IGN);

    printf("=== AISafe Flight Simulation (US105: shared memory + threads) ===\n");
    printf("Mode: %s\n",
           (argc > 1 && strcmp(argv[1], "--collision") == 0)
               ? "COLLISION TEST" : "normal");
    print_config(&params);
    for (int i = 0; i < n_flights; i++)
        printf("Flight loaded: %s\n", plans[i].identifier);
    fflush(stdout);

    /* US105 — Criar segmento de memória partilhada */
    sim_shm_t *shm = shm_create(n_flights);

    /* US105 — Criar semáforos nomeados por voo: pos_sem + ctrl_sem */
    sem_t *pos_sems[MAX_FLIGHTS], *ctrl_sems[MAX_FLIGHTS];
    for (int i = 0; i < n_flights; i++) {
        pos_sems[i]  = open_pos_sem(i, 1);
        ctrl_sems[i] = open_ctrl_sem(i, 1);
    }

    pid_t            pids[MAX_FLIGHTS];
    flight_history_t histories[MAX_FLIGHTS];
    memset(histories, 0, sizeof(histories));
    for (int i = 0; i < MAX_FLIGHTS; i++)
        histories[i].aca_state = ACA_BEFORE;

    geo_boundary_t ACA = {
        .north_latitude = params.aca_north_lat,
        .south_latitude = params.aca_south_lat,
        .west_longitude = params.aca_west_lon,
        .east_longitude = params.aca_east_lon
    };

    /* US105 — Lançar processo filho por voo (sem pipes; IPC via shm + semáforos) */
    for (int i = 0; i < n_flights; i++) {
        pids[i] = fork();
        if (pids[i] == -1) { perror("fork"); exit(1); }
        if (pids[i] == 0) {
            /*
             * US105 GAP-1 FIX: Each child inherits all semaphore handles
             * that the parent opened before fork().  Close them all here so
             * the child holds no stale references.  The child then opens its
             * own fresh handles for exactly its own flight index.
             */
            for (int j = 0; j < n_flights; j++) {
                sem_close(pos_sems[j]);
                sem_close(ctrl_sems[j]);
            }

            /* Processo filho: ligar-se ao shm e abrir os seus dois semáforos */
            sim_shm_t *child_shm  = shm_attach();
            sem_t     *my_pos_sem  = open_pos_sem(i, 0);
            sem_t     *my_ctrl_sem = open_ctrl_sem(i, 0);
            flight_process_main(i, &plans[i], child_shm,
                                my_pos_sem, my_ctrl_sem, &params);
            /* nunca chega aqui */
        }
    }

    /* US105 — Inicializar mutex e variável de condição do processo pai */
    pthread_mutex_t g_mutex;
    pthread_cond_t  g_done_cond;
    int             g_sim_done  = 0;
    pthread_mutex_init(&g_mutex, NULL);
    pthread_cond_init(&g_done_cond, NULL);

    /* Contexto para coordinator_thread */
    coordinator_ctx_t coord_ctx;
    coord_ctx.shm        = shm;
    coord_ctx.histories  = histories;
    coord_ctx.ACA        = ACA;
    coord_ctx.params     = params;
    coord_ctx.n_flights  = n_flights;
    coord_ctx.pids       = pids;
    coord_ctx.plans      = plans;
    coord_ctx.g_mutex    = &g_mutex;
    coord_ctx.g_done_cond = &g_done_cond;
    coord_ctx.g_sim_done = &g_sim_done;
    for (int i = 0; i < n_flights; i++) {
        coord_ctx.pos_sems[i]  = pos_sems[i];
        coord_ctx.ctrl_sems[i] = ctrl_sems[i];
    }

    /* Contexto para report_thread */
    report_ctx_t rep_ctx;
    rep_ctx.shm        = shm;
    rep_ctx.histories  = histories;
    rep_ctx.n_flights  = n_flights;
    rep_ctx.g_mutex    = &g_mutex;
    rep_ctx.g_done_cond = &g_done_cond;
    rep_ctx.g_sim_done = &g_sim_done;

    /* US105 — Lançar threads dedicadas para as funcionalidades do processo pai */
    pthread_t coordinator_tid, report_tid;
    if (pthread_create(&coordinator_tid, NULL, coordinator_thread, &coord_ctx) != 0) {
        perror("pthread_create coordinator");
        exit(1);
    }
    if (pthread_create(&report_tid, NULL, report_thread, &rep_ctx) != 0) {
        perror("pthread_create report");
        exit(1);
    }

    /* Aguardar conclusão de ambas as threads */
    pthread_join(coordinator_tid, NULL);
    pthread_join(report_tid, NULL);

    /* Aguardar todos os processos filho */
    int status;
    for (int i = 0; i < n_flights; i++) {
        waitpid(pids[i], &status, 0);
        if (WIFEXITED(status))
            printf("[FLIGHT_%02d] ended with code %d\n", i + 1, WEXITSTATUS(status));
    }

    /* Limpeza de recursos IPC e sincronização */
    pthread_mutex_destroy(&g_mutex);
    pthread_cond_destroy(&g_done_cond);

    for (int i = 0; i < n_flights; i++) {
        sem_close(pos_sems[i]);
        sem_close(ctrl_sems[i]);
    }
    cleanup_sems(n_flights);
    shm_destroy(shm);
    free(plans);

    return 0;
}
