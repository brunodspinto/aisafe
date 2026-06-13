/*
 * shared_memory.h - Shared memory and named semaphore helpers (US105)
 *
 * Defines sim_shm_t, the single POSIX shared memory object used as IPC between
 * the parent process (coordinator/report threads) and each child flight process.
 * Named semaphores replace the control/position pipes from US100.
 */
#ifndef SHARED_MEMORY_H
#define SHARED_MEMORY_H

#define _POSIX_C_SOURCE 200809L
#include <semaphore.h>
#include "types.h"

#define SHM_NAME      "/aisafe_sim"
#define SEM_POS_FMT   "/aisafe_pos_%d"   /* child posts → coordinator waits */
#define SEM_CTRL_FMT  "/aisafe_ctrl_%d"  /* coordinator posts → child waits */
#define SEM_ENV_NAME  "/aisafe_env"      /* US110 — mutex (init 1) for env block (ex2-6.c) */
#define SEM_NAME_MAX  32

/* Shared memory layout: one slot per flight. */
typedef struct {
    aircraft_position_t positions[MAX_FLIGHTS]; /* written by each child            */
    int ctrl[MAX_FLIGHTS];                      /* 1=GO, 0=STOP set by coordinator  */
    int active[MAX_FLIGHTS];                    /* 1 while flight is still running  */
    int n_flights;
    int total_violations;
    int sim_aborted;
    violation_event_t violation_events[MAX_VIOLATION_EVENTS];
    int violation_event_count;
    int dropped_violation_events;
    /* US110 — environment block written by the environment thread each step,
     * read by every flight child. Mutual exclusion is provided by the named
     * semaphore SEM_ENV_NAME (value 1), the cross-process mutex pattern from
     * the professor's ex2-6.c — the same approach as the pos/ctrl semaphores. */
    environment_t   environment;
    int             env_step;
} sim_shm_t;

/* Parent creates the segment; sets n_flights and initialises active[] to 1. */
sim_shm_t *shm_create(int n_flights);

/* Child attaches to an existing segment created by the parent. */
sim_shm_t *shm_attach(void);

/* Parent unmaps and unlinks the segment. */
void shm_destroy(sim_shm_t *shm);

/* Open a position or control semaphore for flight idx.
 * create=1 (parent, first open with O_CREAT) / create=0 (child, plain open). */
sem_t *open_pos_sem(int idx, int create);
sem_t *open_ctrl_sem(int idx, int create);

/* US110 — open the environment mutex semaphore (value 1 when created). */
sem_t *open_env_sem(int create);

/* Unlink all named semaphores created for n_flights (plus the env semaphore). */
void cleanup_sems(int n_flights);

#endif /* SHARED_MEMORY_H */
