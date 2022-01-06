package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;

import java.io.ByteArrayOutputStream;
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
        artifact.addAsset(getResourceFile(CHART_IMAGE_FILE), "amazing chart.png", false, SOME_METADATA);
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
        Collection<LoggedArtifactAsset> loggedAssets = loggedArtifact.readAssets();
        System.out.printf(
                "Received %d logged artifact assets from the Comet server. Downloading asset files...\n",
                loggedAssets.size());

        // download artifact assets to the local directory
        //
        final Path assetsTmpDir = Files.createTempDirectory("ArtifactExampleAssets");
        loggedAssets.forEach(loggedArtifactAsset -> {
            if (!loggedArtifactAsset.isRemote()) {
                loggedArtifactAsset.download(assetsTmpDir);
            } else {
                URI uri = loggedArtifactAsset.getLink().orElse(null);
                System.out.printf(
                        "Skipping download of the remote asset %s. It must be downloaded using its URI '%s'\n",
                        loggedArtifactAsset.getFileName(), uri);
            }
        });

        System.out.printf("Artifact assets successfully downloaded to the folder: %s\n\n", assetsTmpDir);

        // download artifact to the local directory
        //
        final Path artifactTmpDir = Files.createTempDirectory("ArtifactExampleArtifact");
        System.out.printf("Downloading artifact to the folder: %s\n", artifactTmpDir.toFile().getAbsoluteFile());

        Collection<LoggedArtifactAsset> assets = loggedArtifact.download(artifactTmpDir,
                AssetOverwriteStrategy.FAIL_IF_DIFFERENT);
        System.out.printf(
                "Artifact successfully downloaded. Received %d logged artifact assets from the Comet server.\n\n",
                assets.size());

        // load content of the artifact asset into the memory
        //
        LoggedArtifactAsset asset = assets.stream()
                .filter(loggedArtifactAsset -> !loggedArtifactAsset.isRemote())
                .findFirst().orElse(null);
        if (asset != null) {
            System.out.printf("Loading content of the artifact asset '%s' into memory\n", asset.getAssetId());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            asset.writeTo(out);

            System.out.printf(
                    "Asset's content successfully loaded into memory, data size: %d.\n\n", out.size());
        }

        System.out.println("===== Experiment completed ====");
    }
}
