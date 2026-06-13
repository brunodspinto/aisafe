/*
 * main.c - Flight Simulation entry point (US105 + US106)
 *
 * US105: pipes substituídos por shared memory + named semaphores; processo pai
 *        multi-threaded (coordinator_thread + report_thread).
 *
 * US106 - Separação de funcionalidades por threads. A verificação de segurança
 * (US102) foi extraída para a sua própria thread função-específica, que vive no
 * módulo separado safety_thread.{c,h}. A coordinator_thread e a report_thread
 * permanecem aqui (US105/US101/US109). coordinator_thread e safety_thread
 * cooperam, em cada passo, por um handoff "ping-pong" (mutex + variável de
 * condição) através do safety_channel_t (definido em safety_thread.h).
 *
 * Padrões SCOMP usados:
 *   shm_open + ftruncate + mmap   →  ex1-7.c, ex2-6.c
 *   sem_open / sem_post / sem_wait →  ex1-7.c, ex2-5.c
 *   pthread_create / pthread_join  →  ex1-9.c
 *   mutex + cond var (while-pred)  →  T7/T8 slides
 *   SIGUSR1                        →  ex1-3.c, ex1-4.c (em flight_process.c)
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
#include <errno.h>
#include "types.h"
#include "config.h"
#include "ipc.h"
#include "aca_filter.h"
#include "flight_parser.h"
#include "flight_process.h"
#include "report.h"
#include "shared_memory.h"
#include "safety_thread.h"   /* US106 — safety_thread + safety_channel_t */
#include "weather_service.h" /* US110 — environment (wind) data source */

#define FLIGHT_PLANS_FILE "flight_plans.json"
#define CONFIG_FILE       "simulation.conf"
#define N_FLIGHTS_COLLISION 2

/* Best-effort full write to a file descriptor, used for thread/log output.
 * Loops over partial writes and retries on EINTR; errno is preserved so it is
 * safe to call without clobbering the caller's errno. The return value of
 * write() is always consumed, avoiding -Wunused-result on the fortified write. */
static void safe_write(int fd, const char *buf, size_t len) {
    const int saved_errno = errno;
    size_t off = 0;
    while (off < len) {
        const ssize_t w = write(fd, buf + off, len - off);
        if (w < 0) {
            if (errno == EINTR) continue;
            break;  /* logging is best-effort; give up on real errors */
        }
        off += (size_t) w;
    }
    errno = saved_errno;
}

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
    safety_channel_t    *chan;        /* US106 — handoff p/ safety_thread */
    pthread_mutex_t     *g_notification_mutex;
    pthread_cond_t      *g_report_cond;
    int                 *g_sim_done;
    /* US110 — per-step tick to the environment thread */
    pthread_mutex_t     *env_mutex;
    pthread_cond_t      *env_cond;
    int                 *env_tick;
    int                 *env_done;
} coordinator_ctx_t;

/* US110 — contexto da environment thread (escreve o vento em shm a cada passo) */
typedef struct {
    sim_shm_t       *shm;
    sem_t           *env_sem;     /* mutex nomeado do bloco de ambiente (ex2-6.c) */
    pthread_mutex_t *env_mutex;   /* sinalização coordinator↔env (intra-processo) */
    pthread_cond_t  *env_cond;
    int             *env_tick;
    int             *env_done;
} environment_ctx_t;

typedef struct {
    sim_shm_t        *shm;
    flight_history_t *histories;
    int               n_flights;
    pthread_mutex_t  *g_notification_mutex;
    pthread_cond_t   *g_report_cond;
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

    int total_violations = 0;
    int sim_aborted      = 0;

    /* Padrão ex1-9.c: write() em vez de printf() em threads POSIX */
    char buf[512];
    int  n;

    safe_write(STDOUT_FILENO, "\n=== Air Traffic Control Live Feed ===\n", 39);

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
                safe_write(STDOUT_FILENO, buf, n);
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
                        safe_write(STDOUT_FILENO, buf, n);
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
                    safe_write(STDOUT_FILENO, buf, n);
                } else {
                    if (prev_state == ACA_INSIDE) {
                        n = snprintf(buf, sizeof(buf), "[%s] <<< EXITING ACA\n",
                                     pos.flight_id);
                        safe_write(STDOUT_FILENO, buf, n);
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

        /*
         * US106 — Handoff para a safety_thread. O coordenador entrega o snapshot
         * deste passo e bloqueia até o veredicto de segurança estar pronto
         * (ping-pong com mutex + variável de condição, padrão T7/T8). A previsão
         * de colisões e a verificação do cilindro de segurança (US102) correm
         * agora na sua própria thread (safety_thread.c).
         */
        safety_channel_t *chan = ctx->chan;
        pthread_mutex_lock(&chan->mutex);
        memcpy(chan->prev_positions,    prev_positions,    sizeof(prev_positions));
        memcpy(chan->current_positions, current_positions, sizeof(current_positions));
        memcpy(chan->has_position,      has_position,      sizeof(has_position));
        memcpy(chan->local_active,      local_active,      sizeof(local_active));
        chan->verdict_ready = 0;
        chan->step_ready    = 1;
        pthread_cond_broadcast(&chan->cond);          /* acorda safety_thread */
        while (!chan->verdict_ready)                  /* while-pred: spurious/lost-wakeup */
            pthread_cond_wait(&chan->cond, &chan->mutex);
        int abort_sim    = chan->abort_sim;
        total_violations = chan->total_violations;
        pthread_mutex_unlock(&chan->mutex);
        /* Previsão de colisões futuras (aviso único por par) */
        /* US102: verificação de segurança */

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

        /* US110 — sinalizar a environment thread para actualizar o vento em shm
         * antes de libertar os voos para o próximo passo. */
        pthread_mutex_lock(ctx->env_mutex);
        (*ctx->env_tick)++;
        pthread_cond_signal(ctx->env_cond);
        pthread_mutex_unlock(ctx->env_mutex);

        /* Enviar GO a todos os voos activos */
        for (int i = 0; i < n_flights; i++) {
            if (local_active[i]) {
                shm->ctrl[i] = 1;               /* GO */
                sem_post(ctx->ctrl_sems[i]);
            }
        }
    }

    /*
     * US106 — A simulação terminou: desbloquear a safety_thread para que saia
     * do seu cond_wait e possa ser juntada (pthread_join) sem ficar pendente.
     */
    pthread_mutex_lock(&ctx->chan->mutex);
    ctx->chan->sim_finished = 1;
    pthread_cond_broadcast(&ctx->chan->cond);
    pthread_mutex_unlock(&ctx->chan->mutex);

    print_history(ctx->histories, MAX_FLIGHTS);

    /*
     * US105 — Lado produtor da variável de condição (mutex + cond var).
     * O coordenador escreve os resultados finais e acorda a report_thread.
     */
    /* Guardar contadores finais em shm e notificar report_thread */
    pthread_mutex_lock(ctx->g_notification_mutex);
    shm->total_violations = total_violations;
    shm->sim_aborted      = sim_aborted;
    *ctx->g_sim_done      = 1;
    pthread_cond_signal(ctx->g_report_cond);
    pthread_mutex_unlock(ctx->g_notification_mutex);

    /* US110 — acordar a environment thread para que saia do cond_wait e termine. */
    pthread_mutex_lock(ctx->env_mutex);
    *ctx->env_done = 1;
    pthread_cond_broadcast(ctx->env_cond);
    pthread_mutex_unlock(ctx->env_mutex);

    pthread_exit(NULL);
}

/* ------------------------------------------------------------------ */
/* environment_thread: carrega o vento do "weather service" e escreve-o */
/* em shared memory a cada passo da simulação (US110)                    */
/* ------------------------------------------------------------------ */
static void *environment_thread(void *arg) {
    environment_ctx_t *ctx = (environment_ctx_t *)arg;
    char buf[160];
    int  n;

    /* Escrita inicial (passo 0) para que os voos já tenham vento no 1º passo. */
    environment_t env;
    weather_service_fetch(&env, 0);
    sem_wait(ctx->env_sem);
    ctx->shm->environment = env;
    ctx->shm->env_step    = 0;
    sem_post(ctx->env_sem);
    n = snprintf(buf, sizeof(buf),
                 "[ENV US110] weather service: wind %.1f m/s from %.0f deg\n",
                 env.wind_speed, env.wind_direction);
    safe_write(STDOUT_FILENO, buf, n);

    int last_tick = 0;
    for (;;) {
        pthread_mutex_lock(ctx->env_mutex);
        while (*ctx->env_tick == last_tick && !*ctx->env_done)
            pthread_cond_wait(ctx->env_cond, ctx->env_mutex);   /* while-pred */
        if (*ctx->env_done && *ctx->env_tick == last_tick) {
            pthread_mutex_unlock(ctx->env_mutex);
            break;
        }
        int tick = *ctx->env_tick;
        pthread_mutex_unlock(ctx->env_mutex);

        weather_service_fetch(&env, tick);
        sem_wait(ctx->env_sem);
        ctx->shm->environment = env;
        ctx->shm->env_step    = tick;
        sem_post(ctx->env_sem);
        last_tick = tick;
    }

    pthread_exit(NULL);
}

/* ------------------------------------------------------------------ */
/* report_thread: aguarda fim da simulação e gera relatório (US109)     */
/* ------------------------------------------------------------------ */
static void *report_thread(void *arg) {
    report_ctx_t *ctx = (report_ctx_t *)arg;
    int processed_events = 0;
    int last_reported_drop_count = 0;

    /*
     * US105 — Lado consumidor da variável de condição. Bloqueia até g_sim_done
     * == 1; o while-loop re-verifica o predicado (spurious/lost-wakeup).
     */
    reset_live_violation_log();

    pthread_mutex_lock(ctx->g_notification_mutex);
    while (processed_events < ctx->shm->violation_event_count || !*ctx->g_sim_done) {
        while (processed_events >= ctx->shm->violation_event_count && !*ctx->g_sim_done)
            pthread_cond_wait(ctx->g_report_cond, ctx->g_notification_mutex);

        while (processed_events < ctx->shm->violation_event_count) {
            violation_event_t event = ctx->shm->violation_events[processed_events++];
            char log_buf[256];
            int log_len;
            pthread_mutex_unlock(ctx->g_notification_mutex);

            append_violation_event_to_log(&event);
            log_len = snprintf(log_buf, sizeof(log_buf),
                               "[REPORT US107] Logged violation between %s and %s at %ld\n",
                               event.flight_a, event.flight_b, (long)event.timestamp);
            safe_write(STDOUT_FILENO, log_buf, log_len);

            pthread_mutex_lock(ctx->g_notification_mutex);
        }

        if (ctx->shm->dropped_violation_events > last_reported_drop_count) {
            int new_drop_count = ctx->shm->dropped_violation_events;
            char warn_buf[160];
            int warn_len;
            pthread_mutex_unlock(ctx->g_notification_mutex);

            append_violation_drop_notice(new_drop_count - last_reported_drop_count);
            warn_len = snprintf(
                warn_buf, sizeof(warn_buf),
                "[REPORT US107] Live violation queue overflowed; %d events were dropped\n",
                new_drop_count);
            safe_write(STDOUT_FILENO, warn_buf, warn_len);

            pthread_mutex_lock(ctx->g_notification_mutex);
            last_reported_drop_count = new_drop_count;
        }

        if (*ctx->g_sim_done && processed_events >= ctx->shm->violation_event_count)
            break;
    }
    int total_violations = ctx->shm->total_violations;
    int dropped_events   = ctx->shm->dropped_violation_events;
    int violation_event_count = ctx->shm->violation_event_count;
    const violation_event_t *violation_events = ctx->shm->violation_events;
    int sim_aborted      = ctx->shm->sim_aborted;
    pthread_mutex_unlock(ctx->g_notification_mutex);

    /* Padrão ex1-9.c: write() em vez de printf() em threads POSIX */
    const char *report_msg =
        "\n[SYSTEM] Simulation concluded. Report thread generating final report...\n";
    safe_write(STDOUT_FILENO, report_msg, strlen(report_msg));
    generate_final_report(ctx->histories, ctx->n_flights,
                          violation_events, violation_event_count,
                          total_violations, dropped_events, sim_aborted);

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
    /* US110 — mutex nomeado (valor 1) que protege o bloco de ambiente em shm. */
    sem_t *env_sem = open_env_sem(1);

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
            sem_close(env_sem);   /* US110 — fechar handle herdado; o filho abre o seu */

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
    pthread_mutex_t g_notification_mutex;
    pthread_cond_t  g_report_cond;
    int             g_sim_done  = 0;
    pthread_mutex_init(&g_notification_mutex, NULL);
    pthread_cond_init(&g_report_cond, NULL);

    /* US110 — sincronização coordinator ↔ environment thread (stack/automático,
     * por isso pthread_*_init e não os _INITIALIZER). */
    pthread_mutex_t env_mutex;
    pthread_cond_t  env_cond;
    int             env_tick = 0;
    int             env_done = 0;
    pthread_mutex_init(&env_mutex, NULL);
    pthread_cond_init(&env_cond, NULL);

    /* US106 — Inicializar o canal de handoff coordinator ↔ safety.
     * pthread_mutex_init/pthread_cond_init (não os _INITIALIZER) porque a
     * estrutura é de armazenamento automático (stack), não estática. */
    safety_channel_t chan;
    memset(&chan, 0, sizeof(chan));
    pthread_mutex_init(&chan.mutex, NULL);
    pthread_cond_init(&chan.cond, NULL);

    /* Contexto para coordinator_thread */
    coordinator_ctx_t coord_ctx;
    coord_ctx.shm        = shm;
    coord_ctx.histories  = histories;
    coord_ctx.ACA        = ACA;
    coord_ctx.params     = params;
    coord_ctx.n_flights  = n_flights;
    coord_ctx.pids       = pids;
    coord_ctx.plans      = plans;
    coord_ctx.chan       = &chan;
    coord_ctx.g_notification_mutex = &g_notification_mutex;
    coord_ctx.g_report_cond = &g_report_cond;
    coord_ctx.g_sim_done = &g_sim_done;
    coord_ctx.env_mutex = &env_mutex;       /* US110 */
    coord_ctx.env_cond  = &env_cond;
    coord_ctx.env_tick  = &env_tick;
    coord_ctx.env_done  = &env_done;
    for (int i = 0; i < n_flights; i++) {
        coord_ctx.pos_sems[i]  = pos_sems[i];
        coord_ctx.ctrl_sems[i] = ctrl_sems[i];
    }

    /* US110 — Contexto para environment_thread */
    environment_ctx_t env_ctx;
    env_ctx.shm       = shm;
    env_ctx.env_sem   = env_sem;
    env_ctx.env_mutex = &env_mutex;
    env_ctx.env_cond  = &env_cond;
    env_ctx.env_tick  = &env_tick;
    env_ctx.env_done  = &env_done;

    /* US106 — Contexto para safety_thread */
    safety_ctx_t safety_ctx;
    safety_ctx.chan      = &chan;
    safety_ctx.plans     = plans;
    safety_ctx.params    = params;
    safety_ctx.pids      = pids;
    safety_ctx.n_flights = n_flights;
    safety_ctx.shm       = shm;
    safety_ctx.g_notification_mutex = &g_notification_mutex;
    safety_ctx.g_report_cond = &g_report_cond;

    /* Contexto para report_thread */
    report_ctx_t rep_ctx;
    rep_ctx.shm        = shm;
    rep_ctx.histories  = histories;
    rep_ctx.n_flights  = n_flights;
    rep_ctx.g_notification_mutex = &g_notification_mutex;
    rep_ctx.g_report_cond = &g_report_cond;
    rep_ctx.g_sim_done = &g_sim_done;

    /* US106 — Lançar as TRÊS threads função-específicas do processo pai.
     * A safety_thread é criada primeiro para já estar à espera do primeiro
     * snapshot quando o coordinator começar o loop. */
    pthread_t coordinator_tid, safety_tid, report_tid, environment_tid;
    /* US110 — environment thread criada primeiro para escrever o vento inicial
     * antes do coordinator libertar o primeiro passo. */
    if (pthread_create(&environment_tid, NULL, environment_thread, &env_ctx) != 0) {
        perror("pthread_create environment");
        exit(1);
    }
    if (pthread_create(&safety_tid, NULL, safety_thread, &safety_ctx) != 0) {
        perror("pthread_create safety");
        exit(1);
    }
    if (pthread_create(&coordinator_tid, NULL, coordinator_thread, &coord_ctx) != 0) {
        perror("pthread_create coordinator");
        exit(1);
    }
    if (pthread_create(&report_tid, NULL, report_thread, &rep_ctx) != 0) {
        perror("pthread_create report");
        exit(1);
    }

    /* Aguardar conclusão das quatro threads. O coordinator sinaliza env_done ao
     * terminar, pelo que a environment thread é juntada logo a seguir. */
    pthread_join(coordinator_tid, NULL);
    pthread_join(environment_tid, NULL);
    pthread_join(safety_tid, NULL);
    pthread_join(report_tid, NULL);

    /* Aguardar todos os processos filho */
    int status;
    for (int i = 0; i < n_flights; i++) {
        waitpid(pids[i], &status, 0);
        if (WIFEXITED(status))
            printf("[FLIGHT_%02d] ended with code %d\n", i + 1, WEXITSTATUS(status));
    }

    /* Limpeza de recursos IPC e sincronização */
    pthread_mutex_destroy(&chan.mutex);   /* US106 — canal coordinator ↔ safety */
    pthread_cond_destroy(&chan.cond);
    pthread_mutex_destroy(&g_notification_mutex);
    pthread_cond_destroy(&g_report_cond);
    pthread_mutex_destroy(&env_mutex);    /* US110 */
    pthread_cond_destroy(&env_cond);

    for (int i = 0; i < n_flights; i++) {
        sem_close(pos_sems[i]);
        sem_close(ctrl_sems[i]);
    }
    sem_close(env_sem);          /* US110 */
    cleanup_sems(n_flights);
    shm_destroy(shm);
    free(plans);

    return 0;
}
