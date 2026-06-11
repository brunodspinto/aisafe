/*
 * flight_process.c - Flight child process execution (US105: shared memory IPC)
 *
 * Physics:  altitude-dependent speed/vz from Flight Profile tables (inalterado).
 * IPC:      posição escrita em shared memory → sem_post(pos_sem) sinaliza coordinator.
 *           sem_wait(ctrl_sem) bloqueia até coordinator postar GO(ctrl=1)/STOP(ctrl=0).
 * Sinais:   SIGUSR1 define collision_alert; voo termina na próxima verificação ctrl.
 *
 * Padrão SCOMP: ex1-3.c / ex1-4.c (SIGUSR1 + SA_RESTART), ex1-7.c (sem_open/post/wait).
 */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <time.h>
#include <math.h>
#include <semaphore.h>
#include <sys/mman.h>
#include "types.h"
#include "shared_memory.h"
#include "flight_process.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

/* Definida como volatile sig_atomic_t — requisito POSIX para variáveis modificadas
 * em signal handlers (ex1-3.c / ex1-4.c do professor). */
static volatile sig_atomic_t collision_alert = 0;

/* Async-signal-safe: apenas write() é permitido dentro do handler. */
static void handle_sigusr1(int sig) {
    (void)sig;
    const char msg[] = "[FLIGHT] SIGUSR1 received: collision alert, stopping.\n";
    /* write() is async-signal-safe; consume its result to satisfy warn_unused_result */
    const ssize_t w = write(STDOUT_FILENO, msg, sizeof(msg) - 1);
    (void) w;
    collision_alert = 1;
}

#define STEP_SECONDS  1
#define DEG_TO_RAD    (M_PI / 180.0)
#define KNOTS_TO_MPS  0.51444

static double horiz_distance_m(double lat1, double lon1, double lat2, double lon2) {
    double lat_mid = (lat1 + lat2) / 2.0 * DEG_TO_RAD;
    double dlat    = (lat2 - lat1) * 110574.0;
    double dlon    = (lon2 - lon1) * 111320.0 * cos(lat_mid);
    return sqrt(dlat * dlat + dlon * dlon);
}

static void lookup_perf(const perf_point_t *table, int count, double alt_m,
                        double *speed_kt_out, double *vz_out) {
    if (count <= 0) { *speed_kt_out = 250.0; *vz_out = 0.0; return; }
    if (alt_m <= table[0].altitude_m || count == 1) {
        *speed_kt_out = table[0].speed_knots;
        *vz_out       = table[0].vertical_rate_mps;
        return;
    }
    if (alt_m >= table[count - 1].altitude_m) {
        *speed_kt_out = table[count - 1].speed_knots;
        *vz_out       = table[count - 1].vertical_rate_mps;
        return;
    }
    for (int i = 0; i < count - 1; i++) {
        if (alt_m >= table[i].altitude_m && alt_m < table[i + 1].altitude_m) {
            double t = (alt_m - table[i].altitude_m) /
                       (table[i + 1].altitude_m - table[i].altitude_m);
            *speed_kt_out = table[i].speed_knots +
                            t * (table[i + 1].speed_knots - table[i].speed_knots);
            *vz_out = table[i].vertical_rate_mps +
                      t * (table[i + 1].vertical_rate_mps - table[i].vertical_rate_mps);
            return;
        }
    }
    *speed_kt_out = table[count - 1].speed_knots;
    *vz_out       = table[count - 1].vertical_rate_mps;
}

/* Sinaliza ao coordinator que este voo terminou e liberta os recursos de IPC.
 * Padrão ex1-7.c: filho chama munmap antes de sair. */
static void flight_done(int idx, sim_shm_t *shm, sem_t *pos_sem, sem_t *ctrl_sem) {
    shm->active[idx] = 0;
    sem_post(pos_sem);   /* acorda coordinator para que veja active[idx]=0 */
    sem_close(pos_sem);
    sem_close(ctrl_sem);
    munmap(shm, sizeof(sim_shm_t));  /* padrão ex1-7.c: filho desliga a memória partilhada */
}

void flight_process_main(int flight_idx, const flight_plan_t *plan,
                         sim_shm_t *shm,
                         sem_t *pos_sem, sem_t *ctrl_sem,
                         const simulation_params_t *params) {
    (void)params;

    if (!plan) {
        fprintf(stderr, "[Flight] Error: flight plan is NULL\n");
        exit(1);
    }

    /* Instalar handler SIGUSR1 (padrão ex1-3.c / ex1-4.c).
     * SA_RESTART: sem_wait() é reiniciado após o sinal, evitando EINTR. */
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_handler = handle_sigusr1;
    act.sa_flags   = SA_RESTART;
    sigfillset(&act.sa_mask);
    sigaction(SIGUSR1, &act, NULL);

    printf("[Flight %s] Starting simulation\n", plan->identifier);
    fflush(stdout);

    time_t sim_time = time(NULL);

    for (int leg = 0; leg < plan->leg_count; leg++) {
        const leg_t          *leg_data = &plan->legs[leg];
        const flight_profile_t *prof   = &leg_data->profile;

        for (int seg = 0; seg < leg_data->segment_count; seg++) {
            const segment_t *segment = &leg_data->segments[seg];

            double lat        = segment->from.latitude;
            double lon        = segment->from.longitude;
            double alt        = segment->alt_from_meters;
            double alt_target = segment->alt_to_meters;

            double dlat = segment->to.latitude  - segment->from.latitude;
            double dlon = segment->to.longitude - segment->from.longitude;
            double dist_total_m = horiz_distance_m(segment->from.latitude,
                                                   segment->from.longitude,
                                                   segment->to.latitude,
                                                   segment->to.longitude);
            if (dist_total_m < 1.0) dist_total_m = 1.0;

            double heading = atan2(dlon, dlat) * 180.0 / M_PI;
            if (heading < 0.0) heading += 360.0;

            int is_climb   = (strcmp(segment->mode, "climb")   == 0);
            int is_cruise  = (strcmp(segment->mode, "cruise")  == 0);
            int is_descend = (strcmp(segment->mode, "descend") == 0);

            double dist_covered_m = 0.0;

            while (1) {
                /* Verificar fim do segmento */
                if (is_climb   && alt >= alt_target) break;
                if (is_descend && alt <= alt_target) break;
                if (is_cruise  && dist_covered_m >= dist_total_m) break;

                double speed_kt, vz;
                if (is_cruise) {
                    /* Default a 250kt se o plano de voo não tiver perfil de performance */
                    speed_kt = prof->cruise_speed_knots > 0.0
                               ? prof->cruise_speed_knots : 250.0;
                    vz       = 0.0;
                } else if (is_climb) {
                    lookup_perf(prof->climb, prof->climb_count, alt, &speed_kt, &vz);
                } else {
                    lookup_perf(prof->descend, prof->descend_count, alt, &speed_kt, &vz);
                }

                double speed_mps    = speed_kt * KNOTS_TO_MPS;
                double horiz_step_m = speed_mps * STEP_SECONDS;

                lat += (dlat / dist_total_m) * horiz_step_m;
                lon += (dlon / dist_total_m) * horiz_step_m;
                dist_covered_m += horiz_step_m;

                alt += vz * STEP_SECONDS;
                if (is_climb   && alt > alt_target) alt = alt_target;
                if (is_descend && alt < alt_target) alt = alt_target;

                /* US105: escrever posição em shared memory e sinalizar coordinator */
                aircraft_position_t pos;
                memset(&pos, 0, sizeof(pos));
                pos.latitude        = lat;
                pos.longitude       = lon;
                pos.altitude_meters = alt;
                pos.speed_knots     = speed_kt;
                pos.heading_deg     = heading;
                pos.vz_mps          = vz;
                pos.timestamp       = sim_time;
                snprintf(pos.flight_id, sizeof(pos.flight_id), "%s", plan->identifier);

                shm->positions[flight_idx] = pos;
                sem_post(pos_sem);
                sim_time += STEP_SECONDS;

                /* US105: aguardar GO(ctrl=1) ou STOP(ctrl=0) do coordinator.
                 * SA_RESTART garante que sem_wait() é retomado após SIGUSR1. */
                sem_wait(ctrl_sem);
                if (shm->ctrl[flight_idx] == 0 || collision_alert) {
                    flight_done(flight_idx, shm, pos_sem, ctrl_sem);
                    exit(1);
                }
            }
        }
    }

    /* Todos os segmentos e legs concluídos — voo terminou normalmente. */
    flight_done(flight_idx, shm, pos_sem, ctrl_sem);
    exit(0);
}
