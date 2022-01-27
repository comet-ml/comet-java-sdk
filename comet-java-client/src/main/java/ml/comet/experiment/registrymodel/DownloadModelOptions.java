package ml.comet.experiment.registrymodel;

import com.vdurmont.semver4j.Semver;
import lombok.Getter;

import static ml.comet.experiment.impl.resources.LogMessages.VERSION_AND_STAGE_SET_DOWNLOAD_MODEL;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * Defines options to be used when downloading registry model.
 */
public class DownloadModelOptions {
    private Semver semver;
    @Getter
    private String stage;
    @Getter
    private boolean expand;

    private DownloadModelOptions() {
    }

    /**
     * Returns the model version string or {@code null} if not set.
     *
     * @return the model version string or {@code null} if not set.
     */
    public String getVersion() {
        if (this.semver != null) {
            return this.semver.getValue();
        }
        return null;
    }

    /**
     * Factory to create {@link DownloadModelOptions.DownloadModelOptionsBuilder} which can be used to create
     * properly initialized instance of the {@link DownloadModelOptions}.
     *
     * @return the initialized {@link DownloadModelOptions.DownloadModelOptionsBuilder} instance.
     */
    @SuppressWarnings("checkstyle:MethodName")
    public static DownloadModelOptions.DownloadModelOptionsBuilder Op() {
        return new DownloadModelOptions.DownloadModelOptionsBuilder();
    }

    /**
     * The factory to create properly initialized instance of the {@link DownloadModelOptions}.
     */
    public static final class DownloadModelOptionsBuilder {
        final DownloadModelOptions options;
        Boolean expand;

        private DownloadModelOptionsBuilder() {
            this.options = new DownloadModelOptions();
            // set default value
            this.expand = true;
        }

        /**
         * Creates options with specific model version.
         *
         * @param version the version of the model in the registry.
         * @return the instance of the {@link DownloadModelOptionsBuilder}.
         */
        public DownloadModelOptionsBuilder withVersion(String version) {
            this.options.semver = new Semver(version);
            return this;
        }

        /**
         * Creates options with TAG associated with particular model's version.
         *
         * @param stage the TAG associated with model version, such as: "production", "staging", etc.
         * @return the instance of the {@link DownloadModelOptionsBuilder}.
         */
        public DownloadModelOptionsBuilder withStage(String stage) {
            this.options.stage = stage;
            return this;
        }

        /**
         * Creates options with flag to indicate whether downloaded ZIP with model files should be expanded.
         *
         * @param expand if {@code true} the downloaded ZIP with model files will be unzipped.
         * @return the instance of the {@link DownloadModelOptionsBuilder}.
         */
        public DownloadModelOptionsBuilder withExpand(boolean expand) {
            this.expand = expand;
            return this;
        }

        /**
         * Creates properly initialized instance of the {@link DownloadModelOptions}.
         *
         * @return the properly initialized instance of the {@link DownloadModelOptions}.
         * @throws IllegalArgumentException if simultaneously provided {@code version} and {@code stage} of the model.
         */
        public DownloadModelOptions build() throws IllegalArgumentException {
            if (this.options.stage != null && this.options.semver != null) {
                throw new IllegalArgumentException(getString(VERSION_AND_STAGE_SET_DOWNLOAD_MODEL));
            }
            this.options.expand = this.expand;
            return this.options;
        }
    }
}
