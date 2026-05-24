#ifndef FLIGHT_PARSER_H
#define FLIGHT_PARSER_H

#include "types.h"

/**
 * @brief Parses a JSON file containing an array of flight plans.
 *
 * This function reads a JSON file from the given path, parses it, and populates
 * an array of flight_plan_t structs.
 *
 * @param filename The path to the JSON file.
 * @param flight_plans A pointer to an array of flight_plan_t structs that will be allocated and filled.
 * @param num_flight_plans A pointer to an integer that will be filled with the number of flight plans parsed.
 * @return 0 on success, -1 on failure (e.g., file not found, memory allocation error, parse error).
 */
int parse_flight_plans_from_json(const char *filename, flight_plan_t **flight_plans, int *num_flight_plans);

#endif // FLIGHT_PARSER_H
