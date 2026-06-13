/*
 * thread_io.h - Thread-safe stdout for the simulation's parent threads.
 *
 * Professor's principle (ex1-9.c): "Avoiding the use of printf in Linux POSIX
 * threads is recommended because printf is not inherently thread-safe." Code that
 * runs in the coordinator/safety/report/environment threads must therefore format
 * into a local buffer and emit it with a single write(), instead of printf().
 */
#ifndef THREAD_IO_H
#define THREAD_IO_H

/* printf-style formatting that emits the result with one write(STDOUT_FILENO,…). */
void tprintf(const char *fmt, ...);

#endif /* THREAD_IO_H */
