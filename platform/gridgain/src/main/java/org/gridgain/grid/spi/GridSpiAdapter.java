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

package org.gridgain.grid.spi;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.management.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.*;

/**
 * This class provides convenient adapter for SPI implementations.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public abstract class GridSpiAdapter implements GridSpi, GridSpiManagementMBean {
    /** System line separator. */
    private static final String NL = System.getProperty("line.separator");

    /** Instance of SPI annotation. */
    private GridSpiInfo spiAnn = null;

    /** */
    private ObjectName spiMBean = null;

    /** SPI start timestamp. */
    private long startTstamp = 0;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridMBeanServerResource
    private MBeanServer jmx = null;

    /** */
    @GridHomeResource
    private String ggHome = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** SPI name. */
    private String name = null;

    /** Grid SPI context. */
    private GridSpiContext spiCtx = new GridDummySpiContext(null);

    /** Discovery listener. */
    private GridDiscoveryListener paramsListener = null;

    /**
     * Creates new adapter and initializes it from the current (this) class.
     * SPI name will be initialized to the simple name of the class
     * (see {@link Class#getSimpleName()}).
     */
    protected GridSpiAdapter() {
        init(getClass());

        name = GridUtils.getSimpleName(getClass());

        assert spiAnn != null : "ASSERTION [line=88, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]. " + "Every SPI must have @GridSpiInfo annotation.";
    }

    /**
     * Starts startup stopwatch.
     */
    protected void startStopwatch() {
        startTstamp = System.currentTimeMillis();
    }

    /**
     * Initializes adapter from given class.
     *
     * @param cls Class to initialize from.
     */
    private void init(Class<?> cls) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            if (c.getAnnotation(GridSpiInfo.class) != null) {
                spiAnn = c.getAnnotation(GridSpiInfo.class);

                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final String getAuthor() {
        return spiAnn.author();
    }

    /**
     * {@inheritDoc}
     */
    public final String getVendorUrl() {
        return spiAnn.url();
    }

    /**
     * {@inheritDoc}
     */
    public final String getVendorEmail() {
        return spiAnn.email();
    }

    /**
     * {@inheritDoc}
     */
    public final String getVersion() {
        return spiAnn.version();
    }

    /**
     * {@inheritDoc}
     */
    public final String getStartTimestampFormatted() {
        return DateFormat.getDateTimeInstance().format(new Date(startTstamp));
    }

    /**
     * {@inheritDoc}
     */
    public final String getUpTimeFormatted() {
        return GridUtils.timeSpan2String(getUpTime());
    }

    /**
     * {@inheritDoc}
     */
    public final long getStartTimestamp() {
        return startTstamp;
    }

    /**
     * {@inheritDoc}
     */
    public final long getUpTime() {
        return startTstamp == 0 ? 0 : System.currentTimeMillis() - startTstamp;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getLocalNodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     */
    public final String getGridGainHome() {
        return ggHome;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets SPI name.
     *
     * @param name SPI name.
     */
    @GridSpiConfiguration(optional = true)
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void onContextInitialized(GridSpiContext spiCtx) throws GridSpiException {
        assert spiCtx != null : "ASSERTION [line=204, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";

        this.spiCtx = spiCtx;

        getSpiContext().addDiscoveryListener(paramsListener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                if (type.equals(GridDiscoveryEventType.JOINED) == true) {
                    checkConfigurationConsistency(node);
                }
            }
        });

        Collection<GridNode> rmtNodes = getSpiContext().getRemoteNodes();

        for (GridNode node : rmtNodes) {
            checkConfigurationConsistency(node);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onContextDestroyed() {
        if (spiCtx != null && paramsListener != null) {
            spiCtx.removeDiscoveryListener(paramsListener);
        }

        final GridNode locNode = spiCtx == null ? null : spiCtx.getLocalNode();

        // Set dummy no-op context.
        spiCtx = new GridDummySpiContext(locNode);
    }

    /**
     * Sets SPI context.
     *
     * @param spiCtx SPI context.
     */
    protected void setSpiContext(GridSpiContext spiCtx) {
        assert spiCtx != null : "ASSERTION [line=246, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";

        this.spiCtx = spiCtx;
    }

    /**
     * Gets SPI context.
     *
     * @return SPI context.
     */
    protected GridSpiContext getSpiContext() {
        return spiCtx;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return Collections.emptyMap();
    }

    /**
     * Throws exception with uniform error message if given parameter's assertion condition
     * is <tt>false</tt>.
     *
     * @param cond Assertion condition to check.
     * @param condDescr Description of failed condition. Note that this description should include
     *      JavaBean name of the property (<b>not</b> a variable name) as well condition in
     *      Java syntax like, for example:
     *      <pre name="code" class="java">
     *      assertParameter(dirPath != null, "dirPath != null");
     *      </pre>
     *      Note that in case when variable name is the same as JavaBean property you
     *      can just copy Java condition expression into description as a string.
     * @throws GridSpiException Thrown if given condition is <tt>false</tt>
     */
    protected final void assertParameter(boolean cond, String condDescr) throws GridSpiException {
        if (cond == false) {
            throw (GridSpiException)new GridSpiException("SPI parameter failed condition check: " + condDescr).setData(284, "src/java/org/gridgain/grid/spi/GridSpiAdapter.java");
        }
    }

    /**
     * Gets uniformly formatted message for SPI start.
     *
     * @return Uniformly formatted message for SPI start.
     * @throws GridSpiException If SPI is missing {@link GridSpiInfo} annotation.
     */
    protected final String startInfo() throws GridSpiException {
        GridSpiInfo ann = getClass().getAnnotation(GridSpiInfo.class);

        if (ann == null) {
            throw (GridSpiException)new GridSpiException("@GridSpiInfo annotation is missing for the SPI.").setData(298, "src/java/org/gridgain/grid/spi/GridSpiAdapter.java");
        }

        return "SPI started ok [startMs=" + getUpTime() + ", spiMBean=" + spiMBean + ']';
    }

    /**
     * Gets uniformly format message for SPI stop.
     *
     * @return Uniformly format message for SPI stop.
     */
    protected final String stopInfo() {
        return "SPI stopped ok.";
    }

    /**
     * Gets uniformed string for configuration parameter.
     *
     * @param name Parameter name.
     * @param value Parameter value.
     * @return Uniformed string for configuration parameter.
     */
    protected final String configInfo(String name, Object value) {
        assert name != null : "ASSERTION [line=321, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";

        return "Using parameter [" + name + '=' + value + ']';
    }

    /**
     *
     * @param msg Error message.
     * @param locVal Local node value.
     * @param rmtVal Remote node value.
     * @return Error text.
     */
    private String getMessageText(String msg, Object locVal, Object rmtVal) {
        return msg + NL +
            ">>>     Local node:  " + locVal + NL +
            ">>>     Remote node: " + rmtVal + NL;
    }

    /**
     * Registers SPI MBean. Note that SPI can only register one MBean.
     *
     * @param gridName Grid name. If null, then name will be empty.
     * @param impl MBean implementation.
     * @param mbeanItf MBean interface (if <tt>null</tt>, then standard JMX
     *    naming conventions are used.
     * @param <T> Type of the MBean
     * @throws GridSpiException If registration failed.
     */
    protected final <T extends GridSpiManagementMBean> void registerMBean(String gridName, T impl, Class<T> mbeanItf)
        throws GridSpiException {
        assert mbeanItf == null || mbeanItf.isInterface() == true : "ASSERTION [line=351, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";
        assert jmx != null : "ASSERTION [line=352, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";

        try {
            spiMBean = GridUtils.registerMBean(jmx, gridName, "SPIs", getName(), impl, mbeanItf);

            if (log.isDebugEnabled() == true) {
                log.debug("Registered SPI MBean: " + spiMBean);
            }
        }
        catch (JMException e) {
            throw (GridSpiException)new GridSpiException("Failed to register SPI MBean: " + spiMBean, e).setData(362, "src/java/org/gridgain/grid/spi/GridSpiAdapter.java");
        }
    }

    /**
     * Unregisters MBean.
     *
     * @throws GridSpiException If bean could not be unregistered.
     */
    protected final void unregisterMBean() throws GridSpiException {
        // Unregister SPI MBean.
        if (spiMBean != null) {
            assert jmx != null : "ASSERTION [line=374, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";

            try {
                jmx.unregisterMBean(spiMBean);

                if (log.isDebugEnabled() == true) {
                    log.debug("Unregistered SPI MBean: " + spiMBean);
                }
            }
            catch (JMException e) {
                throw (GridSpiException)new GridSpiException("Failed to unregister SPI MBean: " + spiMBean, e).setData(384, "src/java/org/gridgain/grid/spi/GridSpiAdapter.java");
            }
        }
    }

    /**
     * Checks remote node SPI configuration and prints warnings if necessary.
     *
     * @param node Remote node.
     */
    private void checkConfigurationConsistency(GridNode node) {
        assert node != null : "ASSERTION [line=395, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]";

        List<String> attrNames = getConsistentAttributeNames();

        // SPI is considered as optional if it does not require SPI class check.
        String clsAttrName = createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS);
        String verAttrName = createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER);

        // Optional SPI means that we should not print warning if SPIs are different but
        // still need to compare attributes if SPIs are the same.
        boolean isSpiOptional = attrNames.contains(clsAttrName) == false;
        boolean isSpiConsistent = false;

        StringBuilder buf = new StringBuilder();

        // If there are any attributes do compare class and version
        // (do not print warning for the optional SPIs).
        if (attrNames.isEmpty() == false) {
            // Check SPI class and version.
            String locSpiClsName = (String)getSpiContext().getLocalNode().getAttribute(clsAttrName);
            String rmtSpiClsName = (String)node.getAttribute(clsAttrName);

            String locSpiVer = (String)getSpiContext().getLocalNode().getAttribute(verAttrName);
            String rmtSpiVer = (String)node.getAttribute(verAttrName);

            assert locSpiClsName != null: "ASSERTION [line=420, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]. " + "Local SPI class name attribute not found: " + clsAttrName;
            assert locSpiVer != null: "ASSERTION [line=421, file=src/java/org/gridgain/grid/spi/GridSpiAdapter.java]. " + "Local SPI version attribute not found: " + verAttrName;

            if (rmtSpiClsName == null) {
                if (isSpiOptional == false) {
                    buf.append(getMessageText(">>> Remote node has no " + getName() + " SPI configured", locSpiClsName,
                        rmtSpiClsName));
                }
            }
            else if (locSpiClsName.equals(rmtSpiClsName) == false) {
                if (isSpiOptional == false) {
                    buf.append(getMessageText(">>> Remote node has different " + getName() + " SPI class",
                        locSpiClsName, rmtSpiClsName));
                }
            }
            else if (rmtSpiVer == null || locSpiVer.equals(rmtSpiVer) == false) {
                if (isSpiOptional == false) {
                    buf.append(getMessageText(">>> Remote node has different " + getName() + " SPI version", locSpiVer,
                        rmtSpiVer));
                }
            }
            else {
                isSpiConsistent = true;
            }
        }

        // It makes no sense to compare inconsistent SPIs attributes.
        if (isSpiConsistent == true) {
            // Process all SPI specific attributes.
            for (String attrName: attrNames) {
                // Ignore class and version attributes processed above.
                if (attrName.equals(clsAttrName) == false && attrName.equals(verAttrName) == false) {
                    // This check is considered as optional if no attributes
                    Object rmtVal = node.getAttribute(attrName);
                    Object locVal = getSpiContext().getLocalNode().getAttribute(attrName);

                    if (locVal == null && rmtVal == null) {
                        continue;
                    }

                    if (locVal == null || rmtVal == null || locVal.equals(rmtVal) == false) {
                        buf.append(getMessageText(">>> Remote node has different " + getName() + " SPI attribute " +
                            attrName, locVal, rmtVal));
                    }
                }
            }
        }

        if (buf.length() > 0) {
            log.warning(NL + NL +
                ">>> -----------------------------------------------------------------" + NL +
                ">>> Courtesy notice that joining node has inconsistent configuration." + NL +
                ">>> Ignore this message if you are sure that this is done on purpose." + NL +
                ">>> -----------------------------------------------------------------" + NL +
                ">>> Remote Node ID: " + node.getId().toString().toUpperCase() + NL +
                buf.toString());
        }
    }

    /**
     * Returns back list of attributes that should be consistent
     * for this SPI. Consistency means that remote node has to
     * have the same attribute with the same value.
     *
     * @return List or attribute names.
     */
    protected List<String> getConsistentAttributeNames() {
        return Collections.emptyList();
    }

    /**
     * Creates new name for the given attribute. Name contains
     * SPI name prefix.
     *
     * @param attrName SPI attribute name.
     * @return New name with SPI name prefix.
     */
    protected String createSpiAttributeName(String attrName) {
        return GridUtils.createSpiAttributeName(this, attrName);
    }

    /**
     *
     * FIXDOC: add file description.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private static class GridDummySpiContext implements GridSpiContext {
        /** */
        private final GridNode locNode;

        /**
         * FIXDOC.
         *
         * @param locNode FIXDOC.
         */
        GridDummySpiContext(final GridNode locNode) {
            this.locNode = locNode;
        }

        /**
         * {@inheritDoc}
         */
        public void addDiscoveryListener(GridDiscoveryListener listener) {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public void addLocalEventListener(GridLocalEventListener listener) {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public void addMessageListener(GridMessageListener listener, String topic) {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public Collection<GridNode> getAllNodes() {
            if (locNode == null) {
                return Collections.emptyList();
            }

            return Collections.singletonList(locNode);
        }

        /**
         * {@inheritDoc}
         */
        public GridNode getLocalNode() {
            return locNode;
        }

        /**
         * {@inheritDoc}
         */
        public GridNode getNode(UUID nodeId) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public Collection<GridNode> getRemoteNodes() {
            return Collections.emptyList();
        }

        /**
         * {@inheritDoc}
         */
        public Collection<GridNode> getTopology(GridTaskSession taskSes, Collection<GridNode> grid) {
            return Collections.emptyList();
        }

        /**
         * {@inheritDoc}
         */
        public boolean pingNode(UUID nodeId) {
            return locNode == null ? false : nodeId.equals(locNode.getId()) == true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean removeDiscoveryListener(GridDiscoveryListener listener) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean removeLocalEventListener(GridLocalEventListener listener) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean removeMessageListener(GridMessageListener listener, String topic) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void sendMessage(GridNode node, Serializable msg, String topic) {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public void sendMessage(Collection<GridNode> nodes, Serializable msg, String topic) {
            // No-op.
        }
    }
}
