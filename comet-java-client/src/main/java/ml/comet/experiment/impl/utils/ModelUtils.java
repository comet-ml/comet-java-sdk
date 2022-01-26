package ml.comet.experiment.impl.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * The collection of utilities to work with registry models.
 */
public class ModelUtils {

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
}
