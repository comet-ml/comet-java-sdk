package ml.comet.examples;

import ml.comet.experiment.CometApi;
import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.model.Value;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Provides variety of examples of data logging using REST API client.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_WORKSPACE_NAME=your_workspace \
 * COMET_PROJECT_NAME=your_project_name \
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.ApiExamples"
 * </pre>
 * Make sure to provide correct values above.
 */
public class ApiExamples {
    static final String randomExperimentName = UUID.randomUUID().toString();

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     */
    public static void main(String[] args) throws Exception {
        ApiExamples.run();
    }

    private static void run() throws Exception {
        // create test experiment
        //
        String experimentProjectName;
        String experimentWorkspaceName;
        String experimentKey;
        try (OnlineExperiment experiment = ExperimentBuilder
                .OnlineExperiment()
                .interceptStdout()
                .build()) {

            experiment.setExperimentName(randomExperimentName);
            experiment.logMetric("some-metric", 10);

            experimentProjectName = experiment.getProjectName();
            experimentWorkspaceName = experiment.getWorkspaceName();
            experimentKey = experiment.getExperimentKey();
        }

        // Comet API usage examples
        //
        try (CometApi api = ExperimentBuilder.CometApi().build()) {
            // get all workspaces available for the user
            //
            System.out.println("===== All user's workspaces =====");
            List<String> availableWorkspaces = api.getAllWorkspaces();
            availableWorkspaces.forEach(System.out::println);
            System.out.println("=====\n");

            // get all projects under particular workspace
            //
            String workspace = availableWorkspaces.get(0);
            System.out.printf("===== Projects in workspace: '%s' =====\n", workspace);
            List<Project> projects = api.getAllProjects(workspace);
            projects.forEach(System.out::println);
            System.out.println("=====\n");

            // get all experiments under particular project
            //
            Project project = projects.get(0);
            System.out.printf("===== First 10 Experiments in project: '%s' =====\n", project.getProjectName());
            List<ExperimentMetadata> experiments = api.getAllExperiments(project.getProjectId());
            experiments.stream()
                    .filter(experimentMetadata -> !Objects.isNull(experimentMetadata.getExperimentName()))
                    .limit(10)
                    .forEach(System.out::println);
            System.out.println("=====\n");

            // get experiment(-s) by name/regex (using the one we created above)
            //
            System.out.printf("===== Experiment(-s) by name/regex '%s' in workspace/project: '%s/%s' =====\n",
                    randomExperimentName, experimentWorkspaceName, experimentProjectName);
            experiments = api.getExperiments(experimentWorkspaceName, experimentProjectName, randomExperimentName);
            if (experiments.size() == 1) {
                displayExperiment(experiments.get(0));
            } else {
                System.out.printf("*** No, or more than one experiment was found. Found: %d experiments.\n",
                        experiments.size());
            }
            System.out.println("=====\n");

            // get experiment's meta-data by experiment key
            //
            System.out.printf("===== Experiment's metadata by experiment key: '%s' =====\n", experimentKey);
            ExperimentMetadata metadata = api.getExperimentMetadata(experimentKey);
            System.out.println(metadata);
            System.out.println("=====\n");
        }
    }

    private static void displayExperiment(ExperimentMetadata experimentMetadata) throws Exception {
        try (OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment()
                .withExistingExperimentKey(experimentMetadata.getExperimentKey()).build()) {

            System.out.printf("\nFound experiment: [%s] with key: %s\n",
                    experimentMetadata.getExperimentName(), experimentMetadata.getExperimentKey());

            List<Value> metrics = experiment.getMetrics();
            System.out.println("Metrics:");
            for (Value metric : metrics) {
                System.out.printf("\t%s : %s\n", metric.getName(), metric.getCurrent());
            }

            System.out.println("====================");
        }
    }
}
