package ml.comet.experiment.impl.utils;

import ml.comet.experiment.registrymodel.DownloadModelOptions;
import org.apache.commons.lang3.StringUtils;

/**
 * The collection of utilities to work with registry models.
 */
public class ModelUtils {

    static final String DEFAULT_MODEL_NAME_SUFFIX = "latest";

    /**
     * Creates the proper model name to be used in the Comet registry from provided experiment model name.
     * A proper registry model name is:
     * <ul>
     * <li>lowercase</li>
     * <li>replaces all non-alphanumeric with dashes</li>
     * <li>removes leading and trailing dashes</li>
     * <li>limited to one dash in a sequence</li>
     * </ul>
     *
     * @param modelName the experiment model name.
     * @return the proper model registry name.
     */
    public static String createRegistryModelName(String modelName) {
        modelName = StringUtils.lowerCase(modelName);
        StringBuilder builder = new StringBuilder();
        final int sz = modelName.length();
        for (int i = 0; i < sz; i++) {
            char c = modelName.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            } else {
                builder.append("-");
            }
        }

        // remove extra dashes
        modelName = builder.toString();
        modelName = modelName.replaceAll("--*", "-");

        // remove first/last dashes
        if (modelName.startsWith("-")) {
            modelName = modelName.substring(1);
        }
        if (modelName.endsWith("-")) {
            modelName = modelName.substring(0, modelName.length() - 1);
        }

        return modelName;
    }

    /**
     * Creates standard name of the ZIP file for the registry model.
     *
     * @param registryName the name of model in registry.
     * @param options      the {@link DownloadModelOptions} with additional options.
     * @return the standard name of the ZIP file for the registry model.
     */
    public static String registryModelZipFileName(String registryName, DownloadModelOptions options) {
        String suffix;
        if (StringUtils.isNotBlank(options.getVersion())) {
            suffix = options.getVersion();
        } else if (StringUtils.isNotBlank(options.getStage())) {
            suffix = options.getStage();
        } else {
            suffix = DEFAULT_MODEL_NAME_SUFFIX;
        }
        return String.format("%s_%s.zip", registryName, suffix);
    }

}
