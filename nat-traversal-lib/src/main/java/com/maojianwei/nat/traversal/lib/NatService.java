package com.maojianwei.nat.traversal.lib;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.concurrent.atomic.AtomicInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NatService {

    private String idName;

    private int publicPort; // service public port, -1 means Server bans this service
    private int servicePort; // server local port

    private int tunnelPID;

    private boolean valid; // false - unregistered and wait for cutting down
    private boolean online; // 1. public port allocated 2. tunnel setup (port opened, sshd PID existed)
    private AtomicInteger reconnectTimer;
    private static final int RECONNECT_TIMEOUT = 5; // seconds


    public NatService(String idName, int servicePort) {
        this(idName, 0, servicePort);
    }

    public NatService(String idName, int publicPort, int servicePort) {
        this.idName = idName;
        this.publicPort = publicPort;
        this.servicePort = servicePort;
        this.tunnelPID = 0;

        this.valid = true;
        this.online = false;
        this.reconnectTimer = new AtomicInteger(RECONNECT_TIMEOUT);
    }

    public void disableService() {
        this.reconnectTimer.getAndSet(Integer.MAX_VALUE); // attempt to disable retry reconnecting
        setValid(false);
    }

    public void resetReconnectTimer() {
        // avoid to override invalid timer value
        int old = this.reconnectTimer.get();
        if (old < RECONNECT_TIMEOUT) {
            this.reconnectTimer.compareAndSet(old, RECONNECT_TIMEOUT);
        }
    }

    // ggget for avoiding conflict with getter method.
    // for reconnection checking
    public int gggetAndDecrementReconnectTimer() {
        return this.reconnectTimer.getAndDecrement();
    }

    // for monitor
    public int getReconnectTimer() {
        return this.reconnectTimer.get();
    }


    public String getIdName() {
        return idName;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public int getServicePort() {
        return servicePort;
    }

    public int getTunnelPID() {
        return tunnelPID;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isOnline() {
        return online;
    }


    // set false when unregister one service.
    private void setValid(boolean valid) {
        this.valid = valid;
    }

    public void setPublicPort(int publicPort) {
        this.publicPort = publicPort;
    }

    public void setTunnelPID(int tunnelPID) {
        this.tunnelPID = tunnelPID;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
