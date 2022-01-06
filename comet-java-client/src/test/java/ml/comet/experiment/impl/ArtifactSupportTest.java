package ml.comet.experiment.impl;

import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.asset.ArtifactAsset;
import ml.comet.experiment.impl.asset.RemoteAsset;
import ml.comet.experiment.impl.utils.TestUtils;
import ml.comet.experiment.model.FileAsset;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ml.comet.experiment.impl.ArtifactImplTest.SOME_METADATA;
import static ml.comet.experiment.impl.ExperimentTestFactory.WORKSPACE_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.model.AssetType.ASSET;
import static ml.comet.experiment.model.AssetType.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration E2E test cases to test support for operations related to {@link ml.comet.experiment.artifact.Artifact}
 * and {@link ml.comet.experiment.artifact.LoggedArtifact}.
 */
@DisplayName("ArtifactSupportTest INTEGRATION")
@Tag("integration")
public class ArtifactSupportTest extends AssetsBaseTest {
    @Test
    public void testLogAndGetArtifact() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add remote assets
            //
            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstAssetFileName = "firstAssetFileName";
            artifact.addRemoteAsset(firstAssetLink, firstAssetFileName);

            String secondAssetExpectedFileName = "secondAssetFile.extension";
            URI secondAssetLink = new URI("s3://bucket/folder/" + secondAssetExpectedFileName);
            artifact.addRemoteAsset(secondAssetLink, secondAssetExpectedFileName);

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)),
                    CODE_FILE_NAME, false);
            byte[] someData = "some data".getBytes(StandardCharsets.UTF_8);
            String someDataName = "someDataName";
            artifact.addAsset(someData, someDataName);

            // add assets folder
            //
            artifact.addAssetFolder(assetsFolder.toFile(), true, true);

            // the artifact validator
            BiFunction<LoggedArtifact, List<String>, Void> artifactValidator = (actual, expectedAliases) -> {
                assertNotNull(actual, "logged artifact expected");
                assertEquals(artifact.getType(), actual.getArtifactType(), "wrong artifact type");
                assertEquals(new HashSet<>(expectedAliases), actual.getAliases(), "wrong aliases");
                assertEquals(SOME_METADATA, actual.getMetadata(), "wrong metadata");
                assertEquals(new HashSet<>(artifact.getVersionTags()), actual.getVersionTags(), "wrong version tags");
                assertEquals(WORKSPACE_NAME, actual.getWorkspace(), "wrong workspace");
                assertEquals(experiment.getExperimentKey(), actual.getSourceExperimentKey(), "wrong experiment key");
                assertEquals(artifact.getName(), actual.getName(), "wrong artifact name");
                return null;
            };

            // log artifact and check results
            //
            List<String> expectedAliases = new ArrayList<>(artifact.getAliases());

            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);
            artifactValidator.apply(loggedArtifact, expectedAliases);


            // get artifact details from server and check its correctness
            //
            LoggedArtifact loggedArtifactFromServer = experiment.getArtifact(
                    loggedArtifact.getName(), loggedArtifact.getWorkspace(), loggedArtifact.getVersion());
            expectedAliases.add("Latest"); // added by the backend automatically

            artifactValidator.apply(loggedArtifactFromServer, expectedAliases);

            // check that correct assets was logged
            //
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.readAssets();
            validateLoggedArtifactAssets(artifact.getAssets(), loggedAssets);

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndDownloadArtifactAsset() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndDownloadArtifactAsset");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // get artifact details from server
            //
            LoggedArtifact loggedArtifactFromServer = experiment.getArtifact(
                    loggedArtifact.getName(), loggedArtifact.getWorkspace(), loggedArtifact.getVersion());

            // get logged assets and download to local dir
            //
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.readAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            FileAsset fileAsset = loggedAssets.iterator().next().download(tmpDir);

            assertNotNull(fileAsset, "file asset expected");
            assertEquals(IMAGE_FILE_NAME, fileAsset.getPath().getFileName().toString(), "wrong file name");
            assertEquals(IMAGE_FILE_SIZE, Files.size(fileAsset.getPath()), "wrong file size");
            assertEquals(SOME_METADATA, fileAsset.getMetadata(), "wrong metadata");
            assertEquals(UNKNOWN.type(), fileAsset.getAssetType(), "wrong asset type");

            System.out.println(fileAsset);

        } catch (Throwable t) {
            fail(t);
        } finally {
            PathUtils.delete(tmpDir);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndDownloadArtifact() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndDownloadArtifact");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();
            List<Path> assetsToDownload = new ArrayList<>(assetFolderFiles);

            // add remote assets
            //
            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstAssetFileName = "firstAssetFileName";
            artifact.addRemoteAsset(firstAssetLink, firstAssetFileName);

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);
            assetsToDownload.add(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)).toPath());

            // add assets folder
            //
            artifact.addAssetFolder(assetsFolder.toFile(), true, true);

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // download artifact and check results
            //
            Collection<LoggedArtifactAsset> assets = loggedArtifact.download(tmpDir);

            // check that all assets returned including the remote ones
            validateLoggedArtifactAssets(artifact.getAssets(), assets);

            // check that file assets was saved to the folder
            assetsToDownload = assetsToDownload.stream()
                    .map(Path::getFileName)
                    .collect(Collectors.toList());
            try (Stream<Path> files = Files.walk(tmpDir)) {
                assertTrue(files
                        .filter(Files::isRegularFile)
                        .peek(System.out::println)
                        .map(Path::getFileName)
                        .allMatch(assetsToDownload::contains));
            }

        } catch (Throwable t) {
            fail(t);
        } finally {
            PathUtils.delete(tmpDir);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndWriteToArtifactAsset() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // get artifact details from server
            //
            LoggedArtifact loggedArtifactFromServer = experiment.getArtifact(
                    loggedArtifact.getName(), loggedArtifact.getWorkspace(), loggedArtifact.getVersion());

            // get logged assets and load asset content
            //
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.readAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            loggedAssets.iterator().next().writeTo(out);

            assertEquals(IMAGE_FILE_SIZE, out.size(), "wrong asset data size");

        } catch (Throwable t) {
            fail(t);
        }
    }

    static ArtifactImpl createArtifact() {
        List<String> aliases = Arrays.asList("alias1", "alias2");
        List<String> tags = Arrays.asList("tag1", "tag2");
        String artifactName = "someArtifact";
        String artifactType = "someType";
        return (ArtifactImpl) Artifact
                .newArtifact(artifactName, artifactType)
                .withAliases(aliases)
                .withVersionTags(tags)
                .withMetadata(SOME_METADATA)
                .build();
    }

    static void validateLoggedArtifactAssets(Collection<ArtifactAsset> assets, Collection<LoggedArtifactAsset> loggedAssets) {
        assertEquals(assets.size(), loggedAssets.size(), "wrong size");
        loggedAssets.forEach(loggedArtifactAsset -> validateLoggedArtifactAsset(loggedArtifactAsset, assets));
    }

    static void validateLoggedArtifactAsset(LoggedArtifactAsset loggedArtifactAsset, Collection<ArtifactAsset> assets) {
        AtomicBoolean matchFound = new AtomicBoolean(false);
        assets.stream()
                .filter(asset -> Objects.equals(asset.getFileName(), loggedArtifactAsset.getFileName()))
                .forEach(asset -> {
                    matchFound.set(true);
                    if (asset instanceof RemoteAsset) {
                        assertTrue(loggedArtifactAsset.isRemote());
                        assertTrue(loggedArtifactAsset.getLink().isPresent(), "remote link expected");
                        assertEquals(((RemoteAsset) asset).getLink(), loggedArtifactAsset.getLink().get(), "wrong URI");
                    } else {
                        assertFalse(loggedArtifactAsset.isRemote());
                    }
                    if (asset.getMetadata() != null) {
                        assertEquals(asset.getMetadata(), loggedArtifactAsset.getMetadata(), "wrong metadata");
                    } else {
                        assertEquals(0, loggedArtifactAsset.getMetadata().size(), "empty metadata expected");
                    }
                    if (asset.getType() == ASSET) {
                        assertEquals(UNKNOWN.type(), loggedArtifactAsset.getAssetType());
                    } else {
                        assertEquals(asset.getType().type(), loggedArtifactAsset.getAssetType(), "wrong asset type");
                    }
                });
        assertTrue(matchFound.get(), String.format("no match found for %s", loggedArtifactAsset));
    }
}
