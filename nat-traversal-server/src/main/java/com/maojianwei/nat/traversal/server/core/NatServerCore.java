package com.maojianwei.nat.traversal.server.core;

import com.maojianwei.nat.traversal.lib.NatService;
import com.maojianwei.nat.traversal.server.api.NatServerService;
import com.maojianwei.wechat.message.notify.WeChatNotifyModule;
import com.maojianwei.wechat.message.notify.api.WeChatMessage;
import com.maojianwei.wechat.message.notify.lib.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static com.maojianwei.wechat.message.notify.api.WeChatMessageLevel.INFO;
import static java.lang.String.format;

@Service
@Order(1)
public class NatServerCore implements NatServerService {

    // Configs for WeChatNotifyModule

    @Value("${mao.wechat.notify.corp.id:}")
    private String corpId;

    @Value("${mao.wechat.notify.agent.id:-1}")
    private int agentId;

    @Value("${mao.wechat.notify.corp.secret:}")
    private String corpSecret;

    @Value("${mao.wechat.notify.default.receiver.user.id:}")
    private String defaultUserId; // default receiver

    @Value("${mao.wechat.notify.default.receiver.party.id:-1}")
    private int defaultPartyId; // default receiver

    @Value("${mao.wechat.notify.default.receiver.tag.id:-1}")
    private int defaultTagId; // default receiver


    private Logger logger = LoggerFactory.getLogger(NatServerCore.class);

    private WeChatNotifyModule weChatNotifyModule;

    private int portFrom = 30000;
    private int portTo = 39999;
    private int portPointer = portTo;

    private Map<Integer, String> serviceMapping = new HashMap<>(); //  publicPort -> serviceIdName
    private Map<String, NatService> serviceStore = new HashMap<>(); // serviceIdName -> service info
    private ReentrantLock serviceLock = new ReentrantLock();

    private ExecutorService taskPool = Executors.newCachedThreadPool();


    public NatServerCore() {
        weChatNotifyModule = new WeChatNotifyModule(corpId, agentId, corpSecret);
        weChatNotifyModule.start();
        taskPool.submit(new WeChatInitTask());
        taskPool.submit(new TunnelCheckTask());
    }


    @Override
    public int addService(String serviceIdName, int servicePort) {

        serviceLock.lock();

        if (serviceMapping.size() >= (portTo - portFrom + 1)) {
            serviceLock.unlock();
            return -1; // mapping is full
        }

        NatService oldService = serviceStore.get(serviceIdName);
        if (oldService != null) {
            serviceLock.unlock();
            if (servicePort == oldService.getServicePort()) {
                return oldService.getPublicPort(); // it may be that service reboots.
            } else {
                return -2; // more likely be conflicting serviceIdName
            }
        }

        // adjust pointer to updated range
        if (!(portPointer >= portFrom && portPointer <= portTo)) {
            portPointer = portFrom;
        }

        // find free port
        do {
            portPointer = ((portPointer - portFrom) + 1) % (portTo - portFrom + 1) + portFrom;
        } while (serviceMapping.containsKey(portPointer));

        NatService service = new NatService(serviceIdName, portPointer, servicePort);

        serviceMapping.put(portPointer, serviceIdName);
        serviceStore.put(serviceIdName, service);

        serviceLock.unlock();

        weChatNotifyModule.pushMessage(WeChatMessage.builder()
                .setAppId("NatClientCore").setLevel(INFO)
                .setMessage(format("Service %s added, port %d", serviceIdName, servicePort)).build());
        return portPointer;
    }

    @Override
    public int removeService(String serviceIdName, int publicPort) {

        serviceLock.lock();
        String idName = serviceMapping.get(publicPort);
        if (idName != null) {
            if (!idName.equals(serviceIdName)) {
                serviceLock.unlock();
                logger.warn("Service not match - {}", serviceIdName);
                return -2;
            }

            NatService service = serviceStore.get(serviceIdName);
            if (service == null) {
                serviceLock.unlock();
                logger.error("Service instance not found - {}", serviceIdName);
                return -3;
            }

            if (service.isOnline()) {
                killTunnel(service);
            }

            serviceStore.remove(serviceIdName);
            serviceMapping.remove(publicPort);
        } else {
            serviceLock.unlock();
            logger.warn("Mapping not found - {}", publicPort);
            return -1;
        }
        serviceLock.unlock();
        return 0;
    }

    private void killTunnel(NatService service) {
        // assure Tunnel PID is valid value.
        if (service.getTunnelPID() > 0) {
            try {
                String[] cmd = {"sh", "-c", String.format("kill -9 %d", service.getTunnelPID())};
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                logger.error("Fail to kill tunnel, {}, {}<-{}, {}, {}", service.getIdName(),
                        service.getPublicPort(), service.getServicePort(), service.getTunnelPID(), e.getClass());
            }
        }
    }

    @Override
    public void updatePortRange(int from, int to) {
        portFrom = from;
        portTo = to;
    }

    @Override
    public NatService getService(String serviceIdName) {
        return serviceStore.get(serviceIdName);
    }

    @Override
    public Collection<NatService> getServices() {
        return serviceStore.values();
    }

    private class WeChatInitTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (corpId != null && corpSecret != null) {
                    weChatNotifyModule.updateAuthentication(corpId, agentId, corpSecret);
                }

                if (defaultUserId != null) {
                    weChatNotifyModule.setDefaultReceiver(new WeChatUser(defaultUserId, defaultPartyId, defaultTagId));
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private class TunnelCheckTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                Map<Integer, Integer> onlineServices = findRunningServices(); // port -> pid

                serviceStore.values().forEach(s -> {
                    int pid = onlineServices.getOrDefault(s.getPublicPort(), -1);
                    if (pid != -1) {
                        s.setTunnelPID(pid);
                        s.setOnline(true);
                    } else {
                        s.setOnline(false);
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private Map<Integer, Integer> findRunningServices() {
            Map<Integer, Integer> onlineServices = new HashMap<>(); // port -> pid
            try {
                String[] cmd = {"sh", "-c", "netstat -ntlp | grep sshd | awk '{print $1,$4,$7}'"};
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] entry = line.split(" ");
                    if (entry[0].contains("tcp")) {
                        String[] listenPart = entry[1].split(":");
                        int port = Integer.valueOf(listenPart[listenPart.length - 1]);

                        String[] pidPart = entry[2].split("/");
                        int pid = Integer.valueOf(pidPart[0]);

                        onlineServices.put(port, pid);
                    }
                }
            } catch (IOException | NumberFormatException e) {
                logger.warn("Fail to deal service, {}", e.getClass());
            } catch (Exception e) {
                logger.error("Fail to deal service, {}", e.getClass());
            }
            return onlineServices;
        }
    }
}























