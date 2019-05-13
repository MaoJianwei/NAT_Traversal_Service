package com.maojianwei.nat.traversal.server.rest;

import com.maojianwei.nat.traversal.lib.NatService;
import com.maojianwei.nat.traversal.lib.result.NatAddResult;
import com.maojianwei.nat.traversal.lib.result.NatGetResult;
import com.maojianwei.nat.traversal.lib.result.NatRemoveResult;
import com.maojianwei.nat.traversal.lib.result.NatSummaryResult;
import com.maojianwei.nat.traversal.server.api.NatServerService;
import com.maojianwei.nat.traversal.server.core.NatServerCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Order(2)
@RestController
@RequestMapping("/nat")
public class NatWebController {

    private final String HTML_HEAD = "<html><head><title>Mao NAT Traversal Server</title></head><body>";
    private final String HTML_TABLE_HEAD = "<table border=\"1\" width=\"100%\" style=\"text-align: center\"><tr>" +
            "<th width=\"40%\">Service ID</th>" +
            "<th width=\"15%\">Public Port</th>" +
            "<th width=\"15%\">Service Port</th>" +
            "<th width=\"15%\">Tunnel PID</th>" +
            "<th width=\"15%\">Online</th></tr>";
    private final String HTML_TABLE_ENTRY_PATTERN = "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%b</td></tr>";
    private final String HTML_TABLE_TAIL = "</table>";
    private final String HTML_TAIL = "</body></html>";


    @Autowired
    private NatServerService natServerService;


    @GetMapping(value = "/addService/{serviceIdName}/{servicePort}", produces = {APPLICATION_JSON_UTF8_VALUE})
    public NatAddResult addService(@PathVariable("serviceIdName") String serviceIdName, @PathVariable("servicePort") Integer servicePort) {
        int publicPort = natServerService.addService(serviceIdName, servicePort);
        return new NatAddResult(publicPort > 0 ? 0 : publicPort, publicPort > 0 ? publicPort : 0);
    }

    @RequestMapping("/removeService/{serviceIdName}/{publicPort}")
    public NatRemoveResult removeService(@PathVariable("serviceIdName") String serviceIdName, @PathVariable("publicPort") Integer publicPort) {
        int ret = natServerService.removeService(serviceIdName, publicPort);
        return new NatRemoveResult(ret, ret == 0);
    }

    @RequestMapping("/service/{serviceIdName}")
    public NatGetResult getService(@PathVariable("serviceIdName") String serviceIdName) {
        NatService service = natServerService.getService(serviceIdName);
        return new NatGetResult(service != null ? 0 : -1, service);
    }

    @RequestMapping("/services")
    public NatSummaryResult getServices() {
        return new NatSummaryResult(0, natServerService.getServices());
    }

    @RequestMapping("")
    public String getUI() {
        StringBuilder sb = new StringBuilder();
        sb.append(HTML_HEAD);
        sb.append(HTML_TABLE_HEAD);

        natServerService.getServices().forEach(s -> sb.append(String.format(HTML_TABLE_ENTRY_PATTERN,
                s.getIdName(), s.getPublicPort(), s.getServicePort(), s.getTunnelPID(), s.isOnline())));

        sb.append(HTML_TABLE_TAIL);
        sb.append(HTML_TAIL);

        return sb.toString();
    }
}
