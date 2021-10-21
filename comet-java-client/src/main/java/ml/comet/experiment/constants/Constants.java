package ml.comet.experiment.constants;

public class Constants {
    public static final String UPDATE_API_URL = "/api/rest/v2/write";
    public static final String NEW_EXPERIMENT = UPDATE_API_URL + "/experiment/create";
    public static final String EXPERIMENT_STATUS = UPDATE_API_URL + "/experiment/set-status";
    public static final String ADD_METRIC = UPDATE_API_URL + "/experiment/metric";
    public static final String ADD_PARAMETER = UPDATE_API_URL + "/experiment/parameter";
    public static final String ADD_HTML = UPDATE_API_URL + "/experiment/html";
    public static final String ADD_LOG_OTHER = UPDATE_API_URL + "/experiment/log-other";
    public static final String ADD_GRAPH = UPDATE_API_URL + "/experiment/graph";
    public static final String ADD_START_END_TIME = UPDATE_API_URL + "/experiment/set-start-end-time";
    public static final String ADD_ASSET = UPDATE_API_URL + "/experiment/upload-asset";
    public static final String ADD_GIT_METADATA = UPDATE_API_URL + "/experiment/git/metadata";
    public static final String ADD_TAG = UPDATE_API_URL + "/experiment/tags";
    public static final String ADD_OUTPUT = UPDATE_API_URL + "/experiment/output";

    public static final String READ_API_URL = "/api/rest/v2";
    public static final String WORKSPACES = READ_API_URL + "/workspaces";
    public static final String PROJECTS = READ_API_URL + "/projects";
    public static final String EXPERIMENTS = READ_API_URL + "/experiments";
    public static final String GET_GIT_METADATA = READ_API_URL + "/experiment/git/metadata";
    public static final String GET_METADATA = READ_API_URL + "/experiment/metadata";
    public static final String GET_HTML = READ_API_URL + "/experiment/html";
    public static final String GET_OUTPUT = READ_API_URL + "/experiment/output";
    public static final String GET_GRAPH = READ_API_URL + "/experiment/graph";
    public static final String GET_PARAMETERS = READ_API_URL + "/experiment/parameters";
    public static final String GET_METRICS = READ_API_URL + "/experiment/metrics/summary";
    public static final String GET_LOG_OTHER = READ_API_URL + "/experiment/log-other";
    public static final String GET_TAGS = READ_API_URL + "/experiment/tags";
    public static final String GET_ASSET_INFO = READ_API_URL + "/experiment/asset/list";

    public static final String COMET_API_KEY = "apiKey";
    public static final String COMET_PROJECT = "project";
    public static final String COMET_WORKSPACE = "workspace";
    public static final String BASE_URL_PLACEHOLDER = "url";
    public static final String MAX_AUTH_RETRIES_PLACEHOLDER = "maxAuthRetries";
    public static final String BASE_URL_DEFAULT = "https://www.comet.ml";
    public static final int MAX_AUTH_RETRIES_DEFAULT = 4;
    public static final String EXPERIMENT_KEY = "experimentKey";

    public static final String ASSET_TYPE_ALL = "all";
    public static final String ASSET_TYPE_UNKNOWN = "unknown";
    public static final String ASSET_TYPE_SOURCE_CODE = "source_code";


}
