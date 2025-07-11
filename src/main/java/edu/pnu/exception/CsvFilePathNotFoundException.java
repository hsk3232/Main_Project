package edu.pnu.exception;

public class CsvFilePathNotFoundException extends RuntimeException {
    public CsvFilePathNotFoundException(String message) { super(message); }
}