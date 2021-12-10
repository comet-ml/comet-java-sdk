package ml.comet.experiment.impl.rest;

import lombok.Data;

/**
 * Base class for all model objects having common experiment identifiers.
 */
@Data
public class BaseExperimentObject {
    String experimentKey;
}
