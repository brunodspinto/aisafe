CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2 -I.
LDFLAGS = -lm

SRCS = main.c ipc.c flight_process.c
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

