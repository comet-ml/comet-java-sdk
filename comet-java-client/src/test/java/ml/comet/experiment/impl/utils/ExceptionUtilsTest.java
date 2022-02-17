package ml.comet.experiment.impl.utils;

import ml.comet.experiment.exception.CometGeneralException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionUtilsTest {

    @Test
    public void testUnwrap() {
        // Test exception without cause
        CometGeneralException root = new CometGeneralException("Root cause");
        Throwable unwrapped = ExceptionUtils.unwrap(root);
        assertEquals(root, unwrapped, "wrong unwrapped exception");

        // Test exception with cause
        RuntimeException composite = new RuntimeException("Composite exception 1-st level",
                new RuntimeException("Composite exception 2-nd level", root));
        unwrapped = ExceptionUtils.unwrap(composite);
        assertEquals(root, unwrapped, "wrong unwrapped exception");
    }
}
