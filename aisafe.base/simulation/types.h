/*
 * types.h - Data structures for flight simulation
 */
#ifndef FLIGHT_SIMULATION_TYPES_H
#define FLIGHT_SIMULATION_TYPES_H
#define MAX_FLIGHTS 10
#define MAX_POSITIONS 1000

#include <time.h>

typedef struct {
    double latitude;
    double longitude;
} coordinate_t;

typedef struct {
    char mode[16];           /* "climb", "cruise", "descend" */
    coordinate_t from;
    coordinate_t to;
    double alt_from_meters;
    double alt_to_meters;
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
    double speed_knots;
    double heading_deg;
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

/* Rectangular boundary of an Air Control Area */
typedef struct {
    double north_latitude;
    double south_latitude;
    double east_longitude;
    double west_longitude;
} geo_boundary_t;

/* Tracks whether a flight has entered/exited the ACA */
typedef enum {
    ACA_BEFORE = 0, /* hasn't entered yet (or never will) */
    ACA_INSIDE = 1, /* currently inside the ACA */
    ACA_AFTER  = 2  /* has exited the ACA */
} aca_state_t;

typedef struct {
    aircraft_position_t positions[MAX_POSITIONS];
    int count;
    char flight_id[64];
    aca_state_t aca_state; /* AC6: entry/exit tracking */
} flight_history_t;

#endif /* FLIGHT_SIMULATION_TYPES_H */

