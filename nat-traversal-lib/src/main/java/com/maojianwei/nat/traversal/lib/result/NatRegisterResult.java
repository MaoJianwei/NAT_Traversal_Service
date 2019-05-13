package com.maojianwei.nat.traversal.lib.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatRegisterResult extends NatResult {

    private boolean success;

    public NatRegisterResult() {
        this.success = false;
    }

    public NatRegisterResult(int errCode, boolean success) {
        super(errCode);
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
