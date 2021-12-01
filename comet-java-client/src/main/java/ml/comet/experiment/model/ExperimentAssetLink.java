package ml.comet.experiment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
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

}
