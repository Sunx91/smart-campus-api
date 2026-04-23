package com.sunath.smartcampus.model;

/**
 * Standardised error payload returned by every ExceptionMapper.
 * All fields are serialised as JSON by Jersey/Jackson.
 */
public class ErrorMessage {

    private String errorMessage;
    private int errorCode;
    private String documentation;

    /** Required by Jackson. */
    public ErrorMessage() {
    }

    public ErrorMessage(String errorMessage, int errorCode, String documentation) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.documentation = documentation;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
