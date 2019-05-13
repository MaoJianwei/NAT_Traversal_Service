package com.maojianwei.nat.traversal.lib.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.maojianwei.nat.traversal.lib.NatService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatSummaryResult extends NatResult {

    private List<NatService> services;

    public NatSummaryResult() {
    }

    public NatSummaryResult(int errCode, Collection<NatService> services) {
        super(errCode);
        this.services = new ArrayList<>(services);
    }

    public List<NatService> getServices() {
        return services;
    }

    public void setServices(List<NatService> services) {
        this.services = services;
    }
}
