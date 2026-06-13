/*
 * types.h - Data structures for flight simulation
 */
#ifndef FLIGHT_SIMULATION_TYPES_H
#define FLIGHT_SIMULATION_TYPES_H
#define MAX_FLIGHTS 10
#define MAX_POSITIONS 1000
#define MAX_PERF_POINTS 20
#define MAX_VIOLATION_EVENTS 256

#include <time.h>

typedef struct {
    double latitude;
    double longitude;
} coordinate_t;

/* One row of the aircraft performance table (altitude-indexed) */
typedef struct {
    double altitude_m;
    double speed_knots;
    double vertical_rate_mps;  /* positive = climb, negative = descend */
} perf_point_t;

/* Performance tables parsed from "Flight Profile" in the JSON */
typedef struct {
    perf_point_t climb[MAX_PERF_POINTS];
    int          climb_count;
    perf_point_t descend[MAX_PERF_POINTS];
    int          descend_count;
    double       cruise_speed_knots;
} flight_profile_t;

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

/* US110: current environmental conditions published by the environment thread
 * into shared memory each simulation step. wind_direction is meteorological
 * (direction the wind blows FROM). wind_speed <= 0 means calm. */
typedef struct {
    double wind_speed;      /* m/s */
    double wind_direction;  /* degrees, meteorological (FROM) */
} environment_t;

typedef struct {
    int              segment_count;
    segment_t       *segments;
    endpoint_t       departure;
    endpoint_t       arrival;
    double           fuel_kg;
    flight_profile_t profile;   /* altitude-dependent performance tables */
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
    double vz_mps;       /* current vertical rate (m/s): + climb, - descend */
    time_t timestamp;
    char   flight_id[64];
} aircraft_position_t;

typedef struct {
    double aca_north_lat;       /* ACA north boundary (degrees) */
    double aca_south_lat;       /* ACA south boundary (degrees) */
    double aca_east_lon;        /* ACA east boundary (degrees)  */
    double aca_west_lon;        /* ACA west boundary (degrees)  */
    int    n_flights;           /* number of flights to simulate */
    double safe_dist_horiz_m;   /* horizontal safety cylinder (meters) */
    double safe_dist_vert_m;    /* vertical safety cylinder (meters)   */
    int    max_violations;      /* violations before early termination */
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
    aca_state_t aca_state; /* US101: ACA entry/exit tracking */
} flight_history_t;

typedef struct {
    time_t timestamp;
    char flight_a[64];
    char flight_b[64];
    aircraft_position_t position_a;
    aircraft_position_t position_b;
    double horizontal_distance_m;
    double vertical_distance_m;
} violation_event_t;

#endif /* FLIGHT_SIMULATION_TYPES_H */

