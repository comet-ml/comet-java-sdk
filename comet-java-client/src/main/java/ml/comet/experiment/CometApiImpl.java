package ml.comet.experiment;

import lombok.experimental.UtilityClass;
import ml.comet.experiment.builder.CometApiBuilder;

/**
 * This is stub to support backward compatibility.
 *
 * @deprecated It would be replaced in the future with new experiment creation API.
 */
@UtilityClass
public final class CometApiImpl {
    /**
     * Returns builder to be used to properly create instance of {@link CometApi}.
     *
     * @return the builder to be used to properly create instance of {@link CometApi}.
     */
    public static CometApiBuilder builder() {
        return ml.comet.experiment.impl.CometApiImpl.builder();
    }
}
