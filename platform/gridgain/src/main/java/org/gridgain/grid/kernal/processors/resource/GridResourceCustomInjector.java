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

package org.gridgain.grid.kernal.processors.resource;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import javax.management.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.tostring.*;
import org.springframework.context.*;

/**
 * Custom injector implementation works with user resources.
 * Injector creates and collects all created user resources.
 * All resources will be cleaned before task undeploy.
 * Task resources should be marked in task with {@link GridUserResource} annotation.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridResourceCustomInjector implements GridResourceInjector {
    /** Grid logger. */
    private final GridLogger log;

    /** Deployed Class Loader -> Resource Class -> Created Resource. */
    private final Map<ClassLoader, Map<Class<?>, CachedResource>> ldrCache =
        new HashMap<ClassLoader, Map<Class<?>, CachedResource>>();

    /** Deployed Class -> Resource Class -> Created Resource. */
    private final Map<ClassLoader, Map<Class<?>, Map<Class<?>, CachedResource>>> clsCache =
        new HashMap<ClassLoader, Map<Class<?>, Map<Class<?>, CachedResource>>>();

    /** Grid instance injector. */
    private GridResourceBasicInjector<GridKernal> gridInjector = null;

    /** GridGain home folder injector. */
    private GridResourceBasicInjector<String> ggHomeInjector = null;

    /** MBean server injector. */
    private GridResourceBasicInjector<MBeanServer> mbeanServerInjector = null;

    /** Grid thread executor injector. */
    private GridResourceBasicInjector<Executor> execInjector = null;

    /** Local node ID injector. */
    private GridResourceBasicInjector<UUID> nodeIdInjector = null;

    /** Marshaller injector. */
    private GridResourceBasicInjector<GridMarshaller> marshallerInjector = null;

    /** Spring application context injector. */
    private GridResourceBasicInjector<ApplicationContext> springCtxInjector = null;

    /** Spring bean resources injector. */
    private GridResourceSpringBeanInjector springBeanInjector = null;

    /** Null injector for cleaning resources. */
    private final GridResourceBasicInjector<Object> nullInjector = new GridResourceBasicInjector<Object>(null);

    /** Resource container. */
    private final GridResourceIoc ioc;

    /** */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates injector object.
     *
     * @param log Grid logger.
     * @param ioc Resource container for injections.
     */
    GridResourceCustomInjector(GridLogger log, GridResourceIoc ioc) {
        assert log != null : "ASSERTION [line=100, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";
        assert ioc != null : "ASSERTION [line=101, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        this.log = log;
        this.ioc = ioc;
    }

    /**
     * Sets injector with Grid instance.
     *
     * @param gridInjector Grid instance.
     */
    public void setGridInjector(GridResourceBasicInjector<GridKernal> gridInjector) {
        this.gridInjector = gridInjector;
    }

    /**
     * Sets injector with GridGain home folder.
     *
     * @param ggHomeInjector GridGain home folder.
     */
    public void setGridgainHomeInjector(GridResourceBasicInjector<String> ggHomeInjector) {
        this.ggHomeInjector = ggHomeInjector;
    }

    /**
     * Sets injector with MBean server.
     *
     * @param mbeanServerInjector MBean server.
     */
    public void setMbeanServerInjector(GridResourceBasicInjector<MBeanServer> mbeanServerInjector) {
        this.mbeanServerInjector = mbeanServerInjector;
    }

    /**
     * Sets injector with Grid thread executor.
     *
     * @param execInjector Grid thread executor.
     */
    public void setExecutorInjector(GridResourceBasicInjector<Executor> execInjector) {
        this.execInjector = execInjector;
    }

    /**
     * Sets injector with local node ID.
     *
     * @param nodeIdInjector Local node ID.
     */
    void setNodeIdInjector(GridResourceBasicInjector<UUID> nodeIdInjector) {
        this.nodeIdInjector = nodeIdInjector;
    }

    /**
     * Sets injector with marshaller.
     *
     * @param marshallerInjector Grid marshaller.
     */
    public void setMarshallerInjector(GridResourceBasicInjector<GridMarshaller> marshallerInjector) {
        this.marshallerInjector = marshallerInjector;
    }

    /**
     * Sets injector with Spring application context.
     *
     * @param springCtxInjector Spring application context.
     */
    void setSpringContextInjector(GridResourceBasicInjector<ApplicationContext> springCtxInjector) {
        this.springCtxInjector = springCtxInjector;
    }

    /**
     * Sets injector for Spring beans.
     *
     * @param springBeanInjector Injector for Spring beans.
     */
    public void setSpringBeanInjector(GridResourceSpringBeanInjector springBeanInjector) {
        this.springBeanInjector = springBeanInjector;
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(ClassLoader ldr) {
        lock.writeLock().lock();

        try {
            Map<Class<?>, CachedResource> map = ldrCache.remove(ldr);

            if (map != null) {
                undeploy(map.values());
            }

            Map<Class<?>, Map<Class<?>, CachedResource>> clsRsrcs = clsCache.remove(ldr);

            if (clsRsrcs != null) {
                List<CachedResource> doomed = new LinkedList<CachedResource>();

                for (Iterator<Map<Class<?>, CachedResource>> i1 = clsRsrcs.values().iterator(); i1.hasNext() == true;) {
                    map = i1.next();

                    for (Iterator<CachedResource> i2 = map.values().iterator(); i2.hasNext() == true;) {
                        CachedResource rsrc = i2.next();

                        if (rsrc.getClassLoader().equals(ldr) == true) {
                            doomed.add(rsrc);

                            i2.remove();
                        }
                    }

                    if (map.isEmpty() == true) {
                        i1.remove();
                    }
                }

                undeploy(doomed);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Undeploy all resources for all task classes.
     */
    public void undeployAll() {
        lock.writeLock().lock();

        try {
            for (Map<Class<?>, CachedResource> map : ldrCache.values()) {
                undeploy(map.values());
            }

            for (Map<Class<?>, Map<Class<?>, CachedResource>> clsRsrcs : clsCache.values()) {
                for (Map<Class<?>, CachedResource> map : clsRsrcs.values()) {
                    undeploy(map.values());
                }
            }

            ldrCache.clear();
            clsCache.clear();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Undeploy collection of resources.
     * Every resource method marked with {@link GridUserResourceOnUndeployed}
     * annotation will be invoked before cleanup.
     *
     * @param rsrcs Resources to undeploy.
     */
    private void undeploy(Collection<CachedResource> rsrcs) {
        assert lock.isWriteLockedByCurrentThread() == true : "ASSERTION [line=256, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        for (CachedResource rsrc : rsrcs) {
            try {
                List<Method> finalizers = getMethodsWithAnnotation(rsrc.getResource().getClass(),
                    GridUserResourceOnUndeployed.class);

                for (Method mtd : finalizers) {
                    try {
                        mtd.setAccessible(true);

                        mtd.invoke(rsrc.getResource());
                    }
                    catch (IllegalAccessException e) {
                        log.error("Failed to finalize task shared resource [method=" + mtd + ", resource=" + rsrc +
                            ']', e);
                    }
                    catch (InvocationTargetException e) {
                        log.error("Failed to finalize task shared resource [method=" + mtd + ", resource=" + rsrc +
                            ']', e);
                    }
                }
            }
            catch (GridException e) {
                log.error("Failed to find finalizers for resource: " + rsrc, e);
            }

            // Clean up injected resources.
            cleanup(rsrc, GridLoggerResource.class);
            cleanup(rsrc, GridInstanceResource.class);
            cleanup(rsrc, GridExecutorServiceResource.class);
            cleanup(rsrc, GridLocalNodeIdResource.class);
            cleanup(rsrc, GridMBeanServerResource.class);
            cleanup(rsrc, GridHomeResource.class);
            cleanup(rsrc, GridMarshallerResource.class);
            cleanup(rsrc, GridSpringApplicationContextResource.class);
            cleanup(rsrc, GridSpringResource.class);
        }
    }

    /**
     * Cleanup object where resources was injected before.
     *
     * @param rsrc Object where resources should be cleaned.
     * @param annCls Annotation.
     */
    private void cleanup(CachedResource rsrc, Class<? extends Annotation> annCls) {
        try {
            ioc.injectResource(rsrc.getResource(), annCls, nullInjector, null);
        }
        catch (GridException e) {
            log.error("Failed to clean up resource [ann=" + annCls + ", rsrc=" + rsrc + ']', e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void inject(GridResourceField field, Object target,
        GridDeploymentClass depCls) throws GridException {
        assert depCls != null : "ASSERTION [line=317, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        GridUserResource ann = (GridUserResource)field.getAnnotation();

        assert ann != null : "ASSERTION [line=321, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        if (Modifier.isTransient(field.getField().getModifiers()) == false) {
            throw (GridException)new GridException("@GridUserResource must only be used with 'transient' fields: " +
                field.getField()).setData(324, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java");
        }

        Class<?> rsrcCls = ann.resourceClass().equals(Void.class) == false ? ann.resourceClass() :
            field.getField().getType();

        GridResourceUtils.inject(field.getField(), target, getResource(depCls, rsrcCls));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void inject(GridResourceMethod mtd, Object target, GridDeploymentClass depCls) throws GridException {
        assert depCls != null : "ASSERTION [line=339, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        GridUserResource ann = (GridUserResource)mtd.getAnnotation();

        if (mtd.getMethod().getParameterTypes().length != 1) {
            throw (GridException)new GridException("Method injection setter must have only one parameter: " + mtd.getMethod()).setData(344, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java");
        }

        Class<?> rsrcCls = ann.resourceClass().equals(Void.class) == false ? ann.resourceClass() :
            mtd.getMethod().getParameterTypes()[0];

        GridResourceUtils.inject(mtd.getMethod(), target, getResource(depCls, rsrcCls));
    }

    /**
     * Gets resource for defined task class.
     * If task resource not found it will be created with all necessary grid
     * injections.
     *
     * @param depCls Deployment class.
     * @param rsrcCls Resource class.
     * @return Created resource.
     * @throws GridException If resource creation failed.
     */
    @SuppressWarnings("unchecked")
    private Object getResource(GridDeploymentClass depCls, Class<?> rsrcCls) throws GridException {
        assert depCls != null : "ASSERTION [line=365, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        // For performance reasons we first try to acquire read lock and
        // return the cached resource.
        lock.readLock().lock();

        try {
            Map<Class<?>, CachedResource> map = null;

            if (depCls.getDeploymentMode() == GridDeploymentMode.PRIVATE) {
                Map<Class<?>, Map<Class<?>, CachedResource>> clsRsrcs = clsCache.get(depCls.getClassLoader());

                if (clsRsrcs != null) {
                    map = clsRsrcs.get(depCls.getDeployedClass());
                }
            }
            else {
                map = ldrCache.get(depCls.getClassLoader());
            }

            if (map != null) {
                CachedResource rsrc = map.get(rsrcCls);

                if (rsrc != null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Read resource from cache: " + rsrcCls);
                    }

                    return rsrc.getResource();
                }
            }
        }
        finally {
            lock.readLock().unlock();
        }

        // If resource was not cached, then
        // we acquire write lock and cache it.
        lock.writeLock().lock();

        try {
            Map<Class<?>, CachedResource> map = null;

            if (depCls.getDeploymentMode() == GridDeploymentMode.PRIVATE) {
                Map<Class<?>, Map<Class<?>, CachedResource>> clsRsrcs = clsCache.get(depCls.getClassLoader());

                if (clsRsrcs == null) {
                    clsCache.put(depCls.getClassLoader(), clsRsrcs =
                        new HashMap<Class<?>, Map<Class<?>, CachedResource>>());
                }

                map = clsRsrcs.get(depCls.getDeployedClass());

                if (map == null) {
                    clsRsrcs.put(depCls.getDeployedClass(), map = new HashMap<Class<?>, CachedResource>());
                }
            }
            else {
                map = ldrCache.get(depCls.getClassLoader());

                if (map == null) {
                    ldrCache.put(depCls.getClassLoader(), map = new HashMap<Class<?>, CachedResource>());
                }
            }

            CachedResource rsrc = map.get(rsrcCls);

            if (rsrc == null) {
                rsrc = createResource(rsrcCls, depCls);

                map.put(rsrcCls, rsrc);

                if (log.isDebugEnabled() == true) {
                    log.debug("Created resource [rsrcCls=" + rsrcCls.getName() + ", rsrc=" + rsrc +
                        ", depCls=" + depCls + ']');
                }

            }

            return rsrc.getResource();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates object from class <tt>rsrcCls</tt> and inject all
     * necessary resources for it.
     *
     * @param rsrcCls Class in which resources should be injected.
     * @param depCls Deployment class.
     * @return Created object with injected resources.
     * @throws GridException Thrown in case of any errors during injection.
     */
    private CachedResource createResource(Class<?> rsrcCls, GridDeploymentClass depCls) throws GridException {
        assert depCls != null : "ASSERTION [line=461, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java]";

        try {
            Object rsrc = rsrcCls.newInstance();

            // Inject resources into shared resource.
            ioc.injectResource(rsrc, GridLoggerResource.class, new GridResourceBasicInjector<GridLogger>(
                log.getLogger(rsrcCls)), depCls);
            ioc.injectResource(rsrc, GridInstanceResource.class, gridInjector, depCls);
            ioc.injectResource(rsrc, GridExecutorServiceResource.class, execInjector, depCls);
            ioc.injectResource(rsrc, GridLocalNodeIdResource.class, nodeIdInjector, depCls);
            ioc.injectResource(rsrc, GridMBeanServerResource.class, mbeanServerInjector, depCls);
            ioc.injectResource(rsrc, GridHomeResource.class, ggHomeInjector, depCls);
            ioc.injectResource(rsrc, GridMarshallerResource.class, marshallerInjector, depCls);
            ioc.injectResource(rsrc, GridSpringApplicationContextResource.class, springCtxInjector, depCls);
            ioc.injectResource(rsrc, GridSpringResource.class, springBeanInjector, depCls);

            for (Method mtd : getMethodsWithAnnotation(rsrcCls, GridUserResourceOnDeployed.class)) {
                mtd.setAccessible(true);

                mtd.invoke(rsrc);
            }

            return new CachedResource(rsrc, depCls.getClassLoader());
        }
        catch (InstantiationException e) {
            throw (GridException)new GridException("Failed to instantiate task shared resource: " + rsrcCls, e).setData(487, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java");
        }
        catch (IllegalAccessException e) {
            throw (GridException)new GridException("Failed to access task shared resource (is class public?): " + rsrcCls, e).setData(490, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java");
        }
        catch (InvocationTargetException e) {
            throw (GridException)new GridException("Failed to initialize task shared resource: " + rsrcCls, e).setData(493, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java");
        }
    }


    /**
     * Gets set of methods with given annotation.
     *
     * @param cls Class in which search for methods.
     * @param annCls Annotation.
     * @return Set of methods with given annotations.
     * @throws GridException Thrown in case when method contains parameters.
     */
    private List<Method> getMethodsWithAnnotation(Class<?> cls, Class<? extends Annotation> annCls)
        throws GridException {
        List<Method> mtds = new ArrayList<Method>();

        for (Class<?> c = cls; c.equals(Object.class) == false; c = c.getSuperclass()) {
            for (Method mtd : c.getDeclaredMethods()) {
                if (mtd.getAnnotation(annCls) != null) {
                    if (mtd.getParameterTypes().length > 0) {
                        throw (GridException)new GridException("Task shared resource initialization or finalization method should " +
                            "not have parameters: " + mtd).setData(514, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceCustomInjector.java");
                    }

                    mtds.add(mtd);
                }
            }
        }

        return mtds;
    }

    /**
     * For tests only.
     *
     * @param cls Class for test.
     * @return FIXDOC
     */
    boolean isCached(Class<?> cls) {
        return isCached(cls.getName());
    }

    /**
     * For tests only.
     *
     * @param clsName Class for test.
     * @return FIXDOC
     */
    boolean isCached(String clsName) {
        lock.readLock().lock();

        try {
            for (Map<Class<?>, CachedResource> map : ldrCache.values()) {
                for (Class<?> aClass : map.keySet()) {
                    if (aClass.getName().equals(clsName)) {
                        return true;
                    }
                }
            }

            for (Map<Class<?>, Map<Class<?>, CachedResource>> clsRsrcs : clsCache.values()) {
                for (Map<Class<?>, CachedResource> map : clsRsrcs.values()) {
                    for (Class<?> aClass : map.keySet()) {
                        if (aClass.getName().equals(clsName)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     *
     */
    private class CachedResource {
        /** */
        private final Object rsrc;

        /** */
        private final ClassLoader ldr;

        /**
         *
         * @param rsrc Resource.
         * @param ldr Class loader.
         */
        CachedResource(Object rsrc, ClassLoader ldr) {
            this.rsrc = rsrc;
            this.ldr = ldr;
        }

        /**
         * Gets property rsrc.
         *
         * @return Property rsrc.
         */
        public Object getResource() {
            return rsrc;
        }

        /**
         * Gets property ldr.
         *
         * @return Property ldr.
         */
        public ClassLoader getClassLoader() {
            return ldr;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return GridToStringBuilder.toString(CachedResource.class, this);
        }
    }
}
