package ml.comet.experiment.http;

import com.typesafe.config.Config;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.constants.Constants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@UtilityClass
public class ConnectionInitializer {

    public Connection initConnection(Config config, String apiKey, Logger logger) {
        String cometBaseUrl = config.getString(Constants.BASE_URL_PLACEHOLDER);
        int maxAuthRetries = config.getInt(Constants.MAX_AUTH_RETRIES_PLACEHOLDER);
        if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Api key required!");
        }
        return new Connection(cometBaseUrl, apiKey, logger, maxAuthRetries);
    }
}
