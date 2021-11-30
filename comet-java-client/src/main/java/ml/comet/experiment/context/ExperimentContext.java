package ml.comet.experiment.context;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Describes context of the {@link ml.comet.experiment.Experiment}.
 */
@Data
public final class ExperimentContext {
    private long step;
    private long epoch;
    private String context;

    ExperimentContext() {
    }

    /**
     * Creates new instance with specified parameters.
     *
     * @param step    the current step of the experiment.
     * @param epoch   the current epoch of the experiment.
     * @param context the current context of the data log operation.
     */
    public ExperimentContext(long step, long epoch, String context) {
        this.step = step;
        this.epoch = epoch;
        this.context = context;
    }

    /**
     * Creates new instance with specified parameters.
     *
     * @param step  the current step of the experiment.
     * @param epoch the current epoch of the experiment.
     */
    public ExperimentContext(long step, long epoch) {
        this(step, epoch, StringUtils.EMPTY);
    }

    /**
     * Creates new instance with specified parameters.
     *
     * @param step the current step of the experiment.
     */
    public ExperimentContext(long step) {
        this(step, 0);
    }

    /**
     * Returns builder to create populated instance of the {@link ExperimentContext}.
     *
     * @return the builder to create populated instance of the {@link ExperimentContext}.
     */
    public static ExperimentContextBuilder builder() {
        return new ExperimentContextBuilder();
    }

    /**
     * Builder to create populated instance of the {@link ExperimentContext}.
     */
    public static final class ExperimentContextBuilder {
        private final ExperimentContext context;

        ExperimentContextBuilder() {
            this.context = new ExperimentContext();
        }

        /**
         * Populates context with specified step of the experiment.
         *
         * @param step the experiment's step.
         * @return the instance of this builder.
         */
        public ExperimentContextBuilder withStep(long step) {
            this.context.step = step;
            return this;
        }

        /**
         * Populates context with specified epoch of the experiment.
         *
         * @param epoch the epoch of the experiment.
         * @return the instance of this builder.
         */
        public ExperimentContextBuilder withEpoch(long epoch) {
            this.context.epoch = epoch;
            return this;
        }

        /**
         * Populates context with specified context identifier string.
         *
         * @param context the context identifier string.
         * @return the instance of this builder.
         */
        public ExperimentContextBuilder withContext(String context) {
            this.context.context = context;
            return this;
        }

        /**
         * Creates fully initialized {@link ExperimentContext} instance.
         *
         * @return the fully initialized {@link ExperimentContext} instance.
         */
        public ExperimentContext build() {
            return this.context;
        }
    }
}
