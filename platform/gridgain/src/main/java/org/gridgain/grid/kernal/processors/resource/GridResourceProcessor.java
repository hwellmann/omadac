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
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.springframework.context.*;
import org.springframework.aop.framework.*;

/**
 * Processor for all Grid and task/job resources.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridResourceProcessor extends GridProcessorAdapter {
    /** Grid instance injector. */
    private GridResourceBasicInjector<GridKernal> gridInjector = null;

    /** GridGain home folder injector. */
    private GridResourceBasicInjector<String> ggHomeInjector = null;

    /** MBean server injector. */
    private GridResourceBasicInjector<MBeanServer> mbeanSrvrInjector = null;

    /** Grid thread executor injector. */
    private GridResourceBasicInjector<Executor> execInjector = null;

    /** Grid marshaller injector. */
    private GridResourceBasicInjector<GridMarshaller> marshallerInjector = null;

    /** Local node ID injector. */
    private GridResourceBasicInjector<UUID> nodeIdInjector = null;

    /** Spring application context injector. */
    private GridResourceBasicInjector<ApplicationContext> springCtxInjector = null;

    /** Spring bean resources injector. */
    private GridResourceSpringBeanInjector springBeanInjector = null;

    /** Task resources injector. */
    private GridResourceCustomInjector customInjector = null;

    /** Logger. */
    private GridLogger injectLog = null;

    /** Cleaning injector. */
    private final GridResourceBasicInjector<?> nullInjector = new GridResourceBasicInjector<Object>(null);

    /** */
    private final GridResourceIoc ioc = new GridResourceIoc();

    /**
     * Creates resources processor.
     *
     * @param mgrReg Kernal managers registry.
     * @param procReg Kernal processors registry.
     * @param cfg Grid configuration.
     */
    public GridResourceProcessor(GridManagerRegistry mgrReg, GridProcessorRegistry procReg, GridConfiguration cfg) {
        super(mgrReg, procReg, cfg);

        assert cfg != null : "ASSERTION [line=93, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceProcessor.java]";

        injectLog = cfg.getGridLogger();

        ggHomeInjector = new GridResourceBasicInjector<String>(cfg.getGridGainHome());
        mbeanSrvrInjector = new GridResourceBasicInjector<MBeanServer>(cfg.getMBeanServer());
        marshallerInjector = new GridResourceBasicInjector<GridMarshaller>(cfg.getMarshaller());
        execInjector = new GridResourceBasicInjector<Executor>(cfg.getExecutorService());
        nodeIdInjector = new GridResourceBasicInjector<UUID>(cfg.getNodeId());
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        customInjector = new GridResourceCustomInjector(injectLog, ioc);

        customInjector.setExecutorInjector(execInjector);
        customInjector.setGridgainHomeInjector(ggHomeInjector);
        customInjector.setGridInjector(gridInjector);
        customInjector.setMbeanServerInjector(mbeanSrvrInjector);
        customInjector.setNodeIdInjector(nodeIdInjector);
        customInjector.setMarshallerInjector(marshallerInjector);
        customInjector.setSpringContextInjector(springCtxInjector);
        customInjector.setSpringBeanInjector(springBeanInjector);

        if (log.isDebugEnabled() == true) {
            log.debug("Started resource processor.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(boolean cancel) {
        if (customInjector != null) {
            customInjector.undeployAll();
        }

        ioc.undeployAll();

        if (log.isDebugEnabled() == true) {
            log.debug("Stopped resource processor.");
        }
    }

    /**
     * Sets Grid instance.
     *
     * @param grid Grid instance.
     */
    public void setGrid(GridKernal grid) {
        gridInjector = new GridResourceBasicInjector<GridKernal>(grid);
    }

    /**
     * Sets Spring application context.
     *
     * @param springCtx Spring application context.
     */
    public void setSpringContext(ApplicationContext springCtx) {
        springCtxInjector = new GridResourceBasicInjector<ApplicationContext>(springCtx);

        springBeanInjector = new GridResourceSpringBeanInjector(springCtx);
    }

    /**
     * Callback to be called when class loader is undeployed.
     *
     * @param clsLdr ClassLoader to undeploy.
     */
    @SuppressWarnings("unchecked")
    public void onUndeployed(ClassLoader clsLdr) {
        customInjector.undeploy(clsLdr);

        ioc.onUndeployed(clsLdr);
    }

    /**
     * Injects resources into generic class.
     *
     * @param depCls Deployed class.
     * @param target Target instance to inject into.
     * @throws GridException Thrown in case of any errors.
     */
    public void inject(GridDeploymentClass depCls, Object target) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Injecting resources: " + target);
        }

        // Unwrap Proxy object.
        target = unwrapTarget(target);

        injectBasicResource(target, GridLoggerResource.class, injectLog.getLogger(target.getClass()), depCls);

        ioc.injectResource(target, GridInstanceResource.class, gridInjector, depCls);
        ioc.injectResource(target, GridExecutorServiceResource.class, execInjector, depCls);
        ioc.injectResource(target, GridLocalNodeIdResource.class, nodeIdInjector, depCls);
        ioc.injectResource(target, GridMBeanServerResource.class, mbeanSrvrInjector, depCls);
        ioc.injectResource(target, GridHomeResource.class, ggHomeInjector, depCls);
        ioc.injectResource(target, GridMarshallerResource.class, marshallerInjector, depCls);
        ioc.injectResource(target, GridSpringApplicationContextResource.class, springCtxInjector, depCls);
        ioc.injectResource(target, GridSpringResource.class, springBeanInjector, depCls);

        // Inject users resource.
        ioc.injectResource(target, GridUserResource.class, customInjector, depCls);
    }

    /**
     * Injects held resources into given <tt>job</tt>.
     *
     * @param depCls Deployed class.
     * @param job Grid job to inject resources to.
     * @param ses Current task session.
     * @param jobCtx Job context.
     * @throws GridException Thrown in case of any errors.
     */
    @SuppressWarnings("deprecation")
    public void inject(GridDeploymentClass depCls, GridJob job, GridTaskSessionImpl ses, GridJobContextImpl jobCtx)
        throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Injecting resources: " + job);
        }

        // Unwrap Proxy object.
        Object jobObj = unwrapTarget(job);

        injectBasicResource(jobObj, GridLoggerResource.class, injectLog.getLogger(jobObj.getClass()), depCls);
        injectBasicResource(jobObj, GridTaskSessionResource.class, ses, depCls);
        injectBasicResource(jobObj, GridJobContextResource.class, jobCtx, depCls);
        injectBasicResource(jobObj, GridJobIdResource.class, ses.getJobId(), depCls);

        ioc.injectResource(jobObj, GridInstanceResource.class, gridInjector, depCls);
        ioc.injectResource(jobObj, GridExecutorServiceResource.class, execInjector, depCls);
        ioc.injectResource(jobObj, GridLocalNodeIdResource.class, nodeIdInjector, depCls);
        ioc.injectResource(jobObj, GridMBeanServerResource.class, mbeanSrvrInjector, depCls);
        ioc.injectResource(jobObj, GridHomeResource.class, ggHomeInjector, depCls);
        ioc.injectResource(jobObj, GridMarshallerResource.class, marshallerInjector, depCls);
        ioc.injectResource(jobObj, GridSpringApplicationContextResource.class, springCtxInjector, depCls);
        ioc.injectResource(jobObj, GridSpringResource.class, springBeanInjector, depCls);

        // Inject users resource.
        ioc.injectResource(jobObj, GridUserResource.class, customInjector, depCls);
    }

    /**
     * Injects held resources into given grid task.
     *
     * @param depCls Deployed class.
     * @param task Grid task.
     * @param ses Grid task session.
     * @param balancer Load balancer.
     * @throws GridException Thrown in case of any errors.
     */
    public void inject(GridDeploymentClass depCls, GridTask<?, ?> task, GridTaskSessionImpl ses,
        GridLoadBalancer balancer) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Injecting resources: " + task);
        }

        // Unwrap Proxy object.
        Object taskObj = unwrapTarget(task);

        // Basic injection.
        injectBasicResource(taskObj, GridLoggerResource.class, injectLog.getLogger(taskObj.getClass()), depCls);
        injectBasicResource(taskObj, GridTaskSessionResource.class, ses, depCls);
        injectBasicResource(taskObj, GridLoadBalancerResource.class, balancer, depCls);

        ioc.injectResource(taskObj, GridInstanceResource.class, gridInjector, depCls);
        ioc.injectResource(taskObj, GridExecutorServiceResource.class, execInjector, depCls);
        ioc.injectResource(taskObj, GridLocalNodeIdResource.class, nodeIdInjector, depCls);
        ioc.injectResource(taskObj, GridMBeanServerResource.class, mbeanSrvrInjector, depCls);
        ioc.injectResource(taskObj, GridHomeResource.class, ggHomeInjector, depCls);
        ioc.injectResource(taskObj, GridMarshallerResource.class, marshallerInjector, depCls);
        ioc.injectResource(taskObj, GridSpringApplicationContextResource.class, springCtxInjector, depCls);
        ioc.injectResource(taskObj, GridSpringResource.class, springBeanInjector, depCls);

        // Inject users resource.
        ioc.injectResource(taskObj, GridUserResource.class, customInjector, depCls);
    }

    /**
     * Injects held resources into given SPI implementation.
     *
     * @param spi SPI implementation.
     * @throws GridException Throw in case of any errors.
     */
    public void inject(GridSpi spi) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Injecting resources: " + spi);
        }

        // Unwrap Proxy object.
        Object spiObj = unwrapTarget(spi);

        injectBasicResource(spiObj, GridLoggerResource.class, injectLog.getLogger(spiObj.getClass()));

        // Caching key is null for the SPIs.
        ioc.injectResource(spiObj, GridExecutorServiceResource.class, execInjector, null);
        ioc.injectResource(spiObj, GridLocalNodeIdResource.class, nodeIdInjector, null);
        ioc.injectResource(spiObj, GridMBeanServerResource.class, mbeanSrvrInjector, null);
        ioc.injectResource(spiObj, GridHomeResource.class, ggHomeInjector, null);
        ioc.injectResource(spiObj, GridMarshallerResource.class, marshallerInjector, null);
        ioc.injectResource(spiObj, GridSpringApplicationContextResource.class, springCtxInjector, null);
        ioc.injectResource(spiObj, GridSpringResource.class, springBeanInjector, null);
    }

    /**
     * Cleans up resources from given SPI implementation. Essentially, this
     * method injects <tt>null</tt>s into SPI implementation.
     *
     * @param spi SPI implementation.
     * @throws GridException Thrown in case of any errors.
     */
    public void cleanup(GridSpi spi) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Cleaning up resources: " + spi);
        }

        // Unwrap Proxy object.
        Object spiObj = unwrapTarget(spi);

        ioc.injectResource(spiObj, GridLoggerResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridExecutorServiceResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridLocalNodeIdResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridMBeanServerResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridHomeResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridMarshallerResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridSpringApplicationContextResource.class, nullInjector, null);
        ioc.injectResource(spiObj, GridSpringResource.class, nullInjector, null);
    }

    /**
     * Injects held resources into given SPI implementation.
     *
     * @param lifecycleBean SPI implementation.
     * @throws GridException Throw in case of any errors.
     */
    public void inject(GridLifecycleBean lifecycleBean) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Injecting resources: " + lifecycleBean);
        }

        // Unwrap Proxy object.
        Object lifecycleBeanObj = unwrapTarget(lifecycleBean);

        injectBasicResource(lifecycleBeanObj, GridLoggerResource.class,
            injectLog.getLogger(lifecycleBeanObj.getClass()));

        // No deployment for lifecycle beans.
        ioc.injectResource(lifecycleBeanObj, GridExecutorServiceResource.class, execInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridLocalNodeIdResource.class, nodeIdInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridMBeanServerResource.class, mbeanSrvrInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridHomeResource.class, ggHomeInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridMarshallerResource.class, marshallerInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridSpringApplicationContextResource.class, springCtxInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridSpringResource.class, springBeanInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridInstanceResource.class, gridInjector, null);
    }

    /**
     * Cleans up resources from given SPI implementation. Essentially, this
     * method injects <tt>null</tt>s into SPI implementation.
     *
     * @param lifecycleBean SPI implementation.
     * @throws GridException Thrown in case of any errors.
     */
    public void cleanup(GridLifecycleBean lifecycleBean) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Cleaning up resources: " + lifecycleBean);
        }

        // Unwrap Proxy object.
        Object lifecycleBeanObj = unwrapTarget(lifecycleBean);

        // Caching key is null for the life-cycle beans.
        ioc.injectResource(lifecycleBeanObj, GridLoggerResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridExecutorServiceResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridLocalNodeIdResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridMBeanServerResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridHomeResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridMarshallerResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridSpringApplicationContextResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridSpringResource.class, nullInjector, null);
        ioc.injectResource(lifecycleBeanObj, GridInstanceResource.class, nullInjector, null);
    }

    /**
     * This method is declared public as it is used form tests as well.
     * Note, that this method can be used only with unwrapped objects
     * (see {@link #unwrapTarget(Object)}).
     *
     * @param target Target object.
     * @param annCls Setter annotation.
     * @param rsrc Resource to inject.
     * @param depCls Deployment class.
     * @throws GridException If injection failed.
     */
    public void injectBasicResource(Object target, Class<? extends Annotation> annCls, Object rsrc,
        GridDeploymentClass depCls) throws GridException {
        // Safety.
        assert rsrc instanceof GridResourceInjector == false : "ASSERTION [line=394, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceProcessor.java]. " + "Invalid injection.";

        // Basic injection don't cache anything. Use null as a key.
        ioc.injectResource(target, annCls, new GridResourceBasicInjector<Object>(rsrc), depCls);
    }

    /**
     * This method is declared public as it is used form tests as well.
     * Note, that this method can be used only with unwrapped objects
     * (see {@link #unwrapTarget(Object)}).
     *
     * @param target Target object.
     * @param annCls Setter annotation.
     * @param rsrc Resource to inject.
     * @throws GridException If injection failed.
     */
    public void injectBasicResource(Object target, Class<? extends Annotation> annCls, Object rsrc)
        throws GridException {
        // Safety.
        assert rsrc instanceof GridResourceInjector == false : "ASSERTION [line=413, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceProcessor.java]. " + "Invalid injection.";

        // Basic injection don't cache anything. Use null as a key.
        ioc.injectResource(target, annCls, new GridResourceBasicInjector<Object>(rsrc), null);
    }

    /**
     * Returns GridResourceIoc object. For tests only!!!
     *
     * @return GridResourceIoc object.
     */
    GridResourceIoc getResourceIoc() {
        return ioc;
    }

    /**
     * Returns GridResourceCustomInjector object. For tests only!!!
     *
     * @return GridResourceCustomInjector object.
     */
    GridResourceCustomInjector getResourceCustomInjector() {
        return customInjector;
    }

    /**
     * Return original object if Spring AOP used with proxy objects.
     *
     * @param target Target object.
     * @return Original object wrapped by proxy.
     * @throws GridException If unwrap failed.
     */
    private Object unwrapTarget(Object target) throws GridException {
        if (target instanceof Advised) {
            try {
                return ((Advised)target).getTargetSource().getTarget();
            }
            catch (Exception e) {
                throw (GridException)new GridException("Failed to unwrap Spring proxy target [cls=" + target.getClass().getName() +
                    ", target=" + target + ']', e).setData(450, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceProcessor.java");
            }
        }

        return target;
    }
}
