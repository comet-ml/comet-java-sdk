package ml.comet.experiment.artifact;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GetArtifactOptionsTest {

    @ParameterizedTest
    @CsvSource({
            "someName, someName,,",
            "someWorkspace/someName, someName, someWorkspace,",
            "someWorkspace/someName:1.2.3, someName, someWorkspace, 1.2.3",
            "someName:1.2.3, someName,, 1.2.3"
    })
    public void testParseArtifactFullName(String name, String expectedName,
                                          String expectedWorkspace, String expectedVersionOrAlias) {
        GetArtifactOptions opts = GetArtifactOptions.Op().fullName(name).build();
        assertEquals(expectedName, opts.getArtifactName(), "wrong name");
        if (StringUtils.isNotBlank(expectedWorkspace)) {
            assertEquals(expectedWorkspace, opts.getWorkspace(), "wrong workspace");
        } else {
            assertNull(opts.getWorkspace(), "no workspace expected");
        }
        if (StringUtils.isNotBlank(expectedVersionOrAlias)) {
            assertEquals(expectedVersionOrAlias, opts.getVersionOrAlias(), "wrong version or alias");
        } else {
            assertNull(opts.getVersionOrAlias(), "no version or alias expected");
        }
    }

    @Test
    public void testParseArtifactFullName_workspaceOverride() {
        String workspaceOverride = "workspaceOverride";
        String artifactFullName = "someWorkspace/someName:1.2.3";
        GetArtifactOptions opts = GetArtifactOptions.Op()
                .fullName(artifactFullName)
                .workspaceName(workspaceOverride)
                .build();
        assertEquals(workspaceOverride, opts.getWorkspace());
    }

    @Test
    public void testParseArtifactFullName_versionOrAliasOverride() {
        String versionOrAliasOverride = "versionOrAliasOverride";
        String artifactFullName = "someWorkspace/someName:1.2.3";
        GetArtifactOptions opts = GetArtifactOptions.Op()
                .fullName(artifactFullName)
                .versionOrAlias(versionOrAliasOverride)
                .build();
        assertEquals(versionOrAliasOverride, opts.getVersionOrAlias());
    }
}
