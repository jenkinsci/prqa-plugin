/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.exceptions;

/**
 * @author Praqma
 */
public class PrqaReadingException
        extends PrqaException {

    public PrqaReadingException(String message) {
        super(message);
    }

    public PrqaReadingException(String message,
                                Exception ex) {
        super(message, ex);
    }
}

