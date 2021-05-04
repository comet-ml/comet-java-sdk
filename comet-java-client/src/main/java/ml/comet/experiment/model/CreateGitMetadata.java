package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGitMetadata {
    private String experimentKey;
    private String user;
    private String root;
    private String branch;
    private String parent;
    private String origin;
}
