package ml.comet.experiment.impl.utils;

import ml.comet.experiment.impl.rest.LogAdditionalSystemInfo;
import ml.comet.experiment.impl.rest.SetSystemDetailsRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Utilities providing access to various system properties.
 */
public class SystemUtils {

    static final List<String> extraKeys = Arrays.asList(
            "java.version",
            "java.version.date",
            "java.vendor",
            "java.vendor.version",
            "java.vendor.url",

            "java.runtime.name",
            "java.runtime.version",

            "java.specification.name",
            "java.specification.version",
            "java.specification.vendor",

            "java.home",
            "jdk.debug",
            "java.class.version",

            "java.vm.name",
            "java.vm.version",
            "java.vm.vendor",
            "java.vm.info",
            "java.vm.specification.name",
            "java.vm.specification.version",
            "java.vm.specification.vendor",

            "sun.management.compiler",
            "sun.cpu.endian",
            "sun.cpu.isalist",
            "sun.jnu.encoding",
            "sun.arch.data.model",
            "sun.boot.library.path",
            "sun.os.patch.level",
            "sun.io.unicode.encoding",
            "sun.java.launcher",
            "sun.java.command",

            "http.nonProxyHosts",
            "ftp.nonProxyHosts",
            "socksNonProxyHosts",

            "file.encoding",
            "user.timezone",
            "user.country",
            "user.language",
            "user.dir"
    );

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

        extraKeys.forEach(key -> {
                    if (props.containsKey(key)) {
                        infoList.add(new LogAdditionalSystemInfo(key, props.getProperty(key)));
                    }
                }
        );
        request.setLogAdditionalSystemInfoList(infoList);

        return request;
    }
}
