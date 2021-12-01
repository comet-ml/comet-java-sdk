package ml.comet.experiment.impl.http;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * The factory to build properly initialized connections.
 */
@UtilityClass
public class ConnectionInitializer {

    /**
     * Builds properly configured Connection instance.
     *
     * @param apiKey         the Comet API key
     * @param cometBaseUrl   the base URL of the Comet REST API server
     * @param maxAuthRetries the maximum number of authentication retries.
     * @param logger         the logger to be used for logging
     * @return the properly initialized Connection instance.
     */
    public Connection initConnection(String apiKey, String cometBaseUrl, int maxAuthRetries, Logger logger) {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("Api key required!");
        }
        return new Connection(cometBaseUrl, apiKey, maxAuthRetries, logger);
    }
}
