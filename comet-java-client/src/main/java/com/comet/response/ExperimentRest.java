package com.comet.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ExperimentRest {
    private String code_sha;
    private String file_name;
    private String file_path;
    private long duration_millis;
    private long start_server_timestamp;
    private long end_server_timestamp;
    private Boolean has_images;
    private String experiment_key;
    private String optimization_id;
    private String experimentName;
}
