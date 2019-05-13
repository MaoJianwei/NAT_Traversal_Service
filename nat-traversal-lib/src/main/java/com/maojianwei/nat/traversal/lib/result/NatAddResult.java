package com.maojianwei.nat.traversal.lib.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatAddResult extends NatResult {

    public static final int ADD_OK = 0;
    public static final int MAPPING_FULL = -1;
    public static final int CONFLICTING_SERVICE_ID_NAME = -2;


    private int publicPort;

    private NatAddResult() {
        publicPort = Integer.MIN_VALUE;
    }

    public NatAddResult(int errCode, int publicPort) {
        super(errCode);
        this.publicPort = publicPort;
    }

    public void setPublicPort(int publicPort) {
        this.publicPort = publicPort;
    }

    public int getPublicPort() {
        return publicPort;
    }
}
