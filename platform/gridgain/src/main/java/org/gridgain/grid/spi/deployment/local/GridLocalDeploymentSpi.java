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

package org.gridgain.grid.spi.deployment.local;

import java.util.*;
import java.util.Map.*;
import java.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.*;
import org.gridgain.jsr305.*;

/**
 * Local deployment SPI that implements only within VM deployment on local
 * node via {@link #register(ClassLoader, Class)} method. This SPI requires
 * no configuration.
 * <p>
 * Note that if peer class loading is enabled (which is default behavior,
 * see {@link GridConfiguration#isPeerClassLoadingEnabled()}), then it is
 * enough to deploy a task only on one node and all other nodes will load
 * required classes from the node that initiated task execution.
 * <p>
 * <h1 class="header">Configuration</h1>
 * This SPI requires no configuration.
 * <h2 class="header">Example</h2>
 * There is no point to explicitely configure <tt>GridLocalDeploymentSpi</tt>
 * with {@link GridConfiguration} as it is used by default and has no
 * configuration parameters.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridDeploymentSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridLocalDeploymentSpi extends GridSpiAdapter implements  GridDeploymentSpi, GridLocalDeploymentSpiMBean {
    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** List of all deployed class loaders. */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private final LinkedList<ClassLoader> clsLdrs = new LinkedList<ClassLoader>();

    /** Map of all resources. */
    private Map<ClassLoader, Map<String, String>> ldrRsrcs =
        new HashMap<ClassLoader, Map<String, String>>();

    /** Deployment SPI listener.    */
    private volatile GridDeploymentListener lsnr = null;

    /** Mutex. */
    private final Object mux = new Object();

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        registerMBean(gridName, this, GridLocalDeploymentSpiMBean.class);

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        List<ClassLoader> tmpClsLdrs = null;

        synchronized (mux) {
            tmpClsLdrs = new ArrayList<ClassLoader>(clsLdrs);
        }

        for (ClassLoader ldr : tmpClsLdrs) {
            onClassLoaderReleased(ldr);
        }

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentResource findResource(String rsrcName) {
        assert rsrcName != null : "ASSERTION [line=122, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

        synchronized (mux) {
            // Last updated class loader has highest priority in search.
            for (ClassLoader ldr : clsLdrs) {
                Map<String, String> rsrcs = ldrRsrcs.get(ldr);

                // Return class if it was found in rsrcs map.
                if (rsrcs != null && rsrcs.containsKey(rsrcName) == true) {
                    String clsName = rsrcs.get(rsrcName);

                    // Recalculate resource name in case if access is performed by
                    // class name and not the resource name.
                    rsrcName = getResourceName(clsName, rsrcs);

                    assert clsName != null : "ASSERTION [line=137, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

                    //noinspection UnusedCatchParameter
                    try {
                        Class<?> cls = ldr.loadClass(clsName);

                        assert cls != null : "ASSERTION [line=143, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

                        // Return resource.
                        return new GridDeploymentResourceAdapter(rsrcName, cls, ldr);
                    }
                    catch (ClassNotFoundException e) {
                        // No-op.
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets resource name for a given class name.
     *
     * @param clsName Class name.
     * @param rsrcs Map of resources.
     * @return Resource name.
     */
    private String getResourceName(String clsName, Map<String, String> rsrcs) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=166, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

        String rsrcName = clsName;

        for (Entry<String, String> e : rsrcs.entrySet()) {
            if (e.getValue().equals(clsName) == true &&
                e.getKey().equals(clsName) == false) {
                rsrcName = e.getKey();

                break;
            }
        }

        return rsrcName;
    }

    /**
     * {@inheritDoc}
     */
    public boolean register(ClassLoader ldr, Class<?> rsrc) throws GridSpiException {
        GridArgumentCheck.checkNull(ldr, "ldr");
        GridArgumentCheck.checkNull(rsrc, "rsrc");

        List<ClassLoader> removedClsLdrs = new ArrayList<ClassLoader>();

        Map<String, String> newRsrcs = null;

        synchronized (mux) {
            Map<String, String> clsLdrRsrcs = null;

            clsLdrRsrcs = ldrRsrcs.get(ldr);

            if (clsLdrRsrcs == null) {
                clsLdrRsrcs = new HashMap<String, String>();
            }

            newRsrcs = addResource(ldr, clsLdrRsrcs, rsrc);

            if (newRsrcs != null && newRsrcs.isEmpty() == false) {
                removeResources(ldr, newRsrcs, removedClsLdrs);
            }

            if (ldrRsrcs.containsKey(ldr) == false) {
                assert clsLdrs.contains(ldr) == false : "ASSERTION [line=209, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

                clsLdrs.addFirst(ldr);

                ldrRsrcs.put(ldr, clsLdrRsrcs);
            }
        }

        for (ClassLoader cldLdr : removedClsLdrs) {
            onClassLoaderReleased(cldLdr);
        }

        return newRsrcs != null && newRsrcs.isEmpty() == false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean unregister(String rsrcName) {
        List<ClassLoader> removedClsLdrs = new ArrayList<ClassLoader>();

        boolean removed;

        synchronized (mux) {
            Map<String, String> rsrcs = new HashMap<String, String>(1);

            rsrcs.put(rsrcName, rsrcName);

            removed = removeResources(null, rsrcs, removedClsLdrs);
        }

        for (ClassLoader cldLdr : removedClsLdrs) {
            onClassLoaderReleased(cldLdr);
        }

        return removed;
    }

    /**
     * Add new classes in class loader resource map.
     * Note that resource map may contain two entries for one added class:
     * task name -> class name and class name -> class name.
     *
     * @param ldr Registered class loader.
     * @param ldrRsrcs Class loader resources.
     * @param cls Registered classes collection.
     * @return Map of new resources added for registered class loader.
     * @throws GridSpiException If resource already registered. Exception thrown
     * if registered resources conflicts with rule when all task classes must be
     * annotated with different task names.
     */
    private Map<String, String> addResource(ClassLoader ldr, Map<String, String> ldrRsrcs, Class<?> cls)
        throws GridSpiException {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=262, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";
        assert ldr != null : "ASSERTION [line=263, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";
        assert ldrRsrcs != null : "ASSERTION [line=264, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";
        assert cls != null : "ASSERTION [line=265, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

        // Maps resources to classes.
        // Map may contain 2 entries for one class.
        Map<String, String> regRsrcs = new HashMap<String, String>(2, 1.0f);

        // Check alias collision for added classes.
        String alias = null;

        if (GridTask.class.isAssignableFrom(cls) == true) {
            GridTaskName nameAnn = GridUtils.getAnnotation(cls, GridTaskName.class);

            if (nameAnn != null) {
                alias = nameAnn.value();
            }
        }

        if (alias != null) {
            regRsrcs.put(alias, cls.getName());
        }

        regRsrcs.put(cls.getName(), cls.getName());

        Map<String, String> newRsrcs = null;

        // Check collisions for added classes.
        for (Entry<String, String> entry : regRsrcs.entrySet()) {
            if (ldrRsrcs.containsKey(entry.getKey()) == true) {
                String newAlias = entry.getKey();
                String newName = entry.getValue();

                String existingCls = ldrRsrcs.get(newAlias);

                // Different classes for the same resource name.
                if (ldrRsrcs.get(newAlias).equals(newName) == false) {
                    throw (GridSpiException)new GridSpiException("Failed to register resources with given task name " +
                        "(found another class with same task name in the same class loader) [taskName=" + newAlias +
                        ", existingCls=" + existingCls + ", newCls=" + newName + ", ldr=" + ldr + ']').setData(300, "src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java");
                }
            }
            // Add resources that should be removed for another class loaders.
            else {
                if (newRsrcs == null) {
                    newRsrcs = new HashMap<String, String>(regRsrcs.size());
                }

                newRsrcs.put(entry.getKey(), entry.getValue());
            }
        }

        // New resources to register. Add it all.
        if (newRsrcs != null) {
            ldrRsrcs.putAll(newRsrcs);
        }

        return newRsrcs;
    }

    /**
     * Remove resources for all class loaders except <tt>ignoreClsLdr</tt>.
     *
     * @param clsLdrToIgnore Ignored class loader or <tt>null</tt> to remove for all class loaders.
     * @param rsrcs Resources that should be used in search for class loader to remove.
     * @param removedClsLdrs Class loaders to remove.
     * @return <tt>True</tt> if resource was removed.
     */
    private boolean removeResources(ClassLoader clsLdrToIgnore, Map<String, String> rsrcs,
        List<ClassLoader> removedClsLdrs) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=333, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";
        assert rsrcs != null : "ASSERTION [line=334, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

        boolean res = false;

        for (Iterator<ClassLoader> iter = clsLdrs.iterator(); iter.hasNext() == true;) {
            ClassLoader ldr = iter.next();

            boolean isRemoved = false;

            if (clsLdrToIgnore == null || ldr.equals(clsLdrToIgnore) == false) {
                Map<String, String> clsLdrRrsrcs = ldrRsrcs.get(ldr);

                assert clsLdrRrsrcs != null : "ASSERTION [line=346, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

                // Check classloader's registered resources.
                for (String rsrcName : rsrcs.keySet()) {
                    // Remove classloader if resource found.
                    if (clsLdrRrsrcs.containsKey(rsrcName) == true) {
                        iter.remove();

                        ldrRsrcs.remove(ldr);

                        // Add class loaders in collection to notify listener outside synchronization block.
                        removedClsLdrs.add(ldr);

                        isRemoved = true;
                        res = true;

                        break;
                    }
                }

                if (isRemoved == true) {
                    continue;
                }

                // Check is possible to load resources with classloader.
                for (Entry<String, String> entry : rsrcs.entrySet()) {
                    // Check classes with class loader only when classes points to classes to avoid redundant check.
                    // Resources map contains two entries for class with task name(alias).
                    if (entry.getKey().equals(entry.getValue()) == true &&
                        isResourceExist(ldr, entry.getKey()) == true) {
                        iter.remove();

                        ldrRsrcs.remove(ldr);

                        // Add class loaders in collection to notify listener outside synchronization block.
                        removedClsLdrs.add(ldr);

                        res = true;

                        break;
                    }
                }
            }
        }

        return res;
    }

    /**
     * Check is class can be reached.
     *
     * @param ldr Class loader.
     * @param clsName Class name.
     * @return <tt>true</tt> if class can be loaded.
     */
    private boolean isResourceExist(ClassLoader ldr, String clsName) {
        assert ldr != null : "ASSERTION [line=402, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";
        assert clsName != null : "ASSERTION [line=403, file=src/java/org/gridgain/grid/spi/deployment/local/GridLocalDeploymentSpi.java]";

        String rsrcName = clsName.replaceAll("\\.", "/") + ".class";

        InputStream in = null;

        try {
            in = ldr.getResourceAsStream(rsrcName);

            return in != null;
        }
        finally {
            GridUtils.close(in, log);
        }
    }

    /**
     * Notifies listener about released class loader.
     *
     * @param clsLoader Released class loader.
     */
    private void onClassLoaderReleased(ClassLoader clsLoader) {
        GridDeploymentListener tmp = lsnr;

        if (tmp != null) {
            tmp.onUnregistered(clsLoader);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridDeploymentListener lsnr) {
        this.lsnr = lsnr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(2);

        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridLocalDeploymentSpi.class, this);
    }
}
