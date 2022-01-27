package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static ml.comet.experiment.impl.constants.QueryParamName.MODEL_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.STAGE;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;

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

    /**
     * Creates query parameters to be used to download model from the Comet registry.
     *
     * @param workspace    the name of the model's workspace.
     * @param registryName the model's name in the registry.
     * @param options      the additional download options.
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> downloadModelParams(
            @NonNull String workspace, @NonNull String registryName, @NonNull DownloadModelOptions options) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE_NAME, workspace);
        queryParams.put(MODEL_NAME, registryName);
        if (StringUtils.isNotBlank(options.getVersion())) {
            queryParams.put(VERSION, options.getVersion());
        }
        if (StringUtils.isNotBlank(options.getStage())) {
            queryParams.put(STAGE, options.getStage());
        }
        return queryParams;
    }
}
