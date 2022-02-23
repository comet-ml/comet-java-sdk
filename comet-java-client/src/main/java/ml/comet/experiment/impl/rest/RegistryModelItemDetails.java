package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.registrymodel.ModelVersionOverview;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistryModelItemDetails {
    private String registryModelItemId;
    private ExperimentModelResponse experimentModel;
    private String version;
    private String comment;
    private List<String> stages;
    private List<ExperimentAssetLink> assets;
    private String userName;
    private long createdAt;
    private long lastUpdated;
    private String restApiUrl;

    /**
     * Converts this into {@link ModelVersionOverview}.
     *
     * @param logger the logger to be used for output.
     * @return the initialized {@link ModelVersionOverview} instance.
     */
    public ModelVersionOverview toModelVersionOverview(Logger logger) {
        ModelVersionOverview model = new ModelVersionOverview();
        model.setRegistryModelItemId(this.registryModelItemId);
        model.setVersion(this.version);
        model.setComment(this.comment);
        model.setStages(this.stages);
        model.setUserName(this.userName);
        model.setRestApiUrl(this.restApiUrl);
        if (this.createdAt > 0) {
            model.setCreatedAt(Instant.ofEpochMilli(this.createdAt));
        }
        if (this.lastUpdated > 0) {
            model.setLastUpdated(Instant.ofEpochMilli(this.lastUpdated));
        }
        if (this.assets != null) {
            ArrayList<LoggedExperimentAsset> loggedAssets = this.assets
                    .stream().collect(
                            ArrayList::new,
                            (experimentAssets, assetLink) -> experimentAssets.add(assetLink.toExperimentAsset(logger)),
                            ArrayList::addAll);
            model.setAssets(loggedAssets);
        }
        return model;
    }
}
