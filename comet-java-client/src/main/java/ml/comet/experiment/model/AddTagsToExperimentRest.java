package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTagsToExperimentRest {
    private String experimentKey;
    private List<String> addedTags;
}
