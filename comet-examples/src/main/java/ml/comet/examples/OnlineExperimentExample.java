package ml.comet.examples;

import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.context.ExperimentContext;
import org.apache.commons.io.file.PathUtils;

import java.net.URI;
import java.nio.file.Path;

import static ml.comet.examples.Utils.getResourceFile;
import static ml.comet.examples.Utils.readResourceToString;

/**
 * Provides variety of examples of data logging using OnlineExperiment.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_WORKSPACE_NAME=your_workspace \
 * COMET_PROJECT_NAME=your_project_name \
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.OnlineExperimentExample"
 * </pre>
 * Make sure to provide correct values above.
 */
public class OnlineExperimentExample implements BaseExample {

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     */
    public static void main(String[] args) throws Exception {

        //this will take configs from /comet-java-sdk/comet-examples/src/main/resources/application.conf
        //be sure you have set up apiKey, project, workspace in defaults.conf before you start!

        OnlineExperiment experiment = ExperimentBuilder
                .OnlineExperiment()
                .interceptStdout()
                .build();

        //you can use a default builder or just inject params
        //OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment().builder();

        try {
            OnlineExperimentExample.run(experiment);
        } finally {
            experiment.end();
        }
    }

    private static void run(OnlineExperiment experiment) throws Exception {
        experiment.setExperimentName("Java-SDK 2.0.2");
        experiment.nextStep();

        //metric can be a number, string , or double
        experiment.logMetric("strMetric", "123");
        experiment.logMetric("numMetric", 123, 123, 4);
        experiment.nextEpoch();
        experiment.logMetric("doubleMetric", 123.5d);

        experiment.setEpoch(3);

        BaseExample.generateCharts(experiment);

        experiment.setStep(1234);

        experiment.logHtml(BaseExample.generateCustomHtmlReport(), false);

        experiment.logParameter("batch_size", "500");
        experiment.logParameter("learning_rate", 12);

        // upload assets
        //
        experiment.uploadAsset(getResourceFile(CHART_IMAGE_FILE), "amazing chart.png", false);
        experiment.uploadAsset(getResourceFile(MODEL_FILE), false,
                ExperimentContext.builder().withContext("train").build());

        experiment.nextStep();

        // upload asset files from folder
        //
        Path assetDir = BaseExample.copyResourcesToTmpDir();
        experiment.logAssetFolder(assetDir.toFile(), true, true);

        // log remote assets
        //
        experiment.logRemoteAsset(new URI("s3://bucket/folder/dataCorpus.hd5"), "modelDataCorpus", false);

        experiment.logOther("Parameter", 4);

        System.out.println("Epoch 1/20");
        System.out.println("- loss: 0.7858 - acc: 0.7759 - val_loss: 0.3416 - val_acc: 0.9026");

        experiment.logGraph(readResourceToString(GRAPH_JSON_FILE));

        experiment.logCode(getResourceFile(CODE_FILE),
                ExperimentContext.builder().withContext("test").build());

        System.out.println("===== Experiment completed ====");

        // will close connection, if not called connection will close on jvm exit
        experiment.end();

        // remove tmp directory after experiment closed and everything uploaded
        PathUtils.deleteDirectory(assetDir);
    }
}