/*
 * aca_filter.c - Air Control Area boundary checks (AC5, AC6)
 */
#include "aca_filter.h"

int is_in_aca(const aircraft_position_t *pos, const geo_boundary_t *aca) {
    return pos->latitude  <= aca->north_latitude &&
           pos->latitude  >= aca->south_latitude &&
           pos->longitude >= aca->west_longitude &&
           pos->longitude <= aca->east_longitude;
}
