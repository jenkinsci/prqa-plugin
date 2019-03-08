package net.praqma.prqa;

import java.io.Serializable;

public interface ReportSettings
        extends Serializable {
    String getProduct();

    boolean publishToQAV();
}
