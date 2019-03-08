/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa;

import java.io.Serializable;
import hudson.util.Secret;

/**
 * @author Praqma
 */
public class QAVerifyServerSettings
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String host;
    public final int port;
    public final String protocol;
    public final Secret password;
    public final String user;

    public QAVerifyServerSettings(final String host,
                                  final int port,
                                  final String protocol,
                                  final Secret password,
                                  final String user) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.password = password;
        this.user = user;
    }

    public QAVerifyServerSettings() {
        this.host = "";
        this.port = 0;
        this.protocol = "";
        this.password = null;
        this.user = "";
    }
}
