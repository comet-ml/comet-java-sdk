package ml.comet.experiment.impl.registrymodel;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.vdurmont.semver4j.SemverException;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static ml.comet.experiment.impl.resources.LogMessages.VERSION_AND_STAGE_SET_DOWNLOAD_MODEL;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DownloadModelOptionsTest {
    static final String INCORRECT_VERSION_STRING = "incorrect version string";

    @ParameterizedTest(name = "[{index}] version: {0}, expand: {1}, stage: {2}, throws: {3}")
    @CsvSource({
            "1.0.0,false,,false",
            "1.0.0,true,,false",
            ",false,some_stage,false",
            ",true,some_stage,false",
            "1.0.0,false,some_stage,true",
            "1.0.0,true,some_stage,true",
    })
    public void DownloadModelOptionsBuilder(String version, boolean expand, String stage, boolean exception) {
        DownloadModelOptions.DownloadModelOptionsBuilder builder = DownloadModelOptions.Op()
                .withExpand(expand);

        if (StringUtils.isNotBlank(version)) {
            builder.withVersion(version);
        }
        if (StringUtils.isNotBlank(stage)) {
            builder.withStage(stage);
        }

        if (exception) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
            assertEquals(getString(VERSION_AND_STAGE_SET_DOWNLOAD_MODEL), ex.getMessage(), "wrong exception");
        } else {
            DownloadModelOptions opts = builder.build();
            assertNotNull(opts, "options expected");
            assertEquals(expand, opts.isExpand(), "wrong expand");
            assertEquals(version, opts.getVersion(), "wrong version");
            assertEquals(stage, opts.getStage(), "wrong stage");
        }
    }

    @Test
    public void DownloadModelOptionsBuilder_default() {
        DownloadModelOptions opts = DownloadModelOptions.Op().build();
        assertNotNull(opts, "options expected");
        assertTrue(opts.isExpand(), "must be set");
        assertTrue(StringUtils.isBlank(opts.getVersion()), "no version expected");
        assertTrue(StringUtils.isBlank(opts.getStage()), "no stage expected");
    }

    @Test
    public void DownloadModelOptionsBuilder_wrongVersionString() {
        assertThrows(SemverException.class, () ->
                DownloadModelOptions.Op().withVersion(INCORRECT_VERSION_STRING).build());
    }
}
