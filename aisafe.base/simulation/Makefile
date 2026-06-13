CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2 -I.
LDFLAGS = -lm -lpthread

# Em Linux, shm_open requer -lrt; no macOS está incluído no libc
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -lrt
endif

SRCS = main.c safety_thread.c ipc.c flight_process.c config.c report.c safety_monitor.c aca_filter.c cJSON.c flight_parser.c shared_memory.c environment.c weather_service.c thread_io.c
OBJS = $(SRCS:.c=.o)
EXECUTABLE = flight_simulator

# ---- flight_tester (US085) ----
TESTER_SRCS = flight_tester_main.c flight_parser.c validation.c cJSON.c
TESTER_OBJS = $(TESTER_SRCS:.c=.o)
TESTER_EXECUTABLE = flight_tester

# ---- test_validation ----
TEST_SRCS = tests/test_validation.c validation.c
TEST_OBJS = $(TEST_SRCS:.c=.o)
TEST_EXECUTABLE = test_validation

# ---- test_flight_tester (US085) ----
TEST_TESTER_SRCS = tests/test_flight_tester.c flight_parser.c validation.c cJSON.c
TEST_TESTER_OBJS = $(TEST_TESTER_SRCS:.c=.o)
TEST_TESTER_EXECUTABLE = test_flight_tester

# ---- test_shared_memory (US105) ----
TEST_SHM_SRCS = tests/test_shared_memory.c shared_memory.c
TEST_SHM_OBJS = $(TEST_SHM_SRCS:.c=.o)
TEST_SHM_EXECUTABLE = test_shared_memory

# ---- test_environment (US110) ----
TEST_ENV_SRCS = tests/test_environment.c environment.c
TEST_ENV_OBJS = $(TEST_ENV_SRCS:.c=.o)
TEST_ENV_EXECUTABLE = test_environment

all: $(EXECUTABLE) $(TESTER_EXECUTABLE)

$(EXECUTABLE): $(OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

$(TESTER_EXECUTABLE): $(TESTER_OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

run: $(EXECUTABLE)
	./$(EXECUTABLE)

# $(TESTER_EXECUTABLE) é pré-requisito porque os testes de integração de
# test_flight_tester invocam ./flight_tester via system().
test: $(TEST_EXECUTABLE) $(TEST_TESTER_EXECUTABLE) $(TESTER_EXECUTABLE) \
      $(TEST_SHM_EXECUTABLE) $(TEST_ENV_EXECUTABLE)
	./$(TEST_EXECUTABLE)
	./$(TEST_TESTER_EXECUTABLE)
	./$(TEST_SHM_EXECUTABLE)
	./$(TEST_ENV_EXECUTABLE)

$(TEST_EXECUTABLE): $(TEST_OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

$(TEST_TESTER_EXECUTABLE): $(TEST_TESTER_OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

$(TEST_SHM_EXECUTABLE): $(TEST_SHM_OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

$(TEST_ENV_EXECUTABLE): $(TEST_ENV_OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

clean:
	rm -f $(OBJS) $(EXECUTABLE) \
	      $(TESTER_OBJS) $(TESTER_EXECUTABLE) \
	      $(TEST_OBJS) $(TEST_EXECUTABLE) \
	      $(TEST_TESTER_OBJS) $(TEST_TESTER_EXECUTABLE) \
	      $(TEST_SHM_OBJS) $(TEST_SHM_EXECUTABLE) \
	      $(TEST_ENV_OBJS) $(TEST_ENV_EXECUTABLE)

.PHONY: all run test clean

