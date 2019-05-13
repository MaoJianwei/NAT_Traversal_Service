package com.maojianwei.nat.traversal.lib.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.maojianwei.nat.traversal.lib.NatService;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatGetResult extends NatResult {

    private NatService service;

    public NatGetResult() {
    }

    public NatGetResult(int errCode, NatService service) {
        super(errCode);
        this.service = service;
    }

    public NatService getService() {
        return service;
    }

    public void setService(NatService service) {
        this.service = service;
    }
}
