/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.prqa;

import org.apache.commons.lang.StringUtils;

/**
 * @author Praqma
 */
public enum CodeUploadSetting {
    AllCode("All code uploaded"),
    None("No code uploaded"),
    OnlyNew("Only code not in VCS");

    private final String value;

    CodeUploadSetting(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static CodeUploadSetting getByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return None;
        }

        switch (value) {
            case "All code uploaded":
                return AllCode;
            case "No code uploaded":
                return None;
            default:
                return OnlyNew;
        }
    }
}

