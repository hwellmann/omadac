/*
 * GRIDGAIN - OPEN CLOUD PLATFORM.
 * COPYRIGHT (C) 2005-2008 GRIDGAIN SYSTEMS. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE AS PUBLISHED BY THE FREE SOFTWARE FOUNDATION; EITHER
 * VERSION 2.1 OF THE LICENSE, OR (AT YOUR OPTION) ANY LATER
 * VERSION.
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  SEE THE
 * GNU LESSER GENERAL PUBLIC LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.util;

import java.net.*;
import java.io.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridNetworkHelper {
    /**
     * Cached local host address to make sure that every time the same
     * local host is returned.
     */
    private static InetAddress localHost = null;

    /**
     * Enforces singleton.
     */
    private GridNetworkHelper() {
        // No-op.
    }

    /**
     * Gets display name of the network interface this IP address belongs to.
     *
     * @param addr IP address for which to find network interface name.
     * @return Network interface name or <tt>null</tt> if can't be found.
     */
    public static String getNetworkInterfaceName(String addr) {
        assert addr != null : "ASSERTION [line=54, file=src/java/org/gridgain/grid/util/GridNetworkHelper.java]";

        //noinspection UnusedCatchParameter
        try {
            InetAddress inetAddr = InetAddress.getByName(addr);

            for (NetworkInterface itf : GridUtils.asIterable(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress itfAddr : GridUtils.asIterable(itf.getInetAddresses())) {
                    if (itfAddr.equals(inetAddr) == true) {
                        return itf.getDisplayName();
                    }
                }
            }
        }
        catch (UnknownHostException e) {
            return null;
        }
        catch (SocketException e) {
            return null;
        }

        return null;
    }

    /**
     * Determines whether current local host is different from previously cached.
     *
     * @return <tt>true</tt> or <tt>false</tt> depending on whether or not local host
     *      has changed from the cached value.
     * @throws IOException If attempt to get local host failed.
     */
    public static synchronized boolean isLocalHostChanged() throws IOException {
        return localHost != null && resetLocalHost().equals(localHost) == false;
    }

    /**
     * Gets local host. Implementation will first attempt to get a non-loopback
     * address. If that fails, then loopback address will be returned.
     * <p>
     * Note that this method is synchronized to make sure that local host
     * initialization happens only once.
     *
     * @return Address representing local host.
     * @throws IOException If attempt to get local host failed.
     */
    public static synchronized InetAddress getLocalHost() throws IOException {
        if (localHost == null) {
            // Cache it.
            localHost = resetLocalHost();
        }

        return localHost;
    }

    /**
     *
     * @return FIXDOC
     * @throws IOException If attempt to get local host failed.
     */
    private static InetAddress resetLocalHost() throws IOException {
        localHost = InetAddress.getLocalHost();

        // It should not take longer than 2 seconds to reach
        // local address on any network.
        final int reachTimeout = 2000;

        if (localHost.isLoopbackAddress() == true || localHost.isReachable(reachTimeout) == false) {
            for (NetworkInterface itf : GridUtils.asIterable(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : GridUtils.asIterable(itf.getInetAddresses())) {
                    if (addr.isLoopbackAddress() == false && addr.isLinkLocalAddress() == false &&
                        addr.isReachable(reachTimeout) == true) {
                        localHost = addr;

                        break;
                    }
                }
            }
        }

        return localHost;
    }
}
