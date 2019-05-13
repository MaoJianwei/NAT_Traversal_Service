package com.maojianwei.nat.traversal.client.rest;

import com.maojianwei.nat.traversal.client.api.NatClientService;
import com.maojianwei.nat.traversal.client.core.NatClientCore;
import com.maojianwei.nat.traversal.lib.NatService;
import com.maojianwei.nat.traversal.lib.result.NatGetResult;
import com.maojianwei.nat.traversal.lib.result.NatRegisterResult;
import com.maojianwei.nat.traversal.lib.result.NatSummaryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Order(2)
@RequestMapping("/nat")
public class NatWebController {

    private final String HTML_HEAD = "<html><head><title>Mao NAT Traversal Client</title></head><body>";
    private final String HTML_TABLE_HEAD = "<table border=\"1\" width=\"100%\" style=\"text-align: center\"><tr>" +
            "<th width=\"40%\">Service ID</th>" +
            "<th width=\"12%\">Public Port</th>" +
            "<th width=\"12%\">Service Port</th>" +
            "<th width=\"12%\">Tunnel PID</th>" +
            "<th width=\"12%\">Online</th>" +
            "<th width=\"12%\">Retry Timer</th></tr>";
    private final String HTML_TABLE_ENTRY_PATTERN = "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%b</td><td>%d</td></tr>";
    private final String HTML_TABLE_TAIL = "</table>";
    private final String HTML_TAIL = "</body></html>";


    @Autowired
    private NatClientService natClientService;


    @RequestMapping("/registerService/{serviceIdName}/{servicePort}")
    public NatRegisterResult register(@PathVariable String serviceIdName, @PathVariable int servicePort) {
        int ret = natClientService.registerService(serviceIdName, servicePort);
        return new NatRegisterResult(ret, ret == 0);
    }

    @RequestMapping("/unregisterService/{serviceIdName}/{servicePort}")
    public NatRegisterResult unregister(@PathVariable String serviceIdName, @PathVariable int servicePort) {
        int ret = natClientService.unregisterService(serviceIdName, servicePort);
        return new NatRegisterResult(ret, ret == 0); // reuse NatRegisterResult
    }

    @RequestMapping("/service/{serviceIdName}")
    public NatGetResult getService(@PathVariable("serviceIdName") String serviceIdName) {
        NatService service = natClientService.getService(serviceIdName);
        return new NatGetResult(service != null ? 0 : -1, service);
    }

    @RequestMapping("/services")
    public NatSummaryResult getServices() {
        return new NatSummaryResult(0, natClientService.getServices());
    }

    @RequestMapping("")
    public String getUI() {
        StringBuilder sb = new StringBuilder();
        sb.append(HTML_HEAD);
        sb.append(HTML_TABLE_HEAD);

        natClientService.getServices().forEach(s -> sb.append(String.format(HTML_TABLE_ENTRY_PATTERN,
                s.getIdName(), s.getPublicPort(), s.getServicePort(), s.getTunnelPID(),
                s.isOnline(), s.getReconnectTimer())));

        sb.append(HTML_TABLE_TAIL);
        sb.append(HTML_TAIL);

        return sb.toString();
    }
}
