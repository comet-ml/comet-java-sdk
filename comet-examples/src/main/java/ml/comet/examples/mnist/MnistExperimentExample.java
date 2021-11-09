package ml.comet.examples.mnist;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.OnlineExperimentImpl;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * https://github.com/eclipse/deeplearning4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/quickstart/modeling/feedforward/classification/MNISTSingleLayer.java
 * A Simple Multi Layered Perceptron (MLP) applied to digit classification for
 * the MNIST Dataset (http://yann.lecun.com/exdb/mnist/).
 *
 * <p>This file builds one input layer and one hidden layer.
 *
 * <p>The input layer has input dimension of numRows*numColumns where these variables indicate the
 * number of vertical and horizontal pixels in the image. This layer uses a rectified linear unit
 * (relu) activation function. The weights for this layer are initialized by using Xavier initialization
 * (https://prateekvjoshi.com/2016/03/29/understanding-xavier-initialization-in-deep-neural-networks/)
 * to avoid having a steep learning curve. This layer will have 1000 output signals to the hidden layer.
 *
 * <p>The hidden layer has input dimensions of 1000. These are fed from the input layer. The weights
 * for this layer is also initialized using Xavier initialization. The activation function for this
 * layer is a softmax, which normalizes all the 10 outputs such that the normalized sums
 * add up to 1. The highest of these normalized values is picked as the predicted class.
 */
public final class MnistExperimentExample {
    private static final Logger log = LoggerFactory.getLogger(MnistExperimentExample.class);
    /**
     * The number of epochs to perform.
     */
    @Parameter(names = {"--epochs", "-e"}, description = "number of epochs to perform")
    int numEpochs = 2;

    /**
     * The experiment entry point.
     *
     * <p>You need to set three environment variables to run this experiment:
     * <ul>
     *      <li>COMET_API_KEY - the API key to access Comet</li>
     *      <li>COMET_WORKSPACE_NAME - the name of the workspace for your project</li>
     *      <li>COMET_PROJECT_NAME - the name of the project.</li>
     * </ul>
     *
     * <p>Alternatively you can set these values in the <strong>resources/application.conf</strong> file
     *
     * @param args the command line arguments.
     * @throws Exception if any exception occurs during experiment execution.
     */
    public static void main(String[] args) throws Exception {
        MnistExperimentExample main = new MnistExperimentExample();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);

        // update application.conf or provide environment variables as described in JavaDoc.
        OnlineExperiment experiment = OnlineExperimentImpl.builder().build();
        experiment.setInterceptStdout();
        try {
            main.runMnistExperiment(experiment);
        } catch (Exception e) {
            System.out.println("--- Failed to run experiment ---");
            e.printStackTrace();
        } finally {
            // make sure to close experiment
            experiment.end();
        }
    }

    /**
     * The experiment runner.
     *
     * @param experiment the Comet experiment instance.
     * @throws IOException if any exception raised.
     */
    public void runMnistExperiment(OnlineExperiment experiment) throws IOException {
        log.info("****************MNIST Experiment Example Started********************");

        //number of rows and columns in the input pictures
        final int numRows = 28;
        final int numColumns = 28;
        int outputNum = 10; // number of output classes
        int batchSize = 128; // batch size for each epoch
        int rngSeed = 123; // random number seed for reproducibility

        experiment.logParameter("numRows", numRows);
        experiment.logParameter("numColumns", numColumns);
        experiment.logParameter("outputNum", outputNum);
        experiment.logParameter("batchSize", batchSize);
        experiment.logParameter("rngSeed", rngSeed);
        experiment.logParameter("numEpochs", numEpochs);

        double lr = 0.006;
        double nesterovsMomentum = 0.9;
        double l2Regularization = 1e-4;

        experiment.logParameter("learningRate", lr);
        experiment.logParameter("nesterovsMomentum", nesterovsMomentum);
        experiment.logParameter("l2Regularization", l2Regularization);

        OptimizationAlgorithm optimizationAlgorithm = OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;
        experiment.logParameter("optimizationAlgorithm", OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT);

        log.info("Build model....");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed) //include a random seed for reproducibility
                // use stochastic gradient descent as an optimization algorithm
                .updater(new Nesterovs(lr, nesterovsMomentum))
                .optimizationAlgo(optimizationAlgorithm)
                .l2(l2Regularization)
                .list()
                .layer(new DenseLayer.Builder() //create the first, input layer with xavier initialization
                        .nIn(numRows * numColumns)
                        .nOut(1000)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                        .nIn(1000)
                        .nOut(outputNum)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .build();

        experiment.logGraph(conf.toJson());

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        //print the score with every 1 iteration
        model.setListeners(new StepScoreListener(experiment, 1, log));

        // Get the train dataset iterator
        DataSetIterator mnistTrain = new MnistDataSetIterator(batchSize, true, rngSeed);

        log.info("Train model....");
        model.fit(mnistTrain, numEpochs);

        // Get the test dataset iterator
        DataSetIterator mnistTest = new MnistDataSetIterator(batchSize, false, rngSeed);

        log.info("Evaluate model....");
        Evaluation eval = model.evaluate(mnistTest);
        log.info(eval.stats());

        experiment.logHtml(eval.getConfusionMatrix().toHTML(), false);

        log.info("****************MNIST Experiment Example finished********************");
    }

    /**
     * The listener to be invoked at each iteration of model training.
     */
    static class StepScoreListener extends BaseTrainingListener {
        private final OnlineExperiment experiment;
        private int printIterations;
        private final Logger log;

        StepScoreListener(OnlineExperiment experiment, int printIterations, Logger log) {
            this.experiment = experiment;
            this.printIterations = printIterations;
            this.log = log;
        }

        @Override
        public void iterationDone(Model model, int iteration, int epoch) {
            if (printIterations <= 0) {
                printIterations = 1;
            }
            // print score and log metric
            if (iteration % printIterations == 0) {
                double result = model.score();
                log.info("Score at step/epoch {}/{}  is {} ", iteration, epoch, result);
                experiment.setEpoch(epoch);
                this.experiment.logMetric("score", model.score(), iteration);
            }
        }
    }
}
