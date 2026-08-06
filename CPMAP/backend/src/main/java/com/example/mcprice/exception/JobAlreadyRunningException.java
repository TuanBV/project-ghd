package com.example.mcprice.exception;

public class JobAlreadyRunningException extends RuntimeException {
    public JobAlreadyRunningException(String jobKey) {
        super("Job '" + jobKey + "' dang chay, khong the chay chong lap");
    }
}
