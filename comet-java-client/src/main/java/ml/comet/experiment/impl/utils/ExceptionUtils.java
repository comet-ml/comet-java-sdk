package ml.comet.experiment.impl.utils;

import java.util.Objects;

/**
 * Utilities to handle exceptions.
 */
public class ExceptionUtils {

    /**
     * Unwraps provided throwable to the most specific.
     *
     * @param t the {@link Throwable} to be unwrapped.
     * @return the innermost {@link Throwable} or provided {@link Throwable} if there is no specific cause.
     */
    public static Throwable unwrap(Throwable t) {
        if (t == null) {
            return null;
        }
        Throwable rootCause = null;
        Throwable cause = t.getCause();
        while (cause != null && !Objects.equals(cause, rootCause)) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause == null ? t : rootCause;
    }
}
