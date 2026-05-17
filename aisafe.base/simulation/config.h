/*
 * config.h - Simulation parameter loading and validation
 */
#ifndef SIMULATION_CONFIG_H
#define SIMULATION_CONFIG_H

#include "types.h"

/*
 * Parses a key=value config file into params.
 * Sets safe defaults before parsing so missing keys keep their default value.
 * Returns  0 on success.
 * Returns -1 if the file cannot be opened.
 * Returns the line number (>0) if a line cannot be parsed.
 */
int load_config(const char *path, simulation_params_t *params);

/*
 * Validates all fields in params.
 * Prints a descriptive error message to stderr for each invalid field.
 * Returns 0 if all fields are valid, -1 otherwise.
 */
int validate_config(const simulation_params_t *params);

/* Prints all parameter values to stdout (for startup diagnostics). */
void print_config(const simulation_params_t *params);

#endif /* SIMULATION_CONFIG_H */
