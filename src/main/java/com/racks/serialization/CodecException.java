package com.racks.serialization;

/** A stored item stack could not be read back. Never swallowed: the row is kept and the rack is skipped. */
public class CodecException extends Exception {

    public CodecException(String message) {
        super(message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
