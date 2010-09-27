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

package org.gridgain.grid.spi.deployment.uri;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.RejectedExecutionException;

import org.gridgain.grid.GridTask;
import org.gridgain.grid.GridTaskAdapter;
import org.gridgain.grid.GridTaskName;
import org.gridgain.grid.kernal.GridNodeAttributes;
import org.gridgain.grid.logger.GridLogger;
import org.gridgain.grid.marshaller.GridMarshaller;
import org.gridgain.grid.resources.GridLoggerResource;
import org.gridgain.grid.resources.GridMarshallerResource;
import org.gridgain.grid.spi.GridSpiAdapter;
import org.gridgain.grid.spi.GridSpiConfiguration;
import org.gridgain.grid.spi.GridSpiException;
import org.gridgain.grid.spi.GridSpiInfo;
import org.gridgain.grid.spi.GridSpiMultipleInstancesSupport;
import org.gridgain.grid.spi.deployment.GridDeploymentListener;
import org.gridgain.grid.spi.deployment.GridDeploymentResource;
import org.gridgain.grid.spi.deployment.GridDeploymentResourceAdapter;
import org.gridgain.grid.spi.deployment.GridDeploymentSpi;
import org.gridgain.grid.spi.deployment.uri.scanners.GridUriDeploymentScanner;
import org.gridgain.grid.spi.deployment.uri.scanners.GridUriDeploymentScannerListener;
import org.gridgain.grid.spi.deployment.uri.scanners.file.GridUriDeploymentFileScanner;
import org.gridgain.grid.spi.deployment.uri.scanners.http.GridUriDeploymentHttpScanner;
import org.gridgain.grid.spi.deployment.uri.util.GridUriDeploymentUtils;
import org.gridgain.grid.util.GridArgumentCheck;
import org.gridgain.grid.util.GridUtils;
import org.gridgain.grid.util.tostring.GridToStringBuilder;

/**
 * Implementation of {@link GridDeploymentSpi} which can deploy tasks from
 * different sources like file system folders, FTP, email and HTTP.
 * There are different ways to deploy tasks in grid and every deploy method
 * depends on selected source protocol. This SPI is configured to work
 * with a list of URI's. Every URI contains all data about protocol/transport
 * plus configuration parameters like credentials, scan frequency, and others.
 * <p>
 * When SPI establishes a connection with a URI, it downloads deployable units
 * to the temporary directory in order to prevent it from any changes while
 * scanning. Use method {@link #setTemporaryDirectoryPath(String) setTemporaryDirectoryPath(String)})
 * to set custom temporary folder for downloaded deployment units.
 * <p>
 * SPI tracks all changes of every given URI. This means that if any file is
 * changed or deleted, SPI will re-deploy or delete corresponding tasks.
 * Note that the very first call to {@link #findResource(String)} findClassLoader(String)}
 * is blocked until SPI finishes scanning all URI's at least once.
 * <p>
 * There are several deployable unit types supported:
 * <ul>
 * <li>GAR file.</li>
 * <li>Local disk folder with structure of unpacked GAR file.</li>
 * <li>Local disk folder containing only compiled Java classes.</li>
 * </ul>
 * <h1 class="header">GAR file</h1>
 * GAR file is a deployable unit. GAR file is based on <a href="http://www.gzip.org/zlib/">ZLIB</a>
 * compression format like simple JAR file and its structure is similar to WAR archive.
 * GAR file has <tt>'.gar'</tt> extension.
 * <p>
 * GAR file structure (file or directory ending with <tt>'.gar'</tt>):
 *   <pre class="snippet">
 *      META-INF/
 *              |
 *               - gridgain.xml
 *               - ...
 *      lib/
 *         |
 *          -some-lib.jar
 *          - ...
 *      xyz.class
 *      ...</pre>
 * <ul>
 * <li>
 * <tt>META-INF/</tt> entry may contain <tt>gridgain.xml</tt> file which is a
 * task descriptor file. The purpose of task descriptor XML file is to specify
 * all tasks to be deployed. This file is a regular
 * <a href="http://www.springframework.org/documentation">Spring</a> XML
 * definition file.  <tt>META-INF/</tt> entry may also contain any other file
 * specified by JAR format.
 * </li>
 * <li>
 * <tt>lib/</tt> entry contains all library dependencies.
 * </li>
 * <li>Compiled Java classes must be placed in the root of a GAR file.</li>
 * </ul>
 * GAR file may be deployed without descriptor file. If there is no descriptor file, SPI
 * will scan all classes in archive and instantiate those that implement
 * {@link GridTask} interface. In that case, all grid task classes must have a
 * public no-argument constructor. Use {@link GridTaskAdapter} adapter for
 * convenience when creating grid tasks.
 * <p>
 * By default, all downloaded GAR files that have digital signature in <tt>META-INF</tt>
 * folder will be verified and and deployed only if signature is valid.
 * <p>
 * <h1 class="header">URI</h1>
 * This SPI uses a hierarchical URI definition. For more information about standard URI
 * syntax refer to {@link URI java.net.URI} documentation.
 * <blockquote class="snippet">
 * [<i>scheme</i><tt><b>:</b></tt>][<tt><b>//</b></tt><i>authority</i>][<i>path</i>][<tt><b>?</b></tt><i>query</i>][<tt><b>#</b></tt><i>fragment</i>]
 * </blockquote>
 * <p>
 * Every URI defines its own deployment repository which will be scanned for any changes.
 * URI itself has all information about protocol, connectivity, scan intervals and other
 * parameters.
 * <p>
 * URI's may contain special characters, like spaces. If <tt>encodeUri</tt>
 * flag is set to <tt>true</tt> (see {@link #setEncodeUri(boolean)}), then
 * URI 'path' field will be automatically encoded. By default this flag is
 * set to <tt>true</tt>.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <tt>GridUriDeploymentSpi</tt> has the following optional configuration
 * parameters (there are no mandatory parameters):
 * <ul>
 * <li>
 * Temporary directory path where scanned GAR files and directories are
 * copied to (see {@link #setTemporaryDirectoryPath(String) setTemporaryDirectoryPath(String)}).
 * </li>
 * <li>
 * List of URIs to scan (see {@link #setUriList(List)}). If not
 * specified, then URI specified by {@link #DFLT_DEPLOY_DIR DFLT_DEPLOY_DIR} is used.
 * </li>
 * <li>
 * Flag to control encoding of the <tt>'path'</tt> portion of URI
 * (see {@link #setEncodeUri(boolean) setEncodeUri(boolean)}).
 * </li>
 * </ul>
 * <h1 class="header">Protocols</h1>
 * Following protocols are supported in SPI:
 * <ul>
 * <li><a href="#file">file://</a> - File protocol</li>
 * <li><a href="#classes">classes://</a> - Custom File protocol.</li>
 * <li><a href="#ftp">ftp://</a> - File transfer protocol</li>
 * <li><a href="#mail">pop3://</a> - POP3 mail protocol</li>
 * <li><a href="#mail">pop3s://</a> - Secured POP3 mail protocol</li>
 * <li><a href="#mail">imap://</a> - IMAP mail protocol</li>
 * <li><a href="#mail">imaps://</a> - Secured IMAP mail protocol</li>
 * <li><a href="#http">http://</a> - HTTP protocol</li>
 * <li><a href="#http">https://</a> - Secure HTTP protocol</li>
 * </ul>
 * In addition to SPI configuration parameters, all necessary configuration
 * parameters for selected URI should be defined in URI. Different protocols
 * have different configuration parameters described below. Parameters are
 * separated by '<tt>;</tt>' character.
 * <p>
 * <a name="file"></a>
 * <h1 class="header">File</h1>
 * For this protocol SPI will scan folder specified by URI on file system and
 * download any GAR files or directories that end with .gar from source
 * directory defined in URI. For file system URI must have scheme equal to <tt>file</tt>.
 * <p>
 * Following parameters are supported for FILE protocol:
 * <table class="doctable">
 *  <tr>
 *      <th>Parameter</th>
 *      <th>Description</th>
 *      <th>Optional</th>
 *      <th>Default</th>
 *  </tr>
 *  <tr>
 *      <td>freq</td>
 *      <td>File directory scan frequency in milliseconds.</td>
 *      <td>Yes</td>
 *      <td><tt>5000</tt> ms specified in {@link #DFLT_DISK_SCAN_FREQUENCY DFLT_DISK_SCAN_FREQUENCY}.</td>
 *  </tr>
 * </table>
 * <h2 class="header">File URI Example</h2>
 * The following example will scan <tt>'c:/Program files/gridgain/deployment'</tt>
 * folder on local box every <tt>'5000'</tt> milliseconds. Note that since path
 * has spaces, {@link #setEncodeUri(boolean) setEncodeUri(boolean)} parameter must
 * be set to <tt>true</tt> (which is default behavior).
 * <blockquote class="snippet">
 * <tt>file://freq=5000@localhost/c:/Program files/gridgain/deployment</tt>
 * </blockquote>
 * <a name="classes"></a>
 * <h1 class="header">Classes</h1>
 * For this protocol SPI will scan folder specified by URI on file system
 * looking for compiled classes that implement {@link GridTask} interface.
 * This protocol comes very handy during development, as it allows developer
 * to specify IDE compilation output folder as URI and all task classes
 * in that folder will be deployed automatically.
 * <p>
 * Following parameters are supported for CLASSES protocol:
 * <table class="doctable">
 *  <tr>
 *      <th>Parameter</th>
 *      <th>Description</th>
 *      <th>Optional</th>
 *      <th>Default</th>
 *  </tr>
 *  <tr>
 *      <td>freq</td>
 *      <td>File directory scan frequency in milliseconds.</td>
 *      <td>Yes</td>
 *      <td><tt>5000</tt> ms specified in {@link #DFLT_DISK_SCAN_FREQUENCY DFLT_DISK_SCAN_FREQUENCY}.</td>
 *  </tr>
 * </table>
 * <h2 class="header">Classes URI Example</h2>
 * The following example will scan <tt>'c:/Program files/gridgain/deployment'</tt>
 * folder on local box every <tt>'5000'</tt> milliseconds. Note that since path
 * has spaces, {@link #setEncodeUri(boolean) setEncodeUri(boolean)} parameter must
 * be set to <tt>true</tt> (which is default behavior).
 * <blockquote class="snippet">
 * <tt>classes://freq=5000@localhost/c:/Program files/gridgain/deployment</tt>
 * </blockquote>
 * <a name="ftp"></a>
 * <h1 class="header">FTP</h1>
 * For FTP protocol SPI will scan and download only GAR files from source
 * directory defined in URI. SPI doesn't scan FTP folders recursively.
 * The following parameters are supported for FTP protocol:
 * <table class="doctable">
 *  <tr>
 *      <th>Parameter</th>
 *      <th>Description</th>
 *      <th>Optional</th>
 *      <th>Default</th>
 *  </tr>
 *  <tr>
 *      <td>freq</td>
 *      <td>FTP location scan frequency in milliseconds.</td>
 *      <td>Yes</td>
 *      <td><tt>300000</tt> ms specified in {@link #DFLT_FTP_SCAN_FREQUENCY DFLT_FTP_SCAN_FREQUENCY}.</td>
 *  </tr>
 *  <tr>
 *      <td>username:password</td>
 *      <td>
 *          FTP username and password specified in standard URI server-based
 *          authority format.
 *      </td>
 *      <td>No</td>
 *      <td>---</td>
 *  </tr>
 * </table>
 * <h2 class="header">FTP URI Example</h2>
 * Here is an example of an FTP URI that connects identified as
 * <tt>username:password</tt> to <tt>'localhost'</tt> on port <tt>'21'</tt>,
 * with initial path set to <tt>'gridgain/deployment'</tt>
 * <blockquote class="snippet">
 * ftp://username:password;freq=10000@localhost:21/gridgain/deployment
 * </blockquote>
 * <p>
 * <a name="mail"></a>
 * <h1 class="header">Mail</h1>
 * For Mail protocols this SPI scans mail inboxes for new mail messages looking
 * for GAR file attachments. Once a mail message with GAR file is found, it
 * will be deployed. Mail protocols works with following schemes: <tt>pop3</tt>,
 * <tt>pop3s</tt>, <tt>imap</tt>, and <tt>imaps</tt>.
 * <p>
 * The following parameters are supported for Mail protocols:
 * <table class="doctable">
 *  <tr>
 *      <th>Parameter</th>
 *      <th>Description</th>
 *      <th>Optional</th>
 *      <th>Default</th>
 *  </tr>
 *  <tr>
 *      <td>freq</td>
 *      <td>Main inbox scan frequency in milliseconds.</td>
 *      <td>Yes</td>
 *      <td><tt>300000</tt> ms specified in {@link #DFLT_MAIL_SCAN_FREQUENCY DFLT_MAIL_SCAN_FREQUENCY}.</td>
 *  </tr>
 *  <tr>
 *      <td>username:password</td>
 *      <td>
 *          Mail username and password specified in standard URI server-based
 *          authority format.
 *      </td>
 *      <td>No</td>
 *      <td>---</td>
 *  </tr>
 *  <tr>
 *      <td>auth</td>
 *      <td>
 *          Connection type. Can be one of the following:
 *          <ul>
 *          <li>none</li>
 *          <li>ssl</li>
 *          <li>starttls</li>
 *          </ul>
 *      </td>
 *      <td>Yes</td>
 *      <td><tt>none</tt></td>
 *  </tr>
 *  <tr>
 *      <td>subj</td>
 *      <td>
 *          Subject filter for mail messages used by SPI. All messages with
 *          different subjects will be ignored.
 *      </td>
 *      <td>Yes</td>
 *      <td>
 *          <tt>'grid.email.deploy.msg'</tt> specified in
 *          {@link #DFLT_MAIL_SUBJECT DFLT_MAIL_SUBJECT}
 *      </td>
 *  </tr>
 * </table>
 * <h2 class="header">Mail URI Example</h2>
 * The following example demonstrates Mail URI that will connect user
 * identified as <tt>username:password</tt> with authorization set to
 * <tt>'none'</tt> to host <tt>'pop.gmail.com'</tt> on port <tt>'110'</tt>
 * scanning inbox every <tt>'120000'</tt> milliseconds (2 minutes).
 * <blockquote class="snippet">
 * <tt>pop3://username:password;auth=ssl;freq=120000@pop.gmail.com:995</tt>
 * </blockquote>
 * <p>
 * <a name="http"></a>
 * <h1 class="header">HTTP</h1>
 * For HTTP protocols this SPI scans and downloads GAR files from source
 * directory defined in URI. SPI does not scan HTTP folders recursively. Only
 * HTTP links that end with <tt>'.gar'</tt> extention will be downloaded.
 * HTTP protocol works with scheme <tt>http</tt> and <tt>https</tt>.
 * <p>
 * The following parameters are supported for HTTP protocols:
 * <table class="doctable">
 *  <tr>
 *      <th>Parameter</th>
 *      <th>Description</th>
 *      <th>Optional</th>
 *      <th>Default</th>
 *  </tr>
 *  <tr>
 *      <td>freq</td>
 *      <td>HTTP directory scan frequency in milliseconds.</td>
 *      <td>Yes</td>
 *      <td><tt>300000</tt> ms specified in {@link #DFLT_HTTP_SCAN_FREQUENCY DFLT_HTTP_SCAN_FREQUENCY}.</td>
 *  </tr>
 *  <tr>
 *      <td>username:password</td>
 *      <td>
 *          Optional HTTP directory username and password specified in standard
 *          URI server-based authority format.
 *      </td>
 *      <td>Yes</td>
 *      <td>---</td>
 *  </tr>
 * </table>
 * <h2 class="header">HTTP URI Example</h2>
 * The following example will scan <tt>'gridgain/deployment'</tt> folder with
 * on site <tt>'www.mysite.com'</tt> using authentication
 * <tt>'username:password'</tt> every <tt>'10000'</tt> milliseconds.
 * <blockquote class="snippet">
 * <tt>http://username:password;freq=10000@www.mysite.com:110/gridgain/deployment</tt>
 * </blockquote>
 * <h2 class="header">Java Example</h2>
 * GridUriDeploymentSpi needs to be explicitely configured to override default local deployment SPI.
 * <pre name="code" class="java">
 * GridUriDeploymentSpi deploySpi = new GridUriDeploymentSpi();
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * List&lt;String&gt; uris = new ArrayList&lt;String&gt;(5);
 *
 * uris.add("http://www.site.com/tasks");
 * uris.add("ftp://ftpuser:password;freq=10000@localhost:21/gg-test/deployment");
 * uris.add("file://freq=20000@localhost/c:/Program files/gg-deployment");
 * uris.add("pop3://test%20user:test%20password;subj=grid.deploy.subj;auth=none@pop.mail.ru:110");
 * uris.add("classes:///c:/Java_Projects/myproject/out");
 *
 * // Set URIs.
 * deploySpi.setUriList(uris);
 *
 * // Override temporary directory path.
 * deploySpi.setTemporaryDirectoryPath("c:/tmp/grid");
 *
 * //  Override default deployment SPI.
 * cfg.setDeploymentSpi(deploySpi);
 *
 * //  Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <p>
 * <h2 class="header">Spring Example</h2>
 * GridUriDeploymentSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="deploymentSpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.deployment.uri.GridUriDeploymentSpi"&gt;
 *                 &lt;property name="temporaryDirectoryPath" value="c:/tmp/grid"/&gt;
 *                 &lt;property name="uriList"&gt;
 *                     &lt;list&gt;
 *                         &lt;value&gt;http://www.site.com/tasks&lt;/value&gt;
 *                         &lt;value&gt;ftp://ftpuser:password;freq=10000@localhost:21/gg-test/deployment&lt;/value&gt;
 *                         &lt;value&gt;file://freq=20000@localhost/c:/Program files/gg-deployment&lt;/value&gt;
 *                         &lt;value&gt;pop3://test%20user:test%20password;subj=grid.deploy.subj;auth=none@pop.mail.ru:110&lt;/value&gt;
 *                         &lt;value&gt;classes:///c:/Java_Projects/myproject/out&lt;/value&gt;
 *                     &lt;/list&gt;
 *                 &lt;/property&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
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
public class GridUriDeploymentSpi extends GridSpiAdapter implements GridDeploymentSpi, GridUriDeploymentSpiMBean {
    /** Default deployment directory where SPI will pick up GAR files (value is <tt>work/deployment/file</tt>). */
    public static final String DFLT_DEPLOY_DIR = "work/deployment/file";

    /** Default scan frequency for <tt>file://</tt> and <tt>classes://</tt> protocols (value is <tt>5000</tt>). */
    public static final int DFLT_DISK_SCAN_FREQUENCY = 5000;

    /** Default scan frequency for <tt>ftp://</tt> protocol (value is <tt>300000</tt>). */
    public static final int DFLT_FTP_SCAN_FREQUENCY = 300000;

    /** Default scan frequency for <tt>http://</tt> protocol (value is <tt>300000</tt>). */
    public static final int DFLT_HTTP_SCAN_FREQUENCY = 300000;

    /**
     * Default scan frequency for <tt>pop3://</tt>, <tt>pop3s://</tt>, <tt>imap://</tt>,
     * <tt>imaps://</tt> protocols (value is <tt>300000</tt> which is 5 minutes).
     */
    public static final int DFLT_MAIL_SCAN_FREQUENCY = 300000;

    /** Default Mail subject. */
    public static final String DFLT_MAIL_SUBJECT = "grid.email.deploy.msg";

    /** Default task description file path and name (value is <tt>META-INF/gridgain.xml</tt>). */
    public static final String XML_DESCRIPTOR_PATH = "META-INF/gridgain.xml";

    /**
     * Default temporary directory name relative to file path
     * {@link #setTemporaryDirectoryPath(String)}} (value is <tt>gg.deploy</tt>).
     */
    public static final String DEPLOY_TMP_ROOT_NAME = "gg.deploy";

    /** Temporary directory name. */
    private String tmpDirPath = null;

    /** Sub-folder of 'tmpDirPath'. */
    private String deployTmpDirPath = null;

    /** List of URIs to be scanned. */
    private List<String> uriList = new ArrayList<String>();

    /** List of encoded URIs. */
    private List<URI> uriEncodedList = new ArrayList<URI>();

    /** */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private final LinkedList<GridUriDeploymentUnitDescriptor> unitLoaders =
        new LinkedList<GridUriDeploymentUnitDescriptor>();

    /** */
    private final LastTimeUnitDescriptorComparator unitComparator = new LastTimeUnitDescriptorComparator();

    /** List of scanners. Every URI has it's own scanner. */
    private final List<GridUriDeploymentScanner> scanners = new ArrayList<GridUriDeploymentScanner>();

    /** Whether URIs should be encoded or not. */
    private boolean encodeUri = true;

    /** Whether first scan cycle is completed or not. */
    private int firstScanCntr = 0;

    /** Deployment listener which processes all notifications from scanners. */
    private volatile GridDeploymentListener lsnr = null;

    /** */
    private final Object mux = new Object();

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /**
     * Sets absolute path to temporary directory which will be used by
     * deployment SPI to keep all deployed classes in.
     * <p>
     * If not provided, default value is <tt>java.io.tmpdir</tt> system property value.
     *
     * @param tmpDirPath Temporary directory path.
     */
    @GridSpiConfiguration(optional = true)
    public void setTemporaryDirectoryPath(String tmpDirPath) {
        this.tmpDirPath = tmpDirPath;
    }

    /**
     * Sets list of URI which point to GAR file and which should be
     * scanned by SPI for the new tasks.
     * <p>
     * If not provided, default value is list with
     * <tt>file://${GRIDGAIN_HOME}/work/deployment/file</tt> element.
     *
     * @param uriList GAR file URIs.
     */
    @GridSpiConfiguration(optional = true)
    public void setUriList(List<String> uriList) {
        this.uriList = uriList;
    }

    /**
     * Indicates that URI must be encoded before usage. Encoding means replacing
     * all occurrences of space with '%20', percent sign with '%25'
     * and semicolon with '%3B'.
     * <p>
     * If not provided, default value is <tt>true</tt>.
     *
     * @param encodeUri <tt>true</tt> if every URI should be encoded and
     *      <tt>false</tt> otherwise.
     */
    @GridSpiConfiguration(optional = true)
    public void setEncodeUri(boolean encodeUri) {
        this.encodeUri = encodeUri;
    }

    /**
     * {@inheritDoc}
     */
    public String getTemporaryDirectoryPath() {
        return tmpDirPath;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getUriList() {
        return Collections.unmodifiableList(uriList);
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
    public void spiStop() throws GridSpiException {
        for (GridUriDeploymentScanner scanner : scanners) {
            scanner.cancel();
        }

        for (GridUriDeploymentScanner scanner : scanners) {
            scanner.join();
        }

        // Clear inner collections.
        uriEncodedList.clear();
        scanners.clear();

        List<ClassLoader> tmpClsLdrs = null;

        // Release all class loaders.
        synchronized (mux) {
            tmpClsLdrs = new ArrayList<ClassLoader>(unitLoaders.size());

            for (GridUriDeploymentUnitDescriptor descr : unitLoaders) {
                tmpClsLdrs.add(descr.getClassLoader());
            }
        }

        for (ClassLoader ldr : tmpClsLdrs) {
            onUnitReleased(ldr);
        }

        // Delete temp directory.
        if (deployTmpDirPath != null) {
            GridUtils.delete(new File(deployTmpDirPath));
        }

        unregisterMBean();

        // Acl ok stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(uriList != null, "uriList != null");

        initializeUriList();

        if (uriEncodedList.size() == 0) {
            addDefaultUri();
        }

        initializeTemporaryDirectoryPath();

        registerMBean(gridName, this, GridUriDeploymentSpiMBean.class);

        FilenameFilter filter = new FilenameFilter() {
            /**
             * {@inheritDoc}
             */
            public boolean accept(File dir, String name) {
                assert name != null : "ASSERTION [line=629, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

                return name.toLowerCase().endsWith(".gar") == true;
            }
        };

        firstScanCntr = 0;

        GridUriDeploymentScannerListener listener = new GridUriDeploymentScannerListener() {
            /**
             * {@inheritDoc}
             */
            public void onNewOrUpdatedFile(File file, String uri, long tstamp) {
                if (log.isInfoEnabled() == true) {
                    log.info("Found new or updated GAR units [uri=" + GridUriDeploymentUtils.hidePassword(uri) +
                        ", file=" + file.getAbsolutePath() + ", tstamp=" + tstamp + ']');
                }

                try {
                    GridUriDeploymentFileProcessorResult fileRes = GridUriDeploymentFileProcessor.processFile(file, uri, log);

                    if (fileRes != null) {
                        newUnitReceived(uri, file, tstamp, fileRes.getClassLoader(), fileRes.getTaskClasses());
                    }
                }
                catch (GridSpiException e) {
                    log.error("Error when processing file: " + file.getAbsolutePath(), e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void onDeletedFiles(List<String> uris) {
                if (log.isInfoEnabled() == true) {
                    List<String> uriList = null;

                    if (uris != null) {
                        uriList = new ArrayList<String>();

                        for (String uri : uris) {
                            uriList.add(GridUriDeploymentUtils.hidePassword(uri));
                        }
                    }

                    log.info("Found deleted GAR units [uris=" + uriList + ']');
                }

                processDeletedFiles(uris);
            }

            /**
             * {@inheritDoc}
             */
            public void onFirstScanFinished() {
                synchronized(mux) {
                    firstScanCntr++;

                    if (isFirstScanFinished(firstScanCntr) == true) {
                        mux.notifyAll();
                    }
                }
            }
        };

        for (URI uri : uriEncodedList) {
            GridUriDeploymentScanner scanner = null;

            String proto = uri.getScheme();

            File file = new File(deployTmpDirPath);

            long freq = -1;

            try {
                freq = getFrequencyFromUri(uri);
            }
            catch (NumberFormatException e) {
                log.error("Error parsing parameter value for frequency.", e);
            }

            assert proto != null : "ASSERTION [line=710, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

            if ("file".equals(proto) == true) {
                scanner = new GridUriDeploymentFileScanner(gridName, uri, file, freq > 0 ? freq :
                    DFLT_DISK_SCAN_FREQUENCY, filter, listener, log);
            }
            else if ("http".equals(proto) == true || "https".equals(proto) == true) {
                scanner = new GridUriDeploymentHttpScanner(gridName, uri, file, freq > 0 ? freq :
                    DFLT_HTTP_SCAN_FREQUENCY, filter, listener, log);
            }
// hwellmann: Not needed            
//            else if ("pop3".equals(proto) == true || "pop3s".equals(proto) == true ||
//                "imap".equals(proto) == true || "imaps".equals(proto) == true) {
//                scanner = new GridUriDeploymentMailScanner(gridName, uri, file, freq > 0 ? freq :
//                    DFLT_MAIL_SCAN_FREQUENCY, filter, listener, log, marshaller);
//            }
//            else if ("ftp".equals(proto) == true) {
//                scanner = new GridUriDeploymentFtpScanner(gridName, uri, file, freq > 0 ? freq :
//                    DFLT_FTP_SCAN_FREQUENCY, filter, listener, log);
//            }
            else {
                throw (GridSpiException)new GridSpiException("Unsupported protocol: " + proto).setData(730, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
            }

            scanners.add(scanner);

            try {
                scanner.start();
            }
            catch (RejectedExecutionException e) {
                throw (GridSpiException)new GridSpiException("Failed to start URI deployment due to execution rejection.", e).setData(739, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
            }
        }

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("tmpDirPath", tmpDirPath));
            log.info(configInfo("uriList", uriList));
            log.info(configInfo("encodeUri", encodeUri));
            log.info(configInfo("scanners", scanners));
        }

        // Ack ok start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * Gets URI refresh frequency.
     * URI is parsed and <tt>freq</tt> parameter value returned.
     *
     * @param uri URI to be parsed.
     * @return <tt>-1</tt> if there if no <tt>freq</tt> parameter otherwise
     *      returns frequency.
     * @throws NumberFormatException Thrown if <tt>freq</tt> parameter value
     *      is not a number.
     */
    private long getFrequencyFromUri(URI uri) throws NumberFormatException {
        assert uri != null : "ASSERTION [line=768, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        String userInfo = uri.getUserInfo();

        long freq = -1;

        if (userInfo != null) {
            String[] arr = userInfo.split(";");

            if (arr != null && arr.length > 0) {
                for (String el : arr) {
                    if (el.startsWith("freq=") == true) {
                        return Long.parseLong(el.substring(5));
                    }
                }
            }
        }

        return freq;
    }

    /**
     * {@inheritDoc}
     */
    public GridDeploymentResource findResource(String rsrcName) {
        assert rsrcName != null : "ASSERTION [line=793, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        // Wait until all scanners finish their first scanning.
        try {
            synchronized(mux) {
                while (isFirstScanFinished(firstScanCntr) == false) {
                    mux.wait();
                }
            }
        }
        catch (InterruptedException e) {
            log.error("Failed to wait while all scanners finish their first scanning.", e);

            return null;
        }

        synchronized (mux) {
            GridDeploymentResourceAdapter rsrc = null;

            // Last updated class loader has highest priority in search.
            for (Iterator<GridUriDeploymentUnitDescriptor> iter = unitLoaders.iterator(); iter.hasNext() == true;) {
                GridUriDeploymentUnitDescriptor unitDescr = iter.next();

                Map<String, String> rsrcs = unitDescr.getResources();

                // Try to find resource for current class loader.
                String clsName = rsrcName;

                if (rsrcs.containsKey(rsrcName) == true) {
                    clsName = rsrcs.get(rsrcName);
                }

                //noinspection UnusedCatchParameter
                try {
                    ClassLoader ldr = unitDescr.getClassLoader();

                    Class<?> cls = ldr instanceof GridUriDeploymentClassLoader == true ?
                        ((GridUriDeploymentClassLoader)ldr).loadClassGarOnly(clsName) :
                        ldr.loadClass(clsName);

                    assert cls != null : "ASSERTION [line=833, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

                    if (rsrcs.containsKey(rsrcName) == true) {
                        // Recalculate resource name in case if access is performed by
                        // class name and not the resource name.
                        String alias = getResourceName(clsName, rsrcs);

                        return new GridDeploymentResourceAdapter(alias, cls, unitDescr.getClassLoader());
                    }
                    // Ignore invalid tasks.
                    else if (GridTask.class.isAssignableFrom(cls) == false) {
                        // Add resource in map.
                        rsrcs.put(rsrcName, clsName);

                        return new GridDeploymentResourceAdapter(rsrcName, cls, unitDescr.getClassLoader());
                    }
                }
                catch (ClassNotFoundException e) {
                    // No-op.
                }
            }

            return rsrc;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean register(ClassLoader ldr, Class<?> rsrc) throws GridSpiException {
        GridArgumentCheck.checkNull(ldr, "ldr");
        GridArgumentCheck.checkNull(rsrc, "rsrc");

        long tstamp = System.currentTimeMillis();

        List<ClassLoader> removedClsLdrs = new ArrayList<ClassLoader>();

        Map<String, String> newRsrcs = null;

        synchronized (mux) {
            GridUriDeploymentUnitDescriptor descr = null;

            // Find existing class loader.
            for (GridUriDeploymentUnitDescriptor unitDescr : unitLoaders) {
                if (unitDescr.getClassLoader().equals(ldr) == true) {
                    descr = unitDescr;

                    break;
                }
            }

            if (descr == null) {
                descr = new GridUriDeploymentUnitDescriptor(tstamp, ldr);

                // New unit has largest timestamp.
                //assert (unitLoaders.size() > 0) ? (unitComparator.compare(descr, unitLoaders.getFirst()) <= 0) : "ASSERTION [line=888, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]. " + true;

                unitLoaders.addFirst(descr);
            }

            newRsrcs = addResources(ldr, descr.getResources(), new Class<?>[]{rsrc});

            if (newRsrcs != null && newRsrcs.isEmpty() == false) {
                removeResources(ldr, newRsrcs, removedClsLdrs);
            }
        }

        for (ClassLoader cldLdr : removedClsLdrs) {
            onUnitReleased(cldLdr);
        }

        return newRsrcs != null && newRsrcs.isEmpty() == false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean unregister(String rsrcName) {
        assert rsrcName != null : "ASSERTION [line=911, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        List<ClassLoader> removedClsLdrs = new ArrayList<ClassLoader>();

        boolean removed;

        synchronized (mux) {
            Map<String, String> rsrcs = new HashMap<String, String>(1);

            rsrcs.put(rsrcName, rsrcName);

            removed = removeResources(null, rsrcs, removedClsLdrs);
        }

        for (ClassLoader cldLdr : removedClsLdrs) {
            onUnitReleased(cldLdr);
        }

        return removed;
    }

    /**
     * Add new classes in class loader resource map.
     * Note that resource map may contain two entries for one added class:
     * taskname -> class name and class name -> class name.
     *
     * @param ldr Registered class loader.
     * @param ldrRsrcs Class loader resources.
     * @param clss Registered classes array.
     * @return Map of new resources added for registered class loader.
     * @throws GridSpiException If resource already registered. Exception thrown
     * if registered resources conflicts with rule when all task classes must be
     * annotated with different task names.
     */
    private Map<String, String> addResources(ClassLoader ldr, Map<String, String> ldrRsrcs, Class<?>[] clss)
        throws GridSpiException {
        assert ldr != null : "ASSERTION [line=947, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert ldrRsrcs != null : "ASSERTION [line=948, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert clss != null : "ASSERTION [line=949, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        // Maps resources to classes.
        // Map may contain 2 entries for one class.
        Map<String, String> regRsrcs = new HashMap<String, String>(clss.length * 2, 1.0f);

        Map<String, String> newRsrcs = null;

        // Check alias collision between added classes.
        for (Class<?> cls : clss) {
            String alias = null;

            if (GridTask.class.isAssignableFrom(cls) == true) {
                GridTaskName nameAnn = GridUtils.getAnnotation(cls, GridTaskName.class);

                if (nameAnn != null) {
                    alias = nameAnn.value();
                }
            }

            // If added classes maps to one alias.
            if (alias != null && regRsrcs.containsKey(alias) == true &&
                regRsrcs.get(alias).equals(cls.getName()) == false) {
                throw (GridSpiException)new GridSpiException("Failed to register resources with given task name " +
                    "(found another class with same task name) [taskName=" + alias +
                    ", cls1=" + cls.getName() + ", cls2=" + regRsrcs.get(alias) + ", ldr=" + ldr + ']').setData(972, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
            }

            if (alias != null) {
                regRsrcs.put(alias, cls.getName());
            }

            regRsrcs.put(cls.getName(), cls.getName());
        }

        // Check collisions between added and exist classes.
        for (Entry<String, String> entry : regRsrcs.entrySet()) {
            String newAlias = entry.getKey();
            String newName = entry.getValue();

            if (ldrRsrcs.containsKey(newAlias) == true) {
                String existingCls = ldrRsrcs.get(newAlias);

                // Different classes for the same resource name.
                if (ldrRsrcs.get(newAlias).equals(newName) == false) {
                    throw (GridSpiException)new GridSpiException("Failed to register resources with given task name " +
                        "(found another class with same task name in the same class loader) [taskName=" + newAlias +
                        ", existingCls=" + existingCls + ", newCls=" + newName + ", ldr=" + ldr + ']').setData(994, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
                }
            }
            // Add resources that should be removed for another classloaders.
            else {
                if (newRsrcs == null) {
                    newRsrcs = new HashMap<String, String>(regRsrcs.size());
                }

                newRsrcs.put(newAlias, newName);
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
     * @param ignoreClsLdr Ignored class loader or <tt>null</tt> to remove for all class loaders.
     * @param rsrcs Resources that should be used in search for class loader to remove.
     * @param removedClsLdrs Class loaders to remove.
     * @return <tt>True</tt> if resource was removed.
     */
    private boolean removeResources(ClassLoader ignoreClsLdr, Map<String, String> rsrcs,
        List<ClassLoader> removedClsLdrs) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=1027, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert rsrcs != null : "ASSERTION [line=1028, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        boolean res = false;

        for (Iterator<GridUriDeploymentUnitDescriptor> iter = unitLoaders.iterator(); iter.hasNext() == true;) {
            GridUriDeploymentUnitDescriptor descr = iter.next();
            ClassLoader ldr = descr.getClassLoader();

            boolean isRemoved = false;

            if (ignoreClsLdr == null || ldr.equals(ignoreClsLdr) == false) {
                Map<String, String> clsLdrRrsrcs = descr.getResources();

                assert clsLdrRrsrcs != null : "ASSERTION [line=1041, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

                // Check classloader's registered resources.
                for (String rsrcName : rsrcs.keySet()) {
                    // Remove classloader if resource found.
                    if (clsLdrRrsrcs.containsKey(rsrcName) == true) {
                        iter.remove();

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
     * Gets resource name for a given class name.
     *
     * @param clsName Class name.
     * @param rsrcs Map of resources.
     * @return Resource name.
     */
    private String getResourceName(String clsName, Map<String, String> rsrcs) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=1093, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

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
     * Check is class can be reached.
     *
     * @param ldr Class loader.
     * @param clsName Class name.
     * @return <tt>true</tt> if class can be loaded.
     */
    private boolean isResourceExist(ClassLoader ldr, String clsName) {
        String rsrc = clsName.replaceAll("\\.", "/") + ".class";

        InputStream in = null;

        try {
            in = ldr instanceof GridUriDeploymentClassLoader == true ?
                ((GridUriDeploymentClassLoader)ldr).getResourceAsStreamGarOnly(rsrc) :
                ldr.getResourceAsStream(rsrc);

            return in != null;
        }
        finally {
            GridUtils.close(in, log);
        }
    }

    /**
     * Tests whether first scan is finished or not.
     *
     * @param cntr Number of already scanned URIs.
     * @return <tt>true</tt> if all URIs have been scanned at least once and
     *      <tt>false</tt> otherwise.
     */
    private boolean isFirstScanFinished(int cntr) {
        assert uriEncodedList != null : "ASSERTION [line=1141, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        return cntr >= uriEncodedList.size();
    }

    /**
     * Fills in list of URIs with all available URIs and encodes them if
     * encoding is enabled.
     *
     * @throws GridSpiException Thrown if at least one URI has incorrect syntax.
     */
    private void initializeUriList() throws GridSpiException {
        for (String uri : uriList) {
            assertParameter(uri != null, "uriList.get(X) != null");

            assert uri != null : "ASSERTION [line=1156, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

            String encUri = encodeUri(uri.replaceAll("\\\\", "/"));

            URI uriObj = null;

            try {
                uriObj = new URI(encUri);
            }
            catch (URISyntaxException e) {
                throw (GridSpiException)new GridSpiException("Failed to parse URI [uri=" + GridUriDeploymentUtils.hidePassword(uri) +
                    ", encodedUri=" + GridUriDeploymentUtils.hidePassword(encUri) + ']', e).setData(1166, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
            }

            if (uriObj.getScheme() == null || uriObj.getScheme().trim().length() == 0) {
                throw (GridSpiException)new GridSpiException("Failed to get 'scheme' from URI [uri=" +
                    GridUriDeploymentUtils.hidePassword(uri) +
                    ", encodedUri=" + GridUriDeploymentUtils.hidePassword(encUri) + ']').setData(1171, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
            }

            uriEncodedList.add(uriObj);
        }
    }

    /**
     * Add configuration for file scanner {@link GridUriDeploymentFileScanner}.
     *
     * @throws GridSpiException Thrown if default URI syntax is incorrect.
     */
    private void addDefaultUri() throws GridSpiException {
        assert uriEncodedList != null : "ASSERTION [line=1186, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        URI uri = null;

        try {
            String path = "file://" + (getGridGainHome().charAt(0) == '/' ? "" : '/') + getGridGainHome() + '/' +
                DFLT_DEPLOY_DIR;

            path = encodeUri(path.replaceAll("\\\\", "/"));

            uri = new URI(path);

            uriEncodedList.add(uri);
        }
        catch (URISyntaxException e) {
            throw (GridSpiException)new GridSpiException("Error when initializing default URI for scanner.", e).setData(1201, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
        }
    }

    /**
     * Encode URI path if encoding is enabled. Set of encoded characters
     * in path is (' ', ';', '%').
     *
     * @param path URI which should be encoded.
     * @return Either encoded URI if encoding is enabled or given one
     *      if encoding is disabled.
     */
    private String encodeUri(String path) {
        return encodeUri == true ? new GridUriDeploymentUriParser(path).parse() : path;
    }

    /**
     * Initializes temporary directory path. Path consists of base path
     * (either {@link #tmpDirPath} value or <tt>java.io.tmpdir</tt>
     * system property value if first is <tt>null</tt>) and path relative
     * to base one - {@link #DEPLOY_TMP_ROOT_NAME}.
     *
     * @throws GridSpiException Thrown if temporary directory could not be created.
     */
    private void initializeTemporaryDirectoryPath() throws GridSpiException {
        String tmpDirPath = this.tmpDirPath == null ? System.getProperty("java.io.tmpdir") : this.tmpDirPath;

        if (tmpDirPath == null) {
            throw (GridSpiException)new GridSpiException("Error initializing temporary deployment directory.").setData(1229, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
        }

        File dir = new File(tmpDirPath + File.separator + DEPLOY_TMP_ROOT_NAME);

        if (dir.mkdirs() == false && dir.exists() == false) {
            throw (GridSpiException)new GridSpiException("Error initializing temporary deployment directory: " + dir).setData(1235, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java");
        }

        this.tmpDirPath = tmpDirPath;

        deployTmpDirPath = dir.getPath();
    }

    /**
     * Deploys all tasks that correspond to given descriptor.
     * First method checks tasks versions and stops processing tasks that
     * have both versioned and unversioned instances.
     * <p>
     * Than it deletes tasks with lower version and deploys newest tasks.
     *
     * @param newDescr Tasks deployment descriptor.
     * @param clss Registered classes.
     */
    private void newUnitReceived(GridUriDeploymentUnitDescriptor newDescr, Collection<Class<?>> clss) {
        assert newDescr != null : "ASSERTION [line=1254, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert newDescr.getType() == GridUriDeploymentUnitDescriptor.Type.FILE : "ASSERTION [line=1255, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        if (clss != null && clss.isEmpty() == false) {
            try {
                addResources(newDescr.getClassLoader(), newDescr.getResources(),
                    clss.toArray(new Class<?>[clss.size()]));
            }
            catch (GridSpiException e) {
                log.warning("Failed to register GAR class loader [newDescr=" + newDescr +
                    ", msg=" + e.getMessage() + ']');
            }
        }

        List<ClassLoader> removedClsLdrs = new ArrayList<ClassLoader>();

        synchronized (mux) {
            boolean isAdded = false;
            boolean ignoreNewUnit = false;

            for (ListIterator<GridUriDeploymentUnitDescriptor> iter = unitLoaders.listIterator();
                iter.hasNext() == true;) {
                GridUriDeploymentUnitDescriptor descr = iter.next();

                assert newDescr.getClassLoader().equals(descr.getClassLoader()) == false :
                    "Uri scanners always create new class loader for every GAR file: " + newDescr;

                // Only for GAR files. Undeploy all for overwritten GAR files.
                if (descr.getType() == GridUriDeploymentUnitDescriptor.Type.FILE &&
                    newDescr.getUri().equals(descr.getUri()) == true &&
                    newDescr.getFile().equals(descr.getFile()) == false) {
                    // Remove descriptor.
                    iter.remove();

                    // Add class loaders in collection to notify listener outside synchronization block.
                    removedClsLdrs.add(descr.getClassLoader());

                    // Last descriptor.
                    if (iter.hasNext() == false) {
                        // New descriptor will be added after loop.
                        break;
                    }
                    
                    continue;                  
                }

                if (isAdded == false) {
                    // Unit with largest timestamp win.
                    // Insert it before current element.
                    if (unitComparator.compare(newDescr, descr) <= 0) {
                        // Remove current class loader if found collisions.
                        if (checkUnitCollision(descr, newDescr) == true) {
                            iter.remove();
                            iter.add(newDescr);

                            // Add class loaders in collection to notify listener outside synchronization block.
                            removedClsLdrs.add(descr.getClassLoader());
                        }
                        // Or add new class loader before current class loader.
                        else {
                            iter.set(newDescr);
                            iter.add(descr);
                        }

                        isAdded = true;
                    }
                    else if (checkUnitCollision(newDescr, descr) == true) {
                            // Don't add new unit if found collisions with latest class loader.
                            ignoreNewUnit = true;
                            break;
                    }
                }
                // New descriptor already added and we need to check other class loaders for collisions.
                else if (checkUnitCollision(newDescr, descr) == true) {
                    iter.remove();

                    // Add class loaders in collection to notify listener outside synchronization block.
                    removedClsLdrs.add(descr.getClassLoader());
                }
            }

            if (ignoreNewUnit == false) {
                if (isAdded == false) {
                    unitLoaders.add(newDescr);
                }

                if (log.isInfoEnabled() == true) {
                    log.info("Class loader (re)registered [clsLoader=" + newDescr.getClassLoader() +
                        ", tstamp=" + newDescr.getTimestamp() +
                        ", uri='" + GridUriDeploymentUtils.hidePassword(newDescr.getUri()) +
                        "', file=" + (newDescr.getFile() == null ? "N/A" : newDescr.getFile()) + ']');
                }
            }
        }

        for (ClassLoader cldLdr : removedClsLdrs) {
            onUnitReleased(cldLdr);
        }
    }

    /**
     * Check collisions in added descriptor <tt>newDescr</tt> with another descriptor <tt>existDescr</tt>.
     *
     * @param newDescr New added descriptor.
     * @param existDescr Exist descriptor.
     * @return <tt>True</tt> if collisions found.
     */
    private boolean checkUnitCollision(GridUriDeploymentUnitDescriptor newDescr,
        GridUriDeploymentUnitDescriptor existDescr) {
        assert newDescr != null : "ASSERTION [line=1363, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert existDescr != null : "ASSERTION [line=1364, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        Map<String, String> rsrcs = newDescr.getResources();

        for (Entry<String, String> entry : existDescr.getResources().entrySet()) {
            String rsrcName = entry.getKey();
            String rsrcCls = entry.getValue();

            if (rsrcs.containsKey(rsrcName) == true) {
                log.warning("Found collission with resource name in different GAR files. " +
                    "Class loader will be removed [rsrcName=" + rsrcName + ", cls1=" + rsrcs.get(rsrcName) +
                    ", cls2=" + rsrcCls + ", removedDescr=" + newDescr + ", existDescr=" + existDescr + ']');

                return true;
            }
            else if (isResourceExist(newDescr.getClassLoader(), rsrcCls) == true) {
                log.warning("Found collission with resource class in different GAR files. " +
                    "Class loader will be removed [rsrcName=" + rsrcName + ", rsrcCls=" + rsrcCls +
                    ", removedDescr=" + newDescr + ", existDescr=" + existDescr + ']');

                return true;
            }
        }

        return false;
    }

    /**
     * Deploys or redeploys given tasks.
     *
     * @param uri GAR file deployment URI.
     * @param file GAR file.
     * @param tstamp File modification date.
     * @param ldr Class loader.
     * @param clss List of tasks which were found in GAR file.
     */
    private void newUnitReceived(String uri, File file, long tstamp, ClassLoader ldr,
        Collection<Class<? extends GridTask<?, ?>>> clss) {
        assert uri != null : "ASSERTION [line=1402, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert file != null : "ASSERTION [line=1403, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";
        assert tstamp > 0 : "ASSERTION [line=1404, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        // To avoid units with incorrect timestamp.
        tstamp = Math.min(tstamp, System.currentTimeMillis());

        // Create descriptor.
        GridUriDeploymentUnitDescriptor desc = new GridUriDeploymentUnitDescriptor(uri, file, tstamp, ldr);

        newUnitReceived(desc, clss != null && clss.size() > 0 ? new ArrayList<Class<?>>(clss) : null);
    }

    /**
     * Removes all tasks that belong to GAR files which are on list
     * of removed files.
     *
     * @param uris List of removed files.
     */
    private void processDeletedFiles(List<String> uris) {
        assert uris != null : "ASSERTION [line=1422, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpi.java]";

        if (uris.size() == 0) {
            return;
        }

        synchronized (mux) {
            Set<String> uriSet = new HashSet<String>(uris);

            for (Iterator<GridUriDeploymentUnitDescriptor> iter = unitLoaders.iterator(); iter.hasNext() == true;) {
                GridUriDeploymentUnitDescriptor descr = iter.next();

                if (descr.getType() == GridUriDeploymentUnitDescriptor.Type.FILE &&
                    uriSet.contains(descr.getUri()) == true) {
                    // Remove descriptor.
                    iter.remove();

                    onUnitReleased(descr.getClassLoader());
                }
            }
        }
    }

    /**
     * Notifies listener about released class loader.
     *
     * @param clsLoader Released class loader.
     */
    private void onUnitReleased(ClassLoader clsLoader) {
        GridDeploymentListener tmp = lsnr;

        if (tmp != null) {
            tmp.onUnregistered(clsLoader);
        }
    }

    /**
     * Task deployment descriptor comparator.
     * The greater descriptor is those one that has less timestamp.
     */
    private static class LastTimeUnitDescriptorComparator implements Comparator<GridUriDeploymentUnitDescriptor> {
        /**
         * {@inheritDoc}
         */
        public int compare(GridUriDeploymentUnitDescriptor o1, GridUriDeploymentUnitDescriptor o2) {
            if (o1.getTimestamp() < o2.getTimestamp()) {
                return 1;
            }

            return o1.getTimestamp() == o2.getTimestamp() ? 0 : -1;
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
        return GridToStringBuilder.toString(GridUriDeploymentSpi.class, this);
    }
}
