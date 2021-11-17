package ml.comet.experiment.impl.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * Collection of utilities to process JSON-to-object mappings.
 */
@UtilityClass
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public String toJson(Object object) {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    @SneakyThrows
    public <T> T fromJson(String json, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

}
