package ml.comet.experiment.impl;

import io.reactivex.rxjava3.functions.Action;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The unit tests of the {@link ml.comet.experiment.OnlineExperiment} implementation
 */
@DisplayName("OnlineExperimentImplTest UNIT")
@Tag("unit")
public class OnlineExperimentImplTest {

    /**
     * Tests that assets-in-progress counter is properly maintained during asynchronous action processing.
     */
    @Test
    public void testExecuteLogAction() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            AtomicBoolean stopAction = new AtomicBoolean(false);
            Action testAction = createAsyncNoopAction(stopAction, experiment.getLogAssetOnCompleteAction());

            assertEquals(0, experiment.getAssetsInProgress().get(), "must be zero");

            // execute action for assets and check that assets counter was incremented
            experiment.executeLogAction(testAction, experiment.getAssetsInProgress(), "failed");
            assertEquals(1, experiment.getAssetsInProgress().get(), "wrong number of assets in progress");

            // stop asynchronous action and check that assets counter was decremented
            //
            stopAction.set(true);

            Awaitility.await("assets counter must be decreased")
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.SECONDS)
                    .until(() -> experiment.getAssetsInProgress().get() == 0);

            assertEquals(0, experiment.getAssetsInProgress().get(), "assets counter must be zero");

        } catch (Throwable throwable) {
            fail(throwable);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Action createAsyncNoopAction(final AtomicBoolean stop, Optional<Action> onComplete) {
        return () -> new Thread(() -> {
            Awaitility.await("failed to wait for action stop")
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .atMost(5, TimeUnit.SECONDS)
                    .until(stop::get);

            if (onComplete.isPresent()) {
                try {
                    onComplete.get().run();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
