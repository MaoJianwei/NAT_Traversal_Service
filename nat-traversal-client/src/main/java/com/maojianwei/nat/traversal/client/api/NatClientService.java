package com.maojianwei.nat.traversal.client.api;

import com.maojianwei.nat.traversal.lib.NatService;

import java.util.Collection;

public interface NatClientService {

    /**
     * Register a nat service.
     *
     * @param serviceIdName unique service id
     * @param servicePort port of backend app
     * @return 0 if success
     */
    int registerService(String serviceIdName, int servicePort);

    /**
     * Unregister a nat service.
     *
     * @param serviceIdName unique service id
     * @param servicePort port of backend app
     * @return 0 if success
     */
    int unregisterService(String serviceIdName, int servicePort);

    /**
     * Get detail info of one specific nat service.
     *
     * @param serviceIdName unique service id
     * @return detail info
     */
    NatService getService(String serviceIdName);

    /**
     * Get detail infos of all nat service.
     *
     * @return detail infos
     */
    Collection<NatService> getServices();
}
