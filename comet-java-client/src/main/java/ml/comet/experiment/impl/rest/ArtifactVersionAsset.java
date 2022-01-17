package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.LoggedArtifactAssetImpl;
import ml.comet.experiment.impl.utils.AssetUtils;

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
    private Boolean remote = Boolean.FALSE;

    /**
     * Allows copying of the data fields from this class to the provided {@link LoggedArtifactAssetImpl}.
     *
     * @param asset the {@link LoggedArtifactAssetImpl} instance to be filled.
     */
    public LoggedArtifactAssetImpl copyTo(LoggedArtifactAssetImpl asset) {
        asset.setAssetId(this.assetId);
        asset.setAssetType(this.type);
        asset.setFileName(this.fileName);
        asset.setFileSize(this.fileSize);
        asset.setRemoteUri(this.link);
        asset.setRemote(this.remote);
        asset.setMetadataJson(this.metadata);
        asset.setArtifactVersionId(this.artifactVersionId);
        asset.setArtifactId(this.artifactId);
        return asset;
    }
}
