package ml.comet.experiment.impl.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.Map;

/**
 * Describes asset data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private File file;
    private String fileName;
    private AssetType type;
    private boolean overwrite;
    private long step;
    private String groupingName;
    private Map<String, Object> metadata;
    private String assetId;
}
