/*
 * safety_thread.c - Thread de segurança do processo pai (US106).
 *
 * Espera o snapshot de cada passo (via safety_channel_t), corre a previsão de
 * colisões + a verificação do cilindro de segurança (US102, em safety_monitor.c),
 * conta violações e devolve o veredicto (abortar ou continuar) ao coordenador.
 *
 * Trabalha sobre uma CÓPIA do snapshot: monitor_safety_violations() só lê
 * local_active/pids, por isso é seguro. O mutex é libertado durante o cálculo
 * (não segurar o lock em trabalho longo — lição de increment_safe).
 */
#define _POSIX_C_SOURCE 200809L
#include <string.h>
#include <pthread.h>
#include "safety_thread.h"
#include "safety_monitor.h"

void *safety_thread(void *arg) {
    safety_ctx_t     *ctx  = (safety_ctx_t *)arg;
    safety_channel_t *chan = ctx->chan;
    int n_flights          = ctx->n_flights;

    /* Aviso único de colisão futura por par de voos (estado próprio da thread) */
    int prediction_done[MAX_FLIGHTS][MAX_FLIGHTS];
    memset(prediction_done, 0, sizeof(prediction_done));

    int total_violations = 0;

    while (1) {
        /* Esperar pelo snapshot do passo, ou pelo fim da simulação.
         * while-pred: protege contra spurious/lost-wakeup (T7/T8). */
        pthread_mutex_lock(&chan->mutex);
        while (!chan->step_ready && !chan->sim_finished)
            pthread_cond_wait(&chan->cond, &chan->mutex);
        if (chan->sim_finished && !chan->step_ready) {
            pthread_mutex_unlock(&chan->mutex);
            break;
        }
        chan->step_ready = 0;

        /* Copiar o snapshot e libertar o mutex durante o cálculo */
        aircraft_position_t prev[MAX_FLIGHTS], curr[MAX_FLIGHTS];
        int has_position[MAX_FLIGHTS], local_active[MAX_FLIGHTS];
        memcpy(prev,         chan->prev_positions,    sizeof(prev));
        memcpy(curr,         chan->current_positions, sizeof(curr));
        memcpy(has_position, chan->has_position,      sizeof(has_position));
        memcpy(local_active, chan->local_active,      sizeof(local_active));
        pthread_mutex_unlock(&chan->mutex);

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

        /* US102: verificação do cilindro de segurança */
        int abort_sim = 0;
        for (int i = 0; i < n_flights; i++) {
            if (!local_active[i] || has_position[i] < 2) continue;
            if (monitor_safety_violations(
                    i, prev, curr, has_position,
                    local_active, ctx->pids, n_flights, &total_violations,
                    ctx->shm,
                    ctx->g_notification_mutex, ctx->g_report_cond,
                    ctx->params.safe_dist_horiz_m, ctx->params.safe_dist_vert_m,
                    ctx->params.max_violations)) {
                abort_sim = 1;
                break;
            }
        }

        /* Devolver o veredicto ao coordenador (acorda-o do verdict_ready) */
        pthread_mutex_lock(&chan->mutex);
        chan->abort_sim        = abort_sim;
        chan->total_violations = total_violations;
        chan->verdict_ready    = 1;
        pthread_cond_broadcast(&chan->cond);
        pthread_mutex_unlock(&chan->mutex);
    }

    pthread_exit(NULL);
}
