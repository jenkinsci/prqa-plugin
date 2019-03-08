/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.prqma.prqa.qaframework;

import java.io.Serializable;

/**
 * @author Praqma
 */
public class QAFApplicationSettings
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String qafHome;
    public final String qavClientHome;
    public final String productHome;

    public QAFApplicationSettings(final String qafHome,
                                  final String qavClientHome,
                                  final String productHome) {
        this.qafHome = qafHome;
        this.qavClientHome = qavClientHome;
        this.productHome = productHome;
    }

    public static String resolveQacliExe(boolean isUnix) {
        return "qacli";
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
