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

package org.gridgain.grid.spi.communication.mule;

import java.io.*;
import java.util.*;
import java.net.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.mule.*;
import org.mule.config.*;
import org.mule.config.builders.*;
import org.mule.extras.client.*;
import org.mule.umo.*;
import org.mule.umo.endpoint.*;
import org.mule.umo.manager.*;
import org.mule.umo.routing.*;

/**
 * Mule implementation of {@link GridCommunicationSpi} SPI. It uses
 * Mule ESB implementation to communicate with GridGain nodes.
 * <p>
 * Mule instance may be started before SPI or instantiated during SPI start.
 * SPI uses UMO component which must be declared in Mule configuration file
 * (see {@link #COMPONENT_NAME}). UMO component should have one input endpoint.
 * The name of that input endpoint should be declared in
 * component's descriptor properties (see {@link #ENDPOINT_NAME}).
 * <p>
 * Here is an example of Mule configuration file with
 * {@link GridMuleCommunicationComponent} used by this SPI:
 * <pre name="code" class="xml">
 * &lt;mule-configuration version="1.0"&gt;
 *     &lt;mule-environment-properties embedded="true"/&gt;
 *
 *     &lt;connector name="tcpConnector" className="org.mule.providers.tcp.TcpConnector"&gt;
 *         &lt;properties>
 *             &lt;property name="tcpProtocolClassName" value="org.mule.providers.tcp.protocols.LengthProtocol"/&gt;
 *         &lt;/properties&gt;
 *     &lt;/connector&gt;
 *
 *     &lt;model name="gridgain"&gt;
 *         &lt;mule-descriptor name="GridCommunicationUMO"
 *             implementation="org.gridgain.grid.spi.communication.mule.GridMuleCommunicationComponent"
 *             singleton="true"&gt;
 *             &lt;inbound-router&gt;
 *                 &lt;endpoint name="comm.id" address="tcp://localhost:11001"/&gt;
 *             &lt;/inbound-router&gt;
 *
 *             &lt;properties>
 *                 &lt;property name="communication" value="comm.id"/&gt;
 *             &lt;/properties&gt;
 *         &lt;/mule-descriptor&gt;
 *     &lt;/model&gt;
 * &lt;/mule-configuration&gt;</pre>
 * <p>
 * This SPI has no mandatory parameters.
 * <p>
 * This SPI has the following optional parameters:
 * <ul>
 * <li>
 *      Mule configuration file used when Mule instance isn't already started
 *      (see {@link #setConfigurationFile(String)}, {@link #setConfigurationUrl(URL)}).
 * </li>
 * <li>Component name declared in Mule configuration (see {@link #setComponentName(String)}}).</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridMuleCommunicationSpi needs to be explicitely configured to override default
 * TCP communication SPI.
 * <pre name="code" class="java">
 * GridMuleCommunicationSpi commSpi = new GridMuleCommunicationSpi();
 *
 * commSpi.setConfigurationFile("/my/path/to/mule/config/mule.xml");
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default communication SPI.
 * cfg.setCommunicationSpi(commSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridMuleCommunicationSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="communicationSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.communication.mule.GridMuleCommunicationSpi"&gt;
 *                 &lt;property name="configurationFile" value="/my/config/path/mule.xml"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;</pre>
 * <p>
 * <b>Note</b>: Mule is not shipped with GridGain. If you don't have Mule, you need to
 * download it separately. See <a target=_blank href="http://www.mulesource.com">http://www.mulesource.com</a> for
 * more information. Once installed, Mule should be available on the classpath for
 * GridGain. If you use <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> script to start
 * a grid node you can simply add Mule JARs to <tt>[GRIDGAIN_HOME]/bin/setenv.{sh|bat}</tt>
 * script that is used to set up class path for the main scripts.
 * <p>
 * <b>Note</b>: When using Mule SPI (communication or discovery) you cannot start
 * multiple GridGain instances in the same VM due to Mule limitation. GridGain runtime
 * will detect this situation and prevent GridGain from starting in such case.
 * See {@link GridSpiMultipleInstancesSupport} for details.
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridCommunicationSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(false)
public class GridMuleCommunicationSpi extends GridSpiAdapter implements GridCommunicationSpi,
    GridMuleCommunicationSpiMBean {
    /**
     * Default Mule 1.x configuration path relative to GridGain installation home folder
     * (value is <tt>config/mule1/mule.xml</tt>).
     */
    public static final String DFLT_CONFIG_FILE = "config/mule1/mule.xml";

    /**
     * Name of address attribute added to local node attributes at startup
     * (value is <tt>grid.comm.mule.address</tt>).
     */
    public static final String ATTR_ADDR = "grid.comm.mule.address";

    /**
     * Name of component declared in Mule configuration
     * (value is <tt>GridCommunicationUMO</tt>).
     */
    public static final String COMPONENT_NAME = "GridCommunicationUMO";

    /**
     * Name of property where input endpoint name declared in Mule configuration
     * (value is <tt>communication</tt>).
     */
    public static final String ENDPOINT_NAME = "communication";

    /** Listener that will be informed about incoming messages. */
    private GridMessageListener listener = null;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** IoC configuration parameter to specify the name of the Mule configuration file. */
    private String cfgFile = DFLT_CONFIG_FILE;

    /** IoC configuration parameter to specify the URL of the Mule configuration file. */
    private URL cfgUrl = null;

    /** Name of the component registered in Mule. */
    private String umoName = COMPONENT_NAME;

    /** Mule component. */
    private UMOComponent umo = null;

    /** Communication Mule component. */
    private GridMuleCommunicationComponent muleComponent = null;

    /** Component inbound endpoint uri. */
    private String endpointUri = null;

    /** Mule manager. */
    private UMOManager muleMgr = null;

    /** Mule client. */
    private MuleClient muleClient = null;

    /** Flag indicates whether Mule started before SPI or not.*/
    private boolean stopMule = false;

    /**
     * {@inheritDoc}
     */
    public String getConfigurationFile() {
        return cfgFile;
    }

    /**
     * Sets either absolute or relative to GridGain installation home folder path to Mule XML
     * configuration file. This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_CONFIG_FILE}.
     *
     * @param cfgFile Path to Mule configuration file.
     */
    @GridSpiConfiguration(optional = true)
    public void setConfigurationFile(String cfgFile) {
        this.cfgFile = cfgFile;
    }

    /**
     * Sets URL to Mule XML configuration file.
     * This configuration parameter is optional.
     * <p>
     * If not provided, default value is {@link #DFLT_CONFIG_FILE}.
     *
     * @param cfgUrl URL to Mule configuration file.
     */
    @GridSpiConfiguration(optional = true)
    public void setConfigurationUrl(URL cfgUrl) {
        this.cfgUrl = cfgUrl;
    }

    /**
     * {@inheritDoc}
     */
    public String getComponentName() {
        return umoName;
    }

    /**
     * Sets name for component registered in Mule.
     * SPI use that name for getting component from Mule instance.
     *
     * @param umoName Name for component registered in Mule.
     */
    @GridSpiConfiguration(optional = true)
    public void setComponentName(String umoName) {
        this.umoName = umoName;
    }

    /**
     * {@inheritDoc}
     */
    public String getEndpointUri() {
        return endpointUri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        if (MuleManager.isInstanciated() == true) {
            if (log.isDebugEnabled() == true) {
                log.debug("There is already a Mule manager available, no need to create a new one.");
            }

            assertParameter(umoName != null, "compName != null");

            muleMgr = MuleManager.getInstance();
        }
        else {
            assertParameter(umoName != null, "compName != null");

            File cfgPath = null;

            if (cfgUrl != null) {
                try {
                    cfgPath = GridUrlHelper.downloadUrl(cfgUrl, File.createTempFile("mule", "xml"));
                }
                catch (IOException e) {
                    throw (GridSpiException)new GridSpiException("Failed to download configuration file: " + cfgUrl, e).setData(297, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
                }
            }
            else {
                assertParameter(cfgFile != null, "cfgFile != null");

                cfgPath = GridUtils.resolveGridGainPath(cfgFile);
            }

            if (cfgPath == null || cfgPath.isDirectory() == true) {
                throw (GridSpiException)new GridSpiException("Invalid Mule configuration file path: " + cfgPath).setData(307, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
            }

            if (cfgPath.canRead() == false) {
                throw (GridSpiException)new GridSpiException("Mule configuration file does not have read permission: " + cfgPath).setData(311, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
            }

            try {
                MuleXmlConfigurationBuilder cfgBuilder = new MuleXmlConfigurationBuilder();

                muleMgr = cfgBuilder.configure(cfgPath.getAbsolutePath());
            }
            catch (ConfigurationException e) {
                throw (GridSpiException)new GridSpiException("Failed to configure or start Mule manager.", e).setData(320, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
            }

            stopMule = true;
        }

        // Initialize component and all related data.
        initializeComponent();

        assert endpointUri != null : "ASSERTION [line=329, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";

        return GridUtils.makeMap(createSpiAttributeName(ATTR_ADDR), endpointUri);
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("umoName", umoName));

            if (cfgUrl != null) {
                log.info(configInfo("cfgUrl", cfgUrl));
            }
            else {
                log.info(configInfo("cfgFile", cfgFile));
            }
        }

        // Add SPI listener in component.
        try {
            Object instance = umo.getInstance();

            assert instance instanceof GridMuleCommunicationComponent == true : "ASSERTION [line=357, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";

            ((GridMuleCommunicationComponent)instance).setListener(new GridMuleCommunicationComponentListener() {
                /**
                 * {@inheritDoc}
                 */
                public void onMessage(byte[] msg) {
                    GridByteArrayList buf = new GridByteArrayList(msg, msg.length);

                    try {
                        GridMuleCommunicationMessage muleMsg = (GridMuleCommunicationMessage)
                            GridMarshalHelper.unmarshal(marshaller, buf, getClass().getClassLoader());

                        if (log.isDebugEnabled() == true) {
                            log.debug("Received Mule communication message: " + muleMsg);
                        }

                        notifyMessage(muleMsg.getNodeId(), muleMsg.getMessage());
                    }
                    catch (GridException e) {
                        log.error("Failed to deserialize Mule communication message: " +
                            GridUtils.byteArray2HexString(msg), e);
                    }
                }
            });
        }
        catch (UMOException e) {
            throw (GridSpiException)new GridSpiException("Failed to get component instance to configure SPI listener in component.", e).setData(384, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        try {
            muleClient = new MuleClient();
        }
        catch (UMOException e) {
            throw (GridSpiException)new GridSpiException("Filed to start Mule client.", e).setData(391, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        registerMBean(gridName, this, GridMuleCommunicationSpiMBean.class);

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        if (muleComponent != null) {
            // Clear SPI listener in component.
            muleComponent.setListener(null);
        }

        // Check Mule instance to avoid restarting.
        if (MuleManager.isInstanciated() == true && stopMule == true) {
            muleMgr.dispose();

            assert MuleManager.isInstanciated() == false : "ASSERTION [line=417, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";
        }

        // Clean resources.
        muleMgr = null;
        muleClient = null;
        muleComponent = null;
        umo = null;
        endpointUri = null;
        stopMule = false;

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException {
        assert muleClient != null : "ASSERTION [line=438, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";
        assert nodeId != null : "ASSERTION [line=439, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";

        String addr = (String)destNode.getAttribute(createSpiAttributeName(ATTR_ADDR));

        if (addr == null || addr.length() == 0) {
            throw (GridSpiException)new GridSpiException("Failed to get Mule endpoint address from: " + destNode).setData(444, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        if (nodeId.equals(destNode.getId()) == true) {
            if (log.isDebugEnabled() == true) {
                log.debug("Sending message to local node (no serialization will take place): " + msg);
            }

            notifyMessage(nodeId, msg);
        }
        else {
            try {
                GridByteArrayList buf =
                    GridMarshalHelper.marshal(marshaller, new GridMuleCommunicationMessage(nodeId, msg));

                muleClient.dispatch(addr, buf.getArray(), null);
            }
            catch (UMOException e) {
                throw (GridSpiException)new GridSpiException("Failed to sent message [destNode=" + destNode + ", msg=" + msg + ']', e).setData(462, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
            }
            catch (GridException e) {
                throw (GridSpiException)new GridSpiException("Failed to marshal [destNode=" + destNode + ", msg=" + msg + ']', e).setData(465, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
            }
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Sent message [destNode=" + destNode + ", msg=" + msg + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException {
        assert destNodes != null : "ASSERTION [line=478, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";

        for (GridNode node : destNodes) {
            sendMessage(node, msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridMessageListener listener) {
        this.listener = listener;
    }

    /**
     * Gets component from Mule and initialize all necessary data.
     *
     * @throws GridSpiException Throw in case of any errors.
     */
    // Warning suppression is due to Spring...
    @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
    private void initializeComponent() throws GridSpiException {
        assert muleMgr != null : "ASSERTION [line=500, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";

        umo = muleMgr.getModel().getComponent(umoName);

        if (umo == null) {
            throw (GridSpiException)new GridSpiException("Failed to get Mule UMO component with name: " + umoName).setData(505, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        try {
            Object instance = umo.getInstance();

            if (instance instanceof GridMuleCommunicationComponent == false) {
                throw (GridSpiException)new GridSpiException("Failed to get component instance (invalid type): " +
                    (instance == null ? null : instance.getClass().getName())).setData(512, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
            }

            muleComponent = (GridMuleCommunicationComponent)instance;
        }
        catch (UMOException e) {
            throw (GridSpiException)new GridSpiException("Failed to get component instance to check component type.", e).setData(519, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        UMOInboundMessageRouter inRouter = umo.getDescriptor().getInboundRouter();

        if (inRouter == null) {
            throw (GridSpiException)new GridSpiException("Failed to get inbound router from: " + umo.getDescriptor()).setData(525, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        List<UMOEndpoint> endpoints = inRouter.getEndpoints();

        if (endpoints == null || endpoints.size() == 0) {
            throw (GridSpiException)new GridSpiException("Failed to get inbound endpoints from: " + inRouter).setData(531, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        Map props = umo.getDescriptor().getProperties();

        if (props == null || props.isEmpty() == true || props.get(ENDPOINT_NAME) == null) {
            throw (GridSpiException)new GridSpiException("Failed to get communication inbound endpoint identifier.").setData(537, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        // Get inbound endpoint by name.
        UMOEndpoint inEndpoint = inRouter.getEndpoint((String)props.get(ENDPOINT_NAME));

        if (inEndpoint == null) {
            throw (GridSpiException)new GridSpiException("Failed to get communication inbound endpoint by name: " +
                props.get(ENDPOINT_NAME)).setData(544, "src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java");
        }

        endpointUri = inEndpoint.getEndpointURI().toString();
    }

    /**
     * Notifies listener.
     *
     * @param nodeId Source node Id.
     * @param msg Received message.
     */
    private void notifyMessage(UUID nodeId, Serializable msg) {
        assert nodeId != null : "ASSERTION [line=558, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";
        assert msg != null : "ASSERTION [line=559, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationSpi.java]";

        GridMessageListener localCopy = listener;

        if (localCopy != null) {
            localCopy.onMessage(nodeId, msg);
        }
        else {
            log.warning("Received communication message without any registered listeners (will ignore) [senderNodeId=" +
                nodeId + ']');
        }
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
        return GridToStringBuilder.toString(GridMuleCommunicationSpi.class, this);
    }
}
