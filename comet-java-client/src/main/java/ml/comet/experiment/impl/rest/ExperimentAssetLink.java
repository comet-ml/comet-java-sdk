package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.model.ExperimentAsset;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentAssetLink {
    private String fileName;
    private Long fileSize;
    private String runContext;
    private Long step;
    private boolean remote = false;
    private String link;
    private String compressedAssetLink;
    private Timestamp createdAt;
    private String dir;
    private boolean canView = false;
    private boolean audio = false;
    private boolean video = false;
    private boolean histogram = false;
    private boolean image = false;
    private String type;
    private String metadata;
    private String assetId;
    private List<String> tags;
    private String curlDownload;

    /**
     * Converts this into {@link ExperimentAsset} exposed by public API.
     *
     * @return the initialized {@link ExperimentAsset} instance.
     */
    public ExperimentAsset toExperimentAsset() {
        ExperimentAsset a = new ExperimentAsset();
        a.setAssetId(this.assetId);
        a.setFileName(this.fileName);
        a.setLink(this.link);
        a.setRemote(this.remote);
        a.setFileSize(this.fileSize);
        a.setStep(this.step);
        if (this.createdAt != null) {
            a.setCreatedAt(Instant.ofEpochMilli(this.createdAt.getTime()));
        }
        a.setType(this.type);
        a.setMetadataJson(this.metadata);
        a.setCurlDownload(this.curlDownload);
        return a;
    }
}