package ml.comet.experiment.impl.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArtifactVersionAssetResponse {
    List<ArtifactVersionAsset> files;
}
