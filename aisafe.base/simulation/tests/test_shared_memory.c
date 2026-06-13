/*
 * test_shared_memory.c - Unit tests for US105 shared memory + named semaphores
 *
 * Verifies:
 *   1. shm_create() initialises the segment (n_flights, active[], zeroed counters);
 *   2. shm_attach() maps the SAME segment (writes are visible across mappings);
 *   3. named semaphore initial values — pos/ctrl are event semaphores (0),
 *      the env semaphore is a mutex (1) — checked behaviourally via sem_trywait
 *      (portable; macOS sem_getvalue is unreliable for named semaphores);
 *   4. cleanup_sems()/shm_destroy() unlink the OS objects.
 */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <semaphore.h>
#include <sys/mman.h>   /* shm_unlink (used directly in the cleanup test) */

#include "shared_memory.h"

static int tests_run = 0;
static int tests_failed = 0;

#define ASSERT_TRUE(condition, message) \
    do { \
        tests_run++; \
        if (!(condition)) { \
            fprintf(stderr, "FAIL: %s (line %d)\n", message, __LINE__); \
            tests_failed++; \
        } \
    } while (0)

/* ------------------------------------------------------------------ */

static void test_shm_create_initialises_segment(void) {
    const int n = 3;
    sim_shm_t *shm = shm_create(n);

    ASSERT_TRUE(shm != NULL, "shm_create must return a mapping");
    ASSERT_TRUE(shm->n_flights == n, "shm_create must set n_flights");
    ASSERT_TRUE(shm->active[0] == 1 && shm->active[1] == 1 && shm->active[2] == 1,
                "shm_create must mark the n flights active");
    ASSERT_TRUE(shm->active[n] == 0, "slots beyond n_flights must stay inactive");
    ASSERT_TRUE(shm->total_violations == 0 && shm->sim_aborted == 0,
                "shm_create must zero the result counters");
    ASSERT_TRUE(shm->violation_event_count == 0 && shm->env_step == 0,
                "shm_create must zero the event/env counters");

    shm_destroy(shm);
}

static void test_shm_attach_shares_same_segment(void) {
    sim_shm_t *creator = shm_create(2);
    sim_shm_t *viewer  = shm_attach();   /* second mapping of the same /aisafe_sim */

    ASSERT_TRUE(viewer != NULL, "shm_attach must return a mapping");

    /* A write through one mapping must be visible through the other. */
    creator->total_violations = 42;
    creator->environment.wind_speed = 7.5;
    ASSERT_TRUE(viewer->total_violations == 42,
                "writes through the creator mapping must be visible to attach mapping");
    ASSERT_TRUE(viewer->environment.wind_speed == 7.5,
                "environment block must be shared across mappings");

    /* viewer was mapped via shm_attach (no separate unlink); unmap by destroying
     * the named object once — both mappings reference the same object. */
    shm_destroy(viewer);
    shm_destroy(creator);
}

static void test_pos_and_ctrl_semaphores_start_at_zero(void) {
    sem_t *pos  = open_pos_sem(0, 1);
    sem_t *ctrl = open_ctrl_sem(0, 1);

    /* value 0 (event): an immediate trywait must fail with EAGAIN. */
    ASSERT_TRUE(sem_trywait(pos) == -1 && errno == EAGAIN,
                "pos semaphore must start at 0 (event-signalling)");
    ASSERT_TRUE(sem_trywait(ctrl) == -1 && errno == EAGAIN,
                "ctrl semaphore must start at 0 (event-signalling)");

    /* after a post the event becomes available exactly once. */
    sem_post(pos);
    ASSERT_TRUE(sem_trywait(pos) == 0, "pos semaphore must be available after one post");
    ASSERT_TRUE(sem_trywait(pos) == -1 && errno == EAGAIN,
                "pos semaphore must be consumed by a single wait");

    sem_close(pos);
    sem_close(ctrl);
    cleanup_sems(1);
}

static void test_env_semaphore_is_a_mutex(void) {
    sem_t *env = open_env_sem(1);

    /* value 1 (mutex): first trywait acquires, second would block. */
    ASSERT_TRUE(sem_trywait(env) == 0, "env semaphore must start at 1 (mutex acquire)");
    ASSERT_TRUE(sem_trywait(env) == -1 && errno == EAGAIN,
                "env semaphore must be held after a single acquire");
    sem_post(env);   /* release */
    ASSERT_TRUE(sem_trywait(env) == 0, "env semaphore must be acquirable again after release");
    sem_post(env);

    sem_close(env);
    sem_unlink(SEM_ENV_NAME);
}

static void test_cleanup_unlinks_objects(void) {
    sem_t *env = open_env_sem(1);
    sem_close(env);

    cleanup_sems(0);                       /* unlinks the env semaphore */
    ASSERT_TRUE(sem_unlink(SEM_ENV_NAME) == -1 && errno == ENOENT,
                "cleanup_sems must unlink the env semaphore");

    sim_shm_t *shm = shm_create(1);
    shm_destroy(shm);                      /* munmap + shm_unlink */
    ASSERT_TRUE(shm_unlink(SHM_NAME) == -1 && errno == ENOENT,
                "shm_destroy must unlink the shared memory object");
}

int main(void) {
    test_shm_create_initialises_segment();
    test_shm_attach_shares_same_segment();
    test_pos_and_ctrl_semaphores_start_at_zero();
    test_env_semaphore_is_a_mutex();
    test_cleanup_unlinks_objects();

    printf("Tests run: %d\n", tests_run);
    printf("Tests failed: %d\n", tests_failed);

    return tests_failed == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}
