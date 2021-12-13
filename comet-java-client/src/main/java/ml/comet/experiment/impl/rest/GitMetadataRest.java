package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ml.comet.experiment.model.GitMetaData;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class GitMetadataRest extends BaseExperimentObject {
    private String user;
    private String root;
    private String branch;
    private String parent;
    private String origin;

    /**
     * Convert this into the {@link GitMetaData} object of the public API.
     *
     * @return the initialized {@link GitMetaData} instance.
     */
    public GitMetaData toGitMetaData() {
        GitMetaData g = new GitMetaData();
        g.setUser(this.user);
        g.setRoot(this.root);
        g.setBranch(this.branch);
        g.setParent(this.parent);
        g.setOrigin(this.origin);
        return g;
    }
}
