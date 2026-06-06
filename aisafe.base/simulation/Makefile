CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2 -I.
LDFLAGS = -lm -lpthread

# Em Linux, shm_open requer -lrt; no macOS está incluído no libc
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -lrt
endif

SRCS = main.c safety_thread.c ipc.c flight_process.c config.c report.c safety_monitor.c aca_filter.c cJSON.c flight_parser.c shared_memory.c
OBJS = $(SRCS:.c=.o)
EXECUTABLE = flight_simulator

TEST_SRCS = tests/test_validation.c validation.c
TEST_OBJS = $(TEST_SRCS:.c=.o)
TEST_EXECUTABLE = test_validation

all: $(EXECUTABLE)

$(EXECUTABLE): $(OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

run: $(EXECUTABLE)
	./$(EXECUTABLE)

test: $(TEST_EXECUTABLE)
	./$(TEST_EXECUTABLE)

$(TEST_EXECUTABLE): $(TEST_OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

clean:
	rm -f $(OBJS) $(EXECUTABLE) $(TEST_OBJS) $(TEST_EXECUTABLE)

.PHONY: all run test clean

