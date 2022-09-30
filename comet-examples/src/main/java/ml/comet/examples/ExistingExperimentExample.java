package ml.comet.examples;

import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.impl.config.CometConfig;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Provides variety of examples of updating existing Comet experiment.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_EXPERIMENT_KEY=existing_experiment_key
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.ExistingExperimentExample"
 * </pre>
 * Make sure to provide correct values above.
 */
public class ExistingExperimentExample {

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     */
    public static void main(String[] args) throws Exception {
        // check that COMET_EXPERIMENT_KEY environment variable is set
        if (Objects.isNull(System.getenv(CometConfig.COMET_EXPERIMENT_KEY.getEnvironmentKey()))) {
            System.err.println("To run this experiment you should set COMET_EXPERIMENT_KEY environment variable "
                    + "with ID of existing Comet experiment."
            );
            System.exit(1);
        }

        try (OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment().build()) {
            runExample(experiment);
        }
    }

    private static void runExample(OnlineExperiment experiment) {
        // log some continuous data
        Random rd = new Random();
        String metricName = UUID.randomUUID().toString();
        for (int i = 0; i < 100; i++) {
            experiment.logMetric(metricName, rd.nextDouble() * 20, i);
        }

        // log single metric value
        experiment.logMetric(metricName, 20, 100);
    }
}
