package edu.pnu.exception;

public class CsvFileNotFoundException extends RuntimeException {
    public CsvFileNotFoundException(String message) { super(message); }
}
