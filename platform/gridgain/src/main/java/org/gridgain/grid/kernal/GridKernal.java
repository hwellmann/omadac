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

package org.gridgain.grid.kernal;

import java.io.*;
import java.lang.management.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.executor.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.checkpoint.*;
import org.gridgain.grid.kernal.managers.collision.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.kernal.managers.failover.*;
import org.gridgain.grid.kernal.managers.loadbalancing.*;
import org.gridgain.grid.kernal.managers.metrics.*;
import org.gridgain.grid.kernal.managers.topology.*;
import org.gridgain.grid.kernal.managers.tracing.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.job.*;
import org.gridgain.grid.kernal.processors.jobmetrics.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.spi.tracing.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;
import org.gridgain.grid.util.tostring.*;
import org.springframework.context.*;
import static org.gridgain.grid.kernal.GridNodeAttributes.*;

/**
 * FIXDOC: add file description.
 *
 * See <a href="http://en.wikipedia.org/wiki/Kernal">http://en.wikipedia.org/wiki/Kernal</a> for
 * information on the misspelling.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridKernal implements Grid, GridKernalMBean {
    /** */
    private static final String LICENSE_FILE = "license.txt";

    /** Ant-augmented copyright blurb. */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /** Ant-augmented version number. */
    private static final String VER = "2.1.1";

    /** Ant-augmented build number. */
    private static final String BUILD = "26022009";

    /** System line separator. */
    private static final String NL = System.getProperty("line.separator");

    /** */
    private static final String UPDATE_NOTIFIER_PROP_NAME = "gridgain.update.notifier";

    /** Periodic version check delay. */
    private static final long PERIODIC_VER_CHECK_DELAY = 1000 * 60 * 60 * 24; // One day.

    /** Periodic version check delay. */
    private static final long PERIODIC_VER_CHECK_CONN_TIMEOUT = 10 * 1000; // 10 seconds.

    /** */
    private GridManagerRegistry mgrReg = null;

    /** */
    private GridProcessorRegistry procReg = null;

    /** */
    private GridConfiguration cfg = null;

    /** */
    private GridLogger log = null;

    /** */
    private int callCnt = 0;

    /** */
    private final Object mux = new Object();

    /** */
    private String gridName = null;

    /** */
    private ObjectName kernalMBean = null;

    /** */
    private ObjectName locNodeMBean = null;

    /** */
    private ObjectName execSvcMBean = null;

    /** */
    private ObjectName systemExecSvcMBean = null;

    /** */
    private ObjectName p2PExecSvcMBean = null;

    /** */
    private GridKernalState state = GridKernalState.STOPPED;

    /** Kernal start timestamp. */
    private long startTstamp = System.currentTimeMillis();

    /** Proxy object factory. */
    private final GridProxyFactory proxyFactory;

    /** Spring context, potentially <tt>null</tt>. */
    private final ApplicationContext springCtx;

    /** */
    private Timer updateNotifierTime = new Timer("gridgain-update-notifier-timer");

    /** Indicate error on grid stop. */
    private boolean errOnStop = false;

    /**
     *
     * @param proxyFactory Proxy objects factory.
     * @param springCtx Spring application context.
     */
    public GridKernal(GridProxyFactory proxyFactory, ApplicationContext springCtx) {
        this.proxyFactory = proxyFactory;
        this.springCtx = springCtx;
    }

    /**
     * Gets grid name.
     *
     * @return Grid name (<tt>null</tt> for default grid).
     */
    public String getName() {
        return gridName;
    }

    /**
     * Creates {@link ExecutorService} which delegates all calls to grid.
     * User may run {@link Callable} and {@link Runnable} tasks
     * in grid but that tasks must implement {@link Serializable}
     * interface. User's tasks will be transfered over grid nodes.
     *
     * @return <tt>ExecutorService</tt> which delegates all calls to grid.
     * @see GridExecutorService
     */
    public ExecutorService newGridExecutorService() {
        // Creates executor service.
        return new GridExecutorService(this, log);
    }

    /**
     * {@inheritDoc}
     */
    public String getCopyright() {
        return COPYRIGHT;
    }

    /**
     * {@inheritDoc}
     */
    public String getLicenseFilePath() {
        assert cfg != null : "ASSERTION [line=195, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return new File(cfg.getGridGainHome(), LICENSE_FILE).getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTimestamp() {
        return startTstamp;
    }

    /**
     * {@inheritDoc}
     */
    public String getStartTimestampFormatted() {
        return DateFormat.getDateTimeInstance().format(new Date(startTstamp));

    }

    /**
     * {@inheritDoc}
     */
    public long getUpTime() {
        return System.currentTimeMillis() - startTstamp;
    }

    /**
     * {@inheritDoc}
     */
    public String getUpTimeFormatted() {
        return GridUtils.timeSpan2String(System.currentTimeMillis() - startTstamp);
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        return VER + '-' + BUILD;
    }

    /**
     * {@inheritDoc}
     */
    public String getCheckpointSpiFormatted() {
        assert cfg != null : "ASSERTION [line=240, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getCheckpointSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getCommunicationSpiFormatted() {
        assert cfg != null : "ASSERTION [line=249, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getCommunicationSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getDeploymentSpiFormatted() {
        assert cfg != null : "ASSERTION [line=258, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getDeploymentSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getDiscoverySpiFormatted() {
        assert cfg != null : "ASSERTION [line=267, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getDiscoverySpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getEventStorageSpiFormatted() {
        assert cfg != null : "ASSERTION [line=276, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getEventStorageSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getCollisionSpiFormatted() {
        assert cfg != null : "ASSERTION [line=285, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getCollisionSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getFailoverSpiFormatted() {
        assert cfg != null : "ASSERTION [line=294, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getFailoverSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getLoadBalancingSpiFormatted() {
        assert cfg != null : "ASSERTION [line=303, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getLoadBalancingSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getMetricsSpiFormatted() {
        assert cfg != null : "ASSERTION [line=312, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getMetricsSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getTopologySpiFormatted() {
        assert cfg != null : "ASSERTION [line=321, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getTopologySpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getTracingSpiFormatted() {
        assert cfg != null : "ASSERTION [line=330, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getTracingSpi().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getOsInformation() {
        return GridOs.getOsString();
    }

    /**
     * {@inheritDoc}
     */
    public String getJdkInformation() {
        return GridOs.getJdkString();
    }

    /**
     * {@inheritDoc}
     */
    public String getOsUser() {
        return System.getProperty("user.name");
    }

    /**
     * {@inheritDoc}
     */
    public String getVmName() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getInstanceName() {
        return gridName;
    }

    /**
     * {@inheritDoc}
     */
    public String getExecutorServiceFormatted() {
        assert cfg != null : "ASSERTION [line=374, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getExecutorService().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getGridGainHome() {
        assert cfg != null : "ASSERTION [line=383, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getGridGainHome();
    }

    /**
     * {@inheritDoc}
     */
    public String getGridLoggerFormatted() {
        assert cfg != null : "ASSERTION [line=392, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getGridLogger().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getMBeanServerFormatted() {
        assert cfg != null : "ASSERTION [line=401, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getMBeanServer().toString();
    }

    /**
     * {@inheritDoc}
     */
    public UUID getLocalNodeId() {
        assert cfg != null : "ASSERTION [line=410, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getUserAttributesFormatted() {
        assert cfg != null : "ASSERTION [line=419, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        List<String> attrs = new ArrayList<String>();

        for (Map.Entry<? extends String, ? extends Serializable> entry : cfg.getUserAttributes().entrySet()) {
            attrs.add(entry.getKey() + ", " + entry.getValue().toString());
        }

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPeerClassLoadingEnabled() {
        assert cfg != null : "ASSERTION [line=434, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        return cfg.isPeerClassLoadingEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getLifecycleBeansFormatted() {
        Collection<? extends GridLifecycleBean> beans = cfg.getLifecycleBeans();

        if (beans == null || beans.isEmpty() == true) {
            return Collections.emptyList();
        }

        List<String> fmtBeans = new ArrayList<String>(beans.size());

        for (GridLifecycleBean bean : beans) {
            fmtBeans.add(bean.toString());
        }

        return fmtBeans;
    }

    /**
     *
     * @param spiCls SPI class.
     * @return Spi version.
     * @throws GridException Thrown if {@link GridSpiInfo} annotation cannot be found.
     */
    private String getSpiVersion(Class<? extends GridSpi> spiCls) throws GridException {
        assert spiCls != null : "ASSERTION [line=465, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        GridSpiInfo ann = GridUtils.getAnnotation(spiCls, GridSpiInfo.class);

        if (ann == null) {
            throw (GridException)new GridException("SPI implementation does not have annotation: " + GridSpiInfo.class).setData(470, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }

        return ann.version();
    }

    /**
     *
     * @param attrs Current attributes.
     * @param name New attribute name.
     * @param value New attribute value.
     * @throws GridException If duplicated SPI name found.
     */
    private void addAttribute(Map<String, Serializable> attrs, String name, Serializable value) throws GridException {
        assert name != null : "ASSERTION [line=484, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        Serializable oldValue = attrs.put(name, value);

        if (oldValue != null) {
            if (name.endsWith(ATTR_SPI_CLASS) == true) {
                // User defined duplicated names for the different SPIs.
                throw (GridException)new GridException("Failed to set SPI attribute. Duplicated SPI name found: " +
                    name.substring(0, name.length() - ATTR_SPI_CLASS.length())).setData(491, "src/java/org/gridgain/grid/kernal/GridKernal.java");
            }

            // Otherwise it's a mistake of setting up duplicated attribute.
            assert false : "ASSERTION [line=496, file=src/java/org/gridgain/grid/kernal/GridKernal.java]. " + "Duplicate attribute: " + name;
        }
    }

    /**
     * Notifies life-cycle beans of grid event.
     *
     * @param evt Grid event.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private void notifyLifecycleBeans(GridLifecycleEventType evt) {
        if (cfg.getLifecycleBeans() != null) {
            for (GridLifecycleBean bean : cfg.getLifecycleBeans()) {
                try {
                    bean.onLifecycleEvent(evt);
                }
                // Catch generic throwable to secure against user
                // assertions.
                catch (Throwable e) {
                    log.error("Failed to notify lifecycle bean (safely ignored) [evt=" + evt + ", gridName=" + gridName +
                        ", bean=" + bean + ']', e);
                }
            }
        }
    }

    /**
     *
     * @param cfg Grid configuration to use.
     * @throws GridException Thrown in case of any errors.
     */
    @SuppressWarnings({"CatchGenericClass"})
    public void start(final GridConfiguration cfg) throws GridException {
        synchronized (mux) {
            if (state == GridKernalState.STARTED) {
                throw (GridException)new GridException("Grid has already been started: " + gridName).setData(531, "src/java/org/gridgain/grid/kernal/GridKernal.java");
            }

            if (state == GridKernalState.STARTING) {
                throw (GridException)new GridException("Grid is already in process of being started: " + gridName).setData(535, "src/java/org/gridgain/grid/kernal/GridKernal.java");
            }

            if (state == GridKernalState.STOPPING) {
                throw (GridException)new GridException("Grid is in process of being stopped: " + gridName).setData(539, "src/java/org/gridgain/grid/kernal/GridKernal.java");
            }

            state = GridKernalState.STARTING;
        }

        assert cfg != null : "ASSERTION [line=545, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        // Make sure we got proper configuration.
        assert cfg.getGridGainHome() != null : "ASSERTION [line=548, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getMBeanServer() != null : "ASSERTION [line=549, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getGridLogger() != null : "ASSERTION [line=550, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getMarshaller() != null : "ASSERTION [line=551, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getExecutorService() != null : "ASSERTION [line=552, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getCheckpointSpi() != null : "ASSERTION [line=553, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getCommunicationSpi() != null : "ASSERTION [line=554, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getDeploymentSpi() != null : "ASSERTION [line=555, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getDiscoverySpi() != null : "ASSERTION [line=556, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getEventStorageSpi() != null : "ASSERTION [line=557, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getMetricsSpi() != null : "ASSERTION [line=558, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert cfg.getUserAttributes() != null : "ASSERTION [line=559, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        gridName = cfg.getGridName();

        long tstamp = System.currentTimeMillis();

        this.cfg = cfg;

        log = cfg.getGridLogger().getLogger(getClass().getName() + '%' + gridName);

        // Run background network diagnostics.
        GridDiagnostic.runBackgroundCheck(gridName, cfg.getExecutorService(), log);

        // ASCII-art logo.
        logo();

        RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();

        logVmArguments(rtBean);

        logClassPaths(rtBean);

        GridUpdateNotifier notifier = null;

        String isNotify = System.getProperty(UPDATE_NOTIFIER_PROP_NAME);

        if (isNotify == null || "false".equals(isNotify) == false) {
            notifier = new GridUpdateNotifier(gridName, false);

            // Request latest version update.
            notifier.requestStatus(cfg.getExecutorService(), log);
        }

        // Check and display license.
        checkLicense(cfg.getGridGainHome());

        // Ack 3-rd party licenses location.
        if (log.isInfoEnabled() == true) {
            log.info("3-rd party licenses can be found at: " + cfg.getGridGainHome() + File.separatorChar + "libs" +
                File.separatorChar + "licenses");
        }

        // Ack system properties.
        logSystemProperties();

        // Ack environment variables.
        logEnvironmentVariables();

        // Check that user attributes are not conflicting
        // with internally reserved names.
        for (String name : cfg.getUserAttributes().keySet()) {
            if (name.startsWith(ATTR_PREFIX) == true) {
                throw (GridException)new GridException("User attribute has illegal name: '" + name + "'. Note that all names " +
                    "starting with '" + ATTR_PREFIX + "' are reserved for internal use.").setData(611, "src/java/org/gridgain/grid/kernal/GridKernal.java");
            }
        }

        // Ack local node user attributes.
        logNodeUserAttributes();

        // Ack configuration.
        logConfiguration();

        Map<String, Serializable> attrs = createNodeAttributes(cfg);

        // Spin out SPIs & managers.
        try {
            mgrReg = new GridManagerRegistry();
            procReg = new GridProcessorRegistry();

            // Start and configure resource processor first as it contains resources used
            // by all other managers and processors.
            GridResourceProcessor rsrcProc = new GridResourceProcessor(mgrReg, procReg, cfg);

            rsrcProc.setGrid(this);
            rsrcProc.setSpringContext(springCtx);

            startProcessor(rsrcProc);

            // Inject resources into lifecycle beans.
            if (cfg.getLifecycleBeans() != null) {
                for (GridLifecycleBean bean : cfg.getLifecycleBeans()) {
                    rsrcProc.inject(bean);
                }
            }

            // Lifecycle notification.
            notifyLifecycleBeans(GridLifecycleEventType.BEFORE_GRID_START);

            // Start metrics processor.
            startProcessor(new GridJobMetricsProcessor(mgrReg, procReg, cfg));

            // Start tracing manager first if there is SPI. By default no
            // tracing SPI provided.
            if (cfg.getTracingSpi() != null) {
                GridTracingManager traceMgr = new GridTracingManager(cfg, mgrReg, procReg);

                // Configure proxy factory for tracing.
                traceMgr.setProxyFactory(proxyFactory);

                startManager(traceMgr, attrs);
            }

            // Timeout processor needs to be started before managers,
            // as managers may depend on it.
            startProcessor(new GridTimeoutProcessor(mgrReg, procReg, cfg));

            // Start SPI managers.
            // NOTE: that order matters as there are dependencies between managers.
            startManager(new GridLocalMetricsManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridCommunicationManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridCheckpointManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridEventStorageManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridDeploymentManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridLoadBalancingManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridFailoverManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridCollisionManager(cfg, mgrReg, procReg), attrs);
            startManager(new GridTopologyManager(cfg, mgrReg, procReg), attrs);

            // Start processors before discovery manager, so they will
            // be able to start receiving messages once discovery completes.
            startProcessor(new GridTaskProcessor(mgrReg, procReg, cfg));
            startProcessor(new GridJobProcessor(mgrReg, procReg, cfg));

            synchronized (mux) {
                state = GridKernalState.STARTED;

                // Start discovery manager last to make sure that grid is fully initialized.
                startManager(new GridDiscoveryManager(cfg, mgrReg, procReg), attrs);
            }

            // Callbacks.
            for (GridManager mgr : mgrReg.getManagers()) {
                mgr.onKernalStart();
            }

            // Callbacks.
            for (GridProcessor proc : procReg.getProcessors()) {
                proc.onKernalStart();
            }

            // Register MBeans.
            registerKernalMBean();
            registerLocalNodeMBean();
            registerExecutorMBeans();

            // Lifecycle bean notifications.
            notifyLifecycleBeans(GridLifecycleEventType.AFTER_GRID_START);
        }
        catch (Throwable e) {
            String msg = "Got exception while starting. Will rollback already started managers and SPIs.";

            log.error(msg, e);

            stop(false, false);

            throw (GridException)new GridException(e).setData(715, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }

        long startms = System.currentTimeMillis() - tstamp;

        // Mark start timestamp.
        startTstamp = System.currentTimeMillis();

        // Ack latest version information.
        if (notifier != null) {
            notifier.reportStatus(log);
        }

        // Setup periodic version check.
        updateNotifierTime.scheduleAtFixedRate(new TimerTask() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                GridUpdateNotifier notifier = new GridUpdateNotifier(gridName, true);

                // Request latest version update.
                notifier.requestStatus(cfg.getExecutorService(), log);

                //noinspection UnusedCatchParameter
                try {
                    Thread.sleep(PERIODIC_VER_CHECK_CONN_TIMEOUT);
                }
                catch (InterruptedException e) {
                    // Ignoring interrupted exceptions.
                }

                notifier.reportStatus(log);
            }
        }, PERIODIC_VER_CHECK_DELAY, PERIODIC_VER_CHECK_DELAY);

        if (log.isInfoEnabled() == true) {
            String ack = "GridGain ver. " + VER + '-' + BUILD + " STARTED OK in " + startms + "ms.";

            char[] line = new char[ack.length()];

            Arrays.fill(line, '-');

            String locAddr = getLocalNode().getPhysicalAddress();

            log.info(NL + NL +
                ">>> " + new String(line) + NL +
                ">>> " + ack + NL +
                ">>> " + new String(line) + NL +
                ">>> OS name: " + GridOs.getOsString() + ", " +
                    getLocalNode().getMetrics().getAvailableProcessors() + " CPU(s)" + NL +
                ">>> OS user: " + System.getProperty("user.name") + NL +
                ">>> VM information: " + GridOs.getJdkString() + NL +
                ">>> VM name: " + rtBean.getName() + NL +
                ">>> Optional grid name: " + gridName + NL +
                ">>> Local node ID: " + getLocalNode().getId().toString().toUpperCase() + NL +
                ">>> Local node physical address: " + locAddr + ", " + GridNetworkHelper.getNetworkInterfaceName(locAddr) +
                    NL +
                ">>> GridGain documentation: http://www.gridgain.org/product.html" + NL);
        }
    }

    /**
     * Creates attributes map and fills it in.
     *
     * @param cfg Grid configuration.
     * @return Map of all node attributes.
     * @throws GridException thrown if was unable to set up attribute.
     */
    private Map<String, Serializable> createNodeAttributes(final GridConfiguration cfg) throws GridException {
        Map<String, Serializable> attrs = new HashMap<String, Serializable>();

        try {
            // Stick all environment settings into node attributes.
            attrs.putAll(System.getenv());

            if (log.isDebugEnabled() == true) {
                log.debug("Added environment properties to node attributes.");
            }
        }
        catch (SecurityException e) {
            log.warning("Failed to add environment properties to node attributes due to security violation: " +
                e.getMessage());
        }

        try {
            // Stick all system properties into node's attributes overwriting any
            // identical names from environment properties.
            for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
                //noinspection SuspiciousMethodCalls
                Object val = attrs.get(e.getKey());

                //noinspection SuspiciousMethodCalls
                if (val != null && val.equals(e.getValue()) == false) {
                    log.warning("System property has the same name as environment variable and will take precedence: "
                        + e.getKey());
                }

                attrs.put((String)e.getKey(), (Serializable)e.getValue());
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Added system properties to node attributes.");
            }
        }
        catch (SecurityException e) {
            log.warning("Failed to add system properties to node attributes due to security violation: " +
                e.getMessage());
        }

        // Stick in some system level attributes
        addAttribute(attrs, ATTR_JIT_NAME, ManagementFactory.getCompilationMXBean() == null ? "" :
            ManagementFactory.getCompilationMXBean().getName());
        addAttribute(attrs, ATTR_BUILD_VER, getVersion());
        addAttribute(attrs, ATTR_USER_NAME, System.getProperty("user.name"));
        addAttribute(attrs, ATTR_GRID_NAME, gridName);
        addAttribute(attrs, ATTR_DEPLOYMENT_MODE, cfg.getDeploymentMode());

        /*
         * Stick in SPI versions and classes attributes.
         */

        // Collision SPI.
        Class<? extends GridSpi> spiCls = cfg.getCollisionSpi().getClass();

        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getCollisionSpi(),ATTR_SPI_CLASS), spiCls.getName());
        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getCollisionSpi(), ATTR_SPI_VER),
            getSpiVersion(spiCls));

        // Topology SPIs.
        for (GridTopologySpi topSpi: cfg.getTopologySpi()) {
            spiCls = topSpi.getClass();

            addAttribute(attrs, GridUtils.createSpiAttributeName(topSpi, ATTR_SPI_CLASS), spiCls.getName());
            addAttribute(attrs, GridUtils.createSpiAttributeName(topSpi, ATTR_SPI_VER), getSpiVersion(spiCls));
        }

        // Discovery SPI.
        spiCls = cfg.getDiscoverySpi().getClass();

        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getDiscoverySpi(), ATTR_SPI_CLASS), spiCls.getName());
        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getDiscoverySpi(), ATTR_SPI_VER),
            getSpiVersion(spiCls));

        // Failover SPIs.
        for (GridFailoverSpi failSpi: cfg.getFailoverSpi()) {
            spiCls = failSpi.getClass();

            addAttribute(attrs, GridUtils.createSpiAttributeName(failSpi, ATTR_SPI_CLASS), spiCls.getName());
            addAttribute(attrs, GridUtils.createSpiAttributeName(failSpi, ATTR_SPI_VER), getSpiVersion(spiCls));
        }

        // Communication SPI.
        spiCls = cfg.getCommunicationSpi().getClass();

        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getCommunicationSpi(), ATTR_SPI_CLASS),
            spiCls.getName());
        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getCommunicationSpi(), ATTR_SPI_VER),
            getSpiVersion(spiCls));

        // Event storage SPI.
        spiCls = cfg.getEventStorageSpi().getClass();

        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getEventStorageSpi(), ATTR_SPI_CLASS),
            spiCls.getName());
        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getEventStorageSpi(), ATTR_SPI_VER),
            getSpiVersion(spiCls));

        // Tracing SPIs.
        if (cfg.getTracingSpi() != null) {
            for (GridTracingSpi traceSpi: cfg.getTracingSpi()) {
                spiCls = traceSpi.getClass();

                addAttribute(attrs, GridUtils.createSpiAttributeName(traceSpi, ATTR_SPI_CLASS), spiCls.getName());
                addAttribute(attrs, GridUtils.createSpiAttributeName(traceSpi, ATTR_SPI_VER), getSpiVersion(spiCls));
            }
        }

        // Checkpoints SPIs.
        for (GridCheckpointSpi cpSpi: cfg.getCheckpointSpi()) {
            spiCls = cpSpi.getClass();

            addAttribute(attrs, GridUtils.createSpiAttributeName(cpSpi, ATTR_SPI_CLASS), spiCls.getName());
            addAttribute(attrs, GridUtils.createSpiAttributeName(cpSpi, ATTR_SPI_VER), getSpiVersion(spiCls));
        }

        // Load balancing SPIs.
        for (GridLoadBalancingSpi loadSpi: cfg.getLoadBalancingSpi()) {
            spiCls = loadSpi.getClass();

            addAttribute(attrs, GridUtils.createSpiAttributeName(loadSpi, ATTR_SPI_CLASS), spiCls.getName());
            addAttribute(attrs, GridUtils.createSpiAttributeName(loadSpi, ATTR_SPI_VER), getSpiVersion(spiCls));
        }

        // Metrics SPI.
        spiCls = cfg.getMetricsSpi().getClass();

        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getMetricsSpi(), ATTR_SPI_CLASS), spiCls.getName());
        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getMetricsSpi(), ATTR_SPI_VER),
            getSpiVersion(spiCls));

        spiCls = cfg.getDeploymentSpi().getClass();

        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getDeploymentSpi(), ATTR_SPI_VER), getSpiVersion(spiCls));
        addAttribute(attrs, GridUtils.createSpiAttributeName(cfg.getDeploymentSpi(), ATTR_SPI_CLASS), spiCls.getName());

        // Set user attributes for this node.
        if (cfg.getUserAttributes() != null) {
            for (Map.Entry<String, ? extends Serializable> e : cfg.getUserAttributes().entrySet()) {
                if (attrs.containsKey(e.getKey()) == true) {
                    log.warning("User or internal attribute has the same name as environment or system " +
                        "property and will take precedence: " + e.getKey());
                }

                attrs.put(e.getKey(), e.getValue());
            }
        }

        return attrs;
    }

    /**
     *
     * @throws GridException If registration failed.
     */
    private void registerKernalMBean() throws GridException {
        try {
            kernalMBean = GridUtils.registerMBean(
                cfg.getMBeanServer(),
                cfg.getGridName(),
                "Kernal",
                getClass().getSimpleName(),
                this,
                GridKernalMBean.class);

            if (log.isDebugEnabled() == true) {
                log.debug("Registered kernal MBean: " + kernalMBean);
            }
        }
        catch (JMException e) {
            kernalMBean = null;

            throw (GridException)new GridException("Failed to register kernal MBean.", e).setData(958, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }
    }

    /**
     * @throws GridException If registration failed.
     */
    private void registerLocalNodeMBean() throws GridException {
        GridNodeMetricsMBean mbean = new GridLocalNodeMetrics(mgrReg.getDiscoveryManager().getLocalNode());

        try {
            locNodeMBean = GridUtils.registerMBean(
                cfg.getMBeanServer(),
                cfg.getGridName(),
                "Kernal",
                mbean.getClass().getSimpleName(),
                mbean,
                GridNodeMetricsMBean.class);

            if (log.isDebugEnabled() == true) {
                log.debug("Registered local node MBean: " + locNodeMBean);
            }
        }
        catch (JMException e) {
            locNodeMBean = null;

            throw (GridException)new GridException("Failed to register local node MBean.", e).setData(984, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }
    }

    /**
     * @throws GridException If registration failed.
     */
    private void registerExecutorMBeans() throws GridException {
        execSvcMBean = registerExecutorMBean(cfg.getExecutorService(), "GridExecutionExecutorService");
        systemExecSvcMBean = registerExecutorMBean(cfg.getSystemExecutorService(), "GridSystemExecutorService");
        p2PExecSvcMBean = registerExecutorMBean(cfg.getPeerClassLoadingExecutorService(), "GridClassLoadingExecutorService");
    }

    /**
     * @param exec Executor service to register.
     * @param name Property name for executor.
     * @throws GridException If registration failed.
     * @return Name for created MBean.
     */
    private ObjectName registerExecutorMBean(ExecutorService exec, String name) throws GridException {
        ObjectName res = null;

        try {
            res = GridUtils.registerMBean(
                cfg.getMBeanServer(),
                cfg.getGridName(),
                "Thread Pools",
                name,
                new GridExecutorServiceMBeanAdapter(exec),
                GridExecutorServiceMBean.class);

            if (log.isDebugEnabled() == true) {
                log.debug("Registered executor service MBean: " + res);
            }

            return res;
        }
        catch (JMException e) {
            throw (GridException)new GridException("Failed to register executor service MBean [name=" + name + ", exec=" + exec + ']',
                e).setData(1022, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }
    }

    /**
     *
     * @return Unregister executor services.
     */
    @SuppressWarnings({"NonShortCircuitBooleanExpression"})
    private boolean unregisterExecutorMBeans() {
        // Note that used binary AND operation instead of boolean AND.
        return unregisterMBean(execSvcMBean) &
            unregisterMBean(systemExecSvcMBean) &
            unregisterMBean(p2PExecSvcMBean);
    }

    /**
     * Unregisters given mbean.
     *
     * @param mbean MBean to unregister.
     * @return <tt>True</tt> if successfully unregistered, <tt>false</tt> otherwise.
     */
    private boolean unregisterMBean(ObjectName mbean) {
        if (mbean != null) {
            try {
                cfg.getMBeanServer().unregisterMBean(mbean);

                if (log.isDebugEnabled() == true) {
                    log.debug("Unregistered MBean: " + mbean);
                }

                return true;
            }
            catch (JMException e) {
                log.error("Failed to unregister MBean.", e);

                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param mgr Manager to start.
     * @param attrs SPI attributes to set.
     * @throws GridException Throw in case of any errors.
     */
    private void startManager(GridManager mgr, Map<String, Serializable>attrs) throws GridException {
        assert mgrReg != null : "ASSERTION [line=1073, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        mgr.addSpiAttributes(attrs);

        // Set all node attributes into discovery manager,
        // so they can be distributed to all nodes.
        if (mgr instanceof GridDiscoveryManager) {
            ((GridDiscoveryManager)mgr).setNodeAttributes(attrs);
        }

        // Add manager to registry before it starts to avoid
        // cases when manager is started but registry does not
        // have it yet.
        mgrReg.add(mgr);

        try {
            mgr.start();
        }
        catch (GridException e) {
            throw (GridException)new GridException("Failed to start manager: " + mgr, e).setData(1092, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }
    }

    /**
     *
     * @param proc Processor to start.
     * @throws GridException Thrown in case of any error.
     */
    private void startProcessor(GridProcessor proc) throws GridException {
        procReg.add(proc);

        try {
            proc.start();
        }
        catch (GridException e) {
            throw (GridException)new GridException("Failed to start processor: " + proc, e).setData(1108, "src/java/org/gridgain/grid/kernal/GridKernal.java");
        }
    }

    /**
     *
     *
     */
    private void logo() {
        assert log != null : "ASSERTION [line=1117, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        if (log.isInfoEnabled() == true) {
            log.info(NL +
            "  _____      _     _  _____       _        " + NL +
            " / ____|    (_)   | |/ ____|     (_)       " + NL +
            "| |  __ _ __ _  __| | |  __  __ _ _ _ __    " + NL +
            "| | |_ | '__| |/ _` | | |_ |/ _` | | '_ \\  " + NL +
            "| |__| | |  | | (_| | |__| | (_| | | | | |  " + NL +
            " \\_____|_|  |_|\\__,_|\\_____|\\__,_|_|_| |_| " + NL +
            NL +
            ">>> " + "GridGain ver. " + VER + '-' + BUILD + NL +
            ">>> " + COPYRIGHT +
            NL);
        }
    }

    /**
     * @param ggHome GridGain installation home.
     * @throws GridException Throws in case of any errors.
     */
    private void checkLicense(String ggHome) throws GridException {
        assert log != null : "ASSERTION [line=1139, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        assert ggHome != null : "ASSERTION [line=1140, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        File license = new File(ggHome, LICENSE_FILE);

        if (license.exists() == false) {
            if (log.isInfoEnabled() == true) {
                log.info("License file was not found: " + license +
                    ". GridGain is dual-licensed under LGPL and Apache 2.0 (APIs only). This error is safely ignored. " +
                    "However, it is advised that license file is not renamed or moved from GRIDGAIN_HOME directory.");
            }

            return;
        }

        if (log.isDebugEnabled() == false) {
            if (log.isInfoEnabled() == true) {
                log.info("License file can be found at: " + license.getAbsolutePath());
            }
        }
        else {
            BufferedReader reader = null;

            try {
                try {
                    reader = new BufferedReader(new FileReader(license));

                    log.debug("");
                    log.debug(">>>" );

                    for (String line = null; (line = reader.readLine()) != null;) {
                        log.debug(">>> " + line);
                    }

                    log.debug(">>>");
                    log.debug("");
                }
                finally {
                    GridUtils.close(reader, log);
                }
            }
            catch (IOException e) {
                throw (GridException)new GridException("Failed to read license file: " + license +
                    ". Make sure that license file can be read, i.e. it or its folder has proper permissions.", e).setData(1181, "src/java/org/gridgain/grid/kernal/GridKernal.java");
            }
        }
    }

    /**
     * Stops grid instance.
     *
     * @param cancel Whether or not to cancel running jobs.
     * @param wait If <tt>true</tt> then method will wait for all task being
     *      executed until they finish their execution.
     */
    @SuppressWarnings({"CatchGenericClass"})
    public void stop(boolean cancel, boolean wait) {
        boolean stopInitiated = false;

        synchronized (mux) {
            if (state == GridKernalState.STARTED) {
                state = GridKernalState.BEFORE_STOP;

                stopInitiated = true;
            }
        }

        if (stopInitiated == true) {
            // Lifecycle bean notifications.
            notifyLifecycleBeans(GridLifecycleEventType.BEFORE_GRID_STOP);
        }

        long start = System.currentTimeMillis();

        synchronized (mux) {
            if (state == GridKernalState.STARTING) {
                log.warning("Attempt to stop starting grid. This operation cannot be guaranteed to be successful.");
            }

            if (stopInitiated == false) {
                // If another thread is already stopping grid, then wait and exit.
                try {
                    while (state == GridKernalState.BEFORE_STOP || state == GridKernalState.STOPPING) {
                        mux.wait();
                    }
                }
                catch (InterruptedException e) {
                    errOnStop = true;

                    if (log.isInfoEnabled() == true) {
                        log.warning("Got interrupted while stopping (ignoring but grid may not be stopped properly) " +
                            e.getMessage());
                    }
                }
            }

            // If grid is stopped, nothing to do.
            if (state == GridKernalState.STOPPED) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Grid is already stopped. Nothing to do...");
                }

                return;
            }

            state = GridKernalState.STOPPING;

            // Cancel update notification timer.
            updateNotifierTime.cancel();

            // Wait for external calls to complete. Note,
            // that this wait must happen only after the
            // state is set to STOPPING.
            while (true) {
                assert callCnt >= 0 : "ASSERTION [line=1253, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

                // This condition is taken out of the loop to avoid
                // potentially wrong optimization by the compiler of
                // moving field access out of the loop causing this loop
                // to never exit.
                if (callCnt == 0) {
                    break;
                }

                try {
                    mux.wait();
                }
                catch (InterruptedException e) {
                    errOnStop = true;

                    if (log.isInfoEnabled() == true) {
                        log.warning("Got interrupted while stopping (ignoring but grid may not be stopped properly) " +
                            e.getMessage());
                    }
                }
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Grid " + (gridName == null ? "" : '\'' + gridName + "' ") + "is stopping...");
            }
        }

        if (wait == true) {
            // Wait for all tasks to be finished.
            try {
                procReg.getTaskProcessor().waitForTasksFinishing();
            }
            catch (InterruptedException e) {
                log.error("Failed to wait for the tasks completion", e);
            }
        }

        // Callbacks.
        for (GridProcessor proc : procReg.getProcessors()) {
            try {
                proc.onKernalStop(cancel);
            }
            catch (Throwable e) {
                errOnStop = true;

                log.error("Failed to pre-stop processor: " + proc, e);
            }
        }

        // Callbacks.
        for (GridManager mgr : mgrReg.getManagers()) {
            try {
                mgr.onKernalStop();
            }
            catch (Throwable e) {
                errOnStop = true;

                log.error("Failed to pre-stop manager: " + mgr, e);
            }
        }

        // Note that at this point it is possible for another thread
        // to call Grid.execute(...) method since task processor has not
        // been called yet. We accept it.

        // Stop processors outside of synchronization to allow
        // API access during stop for currently running and finishing
        // jobs. Note that task and job processors will not allow any new
        // executions (local or remote).

        if (procReg.getJobProcessor() != null) {
            try {
                procReg.getJobProcessor().stop(cancel);
            }
            catch (Throwable e) {
                errOnStop = true;

                log.error("Failed to stop job processor (ignoring): " + procReg.getJobProcessor(), e);
            }
        }

        if (procReg.getTaskProcessor() != null) {
            try {
                procReg.getTaskProcessor().stop(cancel);
            }
            catch (Throwable e) {
                errOnStop = true;

                log.error("Failed to stop task processor (ignoring): " + procReg.getTaskProcessor(), e);
            }
        }

        // Wait until out of any call.
        synchronized (mux) {
            while (true) {
                assert callCnt >= 0 : "ASSERTION [line=1349, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

                // This condition is taken out of the loop to avoid
                // potentially wrong optimization by the compiler of
                // moving field access out of the loop causing this loop
                // to never exit.
                if (callCnt == 0) {
                    break;
                }

                try {
                    mux.wait();
                }
                catch (InterruptedException e) {
                    errOnStop = true;

                    if (log.isInfoEnabled() == true) {
                        log.info("Interrupted wait (ignoring): " + e.getMessage());
                    }
                }
            }

            assert callCnt == 0 : "ASSERTION [line=1371, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";
        }

        // Unregister MBeans.
        if (unregisterMBean(kernalMBean) == false) {
            errOnStop = true;
        }

        if (unregisterMBean(locNodeMBean) == false) {
            errOnStop = true;
        }

        if (unregisterExecutorMBeans() == false) {
            errOnStop = true;
        }

        // Check for null. Registry can be null if grid was not started successfully
        // and none of the managers were started.
        if (mgrReg != null) {
            List<GridManager> mgrs = mgrReg.getManagers();

            // Stop managers in reverse order.
            for (int i = mgrs.size() - 1; i >= 0; i--) {
                GridManager mgr = mgrs.get(i);

                try {
                    mgr.stop();

                    if (log.isDebugEnabled() == true) {
                        log.debug("Manager stopped: " + mgr);
                    }
                }
                catch (Throwable e) {
                    errOnStop = true;

                    log.error("Failed to stop manager (ignoring): " + mgr, e);
                }
            }
        }

        // Stop timeout processor after executors and managers, as executors may wish to
        // make calls to it during stop.
        if (procReg.getTimeoutProcessor() != null) {
            try {
                procReg.getTimeoutProcessor().stop(cancel);
            }
            catch (Throwable e) {
                errOnStop = true;

                log.error("Failed to stop timeout processor (ignoring): " + procReg.getTimeoutProcessor(), e);
            }
        }

        if (procReg.getResourceProcessor() != null) {
            try {
                procReg.getResourceProcessor().stop(cancel);
            }
            catch (Throwable e) {
                errOnStop = true;

                log.error("Failed to stop resource processor (ignoring): " + procReg.getResourceProcessor(), e);
            }
        }

        // Lifecycle notification.
        notifyLifecycleBeans(GridLifecycleEventType.AFTER_GRID_STOP);

        mgrReg = null;

        for (GridRunnable r : GridRunnableGroup.getInstance(gridName).getActiveSet()) {
            String n1 = r.getGridName() == null ? "" : r.getGridName();
            String n2 = gridName == null ? "" : gridName;

            /*
             * We should never get a runnable from one grid instance
             * in the runnable group for another grid instance.
             */
            assert n1.equals(n2) == true : "ASSERTION [line=1448, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

            if (log.isDebugEnabled() == true) {
                log.debug("Joining on runnable after grid has stopped: " + r);
            }

            try {
                r.join();
            }
            catch (InterruptedException e) {
                errOnStop = true;

                log.error("Got interrupted during grid stop (ignoring).", e);

                break;
            }
        }

        // Release memory.
        GridRunnableGroup.removeInstance(gridName);

        long duration = System.currentTimeMillis() - start;

        synchronized (mux) {
            state = GridKernalState.STOPPED;

            mux.notifyAll();

            // Ack stop.
            if (log.isInfoEnabled() == true) {
                if (errOnStop == false) {
                    String ack = "GridGain ver. " + VER + '-' + BUILD + " STOPPED OK in " + duration + "ms.";

                    char[] line = new char[ack.length()];

                    Arrays.fill(line, '-');

                    //noinspection CallToNativeMethodWhileLocked
                    log.info(NL + NL +
                        ">>> " + new String(line) + NL +
                        ">>> " + ack + NL +
                        ">>> " + new String(line) + NL +
                        ">>> Optional instance name: " + gridName + NL +
                        ">>> Grid up time: " + GridUtils.timeSpan2String(System.currentTimeMillis() - startTstamp) +
                            NL +
                        NL);
                }
                else {
                    String ack = "GridGain ver. " + VER + '-' + BUILD + " stopped with ERRORS in " + duration + "ms.";

                    char[] under = new char[ack.length()];

                    Arrays.fill(under, '-');

                    //noinspection CallToNativeMethodWhileLocked
                    log.info(NL + NL +
                        ">>> " + ack + NL +
                        ">>> " + new String(under) + NL +
                        ">>> Optional instance name: " + gridName + NL +
                        ">>> Grid up time: " + GridUtils.timeSpan2String(System.currentTimeMillis() - startTstamp) +
                            NL +
                        ">>> See log above for detailed error message." + NL +
                        ">>> Note that some errors during stop can prevent grid from" + NL +
                        ">>> maintaining correct topology since this node may have" + NL +
                        ">>> not exited grid properly." + NL +
                        NL);
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if grid successfully stop.
     * For test only!!!
     * @return true if grid successfully stop.
     */
    public boolean isStopSuccess() {
        return errOnStop == false;
    }

    /**
     * {@inheritDoc}
     */
    public void addLocalEventListener(GridLocalEventListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        beforeCall();

        try {
            mgrReg.getEventStorageManager().addGridLocalEvenListener(listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeLocalEventListener(GridLocalEventListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        beforeCall();

        try {
            return mgrReg.getEventStorageManager().removeGridLocalEventListener(listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addDiscoveryListener(GridDiscoveryListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        beforeCall();

        try {
            mgrReg.getDiscoveryManager().addDiscoveryListener(listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeDiscoveryListener(GridDiscoveryListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().removeDiscoveryListener(listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode node, Serializable msg) throws GridException {
        GridArgumentCheck.checkNull(node, "node");
        GridArgumentCheck.checkNull(msg, "msg");

        beforeCall();

        try {
            mgrReg.getCommunicationManager().sendMessage(node, GridCommunicationManager.USER_COMM_TOPIC, msg,
                GridCommunicationThreadPolicy.POOLED_THREAD);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> nodes, Serializable msg) throws GridException {
        GridArgumentCheck.checkNull(nodes, "nodes");
        GridArgumentCheck.checkNull(msg, "msg");

        beforeCall();

        try {
            mgrReg.getCommunicationManager().sendMessage(nodes, GridCommunicationManager.USER_COMM_TOPIC, msg,
                GridCommunicationThreadPolicy.POOLED_THREAD);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addMessageListener(GridMessageListener listener){
        GridArgumentCheck.checkNull(listener, "listener");

        beforeCall();

        try {
            mgrReg.getCommunicationManager().addMessageListener(GridCommunicationManager.USER_COMM_TOPIC, listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeMessageListener(GridMessageListener listener) {
        GridArgumentCheck.checkNull(listener, "listener");

        beforeCall();

        try {
            return mgrReg.getCommunicationManager().removeMessageListener(GridCommunicationManager.USER_COMM_TOPIC,
                listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getNode(UUID nodeId) {
        GridArgumentCheck.checkNull(nodeId, "nodeId");

        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().getNode(nodeId);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getRemoteNodes(){
        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().getRemoteNodes();
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getAllNodes() {
        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().getAllNodes();
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getNodes(GridNodeFilter filter) {
        GridArgumentCheck.checkNull(filter, "filter");

        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().getNodes(filter);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getLocalNode(){
        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().getLocalNode();
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(String nodeId) {
        GridArgumentCheck.checkNull(nodeId, "nodeId");

        return pingNode(UUID.fromString(nodeId));
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNodeByAddress(String host) {
        GridArgumentCheck.checkNull(host, "host");

        beforeCall();

        try {
            for (GridNode node : mgrReg.getDiscoveryManager().getAllNodes()) {
                if (node.getPhysicalAddress().equals(host) == true) {
                    return mgrReg.getDiscoveryManager().pingNode(node.getId());
                }
            }
        }
        finally {
            afterCall();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId){
        GridArgumentCheck.checkNull(nodeId, "nodeId");

        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().pingNode(nodeId);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String executeTask(String taskName, String arg) throws JMException {
        GridArgumentCheck.checkNull(taskName, "taskName");

        beforeCall();

        try {
            GridTaskFuture<String> future = procReg.getTaskProcessor().execute(taskName, arg, 0, null);

            return future.get();
        }
        // We need to wrap grid exception into JMX friendly one.
        catch (GridException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new JMException(e.getMessage());
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg) {
        return execute(taskName, arg, 0);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, GridTaskListener listener) {
        return execute(taskName, arg, 0, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, long timeout, GridTaskListener listener) {
        GridArgumentCheck.checkNull(taskName, "taskName");
        GridArgumentCheck.checkNull(listener, "listener");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return procReg.getTaskProcessor().execute(taskName, arg, timeout, listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, long timeout) {
        GridArgumentCheck.checkNull(taskName, "taskName");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return procReg.getTaskProcessor().execute(taskName, arg, timeout, null);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg) {
        return execute(taskCls, arg, 0);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg, GridTaskListener listener) {
        return execute(taskCls, arg, 0, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg, long timeout,
        GridTaskListener listener) {
        GridArgumentCheck.checkNull(taskCls, "taskCls");
        GridArgumentCheck.checkNull(listener, "listener");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return procReg.getTaskProcessor().execute(taskCls, arg, timeout, listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg, long timeout) {
        GridArgumentCheck.checkNull(taskCls, "taskCls");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return procReg.getTaskProcessor().execute(taskCls, arg, timeout, null);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, GridTaskListener listener) {
        return execute(task, arg, 0, listener);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg) {
        return execute(task, arg, 0);
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, long timeout, GridTaskListener listener) {
        GridArgumentCheck.checkNull(task, "task");
        GridArgumentCheck.checkNull(listener, "listener");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return procReg.getTaskProcessor().execute(task, arg, timeout, listener);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, long timeout) {
        GridArgumentCheck.checkNull(task, "task");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return procReg.getTaskProcessor().execute(task, arg, timeout, null);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<GridEvent> queryLocalEvents(GridEventFilter filter) {
        GridArgumentCheck.checkNull(filter, "filter");

        beforeCall();

        try {
            return mgrReg.getEventStorageManager().queryLocalEvents(filter);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void deployTask(Class<? extends GridTask> taskCls) throws GridException {
        GridArgumentCheck.checkNull(taskCls, "taskCls");

        deployTask(taskCls, GridUtils.detectClassLoader(taskCls));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void deployTask(Class<? extends GridTask> taskCls, ClassLoader clsLoader) throws GridException {
        GridArgumentCheck.checkNull(taskCls, "taskCls");
        GridArgumentCheck.checkNull(clsLoader, "clsLoader");

        beforeCall();

        try {
            // Explicit deploy.
            mgrReg.getDeploymentManager().deploy(taskCls, clsLoader);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCheckpoint(String key) {
        beforeCall();

        try {
            return mgrReg.getCheckpointManager().removeCheckpoint(key);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Class<? extends GridTask<?, ?>>> getLocalTasks() {
        beforeCall();

        try {
            return mgrReg.getDeploymentManager().findAllTasks();
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeployTask(String taskName) throws GridException {
        GridArgumentCheck.checkNull(taskName, "taskName");

        beforeCall();

        try {
            mgrReg.getDeploymentManager().undeployTask(taskName);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeployTaskFromGrid(String taskName) throws JMException {
        try {
            undeployTask(taskName);
        }
        catch (GridException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new JMException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<GridEvent> queryEvents(GridEventFilter filter, Collection<GridNode> nodes, long timeout)
        throws GridException {
        GridArgumentCheck.checkNull(filter, "filter");
        GridArgumentCheck.checkNull(nodes, "nodes");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");

        beforeCall();

        try {
            return mgrReg.getEventStorageManager().query(filter, nodes, timeout);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getTopologyHash(Collection<GridNode> nodes) {
        GridArgumentCheck.checkNull(nodes, "nodes");

        beforeCall();

        try {
            return mgrReg.getDiscoveryManager().getTopologyHash(nodes);
        }
        finally {
            afterCall();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getAllTopologyHash() {
        return getTopologyHash(getAllNodes());
    }

    /**
     *
     */
    private void beforeCall() {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=2101, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        synchronized (mux) {
            if (state == GridKernalState.STARTING ||
                state == GridKernalState.STOPPING ||
                state == GridKernalState.STOPPED) {
                throw new IllegalStateException("Grid instance is not properly started. " +
                    "Grid instance is either starting, other thread stopped it or shutdown hook has been executed.");
            }

            callCnt++;
        }
    }

    /**
     *
     */
    private void afterCall() {
        assert Thread.holdsLock(mux) == false : "ASSERTION [line=2119, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        synchronized (mux) {
            callCnt--;

            if (callCnt == 0) {
                mux.notifyAll();
            }
        }
    }

    /**
     * Prints all system properties in debug mode.
     */
    private void logSystemProperties() {
        assert log != null : "ASSERTION [line=2134, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        if (log.isDebugEnabled() == true) {
            for (Object key : GridUtils.asIterable(System.getProperties().keys())) {
                log.debug("System property [" + key + '=' + System.getProperty((String)key) + ']');
            }
        }
    }

    /**
     * Prints all user attributes in info mode.
     */
    private void logNodeUserAttributes() {
        assert log != null : "ASSERTION [line=2147, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        if (log.isInfoEnabled() == true) {
            for (Map.Entry<?, ?> attr : cfg.getUserAttributes().entrySet()) {
                log.info("Local node user attribute [" + attr.getKey() + '=' + attr.getValue() + ']');
            }
        }
    }

    /**
     * Prints all environment variables in debug mode.
     */
    private void logEnvironmentVariables() {
        assert log != null : "ASSERTION [line=2160, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        if (log.isDebugEnabled() == true) {
            for (Map.Entry<?,?> envVar : System.getenv().entrySet()) {
                log.debug("Environment variable [" + envVar.getKey() + '=' + envVar.getValue() + ']');
            }
        }
    }

    /**
     * Provides a properly formatted string for executor service.
     *
     * @param name Executor service name.
     * @param execSvc Executor service.
     * @return Formatted string.
     */
    private String getExecutorServiceLogInfo(String name, ExecutorService execSvc) {
        if (execSvc instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor exec = (ThreadPoolExecutor)execSvc;

            String queueCls = exec.getQueue() == null ? "" : exec.getQueue().getClass().getSimpleName();

            String factoryCls = exec.getThreadFactory() == null ?
                "" : exec.getThreadFactory().getClass().getSimpleName();

            String rejectHandlerCls = exec.getRejectedExecutionHandler() == null ?
                "" : exec.getRejectedExecutionHandler().getClass().getSimpleName();

            return "Grid executor service" +
                " [name=" + name +
                ", corePoolSize=" + exec.getCorePoolSize() +
                ", maxPoolSize=" + exec.getMaximumPoolSize() +
                ", keepAliveTime=" + exec.getKeepAliveTime(TimeUnit.MILLISECONDS) + "ms" +
                ", queueCls=" + queueCls +
                ", threadFactoryCls=" + factoryCls +
                ", rejectionHandlerCls=" + rejectHandlerCls + ']';
        }

        return "Grid executor service [name=" + name + ", execSvc=" + execSvc + ']';
    }

    /**
     * Prints all configuration properties in info mode and SPIs in debug mode.
     */
    private void logConfiguration() {
        assert log != null : "ASSERTION [line=2205, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        if (log.isInfoEnabled() == true) {
            log.info("User class loader version: " + GridUserVersionHelper.getUserVersion(getClass().getClassLoader(), log));
            log.info("Local node ID: " + cfg.getNodeId());
            log.info("P2P exclude path: " + cfg.getP2PLocalClassPathExclude());
            log.info("Peer class loading enabled: " + cfg.isPeerClassLoadingEnabled());
            log.info("Peer class loading missed resources cache size: " + cfg.getPeerClassLoadingMissedResourcesCacheSize());
            log.info("Peer class loading timeout (ms): " + cfg.getPeerClassLoadingTimeout());
            log.info("Metrics expiration time: " + cfg.getMetricsExpireTime());
            log.info("Metrics history size: " + cfg.getMetricsHistorySize());
            log.info("Discovery startup delay (ms): " + cfg.getDiscoveryStartupDelay());
            log.info("Object marshaller: " + cfg.getMarshaller());
            log.info(getExecutorServiceLogInfo("executorService", cfg.getExecutorService()));
            log.info(getExecutorServiceLogInfo("systemExecutorService", cfg.getSystemExecutorService()));
            log.info(getExecutorServiceLogInfo("peerClassLoadingExecutorService",
                cfg.getPeerClassLoadingExecutorService()));
        }

        if (log.isDebugEnabled() == true) {
            log.debug("-------------------");
            log.debug("Pre-Start SPI List:");
            log.debug("-------------------");
            log.debug("Grid checkpoint SPI     : " + Arrays.toString(cfg.getCheckpointSpi()));
            log.debug("Grid collision SPI      : " + cfg.getCollisionSpi());
            log.debug("Grid communication SPI  : " + cfg.getCommunicationSpi());
            log.debug("Grid deployment SPI     : " + cfg.getDeploymentSpi());
            log.debug("Grid discovery SPI      : " + cfg.getDiscoverySpi());
            log.debug("Grid event storage SPI  : " + cfg.getEventStorageSpi());
            log.debug("Grid failover SPI       : " + Arrays.toString(cfg.getFailoverSpi()));
            log.debug("Grid load balancing SPI : " + Arrays.toString(cfg.getLoadBalancingSpi()));
            log.debug("Grid metrics SPI        : " + cfg.getMetricsSpi());
            log.debug("Grid topology SPI       : " + Arrays.toString(cfg.getTopologySpi()));
            log.debug("Grid tracing SPI (opt.) : " + Arrays.toString(cfg.getTracingSpi()));
        }
    }

    /**
     * Prints out VM arguments and GRIDGAIN_HOME in info mode.
     *
     * @param rtBean Java runtime bean.
     */
    private void logVmArguments(RuntimeMXBean rtBean) {
        assert log != null : "ASSERTION [line=2248, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        // Ack GRIDGAIN_HOME and VM arguments.
        if (log.isInfoEnabled() == true) {
            log.info("GRIDGAIN_HOME=" + cfg.getGridGainHome());
            log.info("VM arguments: " + rtBean.getInputArguments());
        }
    }

    /**
     * Prints out class paths in debug mode.
     *
     * @param rtBean Java runtime bean.
     */
    private void logClassPaths(RuntimeMXBean rtBean) {
        assert log != null : "ASSERTION [line=2263, file=src/java/org/gridgain/grid/kernal/GridKernal.java]";

        // Ack all class paths.
        if (log.isDebugEnabled() == true) {
            log.debug("Boot class path: " + rtBean.getBootClassPath());
            log.debug("Class path: " + rtBean.getClassPath());
            log.debug("Library path: " + rtBean.getLibraryPath());
        }
    }

    /**
     * Returns processor registry. For tests only.
     * @return processor registry.
     */
    public GridProcessorRegistry getProcessorRegistry() {
        return procReg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridKernal.class, this);
    }
}
