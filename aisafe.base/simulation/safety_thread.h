/*
 * safety_thread.h - Parent-process safety thread (US106).
 *
 * This is the US106 contribution: the function-specific thread that separates
 * the safety verification (US102) into its own thread in the parent process. It
 * also defines safety_channel_t, the "ping-pong" channel (mutex + condition
 * variable) used to exchange, on each step, the position snapshot and the safety
 * verdict with the coordinator_thread (which stays in main.c).
 */
#ifndef SAFETY_THREAD_H
#define SAFETY_THREAD_H

#include <sys/types.h>
#include <pthread.h>
#include "types.h"
#include "shared_memory.h"

/*
 * US106 - "ping-pong" handoff channel between the coordinator_thread (producer of
 * the step snapshot) and the safety_thread (producer of the verdict). Protected by
 * a single mutex + condition variable. On each step:
 *   coordinator writes the snapshot, step_ready=1, and blocks on verdict_ready;
 *   safety reads the snapshot, computes, writes the verdict and verdict_ready=1.
 * sim_finished unblocks the safety_thread when the simulation ends.
 */
typedef struct {
    /* step snapshot: written by the coordinator, read by safety */
    aircraft_position_t prev_positions[MAX_FLIGHTS];
    aircraft_position_t current_positions[MAX_FLIGHTS];
    int                 has_position[MAX_FLIGHTS];
    int                 local_active[MAX_FLIGHTS];
    /* verdict: written by safety, read by the coordinator */
    int                 abort_sim;
    int                 total_violations;
    /* ping-pong state machine */
    int                 step_ready;     /* coordinator -> safety */
    int                 verdict_ready;  /* safety -> coordinator */
    int                 sim_finished;   /* coordinator -> safety: terminate */
    pthread_mutex_t     mutex;
    pthread_cond_t      cond;
} safety_channel_t;

/* Context passed to the safety_thread */
typedef struct {
    safety_channel_t    *chan;
    flight_plan_t       *plans;
    simulation_params_t  params;
    pid_t               *pids;
    int                  n_flights;
    sim_shm_t           *shm;
    pthread_mutex_t     *g_notification_mutex;
    pthread_cond_t      *g_report_cond;
} safety_ctx_t;

void *safety_thread(void *arg);

#endif /* SAFETY_THREAD_H */
