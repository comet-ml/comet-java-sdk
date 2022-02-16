package ml.comet.experiment.impl.utils;

import ml.comet.experiment.impl.rest.LogAdditionalSystemInfo;
import ml.comet.experiment.impl.rest.SetSystemDetailsRequest;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Utilities providing access to various system properties.
 */
public class SystemUtils {

    /**
     * Reads available system details.
     *
     * @return the {@link SetSystemDetailsRequest} populated with available system details.
     */
    public static SetSystemDetailsRequest readSystemDetails() {
        SetSystemDetailsRequest request = new SetSystemDetailsRequest();
        request.setUser(org.apache.commons.lang3.SystemUtils.USER_NAME);
        request.setOs(org.apache.commons.lang3.SystemUtils.OS_NAME);
        request.setOsRelease(org.apache.commons.lang3.SystemUtils.OS_VERSION);
        request.setMachine(org.apache.commons.lang3.SystemUtils.OS_ARCH);

        // set additional system info
        ArrayList<LogAdditionalSystemInfo> infoList = new ArrayList<>();
        Properties props = System.getProperties();
        props.forEach((k, v) -> infoList.add(new LogAdditionalSystemInfo(k.toString(), v.toString())));
        request.setLogAdditionalSystemInfoList(infoList);

        return request;
    }
}
