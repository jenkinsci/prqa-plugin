/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa.exceptions;

/**
 * @author Praqma
 */
public class PrqaUploadException
        extends PrqaException {

    public PrqaUploadException(String message,
                               Exception ex) {
        super(message, ex);
    }
}


