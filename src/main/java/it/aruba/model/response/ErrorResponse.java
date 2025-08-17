package it.aruba.model.response;

import lombok.Data;

@Data
public class ErrorResponse {

    private String errorCode;
    private long timestamp;

    public ErrorResponse(String errorCode, long timestamp) {
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

}
