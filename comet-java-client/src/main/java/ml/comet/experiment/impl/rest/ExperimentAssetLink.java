package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.utils.AssetUtils;
import ml.comet.experiment.impl.utils.DataModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
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
     * Converts this into {@link LoggedExperimentAsset} exposed by public API.
     *
     * @return the initialized {@link LoggedExperimentAsset} instance.
     */
    public LoggedExperimentAsset toExperimentAsset(Logger logger) {
        LoggedExperimentAsset a = new LoggedExperimentAsset();
        a.setAssetId(this.assetId);
        a.setLogicalPath(this.fileName);
        a.setLink(this.link);
        a.setRemote(this.remote);
        a.setFileSize(this.fileSize);
        a.setExperimentContext(this.readContext());
        a.setMetadata(this.parseMetadata(logger));
        a.setType(AssetUtils.toAssetType(this.type));
        a.setCurlDownload(this.curlDownload);

        if (this.createdAt != null) {
            a.setCreatedAt(Instant.ofEpochMilli(this.createdAt.getTime()));
        }

        return a;
    }

    private ExperimentContext readContext() {
        long ctxStep = 0;
        String ctxStr = "";
        if (this.step != null) {
            ctxStep = this.step;
        }
        if (StringUtils.isNotBlank(this.runContext)) {
            ctxStr = this.runContext;
        }

        return new ExperimentContext(ctxStep, 0, ctxStr);
    }

    private Map<String, Object> parseMetadata(Logger logger) {
        if (StringUtils.isNotBlank(this.metadata)) {
            try {
                return DataModelUtils.metadataFromJson(this.metadata);
            } catch (Throwable e) {
                logger.error("Failed to parse experiment's asset metadata from JSON {}", this.metadata, e);
            }
        }
        return Collections.emptyMap();
    }
}
