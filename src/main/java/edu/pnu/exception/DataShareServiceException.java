package edu.pnu.exception;

public class DataShareServiceException extends RuntimeException {
    public DataShareServiceException(String message) {
        super(message);
    }
    public DataShareServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
