package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds GIT meta-data associated with particular Comet experiment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitMetaData {
    private String user;
    private String root;
    private String branch;
    private String parent;
    private String origin;
}
