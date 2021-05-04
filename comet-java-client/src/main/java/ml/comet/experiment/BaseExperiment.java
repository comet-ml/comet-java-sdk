package ml.comet.experiment;

import lombok.RequiredArgsConstructor;
import ml.comet.experiment.constants.Constants;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.model.AddGraphRest;
import ml.comet.experiment.model.AddTagsToExperimentRest;
import ml.comet.experiment.model.CreateGitMetadata;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentAssetListResponse;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.ExperimentTimeRequest;
import ml.comet.experiment.model.GetGraphResponse;
import ml.comet.experiment.model.GetHtmlResponse;
import ml.comet.experiment.model.GetOutputResponse;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.model.LogOtherRest;
import ml.comet.experiment.model.MetricRest;
import ml.comet.experiment.model.MinMaxResponse;
import ml.comet.experiment.model.ParameterRest;
import ml.comet.experiment.model.TagsResponse;
import ml.comet.experiment.model.ValueMinMaxDto;
import ml.comet.experiment.utils.JsonUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ml.comet.experiment.constants.Constants.ADD_ASSET;
import static ml.comet.experiment.constants.Constants.ADD_GIT_METADATA;
import static ml.comet.experiment.constants.Constants.ADD_GRAPH;
import static ml.comet.experiment.constants.Constants.ADD_HTML;
import static ml.comet.experiment.constants.Constants.ADD_LOG_OTHER;
import static ml.comet.experiment.constants.Constants.ADD_METRIC;
import static ml.comet.experiment.constants.Constants.ADD_PARAMETER;
import static ml.comet.experiment.constants.Constants.ADD_START_END_TIME;
import static ml.comet.experiment.constants.Constants.ADD_TAG;
import static ml.comet.experiment.constants.Constants.ASSET_TYPE_SOURCE_CODE;
import static ml.comet.experiment.constants.Constants.EXPERIMENT_KEY;
import static ml.comet.experiment.constants.Constants.GET_ASSET_INFO;
import static ml.comet.experiment.constants.Constants.GET_GIT_METADATA;
import static ml.comet.experiment.constants.Constants.GET_GRAPH;
import static ml.comet.experiment.constants.Constants.GET_HTML;
import static ml.comet.experiment.constants.Constants.GET_LOG_OTHER;
import static ml.comet.experiment.constants.Constants.GET_METADATA;
import static ml.comet.experiment.constants.Constants.GET_METRICS;
import static ml.comet.experiment.constants.Constants.GET_OUTPUT;
import static ml.comet.experiment.constants.Constants.GET_PARAMETERS;
import static ml.comet.experiment.constants.Constants.GET_TAGS;

@RequiredArgsConstructor
public abstract class BaseExperiment implements Experiment {

    protected abstract String getContext();

    protected abstract Connection getConnection();

    protected abstract Logger getLogger();

    @Override
    public void setExperimentName(String experimentName) {
        logOther("Name", experimentName);
    }

    @Override
    public void logMetric(String metricName, Object metricValue, long step) {
        getLogger().debug("logMetric {} {}", metricName, metricValue);
        validateExperimentKeyPresent();

        MetricRest request = getLogMetricRequest(metricName, metricValue, step);
        getConnection().sendPostAsync(request, ADD_METRIC);
    }

    @Override
    public void logParameter(String parameterName, Object paramValue, long step) {
        getLogger().debug("logParameter {} {}", parameterName, paramValue);
        validateExperimentKeyPresent();

        ParameterRest request = getLogParameterRequest(parameterName, paramValue, step);
        getConnection().sendPostAsync(request, ADD_PARAMETER);
    }

    @Override
    public void logHtml(String html, boolean override) {
        getLogger().debug("logHtml {} {}", html, override);
        validateExperimentKeyPresent();

        HtmlRest request = getLogHtmlRequest(html, override);
        getConnection().sendPostAsync(request, ADD_HTML);
    }

    @Override
    public void logCode(String code, String fileName) {
        getLogger().debug("log raw code");

        validateExperimentKeyPresent();

        getConnection().sendPostAsync(code.getBytes(StandardCharsets.UTF_8),
                ADD_ASSET,
                new HashMap<String, String>() {{
                    put(EXPERIMENT_KEY, getExperimentKey());
                    put("fileName", fileName);
                    put("context", getContext());
                    put("type", ASSET_TYPE_SOURCE_CODE);
                    put("overwrite", Boolean.toString(false));
                }});
    }

    @Override
    public void logCode(File asset) {
        getLogger().debug("logCode {}", asset.getName());
        validateExperimentKeyPresent();

        getConnection().sendPostAsync(asset, ADD_ASSET, new HashMap<String, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put("fileName", asset.getName());
            put("context", getContext());
            put("type", ASSET_TYPE_SOURCE_CODE);
            put("overwrite", Boolean.toString(false));
        }});
    }


    @Override
    public void logOther(String key, Object value) {
        getLogger().debug("logOther {} {}", key, value);
        validateExperimentKeyPresent();

        LogOtherRest request = getLogOtherRequest(key, value);
        getConnection().sendPostAsync(request, ADD_LOG_OTHER);
    }

    @Override
    public void addTag(String tag) {
        getLogger().debug("logTag {}", tag);
        validateExperimentKeyPresent();

        AddTagsToExperimentRest request = getTagRequest(tag);
        getConnection().sendPostAsync(request, ADD_TAG);
    }

    @Override
    public void logGraph(String graph) {
        getLogger().debug("logOther {}", graph);
        validateExperimentKeyPresent();

        AddGraphRest request = getGraphRequest(graph);
        getConnection().sendPostAsync(request, ADD_GRAPH);
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        getLogger().debug("logStartTime {}", startTimeMillis);
        validateExperimentKeyPresent();

        ExperimentTimeRequest request = getLogStartTimeRequest(startTimeMillis);
        getConnection().sendPostAsync(request, ADD_START_END_TIME);
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        getLogger().debug("logEndTime {}", endTimeMillis);
        validateExperimentKeyPresent();

        ExperimentTimeRequest request = getLogEndTimeRequest(endTimeMillis);
        getConnection().sendPostAsync(request, ADD_START_END_TIME);
    }

    @Override
    public void uploadAsset(File asset, String fileName, boolean overwrite, long step) {
        getLogger().debug("uploadAsset {} {} {}", asset.getName(), fileName, overwrite);
        validateExperimentKeyPresent();

        getConnection().sendPostAsync(asset, ADD_ASSET, new HashMap<String, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put("fileName", fileName);
            put("step", Long.toString(step));
            put("context", getContext());
            put("overwrite", Boolean.toString(overwrite));
        }});
    }

    @Override
    public void uploadAsset(File asset, boolean overwrite, long step) {
        uploadAsset(asset, asset.getName(), overwrite, step);
    }

    @Override
    public void logGitMetadata(CreateGitMetadata gitMetadata) {
        getLogger().debug("gitMetadata {}", gitMetadata);
        validateExperimentKeyPresent();

        getConnection().sendPostAsync(gitMetadata, ADD_GIT_METADATA);
    }

    @Override
    public ExperimentMetadataRest getMetadata() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get metadata for experiment {}", experimentKey);

        return getForExperimentByKey(GET_METADATA, ExperimentMetadataRest.class);
    }

    @Override
    public GitMetadataRest getGitMetadata() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get git metadata for experiment {}", experimentKey);

        return getForExperimentByKey(GET_GIT_METADATA, GitMetadataRest.class);
    }

    @Override
    public Optional<String> getHtml() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get html for experiment {}", experimentKey);

        GetHtmlResponse response = getForExperimentByKey(GET_HTML, GetHtmlResponse.class);
        return Optional.ofNullable(response.getHtml());
    }

    @Override
    public Optional<String> getOutput() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get output for experiment {}", experimentKey);

        GetOutputResponse response = getForExperimentByKey(GET_OUTPUT, GetOutputResponse.class);
        return Optional.ofNullable(response.getOutput());
    }

    @Override
    public Optional<String> getGraph() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get graph for experiment {}", experimentKey);

        GetGraphResponse response = getForExperimentByKey(GET_GRAPH, GetGraphResponse.class);
        return Optional.ofNullable(response.getGraph());
    }

    @Override
    public List<ValueMinMaxDto> getParameters() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get params for experiment {}", experimentKey);

        MinMaxResponse response = getForExperimentByKey(GET_PARAMETERS, MinMaxResponse.class);
        return response.getValues();
    }

    @Override
    public List<ValueMinMaxDto> getMetrics() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get metrics summary for experiment {}", experimentKey);

        MinMaxResponse response = getForExperimentByKey(GET_METRICS, MinMaxResponse.class);
        return response.getValues();
    }

    @Override
    public List<ValueMinMaxDto> getLogOther() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get log other for experiment {}", experimentKey);

        MinMaxResponse response = getForExperimentByKey(GET_LOG_OTHER, MinMaxResponse.class);
        return response.getValues();
    }

    @Override
    public List<String> getTags() {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get tags for experiment {}", experimentKey);

        TagsResponse response = getForExperimentByKey(GET_TAGS, TagsResponse.class);
        return response.getTags();
    }

    @Override
    public List<ExperimentAssetLink> getAssetList(String type) {
        String experimentKey = validateAndGetExperimentKey();
        getLogger().debug("get tags for experiment {}", experimentKey);

        HashMap<String, String> params = new HashMap<String, String>() {{
            put("experimentKey", experimentKey);
            put("type", type);
        }};
        ExperimentAssetListResponse response = getForExperiment(GET_ASSET_INFO, params, ExperimentAssetListResponse.class);
        return response.getAssets();
    }

    private <T> T getForExperimentByKey(String endpoint, Class<T> clazz) {
        return getForExperiment(endpoint, Collections.singletonMap("experimentKey", getExperimentKey()), clazz);
    }

    private <T> T getForExperiment(String endpoint, Map<String, String> params, Class<T> clazz) {
        return getConnection().sendGet(endpoint, params)
                .map(body -> JsonUtils.fromJson(body, clazz))
                .orElseThrow(() -> new IllegalArgumentException("Empty response received for experiment from " + endpoint));
    }

    private String getObjectValue(Object val) {
        return val.toString();
    }

    private void validateExperimentKeyPresent() {
        if (getExperimentKey() == null) {
            throw new IllegalStateException("Experiment key must be present!");
        }
    }

    private String validateAndGetExperimentKey() {
        validateExperimentKeyPresent();
        return getExperimentKey();
    }

    private MetricRest getLogMetricRequest(String metricName, Object metricValue, long step) {
        MetricRest request = new MetricRest();
        request.setExperimentKey(getExperimentKey());
        request.setMetricName(metricName);
        request.setMetricValue(getObjectValue(metricValue));
        request.setStep(step);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private ParameterRest getLogParameterRequest(String parameterName, Object paramValue, long step) {
        ParameterRest request = new ParameterRest();
        request.setExperimentKey(getExperimentKey());
        request.setParameterName(parameterName);
        request.setParameterValue(getObjectValue(paramValue));
        request.setStep(step);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private HtmlRest getLogHtmlRequest(String html, boolean override) {
        HtmlRest request = new HtmlRest();
        request.setExperimentKey(getExperimentKey());
        request.setHtml(html);
        request.setOverride(override);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private LogOtherRest getLogOtherRequest(String key, Object value) {
        LogOtherRest request = new LogOtherRest();
        request.setExperimentKey(getExperimentKey());
        request.setKey(key);
        request.setValue(getObjectValue(value));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private AddTagsToExperimentRest getTagRequest(String tag) {
        AddTagsToExperimentRest request = new AddTagsToExperimentRest();
        request.setExperimentKey(getExperimentKey());
        request.setAddedTags(Collections.singletonList(tag));
        return request;
    }

    private AddGraphRest getGraphRequest(String graph) {
        AddGraphRest request = new AddGraphRest();
        request.setExperimentKey(getExperimentKey());
        request.setGraph(graph);
        return request;
    }

    private ExperimentTimeRequest getLogStartTimeRequest(long startTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setExperimentKey(getExperimentKey());
        request.setStartTimeMillis(startTimeMillis);
        return request;
    }

    private ExperimentTimeRequest getLogEndTimeRequest(long endTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setExperimentKey(getExperimentKey());
        request.setEndTimeMillis(endTimeMillis);
        return request;
    }


}
