package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.LoggedArtifact;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
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

        System.out.printf("\n\nArtifact upload complete, new artifact version: %s\n\n", loggedArtifact.getVersion());

        System.out.println("===== Experiment completed ====");
    }
}
