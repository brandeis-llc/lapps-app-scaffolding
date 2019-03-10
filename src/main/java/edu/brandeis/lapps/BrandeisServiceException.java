package edu.brandeis.lapps;

public class BrandeisServiceException extends Exception {

    public BrandeisServiceException(String message) {
        super(message);
    }

    public BrandeisServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
