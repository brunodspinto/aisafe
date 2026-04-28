/*
 * types.h - Data structures for flight simulation
 */
#ifndef FLIGHT_SIMULATION_TYPES_H
#define FLIGHT_SIMULATION_TYPES_H

#include <time.h>

typedef struct {
    double latitude;
    double longitude;
} coordinate_t;

typedef struct {
    coordinate_t from;
    coordinate_t to;
    double altitude_meters;
    double width_meters;
    double wind_speed;
    double wind_direction;
} segment_t;

typedef struct {
    char airport_code[5];
    time_t datetime;
} endpoint_t;

typedef struct {
    int segment_count;
    segment_t *segments;
    endpoint_t departure;
    endpoint_t arrival;
    double fuel_kg;
} leg_t;

typedef struct {
    char identifier[64];
    char flight_type[16];  /* REGULAR or CHARTER */
    int leg_count;
    leg_t *legs;
} flight_plan_t;

typedef struct {
    double latitude;
    double longitude;
    double altitude_meters;
    time_t timestamp;
    char flight_id[64];
} aircraft_position_t;

typedef struct {
    time_t start_time;
    time_t end_time;
    double min_latitude;
    double max_latitude;
    double min_longitude;
    double max_longitude;
    int max_flights;
    double safety_threshold;
    double performance_threshold;
} simulation_params_t;

#endif /* FLIGHT_SIMULATION_TYPES_H */

