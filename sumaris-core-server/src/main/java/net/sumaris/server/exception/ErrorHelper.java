package net.sumaris.server.exception;

public final class ErrorHelper {

    private ErrorHelper() {
        // helper class
    }

    /**
     * <p>getInternalServerErrorMessage.</p>
     *
     * @param errorCode a int.
     * @param message a {@link String} object.
     * @return a {@link String} object.
     */
    public static String toJsonErrorString(int errorCode, String message) {
        return String.format("{\"code\": %s, \"message\": \"%s\"}", errorCode, message);
    }
}
