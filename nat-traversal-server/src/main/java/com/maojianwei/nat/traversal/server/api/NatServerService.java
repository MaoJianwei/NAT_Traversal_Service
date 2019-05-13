package com.maojianwei.nat.traversal.server.api;

import com.maojianwei.nat.traversal.lib.NatService;
import java.util.Collection;

public interface NatServerService {

    /**
     * Add a nat service.
     *
     * @param serviceIdName unique service id
     * @param servicePort port of backend app
     * @return allocated port used to provide public access
     */
    int addService(String serviceIdName, int servicePort);

    /**
     * Remove a nat service.
     *
     * @param serviceIdName unique service id
     * @param publicPort port for public access, allocated by server before
     * @return 0 if success
     */
    int removeService(String serviceIdName, int publicPort);

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

    /**
     * Range of public port is [from, to]
     *
     * @param from first valid public port
     * @param to last valid public port
     */
    void updatePortRange(int from, int to);
}
