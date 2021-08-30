package ml.comet.experiment.http;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@UtilityClass
public class ConnectionInitializer {

    public Connection initConnection(String apiKey, String cometBaseUrl, int maxAuthRetries, Logger logger) {
        if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Api key required!");
        }
        return new Connection(cometBaseUrl, apiKey, logger, maxAuthRetries);
    }
}
