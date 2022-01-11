package ml.comet.experiment.impl;

import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.asset.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test cases to test implementation of {@link ml.comet.experiment.artifact.Artifact} which was downloaded.
 */
@DisplayName("Artifact")
public class DownloadedArtifactTest extends AssetsBaseTest {

    static HashSet<String> downloadedIdentifiers = new HashSet<>();
    static {
        downloadedIdentifiers.add("firstDownloaded");
        downloadedIdentifiers.add("secondDownloaded");
        downloadedIdentifiers.add("thirdDownloaded");
    }

    @Test
    @DisplayName("is created with new")
    void isCreatedWithNewArtifact() {
        DownloadedArtifact artifact = new DownloadedArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE);
        assertNotNull(artifact);
    }

    @Nested
    @DisplayName("when new")
    class WhenNewTest {
        DownloadedArtifact artifact;

        @BeforeEach
        void createNewArtifact() {
            this.artifact = new DownloadedArtifact(SOME_ARTIFACT_NAME, SOME_ARTIFACT_TYPE);
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
        @DisplayName("after adding loaded assets")
        class AfterAddingLoadedAssetsTest {
            LoggedArtifactAssetImpl fileAsset;
            LoggedArtifactAssetImpl remoteAsset;

            List<LoggedArtifactAsset> loadedAssets;

            @BeforeEach
            void createLoadedAssets() throws IOException {
                this.loadedAssets = new ArrayList<>();
                // create file asset
                this.fileAsset = new LoggedArtifactAssetImpl(null);
                Path assetFile = assetFolderFiles.get(0);
                this.fileAsset.setFileName(assetFile.getFileName().toString());
                this.fileAsset.setFileSize(Files.size(assetFile));
                this.fileAsset.setAssetType(AssetType.ASSET);
                this.fileAsset.setMetadata(SOME_METADATA);
                this.loadedAssets.add(this.fileAsset);
                // create remote asset
                this.remoteAsset = new LoggedArtifactAssetImpl(null);
                this.remoteAsset.setRemoteUri(SOME_REMOTE_ASSET_LINK);
                this.remoteAsset.setFileName(SOME_REMOTE_ASSET_NAME);
                this.loadedAssets.add(this.remoteAsset);
            }

            @Test
            @DisplayName("after adding loaded assets")
            @Order(1)
            void addLoadedAssets() {
                artifact.addLoadedAssets(this.loadedAssets);
                assertEquals(this.loadedAssets.size(), artifact.getAssets().size());
            }


        }
    }
}
