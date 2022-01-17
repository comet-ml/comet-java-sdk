package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.DownloadedArtifact;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ml.comet.examples.Utils.getResourceFile;
import static ml.comet.experiment.ExperimentBuilder.OnlineExperiment;

/**
 * Provides examples of working with Comet artifact.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_WORKSPACE_NAME=your_workspace \
 * COMET_PROJECT_NAME=your_project_name \
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.ArtifactExample"
 * </pre>
 * Make sure to provide correct values above.
 */
public class ArtifactExample implements BaseExample {
    private static final Map<String, Object> SOME_METADATA = new HashMap<>();

    static {
        SOME_METADATA.put("someInt", 10);
        SOME_METADATA.put("someString", "test string");
        SOME_METADATA.put("someBoolean", true);
    }

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     */
    public static void main(String[] args) throws Exception {
        try (OnlineExperiment experiment = OnlineExperiment().interceptStdout().build()) {
            ArtifactExample.run(experiment);
        }
    }

    private static void run(OnlineExperiment experiment) throws Exception {
        experiment.setExperimentName("Artifact Example");

        List<String> aliases = Arrays.asList("alias1", "alias2");
        List<String> tags = Arrays.asList("tag1", "tag2");
        String artifactName = "someArtifact";
        String artifactType = "someType";
        Artifact artifact = Artifact
                .newArtifact(artifactName, artifactType)
                .withAliases(aliases)
                .withVersionTags(tags)
                .withMetadata(SOME_METADATA)
                .build();

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
        artifact.addAsset(getResourceFile(CHART_IMAGE_FILE), AMAZING_CHART_NAME, false, SOME_METADATA);
        artifact.addAsset(getResourceFile(MODEL_FILE), false);
        byte[] someData = "some data".getBytes(StandardCharsets.UTF_8);
        String someDataName = "someDataName";
        artifact.addAsset(someData, someDataName);

        // add assets folder
        //
        Path assetDir = BaseExample.copyResourcesToTmpDir();
        artifact.addAssetFolder(assetDir.toFile(), true, true);

        // log artifact
        //
        CompletableFuture<LoggedArtifact> futureArtifact = experiment.logArtifact(artifact);
        LoggedArtifact loggedArtifact = futureArtifact.get(60, SECONDS);

        System.out.printf("\nArtifact upload complete: %s\n\n", loggedArtifact.getFullName());

        // get logged assets
        //
        Collection<LoggedArtifactAsset> loggedAssets = loggedArtifact.getAssets();
        System.out.printf(
                "Received %d logged artifact assets from the Comet server. Downloading asset files...\n",
                loggedAssets.size());

        // download artifact assets to the local directory one by one
        //
        final Path assetsTmpDir = Files.createTempDirectory("ArtifactExampleAssets");
        loggedAssets.forEach(loggedArtifactAsset -> {
            if (!loggedArtifactAsset.isRemote()) {
                ArtifactAsset asset = loggedArtifactAsset.download(assetsTmpDir);
                System.out.printf("Downloaded asset '%s' of size %d bytes to '%s'\n",
                        asset.getLogicalPath(), asset.getSize().orElse(0L), asset.getFile());
            } else {
                URI uri = loggedArtifactAsset.getLink().orElse(null);
                System.out.printf(
                        "Skipping download of the remote asset %s. It must be downloaded using its URI '%s'\n",
                        loggedArtifactAsset.getLogicalPath(), uri);
            }
        });

        System.out.printf("Assets of the artifact's '%s' successfully downloaded to the folder: %s\n\n",
                loggedArtifact.getFullName(), assetsTmpDir);

        // load content of the artifact asset into the memory
        //
        LoggedArtifactAsset asset = loggedAssets.stream()
                .filter(loggedArtifactAsset -> {
                    Long size = loggedArtifactAsset.getSize().orElse(0L);
                    return !loggedArtifactAsset.isRemote() && size > 0;
                })
                .findFirst().orElse(null);
        if (asset != null) {
            System.out.printf("Loading content of the artifact asset '%s' into memory\n", asset.getAssetId());

            int buffSize = 512;
            try (InputStream in = new BufferedInputStream(asset.openStream(), buffSize)) {
                // work with input stream
                byte[] buff = IOUtils.byteArray(buffSize);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                for (int count; (count = in.read(buff)) > 0; ) {
                    out.write(buff, 0, count);
                }
                System.out.printf("Content of the asset '%s' successfully loaded into memory, data size: %d.\n\n",
                        asset.getLogicalPath(), out.size());
            } catch (Throwable t) {
                System.err.printf("Failed to read asset data, reason: %s\n\n", t);
                throw t;
            }
        }

        // download artifact to the local directory
        //
        final Path artifactTmpDir = Files.createTempDirectory("ArtifactExampleArtifact");
        System.out.printf("Downloading artifact to the folder: %s\n", artifactTmpDir.toFile().getAbsoluteFile());

        DownloadedArtifact downloadedArtifact = loggedArtifact.download(artifactTmpDir,
                AssetOverwriteStrategy.FAIL_IF_DIFFERENT);
        Collection<ArtifactAsset> assets = downloadedArtifact.getAssets();
        System.out.printf(
                "Artifact '%s' successfully downloaded. Received %d artifact assets from the Comet server.\n\n",
                downloadedArtifact.getFullName(), assets.size());

        // update downloaded artifact
        //
        System.out.printf("Starting update of the artifact: %s\n", downloadedArtifact.getFullName());

        downloadedArtifact.getAliases().add("downloaded");
        downloadedArtifact.getMetadata().put("updated", "someUpdatedValue");
        downloadedArtifact.addAsset(getResourceFile(GRAPH_JSON_FILE), "updated_graph.json",
                false, SOME_METADATA);
        downloadedArtifact.incrementMinorVersion();

        CompletableFuture<LoggedArtifact> futureUpdatedArtifact = experiment.logArtifact(downloadedArtifact);
        loggedArtifact = futureUpdatedArtifact.get(60, SECONDS);

        System.out.printf("\nArtifact update completed, new artifact version created: %s\n\n",
                loggedArtifact.getFullName());

        // get artifact asset by logical path
        //
        System.out.printf("Finding asset '%s' of the artifact: %s\n", AMAZING_CHART_NAME, loggedArtifact.getFullName());
        asset = loggedArtifact.getAsset(AMAZING_CHART_NAME);
        System.out.printf("Successfully found asset '%s' of the artifact: %s\n\n",
                asset, loggedArtifact.getFullName());

        System.out.println("===== Experiment completed ====");
    }
}
