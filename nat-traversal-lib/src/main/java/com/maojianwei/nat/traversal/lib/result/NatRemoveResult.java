package com.maojianwei.nat.traversal.lib.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatRemoveResult extends NatResult {

    private boolean success;

    public NatRemoveResult() {
    }

    public NatRemoveResult(int errCode, boolean success) {
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
