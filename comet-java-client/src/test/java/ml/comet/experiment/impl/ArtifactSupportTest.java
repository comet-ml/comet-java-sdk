package ml.comet.experiment.impl;

import io.reactivex.rxjava3.functions.Function4;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.ArtifactAssetNotFoundException;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.DownloadedArtifact;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ml.comet.experiment.impl.ArtifactImplTest.SOME_METADATA;
import static ml.comet.experiment.impl.ExperimentTestFactory.WORKSPACE_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.asset.AssetType.ASSET;
import static ml.comet.experiment.impl.asset.AssetType.UNKNOWN;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_FIND_ASSET_IN_ARTIFACT;
import static ml.comet.experiment.impl.resources.LogMessages.REMOTE_ASSET_CANNOT_BE_DOWNLOADED;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration E2E test cases to test support for operations related to {@link ml.comet.experiment.artifact.Artifact}
 * and {@link ml.comet.experiment.artifact.LoggedArtifact}.
 */
@DisplayName("ArtifactSupportTest INTEGRATION")
@Tag("integration")
public class ArtifactSupportTest extends AssetsBaseTest {

    static final String ALIAS_LATEST = "Latest";

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

            // the logged artifact validator
            Function4<LoggedArtifact, ArtifactImpl, String, List<String>, Void> loggedArtifactValidator =
                    (actual, original, experimentKey, expectedAliases) -> {
                        assertNotNull(actual, "logged artifact expected");
                        assertEquals(original.getType(), actual.getArtifactType(), "wrong artifact type");
                        assertEquals(new HashSet<>(expectedAliases), actual.getAliases(), "wrong aliases");
                        assertEquals(SOME_METADATA, actual.getMetadata(), "wrong metadata");
                        assertEquals(new HashSet<>(original.getVersionTags()), actual.getVersionTags(), "wrong version tags");
                        assertEquals(WORKSPACE_NAME, actual.getWorkspace(), "wrong workspace");
                        assertEquals(experimentKey, actual.getSourceExperimentKey(), "wrong experiment key");
                        assertEquals(original.getName(), actual.getName(), "wrong artifact name");
                        return null;
                    };

            // check artifacts-in-progress counter before
            assertEquals(0, experiment.getArtifactsInProgress().get(),
                    "artifacts-in-progress counter must be zero at start");

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            // check artifacts-in-progress counter while in progress
            assertEquals(1, experiment.getArtifactsInProgress().get(),
                    "artifacts-in-progress counter has wrong value while still in progress");

            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // check artifacts-in-progress counter after
            Awaitility.await("artifacts-in-progress counter must be decreased")
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.SECONDS)
                    .until(() -> experiment.getArtifactsInProgress().get() == 0);
            assertEquals(0, experiment.getArtifactsInProgress().get(),
                    "artifacts-in-progress counter must be zero after log operation completed");

            List<String> expectedAliases = new ArrayList<>(artifact.getAliases());
            loggedArtifactValidator.apply(loggedArtifact, artifact, experiment.getExperimentKey(), expectedAliases);

            // get artifact details from server and check its correctness
            //
            LoggedArtifact loggedArtifactFromServer = experiment.getArtifact(
                    loggedArtifact.getName(), loggedArtifact.getWorkspace(), loggedArtifact.getVersion());
            expectedAliases.add(ALIAS_LATEST); // added by the backend automatically

            loggedArtifactValidator.apply(loggedArtifactFromServer, artifact, experiment.getExperimentKey(), expectedAliases);

            // check that correct assets was logged
            //
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            Collection<ArtifactAsset> assets = artifact.getAssets();
            assertEquals(assets.size(), loggedAssets.size(), "wrong size");
            loggedAssets.forEach(loggedArtifactAsset -> validateArtifactAsset(
                    new ArtifactAssetImpl((LoggedArtifactAssetImpl) loggedArtifactAsset), assets));

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    public void testLogAndGetArtifact_getRemoteAssets() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add remote assets
            //
            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstRemoteAssetFileName = "firstAssetFileName";
            artifact.addRemoteAsset(firstAssetLink, firstRemoteAssetFileName);

            String secondRemoteAssetFileName = "secondAssetFile.extension";
            URI secondAssetLink = new URI("s3://bucket/folder/" + secondRemoteAssetFileName);
            artifact.addRemoteAsset(secondAssetLink, secondRemoteAssetFileName);

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

            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // check that correct assets was logged
            //
            Collection<LoggedArtifactAsset> loggedRemoteAssets = loggedArtifact.getRemoteAssets();
            assertEquals(2, loggedRemoteAssets.size(), "wrong number of remote assets returned");
            assertTrue(loggedRemoteAssets
                    .stream()
                    .allMatch(loggedArtifactAsset -> loggedArtifactAsset.isRemote()
                            && (loggedArtifactAsset.getLogicalPath().equals(firstRemoteAssetFileName) ||
                            loggedArtifactAsset.getLogicalPath().equals(secondRemoteAssetFileName))));

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
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            ArtifactAssetImpl fileAsset = (ArtifactAssetImpl) loggedAssets.iterator().next().download(tmpDir);

            assertNotNull(fileAsset, "file asset expected");
            assertEquals(IMAGE_FILE_NAME, fileAsset.getRawFile().getName(), "wrong file name");
            assertEquals(IMAGE_FILE_SIZE, Files.size(fileAsset.getRawFile().toPath()), "wrong downloaded file size");
            assertEquals(IMAGE_FILE_SIZE, fileAsset.getSize().orElse(0L), "wrong file size");
            assertEquals(SOME_METADATA, fileAsset.getMetadata(), "wrong metadata");
            assertEquals(UNKNOWN.type(), fileAsset.getType(), "wrong asset type");

            System.out.println(fileAsset);

        } catch (Throwable t) {
            fail(t);
        } finally {
            PathUtils.delete(tmpDir);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndDownloadArtifactAsset_failForRemote() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndDownloadArtifactAsset");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add remote assets
            //
            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstAssetFileName = "firstAssetFileName";
            artifact.addRemoteAsset(firstAssetLink, firstAssetFileName);

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
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            LoggedArtifactAsset asset = loggedAssets.iterator().next();
            assertNotNull(asset);

            ArtifactException ex = assertThrows(ArtifactException.class, () -> asset.download(tmpDir));
            assertEquals(getString(REMOTE_ASSET_CANNOT_BE_DOWNLOADED, asset), ex.getMessage());

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

            // get artifact details from server
            //
            LoggedArtifact loggedArtifactFromServer = experiment.getArtifact(
                    loggedArtifact.getName(), loggedArtifact.getWorkspace(), loggedArtifact.getVersion());

            // download artifact and check results
            //
            DownloadedArtifact downloadedArtifact = loggedArtifactFromServer.download(tmpDir);

            List<String> expectedAliases = new ArrayList<>(artifact.getAliases());
            expectedAliases.add(ALIAS_LATEST);

            assertNotNull(downloadedArtifact, "downloaded artifact expected");
            assertEquals(artifact.getType(), downloadedArtifact.getArtifactType(), "wrong artifact type");
            assertEquals(new HashSet<>(expectedAliases), downloadedArtifact.getAliases(), "wrong aliases");
            assertEquals(SOME_METADATA, downloadedArtifact.getMetadata(), "wrong metadata");
            assertEquals(new HashSet<>(artifact.getVersionTags()), downloadedArtifact.getVersionTags(), "wrong version tags");
            assertEquals(WORKSPACE_NAME, downloadedArtifact.getWorkspace(), "wrong workspace");
            assertEquals(artifact.getName(), downloadedArtifact.getName(), "wrong artifact name");

            assertEquals(loggedArtifactFromServer.getFullName(), downloadedArtifact.getFullName(), "wrong full name");
            assertEquals(loggedArtifactFromServer.getArtifactId(), downloadedArtifact.getArtifactId(), "wrong artifact ID");
            assertEquals(loggedArtifactFromServer.getVersion(), downloadedArtifact.getVersion(), "wrong version");


            // check that all assets returned including the remote ones
            Collection<ArtifactAsset> downloadedArtifactAssets = downloadedArtifact.getAssets();
            assertEquals(artifact.getAssets().size(), downloadedArtifactAssets.size(), "wrong downloaded assets size");
            downloadedArtifactAssets.forEach(artifactAsset ->
                    validateArtifactAsset(artifactAsset, artifact.getAssets()));

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
    public void testLogAndDownloadArtifact_FAIL_error() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndDownloadArtifact_FAIL_error");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // Create a conflicting file in the target directory
            //
            Path conflictPath = tmpDir.resolve(new File(IMAGE_FILE_NAME).toPath());
            Files.write(conflictPath, "some data".getBytes(StandardCharsets.UTF_8));

            // download artifact and check that appropriate exception is thrown
            //
            Exception ex = assertThrows(ArtifactException.class, () -> loggedArtifact.download(
                    tmpDir, AssetOverwriteStrategy.FAIL_IF_DIFFERENT));

            String message = getString(FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS, loggedArtifact.getFullName(), tmpDir);
            assertEquals(message, ex.getMessage());

        } catch (Throwable t) {
            fail(t);
        } finally {
            PathUtils.delete(tmpDir);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndDownloadArtifact_OVERWRITE() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndDownloadArtifact_OVERWRITE");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // Create a conflicting file in the target directory
            //
            Path conflictPath = tmpDir.resolve(new File(IMAGE_FILE_NAME).toPath());
            Files.write(conflictPath, "some data".getBytes(StandardCharsets.UTF_8));

            // download artifact and check that file was overwritten
            //
            DownloadedArtifact downloadedArtifact = loggedArtifact.download(tmpDir, AssetOverwriteStrategy.OVERWRITE);
            assertNotNull(downloadedArtifact, "downloaded artifact expected");
            Collection<ArtifactAsset> assets = downloadedArtifact.getAssets();
            assertEquals(1, assets.size());

            ArtifactAsset asset = assets.iterator().next();
            Path assetFile = tmpDir.resolve(asset.getLogicalPath());
            assertTrue(PathUtils.fileContentEquals(
                    Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)).toPath(), assetFile));

        } catch (Throwable t) {
            fail(t);
        } finally {
            PathUtils.delete(tmpDir);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndDownloadArtifact_PRESERVE() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndDownloadArtifact_PREVENT");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            artifact.addAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)),
                    IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // Create a conflicting file in the target directory
            //
            Path conflictPath = tmpDir.resolve(new File(IMAGE_FILE_NAME).toPath());
            byte[] someData = "some data".getBytes(StandardCharsets.UTF_8);
            Files.write(conflictPath, someData);

            // download artifact and check that file was preserved
            //
            DownloadedArtifact downloadedArtifact = loggedArtifact.download(tmpDir, AssetOverwriteStrategy.PRESERVE);
            assertNotNull(downloadedArtifact, "downloaded artifact expected");
            Collection<ArtifactAsset> assets = downloadedArtifact.getAssets();
            assertEquals(1, assets.size());

            byte[] fileData = Files.readAllBytes(conflictPath);
            assertArrayEquals(someData, fileData);

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
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            loggedAssets.iterator().next().writeTo(out);

            assertEquals(IMAGE_FILE_SIZE, out.size(), "wrong asset data size");

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndOpenStreamToArtifactAsset() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            File imageFile = TestUtils.getFile(IMAGE_FILE_NAME);
            assertNotNull(imageFile);
            artifact.addAsset(imageFile, IMAGE_FILE_NAME, false, SOME_METADATA);

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
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            LoggedArtifactAsset asset = loggedAssets.iterator().next();
            assertNotNull(asset);

            try (InputStream in = asset.openStream()) {
                byte[] data = IOUtils.readFully(in, (int) IMAGE_FILE_SIZE);
                byte[] expectedData = Files.readAllBytes(imageFile.toPath());

                assertArrayEquals(expectedData, data, "wrong asset data read");
            } catch (Throwable t) {
                fail(t);
            }

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogAndOpenStreamToArtifactAsset_failedForRemote() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add remote assets
            //
            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstAssetFileName = "firstAssetFileName";
            artifact.addRemoteAsset(firstAssetLink, firstAssetFileName);

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
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            assertEquals(1, loggedAssets.size(), "wrong number of assets returned");

            LoggedArtifactAsset asset = loggedAssets.iterator().next();
            assertNotNull(asset);

            ArtifactException ex = assertThrows(ArtifactException.class, asset::openStream);
            assertEquals(getString(REMOTE_ASSET_CANNOT_BE_DOWNLOADED, asset), ex.getMessage());

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    void testLogAndUpdateArtifact() throws IOException {
        Path tmpDir = Files.createTempDirectory("testLogAndUpdateArtifact");
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            File imageFile = TestUtils.getFile(IMAGE_FILE_NAME);
            assertNotNull(imageFile);
            artifact.addAsset(imageFile, IMAGE_FILE_NAME, false, SOME_METADATA);
            File textFile = TestUtils.getFile(SOME_TEXT_FILE_NAME);
            assertNotNull(textFile);
            artifact.addAsset(textFile, SOME_TEXT_FILE_NAME, false, SOME_METADATA);

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // download artifact and check that file was preserved
            //
            DownloadedArtifact downloadedArtifact = loggedArtifact.download(tmpDir, AssetOverwriteStrategy.PRESERVE);
            assertNotNull(downloadedArtifact, "downloaded artifact expected");

            int originalAssetsCount = 2;
            assertEquals(originalAssetsCount, downloadedArtifact.getAssets().size(), "downloaded artifact has wrong assets size");

            // update artifact
            //
            Set<String> newTags = new HashSet<>(Collections.singletonList("downloaded tag"));
            downloadedArtifact.setVersionTags(newTags);

            String extraAlias = "downloaded alias";
            assertTrue(downloadedArtifact.getAliases().add(extraAlias), "failed to add alias");

            String extraKey = "downloaded key";
            String extraMetaValue = "some value";
            downloadedArtifact.getMetadata().put(extraKey, extraMetaValue);

            downloadedArtifact.addAsset(Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)),
                    CODE_FILE_NAME, false);

            downloadedArtifact.incrementMinorVersion();
            String newArtifactVersion = downloadedArtifact.getVersion();

            Collection<ArtifactAsset> assets = downloadedArtifact.getAssets();
            assertEquals(originalAssetsCount + 1, assets.size(), "wrong number of assets after update");

            // log downloaded artifact
            //
            CompletableFuture<LoggedArtifact> futureUpdatedArtifact = experiment.logArtifact(downloadedArtifact);
            loggedArtifact = futureUpdatedArtifact.get(60, SECONDS);

            // read artifact from server and check that it was actually updated
            LoggedArtifact loggedArtifactFromServer = experiment.getArtifact(
                    loggedArtifact.getName(), loggedArtifact.getWorkspace(), loggedArtifact.getVersion());

            assertEquals(newArtifactVersion, loggedArtifactFromServer.getVersion(), "wrong version");
            assertEquals(newTags, loggedArtifactFromServer.getVersionTags(), "wrong version tags");

            Set<String> expectedAliases = new HashSet<>(artifact.getAliases());
            expectedAliases.add(extraAlias);
            expectedAliases.add(ALIAS_LATEST);
            assertEquals(expectedAliases, loggedArtifactFromServer.getAliases(), "wrong aliases");

            Map<String, Object> expectedMetadata = new HashMap<>(artifact.getMetadata());
            expectedMetadata.put(extraKey, extraMetaValue);
            assertEquals(expectedMetadata, loggedArtifactFromServer.getMetadata(), "wrong metadata");

            // get assets from server and check that all assets are correct including new one
            Collection<LoggedArtifactAsset> loggedAssets = loggedArtifactFromServer.getAssets();
            assertNotNull(loggedAssets, "assets expected");
            assertEquals(assets.size(), loggedAssets.size(), "wrong assets size");
            loggedAssets.forEach(loggedArtifactAsset -> validateArtifactAsset(
                    new ArtifactAssetImpl((LoggedArtifactAssetImpl) loggedArtifactAsset), assets));

        } catch (Throwable t) {
            fail(t);
        } finally {
            PathUtils.delete(tmpDir);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogArtifactAndGetAsset() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            File imageFile = TestUtils.getFile(IMAGE_FILE_NAME);
            assertNotNull(imageFile);
            artifact.addAsset(imageFile, IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // try to get asset by logical path and check result
            //
            LoggedArtifactAsset asset = loggedArtifact.getAsset(IMAGE_FILE_NAME);
            assertNotNull(asset, "asset expected");
            assertEquals(IMAGE_FILE_NAME, asset.getLogicalPath(), "wrong logical path");
            assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata");

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    @Timeout(value = 300, unit = SECONDS)
    public void testLogArtifactAndGetAsset_notFoundError() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            ArtifactImpl artifact = createArtifact();

            // add local assets
            //
            File imageFile = TestUtils.getFile(IMAGE_FILE_NAME);
            assertNotNull(imageFile);
            artifact.addAsset(imageFile, IMAGE_FILE_NAME, false, SOME_METADATA);

            // log artifact and check results
            //
            CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
            LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

            // try to get asset by logical path and check that appropriate exception raised
            //
            ArtifactAssetNotFoundException ex = assertThrows(ArtifactAssetNotFoundException.class,
                    () -> loggedArtifact.getAsset(SOME_TEXT_FILE_NAME));

            assertNotNull(ex.getMessage());
            assertEquals(ex.getMessage(), getString(FAILED_TO_FIND_ASSET_IN_ARTIFACT,
                    SOME_TEXT_FILE_NAME, loggedArtifact.getFullName()));

        } catch (Throwable t) {
            fail(t);
        }
    }

    static ArtifactImpl createArtifact() {
        List<String> aliases = Arrays.asList("alias1", "alias2");
        List<String> tags = Arrays.asList("tag1", "tag2");
        String artifactName = "anotherArtifact1";
        String artifactType = "someType";
        return (ArtifactImpl) Artifact
                .newArtifact(artifactName, artifactType)
                .withAliases(aliases)
                .withVersionTags(tags)
                .withMetadata(SOME_METADATA)
                .build();
    }

    static void validateArtifactAsset(ArtifactAsset artifactAsset, Collection<ArtifactAsset> assets) {
        AtomicBoolean matchFound = new AtomicBoolean(false);
        assets.stream()
                .filter(asset -> Objects.equals(asset.getLogicalPath(), artifactAsset.getLogicalPath()))
                .forEach(asset -> {
                    matchFound.set(true);
                    if (asset.isRemote()) {
                        assertTrue(artifactAsset.isRemote());
                        assertTrue(artifactAsset.getLink().isPresent(), "remote link expected in logged asset");
                        assertTrue(asset.getLink().isPresent(), "remote link expected in asset");
                        assertEquals(asset.getLink().get(), artifactAsset.getLink().get(), "wrong URI");
                    } else {
                        assertFalse(artifactAsset.isRemote());
                    }
                    if (asset.getMetadata() != null) {
                        assertEquals(asset.getMetadata(), artifactAsset.getMetadata(), "wrong metadata");
                    } else {
                        assertEquals(0, artifactAsset.getMetadata().size(), "empty metadata expected");
                    }
                    if (Objects.equals(asset.getType(), ASSET.type())) {
                        assertEquals(UNKNOWN.type(), artifactAsset.getType());
                    } else {
                        assertEquals(asset.getType(), artifactAsset.getType(), "wrong asset type");
                    }
                });
        assertTrue(matchFound.get(), String.format("no match found for %s", artifactAsset));
    }
}
