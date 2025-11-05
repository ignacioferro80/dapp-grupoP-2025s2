package predictions.dapp.exceptions;

public class PerformanceDataException extends RuntimeException {

    public PerformanceDataException(String message) {
        super(message);
    }

    public PerformanceDataException(String message, Throwable cause) {
        super(message, cause);
    }
}