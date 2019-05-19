package io.vertx;

public enum MessagingErrorCodes {

    INSERT_FAILURE("Insert Failed: "),
    AUTHENTICATION_FAILED("Authentication Failed: "),
    NOT_FOUND("Not found: "),
    UPDATE_FAILURE("Update Failure: "),
    UNKNOWN_ACTION("Unknown Action: "),
    DELETE_FAILURE("Deletion failed: ");

    private MessagingErrorCodes(String msg){
        this.message = msg;
    }

    public final String message;

}
