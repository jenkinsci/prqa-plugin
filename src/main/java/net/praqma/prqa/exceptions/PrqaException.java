package net.praqma.prqa.exceptions;

/**
 * @author jes
 */
public class PrqaException
        extends Exception {

    public PrqaException(String string) {
        super(string);
    }

    public PrqaException(Throwable cause) {
        super(cause);
    }

    public PrqaException(String msg,
                         Throwable cause) {
        super(msg, cause);
    }

    @Override
    public String toString() {
        return String.format("Caught exception with message:%n\t%s\nCaused by:%n\t%s", getMessage(), getCause());
    }
}

