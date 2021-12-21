package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactVersionAsset {
    private String artifactId;
    private String artifactVersionId;
    private String assetId;
    private String fileName;
    private long fileSize;
    private String link;
    private String dir;
    private String type;
    private String metadata;
    private Boolean remote;
}
