#include "flight_parser.h"
#include "cJSON.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Helper function to read a file into a string
static char* read_file_to_string(const char *filename) {
    FILE *file = fopen(filename, "rb");
    if (!file) {
        perror("fopen");
        return NULL;
    }

    fseek(file, 0, SEEK_END);
    long length = ftell(file);
    fseek(file, 0, SEEK_SET);

    char *buffer = (char *)malloc(length + 1);
    if (!buffer) {
        fprintf(stderr, "Failed to allocate memory to read file.\n");
        fclose(file);
        return NULL;
    }

    if (fread(buffer, 1, length, file) != (size_t)length) {
        fprintf(stderr, "Failed to read file content.\n");
        fclose(file);
        free(buffer);
        return NULL;
    }

    buffer[length] = '\0';
    fclose(file);
    return buffer;
}

int parse_flight_plans_from_json(const char *filename, flight_plan_t **flight_plans_out, int *num_flight_plans_out) {
    char *json_string = read_file_to_string(filename);
    if (!json_string) {
        return -1;
    }

    cJSON *root = cJSON_Parse(json_string);
    free(json_string);
    if (!root) {
        const char *error_ptr = cJSON_GetErrorPtr();
        if (error_ptr != NULL) {
            fprintf(stderr, "Error before: %s\n", error_ptr);
        }
        return -1;
    }

    if (!cJSON_IsArray(root)) {
        fprintf(stderr, "Error: Root JSON element is not an array.\n");
        cJSON_Delete(root);
        return -1;
    }

    int num_flight_plans = cJSON_GetArraySize(root);
    *num_flight_plans_out = num_flight_plans;
    flight_plan_t *flight_plans = (flight_plan_t *)malloc(num_flight_plans * sizeof(flight_plan_t));
    if (!flight_plans) {
        fprintf(stderr, "Failed to allocate memory for flight plans.\n");
        cJSON_Delete(root);
        return -1;
    }

    cJSON *flight_plan_json = NULL;
    int i = 0;
    cJSON_ArrayForEach(flight_plan_json, root) {
        flight_plan_t *current_plan = &flight_plans[i];

        cJSON *identifier = cJSON_GetObjectItemCaseSensitive(flight_plan_json, "identifier");
        if (cJSON_IsString(identifier) && (identifier->valuestring != NULL)) {
            strncpy(current_plan->identifier, identifier->valuestring, sizeof(current_plan->identifier) - 1);
            current_plan->identifier[sizeof(current_plan->identifier) - 1] = '\0';
        }

        cJSON *flight_type_json = cJSON_GetObjectItemCaseSensitive(flight_plan_json, "flight_type");
        if (cJSON_IsString(flight_type_json) && (flight_type_json->valuestring != NULL)) {
            strncpy(current_plan->flight_type, flight_type_json->valuestring, sizeof(current_plan->flight_type) - 1);
            current_plan->flight_type[sizeof(current_plan->flight_type) - 1] = '\0';
        }

        cJSON *legs_json = cJSON_GetObjectItemCaseSensitive(flight_plan_json, "legs");
        if (cJSON_IsArray(legs_json)) {
            current_plan->leg_count = cJSON_GetArraySize(legs_json);
            current_plan->legs = (leg_t *)malloc(current_plan->leg_count * sizeof(leg_t));
            if (!current_plan->legs) {
                fprintf(stderr, "Failed to allocate memory for legs.\n");
                // Cleanup previously allocated memory
                for (int j = 0; j < i; j++) {
                    free(flight_plans[j].legs);
                }
                free(flight_plans);
                cJSON_Delete(root);
                return -1;
            }

            cJSON *leg_json = NULL;
            int j = 0;
            cJSON_ArrayForEach(leg_json, legs_json) {
                leg_t *current_leg = &current_plan->legs[j];

                cJSON *segments_json = cJSON_GetObjectItemCaseSensitive(leg_json, "segments");
                if (cJSON_IsArray(segments_json)) {
                    current_leg->segment_count = cJSON_GetArraySize(segments_json);
                    current_leg->segments = (segment_t *)malloc(current_leg->segment_count * sizeof(segment_t));
                     if (!current_leg->segments) {
                        fprintf(stderr, "Failed to allocate memory for segments.\n");
                        // Complex cleanup needed here, simplified for brevity
                        cJSON_Delete(root);
                        return -1;
                    }

                    cJSON *segment_json = NULL;
                    int k = 0;
                    cJSON_ArrayForEach(segment_json, segments_json) {
                        segment_t *current_segment = &current_leg->segments[k];
                        
                        cJSON *start_coord = cJSON_GetObjectItemCaseSensitive(segment_json, "start_coord");
                        if (cJSON_IsArray(start_coord) && cJSON_GetArraySize(start_coord) == 2) {
                            current_segment->from.latitude = cJSON_GetArrayItem(start_coord, 0)->valuedouble;
                            current_segment->from.longitude = cJSON_GetArrayItem(start_coord, 1)->valuedouble;
                        }

                        cJSON *end_coord = cJSON_GetObjectItemCaseSensitive(segment_json, "end_coord");
                        if (cJSON_IsArray(end_coord) && cJSON_GetArraySize(end_coord) == 2) {
                            current_segment->to.latitude = cJSON_GetArrayItem(end_coord, 0)->valuedouble;
                            current_segment->to.longitude = cJSON_GetArrayItem(end_coord, 1)->valuedouble;
                        }

                        cJSON *altitude = cJSON_GetObjectItemCaseSensitive(segment_json, "altitude_m");
                        if(cJSON_IsNumber(altitude)) {
                            current_segment->alt_from_meters = altitude->valuedouble;
                            current_segment->alt_to_meters = altitude->valuedouble;
                        }

                        cJSON *mode_json = cJSON_GetObjectItemCaseSensitive(segment_json, "mode");
                        if (cJSON_IsString(mode_json) && mode_json->valuestring) {
                            strncpy(current_segment->mode, mode_json->valuestring,
                                    sizeof(current_segment->mode) - 1);
                        } else {
                            /* Default to cruise when mode is not specified in JSON */
                            strncpy(current_segment->mode, "cruise",
                                    sizeof(current_segment->mode) - 1);
                        }

                        k++;
                    }
                }
                j++;
            }
        }
        i++;
    }

    *flight_plans_out = flight_plans;
    cJSON_Delete(root);
    return 0;
}
