/*
 * flight_process.h - Flight child process entry point (US105: shared memory IPC)
 */
#ifndef FLIGHT_SIMULATION_FLIGHT_PROCESS_H
#define FLIGHT_SIMULATION_FLIGHT_PROCESS_H

#include <semaphore.h>
#include "types.h"
#include "shared_memory.h"

/* US105: communication is now via shared memory + named semaphores instead of pipes.
 *   shm      – the shared memory segment (mapped in both parent and child)
 *   pos_sem  – child posts after writing its position; coordinator waits on this
 *   ctrl_sem – coordinator posts GO/STOP; child waits on this before next step */
void flight_process_main(int flight_idx, const flight_plan_t *plan,
                         sim_shm_t *shm,
                         sem_t *pos_sem, sem_t *ctrl_sem,
                         const simulation_params_t *params);

#endif /* FLIGHT_SIMULATION_FLIGHT_PROCESS_H */
