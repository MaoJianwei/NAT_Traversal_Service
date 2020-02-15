package com.maojianwei.nat.traversal.client.core;

import com.maojianwei.nat.traversal.client.api.NatClientService;
import com.maojianwei.nat.traversal.lib.NatService;
import com.maojianwei.nat.traversal.lib.result.NatAddResult;
import com.maojianwei.nat.traversal.lib.result.NatRemoveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static com.maojianwei.nat.traversal.lib.result.NatAddResult.ADD_OK;
import static com.maojianwei.nat.traversal.lib.result.NatAddResult.CONFLICTING_SERVICE_ID_NAME;
import static java.lang.String.format;

@Service
@Order(1)
public class NatClientCore implements NatClientService {

    @Value("${mao.nat.server.addr:127.0.0.1}")
    private String natServerAddr;

    @Value("${mao.nat.server.port:6543}")
    private int natServerPort;

    @Value("${mao.nat.server.ssh.cert.login.username:mao}")
    private String sshCertLoginUsername;


    private Map<Integer, NatService> serviceStore = new HashMap<>(); // servicePort -> service info
    private ReentrantLock lock = new ReentrantLock();

    private ExecutorService taskPool = Executors.newCachedThreadPool();

    public NatClientCore() {
        taskPool.submit(new ServiceCheckTask());
    }

    public int registerService(String serviceIdName, int servicePort) {

        if (servicePort < 1 || servicePort > 65535)
            return -1;

        lock.lock();
        NatService old = serviceStore.get(servicePort);
        if (old != null) {
            lock.unlock();
            return old.getIdName().equals(serviceIdName) ? 0 : -2;
        }

        serviceStore.put(servicePort, new NatService(serviceIdName, servicePort));
        lock.unlock();

        return 0;
    }

    public int unregisterService(String serviceIdName, int servicePort) {

        if (servicePort < 1 || servicePort > 65535)
            return -1;

        lock.lock();
        NatService old = serviceStore.get(servicePort);
        lock.unlock();

        if (old == null) {
            return -2;
        }

        if (!old.getIdName().equals(serviceIdName)) {
            return -3;
        }

        old.disableService();
        return 0;
    }

    public NatService getService(String serviceIdName) {
        return serviceStore.values().stream()
                .filter(s -> s.getIdName().equals(serviceIdName))
                .findFirst().orElse(null);
    }

    public Collection<NatService> getServices() {
        return serviceStore.values();
    }


    private class ServiceCheckTask implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(ServiceCheckTask.class);

        @Override
        public void run() {

            List<NatService> invalidServices = new ArrayList<>();

            while (true) {
                Set<Integer> onlineTunnels = findRunningTunnels();
                serviceStore.values().forEach(s -> {
                    if (!s.isValid()) {
                        invalidServices.add(s);
                    } else {
                        if (s.getTunnelPID() != 0) {
                            // 3. verify ssh PID, set/unset online flag, and re-setup tunnel if needed
                            boolean online = onlineTunnels.contains(s.getTunnelPID());
                            s.setOnline(online);
                            if (online) {
                                s.resetReconnectTimer();
                            } else {
                                retrySshTunnel(s);
                            }
                        } else if (s.getPublicPort() == 0) {
                            // 1. send request to server, and save public port
                            // 2. start ssh, and save tunnel PID
                            NatAddResult addResult = new RestTemplate().getForObject(format("http://%s:%d/nat/addService/%s/%d",
                                    natServerAddr, natServerPort, s.getIdName(), s.getServicePort()), NatAddResult.class);

                            if (addResult == null)
                                return;

                            if (addResult.getErrCode() == ADD_OK) {
                                s.setPublicPort(addResult.getPublicPort());
                                startSshTunnel(s);
                            } else if (addResult.getErrCode() == CONFLICTING_SERVICE_ID_NAME) {
                                s.setPublicPort(-1);
                            }
                        } else {
                            logger.warn("Service is unavailable {}", s.getIdName());
                        }
                    }
                });

                if (!invalidServices.isEmpty()) {
                    // split to two parts.
                    // avoid to block interface functions for a long time.
                    invalidServices.forEach(s -> {
                        NatRemoveResult removeResult = new RestTemplate().getForObject(format("http://%s:%d/nat/removeService/%s/%d",
                                natServerAddr, natServerPort, s.getIdName(), s.getPublicPort()), NatRemoveResult.class);

                        if (removeResult == null)
                            return;

                        if (!removeResult.isSuccess()) {
                            logger.warn("Fail to remove service, {}, {}", s.getIdName(), removeResult.getErrCode());
                        }
                        stopSshTunnel(s);
                    });

                    lock.lock();
                    invalidServices.forEach(s -> serviceStore.remove(s.getServicePort()));
                    lock.unlock();

                    invalidServices.clear();
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private Set<Integer> findRunningTunnels() {
            Set<Integer> onlineTunnels = new HashSet<>();
            try {
                String[] cmd = {"sh", "-c", "netstat -ntp | grep ESTABLISHED | awk '{print $6,$7}'"};
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] status = line.split(" ");
                    if (status[0].equals("ESTABLISHED")) { // skip "(Not all processes could be identified, non-owned process info"
                        String[] pair = status[1].split("/");
                        if (pair.length == 2 && pair[1].equals("ssh")) {
                            onlineTunnels.add(Integer.valueOf(pair[0]));
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                logger.warn("Fail to find running tunnels. {}", e.getClass());
            }
            return onlineTunnels;
        }

        private void startSshTunnel(NatService service) {
            try {
                //"ssh -N -R 127.0.0.1:2222:127.0.0.1:22 mao@172.20.0.1";
                String[] cmd = {"ssh", "-N", "-R", format("%d:127.0.0.1:%d", service.getPublicPort(), service.getServicePort()),
                        format("%s@%s", sshCertLoginUsername,natServerAddr), "&"};
                Process process = Runtime.getRuntime().exec(cmd);
                Field pid = process.getClass().getDeclaredField("pid");
                pid.setAccessible(true);
                service.setTunnelPID(pid.getInt(process));
                service.resetReconnectTimer();
            } catch (IOException e) {
                logger.warn("Fail to start ssh for Service {}", service.getIdName());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.warn("Fail to get PID for Service {}. {}", service.getIdName(), e.getClass());
            }
        }

        private void retrySshTunnel(NatService service) {
            if (service.gggetAndDecrementReconnectTimer() == 0) {
                startSshTunnel(service);
            }
        }

        private void stopSshTunnel(NatService service) {
            // assure Tunnel PID is valid value.
            if (service.getTunnelPID() > 0) {
                try {
                    String[] cmd = {"sh", "-c", String.format("kill -9 %d", service.getTunnelPID())};
                    Runtime.getRuntime().exec(cmd);
                } catch (IOException e) {
                    logger.error("Fail to stop tunnel, {}, {}<-{}, {}, {}", service.getIdName(),
                            service.getPublicPort(), service.getServicePort(), service.getTunnelPID(), e.getClass());
                }
            }
        }
    }
}

















