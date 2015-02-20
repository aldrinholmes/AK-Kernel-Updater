package com.aldrinholmes.akkernelupdater;

/**
 * Created by Mike on 1/4/2015.
 */
public class InvalidResponseException extends Exception {
    public InvalidResponseException() {
        super();
    }

    @Override
    public String toString() {
        return "Invalid Response: Could not connect to a valid update source. Try using a proxy.";
    }

}
