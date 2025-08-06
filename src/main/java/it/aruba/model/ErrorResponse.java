package it.aruba.model;

import lombok.Data;

@Data
public class ErrorResponse {

    private String errorCode;
    private String message;
    private long timestamp;

    public ErrorResponse(String errorCode, String message, long timestamp) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = timestamp;
    }

}
