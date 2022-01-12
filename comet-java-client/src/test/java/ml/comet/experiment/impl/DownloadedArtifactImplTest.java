package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.asset.AssetType;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static ml.comet.experiment.impl.ArtifactImplTest.SOME_ARTIFACT_NAME;
import static ml.comet.experiment.impl.ArtifactImplTest.SOME_ARTIFACT_TYPE;
import static ml.comet.experiment.impl.ArtifactImplTest.SOME_METADATA;
import static ml.comet.experiment.impl.ArtifactImplTest.SOME_REMOTE_ASSET_LINK;
import static ml.comet.experiment.impl.ArtifactImplTest.SOME_REMOTE_ASSET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases to test implementation of {@link ml.comet.experiment.artifact.Artifact} which was downloaded.
 */
@DisplayName("Downloaded Artifact")
public class DownloadedArtifactImplTest extends AssetsBaseTest {
    static String SOME_VERSION_STRING = "1.0.0";
    static String SOME_UP_VERSION_STRING = "1.1.0";
    static String SOME_DOWN_VERSION_STRING = "0.1.0";
    static Semver SOME_VERSION = new Semver(SOME_VERSION_STRING);

    @Test
    @DisplayName("is created with new")
    void isCreatedWithNewArtifact() {
        DownloadedArtifactImpl artifact = new DownloadedArtifactImpl(
                SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE, SOME_VERSION_STRING);
        assertNotNull(artifact);
    }

    @Nested
    @DisplayName("when new")
    class WhenNewTest {
        DownloadedArtifactImpl artifact;

        @BeforeEach
        void createNewArtifact() {
            this.artifact = new DownloadedArtifactImpl(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE, SOME_VERSION_STRING);
        }

        @Test
        @DisplayName("artifact name and type is set")
        void nameAndTypeIsSet() {
            assertEquals(SOME_ARTIFACT_NAME, this.artifact.getName());
            assertEquals(SOME_ARTIFACT_TYPE, this.artifact.getArtifactType());
        }

        @Test
        @DisplayName("artifact version is set")
        void versionSet() {
            assertEquals(SOME_VERSION_STRING, this.artifact.getVersion());
        }

        @Test
        @DisplayName("artifact has no assets")
        void hasNoAssets() {
            assertEquals(0, this.artifact.getAssets().size());
        }

        @Test
        @DisplayName("able to increment major version")
        void incrementMajorVersion() {
            String versionStr = this.artifact.incrementMajorVersion();
            assertNotNull(versionStr, "version string expected");
            Semver version = new Semver(versionStr);
            assertTrue(SOME_VERSION.withIncMajor().isEqualTo(version), "wrong version");
        }

        @Test
        @DisplayName("able to increment minor version")
        void incrementMinorVersion() {
            String versionStr = this.artifact.incrementMinorVersion();
            assertNotNull(versionStr, "version string expected");
            Semver version = new Semver(versionStr);
            assertTrue(SOME_VERSION.withIncMinor().isEqualTo(version), "wrong version");
        }

        @Test
        @DisplayName("able to increment patch version")
        void incrementPatchVersion() {
            String versionStr = this.artifact.incrementPatchVersion();
            assertNotNull(versionStr, "version string expected");
            Semver version = new Semver(versionStr);
            assertTrue(SOME_VERSION.withIncPatch().isEqualTo(version), "wrong version");
        }

        @Test
        @DisplayName("able to set version string")
        void setVersion() {
            assertTrue(this.artifact.setVersion(SOME_UP_VERSION_STRING), "version must be set");
            assertFalse(this.artifact.setVersion(SOME_UP_VERSION_STRING), "equal version must be skipped");
            assertFalse(this.artifact.setVersion(SOME_DOWN_VERSION_STRING), "lower version must be skipped");
        }

        @Test
        @DisplayName("default version tags is not null")
        void versionTagsNotNull() {
            assertNotNull(this.artifact.getVersionTags());
        }

        @Test
        @DisplayName("able to set version tags")
        void setVersionTags() {
            HashSet<String > tags = new HashSet<>();
            tags.add("tag1");
            tags.add("tag2");
            this.artifact.setVersionTags(tags);
            assertEquals(tags, this.artifact.getVersionTags());
        }

        @Test
        @DisplayName("default aliases is not null")
        void aliasesNotNull() {
            assertNotNull(this.artifact.getAliases());
        }

        @Test
        @DisplayName("able to set aliases")
        void setAliases() {
            HashSet<String > aliases = new HashSet<>();
            aliases.add("alias1");
            aliases.add("alias2");
            this.artifact.setAliases(aliases);
            assertEquals(aliases, this.artifact.getAliases());
        }

        @Nested
        @DisplayName("after adding logged assets")
        class AfterAddingLoggedAssetsTest {
            LoggedArtifactAssetImpl loggedFileAsset;
            LoggedArtifactAssetImpl loggedRemoteAsset;
            List<LoggedArtifactAsset> loggedAssets;

            @BeforeEach
            void createLoadedAssets() throws IOException {
                this.loggedAssets = new ArrayList<>();
                // create file asset
                this.loggedFileAsset = new LoggedArtifactAssetImpl(null);
                Path assetFile = assetFolderFiles.get(0);
                this.loggedFileAsset.setFileName(assetFile.getFileName().toString());
                this.loggedFileAsset.setFileSize(Files.size(assetFile));
                this.loggedFileAsset.setAssetType(AssetType.ASSET);
                this.loggedFileAsset.setMetadata(SOME_METADATA);
                this.loggedAssets.add(this.loggedFileAsset);
                // create remote asset
                this.loggedRemoteAsset = new LoggedArtifactAssetImpl(null);
                this.loggedRemoteAsset.setRemoteUri(SOME_REMOTE_ASSET_LINK);
                this.loggedRemoteAsset.setFileName(SOME_REMOTE_ASSET_NAME);
                this.loggedRemoteAsset.setAssetType(AssetType.ASSET);
                this.loggedAssets.add(this.loggedRemoteAsset);

                artifact.addLoggedAssets(this.loggedAssets);
                assertEquals(this.loggedAssets.size(), artifact.getAssets().size());
            }

            @Nested
            @DisplayName("after updating logged file asset")
            class AfterUpdatingLoggedFileAssetTest {
                File assetFile;
                File anotherAssetFile;
                String logicalPath;
                boolean overwrite = true;

                @BeforeEach
                void updateFileAsset(){
                    this.assetFile = assetFolderFiles.get(1).toFile();
                    this.anotherAssetFile = assetFolderFiles.get(2).toFile();
                    this.logicalPath = loggedFileAsset.getFileName();
                    // update logged file asset
                    artifact.addAsset(this.assetFile, this.logicalPath, this.overwrite, SOME_METADATA);
                }

                @Test
                @DisplayName("has correct file asset after update")
                void hasCorrectUpdatedFileAsset() {
                    ArtifactAssetImpl asset = (ArtifactAssetImpl) artifact.findAsset(this.logicalPath);
                    assertNotNull(asset, "asset not found");
                    assertEquals(this.assetFile, asset.getRawFile(), "wrong file");
                    assertEquals(this.logicalPath, asset.getLogicalPath(), "wrong file name");
                    assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");
                    assertEquals(this.overwrite, asset.getOverwrite(), "wrong overwrite");
                }

                @Test
                @DisplayName("doesn't throw exception when updated again")
                void doesNotThrowExceptionWhenUpdatedAgain() {
                    artifact.addAsset(this.anotherAssetFile , this.logicalPath, true);
                }
            }

            @Nested
            @DisplayName("after updating logged remote asset")
            class AfterUpdatingLoggedRemoteAssetTest {
                URI remoteUri;
                URI anotherRemoteUri;
                String logicalPath;
                boolean overwrite = true;

                @BeforeEach
                void updateRemoteAsset() throws URISyntaxException {
                    this.remoteUri = new URI("s3://newUri/DownloadedArtifactTest");
                    this.anotherRemoteUri = new URI("s3://newUri/DownloadedArtifactTest/another");
                    this.logicalPath = loggedRemoteAsset.getFileName();

                    // update remote asset
                    artifact.addRemoteAsset(this.remoteUri, this.logicalPath, this.overwrite, SOME_METADATA);
                }

                @Test
                @DisplayName("has correct remote asset after update")
                void hasCorrectRemoteAsset() {
                    ArtifactAssetImpl asset = (ArtifactAssetImpl) artifact.findAsset(this.logicalPath);
                    assertNotNull(asset, "remote asset expected");
                    assertTrue(asset.isRemote(), "remote asset expected");
                    assertTrue(asset.getLink().isPresent(), "link expected");
                    assertEquals(this.remoteUri, asset.getLink().get(), "wrong link");
                    assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");
                    assertEquals(this.overwrite, asset.getOverwrite(), "wrong overwrite");
                }

                @Test
                @DisplayName("doesn't throw exception when updated again")
                void doesNotThrowExceptionWhenUpdatedAgain() {
                    artifact.addRemoteAsset(this.anotherRemoteUri, loggedRemoteAsset.getFileName(), true);
                }
            }
        }
    }
}
