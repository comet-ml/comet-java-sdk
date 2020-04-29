package ml.comet.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ProjectRest {
    private String project_id;
    private String user_name;
    private String project_name;
    private String project_desc;
    private String team_id;
    private boolean is_owner;
    private boolean is_public;
    private boolean is_shared;
    private int num_of_experiments;
    private Long last_updated;
    private String team_name;
}
