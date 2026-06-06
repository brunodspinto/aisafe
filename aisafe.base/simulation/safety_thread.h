/*
 * safety_thread.h - Thread de segurança do processo pai (US106).
 *
 * Esta é a contribuição da US106: a função-específica que separa a verificação
 * de segurança (US102) para a sua própria thread no processo pai. Define também
 * o safety_channel_t, o canal "ping-pong" (mutex + variável de condição) usado
 * para trocar, em cada passo, o snapshot das posições e o veredicto de segurança
 * com a coordinator_thread (que permanece em main.c).
 */
#ifndef SAFETY_THREAD_H
#define SAFETY_THREAD_H

#include <sys/types.h>
#include <pthread.h>
#include "types.h"

/*
 * US106 — Canal de handoff "ping-pong" entre a coordinator_thread (produtor do
 * snapshot do passo) e a safety_thread (produtor do veredicto). Protegido por um
 * único mutex + variável de condição (padrão T7/T8). Em cada passo:
 *   coordinator escreve o snapshot, step_ready=1, e bloqueia em verdict_ready;
 *   safety lê o snapshot, calcula, escreve o veredicto e verdict_ready=1.
 * sim_finished desbloqueia a safety_thread quando a simulação termina.
 */
typedef struct {
    /* snapshot do passo: escrito pelo coordinator, lido pela safety */
    aircraft_position_t prev_positions[MAX_FLIGHTS];
    aircraft_position_t current_positions[MAX_FLIGHTS];
    int                 has_position[MAX_FLIGHTS];
    int                 local_active[MAX_FLIGHTS];
    /* veredicto: escrito pela safety, lido pelo coordinator */
    int                 abort_sim;
    int                 total_violations;
    /* máquina de estados do ping-pong */
    int                 step_ready;     /* coordinator → safety */
    int                 verdict_ready;  /* safety → coordinator */
    int                 sim_finished;   /* coordinator → safety: termina */
    pthread_mutex_t     mutex;
    pthread_cond_t      cond;
} safety_channel_t;

/* Contexto passado à safety_thread */
typedef struct {
    safety_channel_t    *chan;
    flight_plan_t       *plans;
    simulation_params_t  params;
    pid_t               *pids;
    int                  n_flights;
} safety_ctx_t;

void *safety_thread(void *arg);

#endif /* SAFETY_THREAD_H */
