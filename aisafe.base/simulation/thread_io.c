/*
 * thread_io.c - Thread-safe stdout helper (see thread_io.h).
 */
#define _POSIX_C_SOURCE 200809L
#include "thread_io.h"
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>

void tprintf(const char *fmt, ...) {
    char buf[512];
    va_list ap;
    va_start(ap, fmt);
    const int n = vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    if (n < 0) return;

    /* vsnprintf returns the length it WOULD have written; clamp to the buffer. */
    size_t len = ((size_t)n < sizeof(buf)) ? (size_t)n : sizeof(buf) - 1;

    /* Best-effort full write, retrying on EINTR; errno preserved (ex1-9.c pattern). */
    const int saved_errno = errno;
    size_t off = 0;
    while (off < len) {
        const ssize_t w = write(STDOUT_FILENO, buf + off, len - off);
        if (w < 0) {
            if (errno == EINTR) continue;
            break;
        }
        off += (size_t) w;
    }
    errno = saved_errno;
}
