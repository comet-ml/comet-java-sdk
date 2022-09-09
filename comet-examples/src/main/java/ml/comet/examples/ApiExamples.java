package ml.comet.examples;

import ml.comet.experiment.CometApi;
import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.model.Value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    static final String someExperimentName = "some-experiment-name";

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
        String projectName;
        String workspaceName;
        try (OnlineExperiment experiment = ExperimentBuilder
                .OnlineExperiment()
                .interceptStdout()
                .build()) {

            experiment.setExperimentName(someExperimentName);
            experiment.logMetric("some-metric", 10);

            projectName = experiment.getProjectName();
            workspaceName = experiment.getWorkspaceName();
        }

        // get test experiment by name
        //
        try (CometApi api = ExperimentBuilder.CometApi().build()) {
            // get project where experiment saved
            List<Project> projects = api.getAllProjects(workspaceName);
            Optional<Project> optionalProject = projects.stream()
                    .filter(project -> project.getProjectName().equals(projectName))
                    .findAny();
            if (!optionalProject.isPresent()) {
                return;
            }
            Project project = optionalProject.get();
            System.out.printf("Looking for experiments in project: %s\n---------\n", project);

            // list all experiments in the project and select the one we are looking for
            List<ExperimentMetadata> experiments = api.getAllExperiments(project.getProjectId());
            Optional<ExperimentMetadata> experimentMetadata = experiments.stream()
                    .peek(System.out::println)
                    .filter(meta -> Objects.equals(meta.getExperimentName(), someExperimentName))
                    .findAny();
            if (experimentMetadata.isPresent()) {
                displayExperiment(experimentMetadata.get());
            } else {
                System.out.printf("Failed to find experiment with name: %s\n", someExperimentName);
            }
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
