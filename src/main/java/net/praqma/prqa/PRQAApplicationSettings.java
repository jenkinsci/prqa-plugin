/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa;

import java.io.Serializable;


public class PRQAApplicationSettings
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String productHome;

    public PRQAApplicationSettings(final String productHome) {
        this.productHome = productHome;
    }

    public static String addSlash(String value,
                                  String pathSeperator) {
        if (value.endsWith(pathSeperator)) {
            return value;
        } else {
            return value + pathSeperator;
        }
    }
}
