package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetWorkspacesResponse {
    private List<String> workspaceNames;
}
