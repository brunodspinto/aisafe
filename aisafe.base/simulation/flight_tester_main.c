/*
 * flight_tester_main.c - Single-flight POSIX tester for US085.
 *
 * Tests one flight plan in isolation and outputs a JSON result to stdout:
 *   PASS: {"identifier":"TP85","status":"PASS","steps":N}
 *   FAIL: {"identifier":"TP85","status":"FAIL","reason":"..."}
 *
 * Exit code: 0 = PASS, 1 = FAIL.
 *
 * POSIX APIs exercised (AC085.4):
 *   fork()                   — isolates simulation in a child process
 *   pipe()                   — child sends aircraft_position_t to coordinator
 *   shm_open() / mmap()      — shared memory holds latest position
 *   sem_open() (named sem.)  — coordinator posts GO; child waits before next step
 *   pthread_create()         — coordinator thread reads pipe concurrently
 *   pthread_mutex_t          — protects shared history array
 *   pthread_cond_t           — coordinator signals main thread when done
 *   sigaction(SIGUSR1)       — external abort: kills child, marks FAIL
 */

#define _POSIX_C_SOURCE 200809L

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <pthread.h>
#include <errno.h>

#include "flight_parser.h"
#include "validation.h"
#include "types.h"

/* ---- global state (written by SIGUSR1 handler, read by main) ---- */

static volatile pid_t   g_child_pid      = -1;
static volatile sig_atomic_t g_sigusr1_fired  = 0;

/* IPC resource names stored for SIGTERM cleanup (written before handlers are installed) */
static char g_sem_name[64]       = "";
static char g_shm_mutex_name[64] = "";
static char g_shm_name[64]       = "";

static void sigusr1_handler(int sig) {
    (void)sig;
    g_sigusr1_fired = 1;
    if (g_child_pid > 0) {
        kill(g_child_pid, SIGTERM);
    }
}

/* Handles SIGTERM (sent by Java on timeout via process.destroy()).
 * Unlinks named IPC objects so they are not leaked in /dev/shm, then exits.
 * sem_unlink/shm_unlink are not in the POSIX async-signal-safe list but work
 * reliably on Linux/macOS; this is best-effort cleanup on abnormal termination. */
static void sigterm_handler(int sig) {
    (void)sig;
    if (g_sem_name[0])       sem_unlink(g_sem_name);
    if (g_shm_mutex_name[0]) sem_unlink(g_shm_mutex_name);
    if (g_shm_name[0])       shm_unlink(g_shm_name);
    _exit(1);
}

/* ---- coordinator thread state ---- */

typedef struct {
    int                  pipe_read_fd;
    aircraft_position_t *history;
    int                 *history_count;
    sem_t               *go_sem;           /* named semaphore: post GO to child   */
    pthread_mutex_t     *mutex;
    pthread_cond_t      *done_cond;
    int                  done;             /* set to 1 when pipe EOF is reached   */
} coord_args_t;

static void *coordinator_thread(void *arg) {
    coord_args_t        *a   = (coord_args_t *)arg;
    aircraft_position_t  pos;
    ssize_t              n;

    while ((n = read(a->pipe_read_fd, &pos, sizeof(pos))) == (ssize_t)sizeof(pos)) {
        pthread_mutex_lock(a->mutex);
        if (*a->history_count < MAX_POSITIONS) {
            a->history[(*a->history_count)++] = pos;
        }
        pthread_mutex_unlock(a->mutex);
        /* Tell child to advance to the next step */
        sem_post(a->go_sem);
    }

    /* pipe EOF: child has closed its write end (exited or finished) */
    pthread_mutex_lock(a->mutex);
    a->done = 1;
    pthread_cond_signal(a->done_cond);
    pthread_mutex_unlock(a->mutex);

    return NULL;
}

/* ---- child: simulate flight step by step ---- */

static void child_simulate(flight_plan_t *plan, int pipe_write_fd, sem_t *go_sem,
                            sem_t *shm_mutex, aircraft_position_t *shm_pos) {
    for (int li = 0; li < plan->leg_count; li++) {
        leg_t *leg = &plan->legs[li];
        for (int si = 0; si < leg->segment_count; si++) {
            segment_t *seg = &leg->segments[si];

            if (validate_coordinate(&seg->from) == 0 ||
                validate_coordinate(&seg->to)   == 0) {
                close(pipe_write_fd);
                exit(1);
            }

            aircraft_position_t pos;
            memset(&pos, 0, sizeof(pos));
            pos.latitude        = seg->to.latitude;
            pos.longitude       = seg->to.longitude;
            pos.altitude_meters = seg->alt_to_meters;
            snprintf(pos.flight_id, sizeof(pos.flight_id), "%s", plan->identifier);

            /* Write position update to parent via pipe */
            if (write(pipe_write_fd, &pos, sizeof(pos)) != (ssize_t)sizeof(pos)) {
                close(pipe_write_fd);
                exit(1);
            }

            /* Update shared memory with latest position (mutex-protected per SCOMP T6) */
            if (shm_pos) {
                sem_wait(shm_mutex);
                memcpy(shm_pos, &pos, sizeof(pos));
                sem_post(shm_mutex);
            }

            /* Wait for GO signal from coordinator (step synchronisation) */
            sem_wait(go_sem);
        }
    }
    close(pipe_write_fd);
    exit(0);
}

/* ---- main ---- */

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <flight_plan.json>\n", argv[0]);
        printf("{\"identifier\":\"unknown\",\"status\":\"FAIL\","
               "\"reason\":\"missing argument: flight plan JSON path\"}\n");
        return 1;
    }

    /* ---- Parse and validate ---- */
    flight_plan_t *plans    = NULL;
    int            n_plans  = 0;

    if (parse_flight_plans_from_json(argv[1], &plans, &n_plans) != 0 || n_plans == 0) {
        printf("{\"identifier\":\"unknown\",\"status\":\"FAIL\","
               "\"reason\":\"failed to parse flight plan JSON\"}\n");
        return 1;
    }

    flight_plan_t *plan = &plans[0];

    if (validate_flight_plan(plan) == 0) {
        char id[64];
        strncpy(id, plan->identifier[0] ? plan->identifier : "unknown", sizeof(id) - 1);
        id[sizeof(id) - 1] = '\0';
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"flight plan failed validation\"}\n", id);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }

    char identifier[64];
    strncpy(identifier, plan->identifier, sizeof(identifier) - 1);
    identifier[sizeof(identifier) - 1] = '\0';

    /* ---- IPC resource names (pid-suffixed to avoid collisions) ---- */
    char sem_name[64];
    char shm_mutex_name[64];
    char shm_name[64];
    snprintf(sem_name,       sizeof(sem_name),       "/fp_test_%d",  (int)getpid());
    snprintf(shm_mutex_name, sizeof(shm_mutex_name), "/fp_shm_m_%d", (int)getpid());
    snprintf(shm_name,       sizeof(shm_name),       "/fp_shm_%d",   (int)getpid());

    /* ---- Named semaphore (initial value 0 — child waits, coordinator posts GO) ---- */
    sem_t *go_sem = sem_open(sem_name, O_CREAT | O_EXCL, 0600, 0);
    if (go_sem == SEM_FAILED) {
        perror("sem_open");
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"sem_open failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }

    /* ---- Anonymous pipe (child writes positions; coordinator reads) ---- */
    int pfd[2];
    if (pipe(pfd) != 0) {
        perror("pipe");
        sem_close(go_sem); sem_unlink(sem_name);
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"pipe failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }

    /* ---- Shared memory (latest aircraft position; readable by parent and child) ---- */
    int shm_fd = shm_open(shm_name, O_CREAT | O_RDWR, 0600);
    if (shm_fd < 0) {
        perror("shm_open");
        close(pfd[0]); close(pfd[1]);
        sem_close(go_sem); sem_unlink(sem_name);
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"shm_open failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }
    if (ftruncate(shm_fd, (off_t)sizeof(aircraft_position_t)) < 0) {
        perror("ftruncate");
    }
    void *shm_ptr = mmap(NULL, sizeof(aircraft_position_t),
                         PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
    close(shm_fd);  /* fd no longer needed after mmap */
    if (shm_ptr == MAP_FAILED) {
        perror("mmap");
        close(pfd[0]); close(pfd[1]);
        sem_close(go_sem); sem_unlink(sem_name);
        shm_unlink(shm_name);
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"mmap failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }
    memset(shm_ptr, 0, sizeof(aircraft_position_t));

    /* ---- Named semaphore for shared-memory write protection (init=1 → binary mutex) ---- */
    sem_t *shm_mutex = sem_open(shm_mutex_name, O_CREAT | O_EXCL, 0600, 1);
    if (shm_mutex == SEM_FAILED) {
        perror("sem_open shm_mutex");
        munmap(shm_ptr, sizeof(aircraft_position_t));
        close(pfd[0]); close(pfd[1]);
        sem_close(go_sem); sem_unlink(sem_name);
        shm_unlink(shm_name);
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"sem_open shm_mutex failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }

    /* Publish IPC names for signal handlers before installing them */
    strncpy(g_sem_name,       sem_name,       sizeof(g_sem_name)       - 1);
    strncpy(g_shm_mutex_name, shm_mutex_name, sizeof(g_shm_mutex_name) - 1);
    strncpy(g_shm_name,       shm_name,       sizeof(g_shm_name)       - 1);

    /* ---- SIGUSR1 handler (external abort) ---- */
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = sigusr1_handler;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGUSR1, &sa, NULL);

    /* ---- SIGTERM handler (Java timeout: destroy() → graceful IPC cleanup) ---- */
    struct sigaction sa_term;
    memset(&sa_term, 0, sizeof(sa_term));
    sa_term.sa_handler = sigterm_handler;
    sigemptyset(&sa_term.sa_mask);
    sigaction(SIGTERM, &sa_term, NULL);

    /* ---- Fork child ---- */
    g_child_pid = fork();
    if (g_child_pid < 0) {
        perror("fork");
        munmap(shm_ptr, sizeof(aircraft_position_t));
        close(pfd[0]); close(pfd[1]);
        sem_close(go_sem); sem_unlink(sem_name);
        sem_close(shm_mutex); sem_unlink(shm_mutex_name);
        shm_unlink(shm_name);
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"fork failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }

    if (g_child_pid == 0) {
        /* ---- Child process ---- */
        close(pfd[0]);  /* close unused read end */
        child_simulate(plan, pfd[1], go_sem, shm_mutex, (aircraft_position_t *)shm_ptr);
        /* child_simulate calls exit() */
        exit(0);
    }

    /* ---- Parent process ---- */
    close(pfd[1]);  /* close unused write end */

    /* History array (on the stack; bounded by MAX_POSITIONS) */
    static aircraft_position_t history[MAX_POSITIONS];
    static int                 history_count = 0;

    pthread_mutex_t mutex     = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t  done_cond = PTHREAD_COND_INITIALIZER;

    coord_args_t coord_args = {
        .pipe_read_fd  = pfd[0],
        .history       = history,
        .history_count = &history_count,
        .go_sem        = go_sem,
        .mutex         = &mutex,
        .done_cond     = &done_cond,
        .done          = 0
    };

    /* ---- Spawn coordinator thread ---- */
    pthread_t coord_thread;
    if (pthread_create(&coord_thread, NULL, coordinator_thread, &coord_args) != 0) {
        perror("pthread_create");
        /* kill child and fail cleanly */
        kill(g_child_pid, SIGTERM);
        waitpid(g_child_pid, NULL, 0);
        munmap(shm_ptr, sizeof(aircraft_position_t));
        close(pfd[0]);
        sem_close(go_sem); sem_unlink(sem_name);
        sem_close(shm_mutex); sem_unlink(shm_mutex_name);
        shm_unlink(shm_name);
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\","
               "\"reason\":\"pthread_create failed\"}\n", identifier);
        for (int i = 0; i < n_plans; i++) { free(plans[i].legs); }
        free(plans);
        return 1;
    }

    /* ---- Wait for coordinator to signal done (all pipe data consumed) ---- */
    pthread_mutex_lock(&mutex);
    while (!coord_args.done) {
        pthread_cond_wait(&done_cond, &mutex);
    }
    pthread_mutex_unlock(&mutex);

    /* ---- Collect child exit status ---- */
    int child_status = 0;
    waitpid(g_child_pid, &child_status, 0);
    pthread_join(coord_thread, NULL);

    int child_exit = (WIFEXITED(child_status)) ? WEXITSTATUS(child_status) : 1;
    if (g_sigusr1_fired) {
        child_exit = 1;
    }

    /* Read final position from shared memory.
     * Child has exited; no concurrent writer, so no synchronisation needed here. */
    aircraft_position_t shm_last;
    memset(&shm_last, 0, sizeof(shm_last));
    memcpy(&shm_last, shm_ptr, sizeof(shm_last));

    /* ---- Cleanup IPC ---- */
    close(pfd[0]);
    munmap(shm_ptr, sizeof(aircraft_position_t));
    sem_close(go_sem);
    sem_unlink(sem_name);
    sem_close(shm_mutex);
    sem_unlink(shm_mutex_name);
    shm_unlink(shm_name);

    /* ---- Free parsed plans ---- */
    for (int i = 0; i < n_plans; i++) {
        if (plans[i].legs) {
            for (int j = 0; j < plans[i].leg_count; j++) {
                free(plans[i].legs[j].segments);
            }
            free(plans[i].legs);
        }
    }
    free(plans);

    /* ---- Output result ---- */
    if (child_exit == 0) {
        printf("{\"identifier\":\"%s\",\"status\":\"PASS\",\"steps\":%d,"
               "\"last_lat\":%.6f,\"last_lon\":%.6f}\n",
               identifier, history_count,
               shm_last.latitude, shm_last.longitude);
        return 0;
    } else {
        const char *reason = g_sigusr1_fired
                ? "test aborted by SIGUSR1"
                : "coordinate validation failed or simulation error";
        printf("{\"identifier\":\"%s\",\"status\":\"FAIL\",\"reason\":\"%s\"}\n",
               identifier, reason);
        return 1;
    }
}
