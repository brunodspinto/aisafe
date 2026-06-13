/*
 * shared_memory.c - POSIX shared memory and named semaphore implementation (US105)
 *
 * Patterns from professor examples:
 *   shm_open + ftruncate + mmap  →  ex1-7.c, ex2-6.c
 *   sem_open (named semaphores)  →  ex1-7.c, ex2-5.c
 */
#define _POSIX_C_SOURCE 200809L
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <semaphore.h>
#include <unistd.h>
#include "shared_memory.h"

sim_shm_t *shm_create(int n_flights) {
    /* Remover qualquer objeto residual de runs anteriores que tenham crashado */
    shm_unlink(SHM_NAME);
    int fd = shm_open(SHM_NAME, O_CREAT | O_EXCL | O_RDWR, S_IRUSR | S_IWUSR);
    if (fd == -1) { perror("shm_open (create)"); exit(1); }
    if (ftruncate(fd, sizeof(sim_shm_t)) == -1) { perror("ftruncate"); exit(1); }
    sim_shm_t *shm = mmap(NULL, sizeof(sim_shm_t),
                          PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (shm == MAP_FAILED) { perror("mmap (create)"); exit(1); }
    close(fd);

    memset(shm, 0, sizeof(sim_shm_t));
    shm->n_flights = n_flights;
    for (int i = 0; i < n_flights; i++)
        shm->active[i] = 1;

    return shm;
}

sim_shm_t *shm_attach(void) {
    int fd = shm_open(SHM_NAME, O_RDWR, 0);
    if (fd == -1) { perror("shm_open (attach)"); exit(1); }
    sim_shm_t *shm = mmap(NULL, sizeof(sim_shm_t),
                          PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (shm == MAP_FAILED) { perror("mmap (attach)"); exit(1); }
    close(fd);
    return shm;
}

void shm_destroy(sim_shm_t *shm) {
    munmap(shm, sizeof(sim_shm_t));
    shm_unlink(SHM_NAME);
}

/* value is the initial count when the semaphore is created (create=1): 0 for the
 * event-signalling pos/ctrl semaphores (ex2-5.c), 1 for the env mutex (ex2-6.c). */
static sem_t *open_named_sem(const char *name, int create, unsigned int value) {
    sem_t *sem;
    if (create) {
        /* Remove any leftover from a previous crashed run before re-creating. */
        sem_unlink(name);
        sem = sem_open(name, O_CREAT | O_EXCL, 0644, value);
    } else {
        sem = sem_open(name, 0);
    }
    if (sem == SEM_FAILED) { perror(name); exit(1); }
    return sem;
}

sem_t *open_pos_sem(int idx, int create) {
    char name[SEM_NAME_MAX];
    snprintf(name, SEM_NAME_MAX, SEM_POS_FMT, idx);
    return open_named_sem(name, create, 0);
}

sem_t *open_ctrl_sem(int idx, int create) {
    char name[SEM_NAME_MAX];
    snprintf(name, SEM_NAME_MAX, SEM_CTRL_FMT, idx);
    return open_named_sem(name, create, 0);
}

sem_t *open_env_sem(int create) {
    /* US110 — value 1: mutual-exclusion mutex pattern from ex2-6.c. */
    return open_named_sem(SEM_ENV_NAME, create, 1);
}

void cleanup_sems(int n_flights) {
    char name[SEM_NAME_MAX];
    for (int i = 0; i < n_flights; i++) {
        snprintf(name, SEM_NAME_MAX, SEM_POS_FMT, i);
        sem_unlink(name);
        snprintf(name, SEM_NAME_MAX, SEM_CTRL_FMT, i);
        sem_unlink(name);
    }
    sem_unlink(SEM_ENV_NAME);   /* US110 */
}
