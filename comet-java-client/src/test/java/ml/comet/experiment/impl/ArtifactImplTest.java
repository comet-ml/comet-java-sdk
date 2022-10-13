package ml.comet.experiment.impl;

import com.vdurmont.semver4j.SemverException;
import lombok.NonNull;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.ConflictingArtifactAssetNameException;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases to test {@link ml.comet.experiment.artifact.Artifact} implementation.
 */
@DisplayName("Artifact")
public class ArtifactImplTest extends AssetsBaseTest {

    static final String SOME_ARTIFACT_NAME = "artifactName";
    static final String SOME_ARTIFACT_TYPE = "artifactType";
    static final List<String> SOME_ALIASES = Arrays.asList("one", "two", "three", "three");
    static final Set<String> UNIQUE_ALIASES = new HashSet<>(SOME_ALIASES);
    static final List<String> SOME_TAGS = Arrays.asList("tag_1", "tag_2", "tag_3", "tag_3");
    static final Set<String> UNIQUE_TAGS = new HashSet<>(SOME_TAGS);
    static final String SOME_VERSION = "1.2.3-beta.4+sha899d8g79f87";
    static final String INVALID_VERSION = "1.2";
    static final Map<String, Object> SOME_METADATA = new HashMap<String, Object>() {{
        put("someString", "string");
        put("someInt", 10);
    }};
    static final String SOME_REMOTE_ASSET_LINK = "s3://bucket/folder/someFile";
    static final String SOME_REMOTE_ASSET_NAME = "someRemoteAsset";

    @Test
    @DisplayName("is created with newArtifact()")
    void isCreatedWithNewArtifact() {
        ArtifactImpl artifact = (ArtifactImpl) Artifact
                .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                .build();
        assertNotNull(artifact);
    }

    @Test
    @DisplayName("is created with newArtifact().withAliases()")
    void isCreatedWithNewArtifact_WithAliases() {
        ArtifactImpl artifact = (ArtifactImpl) Artifact
                .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                .withAliases(SOME_ALIASES)
                .build();
        assertNotNull(artifact);
        assertEquals(UNIQUE_ALIASES, artifact.getAliases());
    }

    @Test
    @DisplayName("is created with newArtifact().withMetadata()")
    void isCreatedWithNewArtifact_withMetadata() {
        ArtifactImpl artifact = (ArtifactImpl) Artifact
                .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                .withMetadata(SOME_METADATA)
                .build();
        assertNotNull(artifact);
        assertEquals(SOME_METADATA, artifact.getMetadata());
    }

    @Test
    @DisplayName("is created with newArtifact().withVersionTags()")
    void isCreatedWithNewArtifact_withVersionTags() {
        ArtifactImpl artifact = (ArtifactImpl) Artifact
                .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                .withVersionTags(SOME_TAGS)
                .build();
        assertNotNull(artifact);
        assertEquals(UNIQUE_TAGS, artifact.getVersionTags());
    }

    @Test
    @DisplayName("is created with newArtifact().withVersion()")
    void isCreatedWithNewArtifact_withVersion() {
        ArtifactImpl artifact = (ArtifactImpl) Artifact
                .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                .withVersion(SOME_VERSION)
                .build();
        assertNotNull(artifact);
        assertTrue(artifact.getSemanticVersion().isEqualTo(SOME_VERSION));
    }

    @Test
    @DisplayName("is created with newArtifact().withVersion() throws exception with wrong version format")
    void isCreatedWithNewArtifact_withVersion_throwsException_wrongVersion() {
        assertThrows(SemverException.class, () -> Artifact
                .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                .withVersion(INVALID_VERSION)
                .build()
        );
    }

    @Nested
    @DisplayName("when new")
    class WhenNewTest {
        ArtifactImpl artifact;

        @BeforeEach
        void createNewArtifact() {
            this.artifact = (ArtifactImpl) Artifact
                    .newArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE)
                    .build();
        }

        @Test
        @DisplayName("artifact name and type is set")
        void nameAndTypeIsSet() {
            assertEquals(SOME_ARTIFACT_NAME, this.artifact.getName());
            assertEquals(SOME_ARTIFACT_TYPE, this.artifact.getType());
        }

        @Test
        @DisplayName("artifact has no assets")
        void hasNoAssets() {
            assertEquals(0, this.artifact.getAssets().size());
        }

        @Nested
        @DisplayName("after adding file asset")
        class AfterAddingFileAssetTest {
            File assetFile;
            String assetFileName;
            final boolean overwrite = true;

            @BeforeEach
            void addFileAsset() {
                this.assetFile = assetFolderFiles.get(0).toFile();
                this.assetFileName = this.assetFile.getName();
                artifact.addAsset(this.assetFile, this.overwrite, SOME_METADATA);
            }

            @Test
            @DisplayName("has correct file asset")
            void hasFileAsset() {
                ArtifactAssetImpl asset = (ArtifactAssetImpl) artifact.findAsset(this.assetFileName);
                assertEquals(this.assetFile, asset.getRawFile(), "wrong file");
                assertEquals(this.assetFileName, asset.getLogicalPath(), "wrong file name");
                assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");
                assertEquals(this.overwrite, asset.getOverwrite(), "wrong overwrite");
            }

            @Test
            @DisplayName("throws ConflictingArtifactAssetName when adding asset with existing name")
            void throwsExceptionWhenAddingSameName() {
                assertThrows(ConflictingArtifactAssetNameException.class, () ->
                        artifact.addAsset(this.assetFile, false));
            }
        }

        @Nested
        @DisplayName("after adding file-like asset")
        class AfterAddingFileLikeAssetTest {
            byte[] data;
            String assetFileName;
            final boolean overwrite = true;

            @BeforeEach
            void addFileLikeAsset() {
                this.data = "some asset data".getBytes(StandardCharsets.UTF_8);
                this.assetFileName = "someAssetFileName";
                artifact.addAsset(this.data, this.assetFileName, this.overwrite, SOME_METADATA);
            }

            @Test
            @DisplayName("has correct file-like asset")
            void hasFileLikeAsset() {
                ArtifactAssetImpl asset = (ArtifactAssetImpl) artifact.findAsset(this.assetFileName);
                assertEquals(this.data, asset.getRawFileLikeData(), "wrong data");
                assertEquals(this.assetFileName, asset.getLogicalPath(), "wrong file name");
                assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");
                assertEquals(this.overwrite, asset.getOverwrite(), "wrong overwrite");
            }

            @Test
            @DisplayName("throws ConflictingArtifactAssetName when adding asset with existing name")
            void throwsExceptionWhenAddingSameName() {
                assertThrows(ConflictingArtifactAssetNameException.class, () ->
                        artifact.addAsset(this.data, this.assetFileName));
            }
        }

        @Nested
        @DisplayName("after adding remote asset")
        class AfterAddingRemoteAssetTest {
            URI uri;
            String assetFileName;
            final boolean overwrite = true;

            @BeforeEach
            void addRemoteAsset() throws URISyntaxException {
                this.uri = new URI(SOME_REMOTE_ASSET_LINK);
                this.assetFileName = SOME_REMOTE_ASSET_NAME;
                artifact.addRemoteAsset(this.uri, this.assetFileName, this.overwrite, SOME_METADATA);
            }

            @Test
            @DisplayName("has correct remote asset")
            void hasRemoteAsset() {
                ArtifactAssetImpl asset = (ArtifactAssetImpl) artifact.findAsset(this.assetFileName);
                assertTrue(asset.isRemote(), "must be remote");
                assertEquals(this.uri, asset.getUri(), "wrong link");
                assertEquals(this.assetFileName, asset.getLogicalPath(), "wrong file name");
                assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");
                assertEquals(this.overwrite, asset.getOverwrite(), "wrong overwrite");
            }

            @Test
            @DisplayName("throws ConflictingArtifactAssetName when adding asset with existing name")
            void throwsExceptionWhenAddingSameName() {
                assertThrows(ConflictingArtifactAssetNameException.class, () ->
                        artifact.addRemoteAsset(this.uri, this.assetFileName));
            }
        }

        @Nested
        @DisplayName("after adding assets folder")
        class AfterAddingAssetsFolderTest {
            final boolean logFilePath = true;
            final boolean recursive = true;

            @BeforeEach
            void addAssetsFolder() throws IOException {
                artifact.addAssetFolder(assetsFolder.toFile(), logFilePath, recursive, SOME_METADATA);
            }

            @Test
            @DisplayName("has all assets in the folder")
            void hasAllFolderAssetsAdded() {
                assertEquals(assetFolderFiles.size(), artifact.getAssets().size(), "wrong number of assets");
                artifact.getAssets().forEach(this::validateAsset);
            }

            @Test
            @DisplayName("throws ConflictingArtifactAssetName when adding asset with existing name")
            void throwsExceptionWhenAddingSameName() {
                assertThrows(ConflictingArtifactAssetNameException.class, () ->
                        artifact.addAssetFolder(assetsFolder.toFile(), logFilePath, recursive));
            }

            void validateAsset(@NonNull Asset asset) {
                assertTrue(asset instanceof ArtifactAsset, "wrong asset class");
                assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");
                assertTrue(
                        assetFolderFiles
                                .stream()
                                .anyMatch(path -> path.endsWith(asset.getLogicalPath()))
                );
            }
        }
    }
}
