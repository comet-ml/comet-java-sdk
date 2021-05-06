package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetProjectsResponse {
    private List<RestProject> projects;
}
