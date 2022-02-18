package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SetSystemDetailsRequest {
    private String experimentKey;
    private String user;
    private String pythonVersion;
    private String pythonVersionVerbose;
    private Integer pid;
    private String osType;
    private String os;
    private String osRelease;
    private String machine;
    private String processor;
    private String ip;
    private String hostname;
    private JsonNode env;
    private List<GpuStaticInfoRest> gpuStaticInfoList;
    private List<LogAdditionalSystemInfo> logAdditionalSystemInfoList;
    private List<String> networkInterfaceIps;
    private List<String> command;
    private String executable;
    private List<String> osPackages;
    private List<String> installedPackages;
}
