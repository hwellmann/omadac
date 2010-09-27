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

package org.gridgain.grid.spi.checkpoint.sharedfs;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class defines shared file system {@link GridCheckpointSpi} implementation for
 * checkpoint SPI. All checkpoints are stored on shared storage and available for all
 * nodes in the grid. Note that every node must have access to the shared directory. The
 * reason the directory needs to be <tt>shared</tt> is because a job state
 * can be saved on one node and loaded on another (e.g. if a job gets
 * preempted on a different node after node failure). When started, this SPI tracks
 * all checkpoints saved by localhost for expiration. Note that this SPI does not
 * cache data stored in checkpoints - all the data is loaded from file system
 * on demand.
 * <p>
 * Directory path for shared checkpoints should either be empty or contain previously
 * stored checkpoint files.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 * <li>Directory path (see {@link #setDirectoryPath(String)})</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridSharedFsCheckpointSpi can be configured as follows:
 * <pre name="code" class="java">
 * GridSharedFsCheckpointSpi checkpointSpi = new GridSharedFsCheckpointSpi();
 *
 * // Override default directory path.
 * checkpointSpi.setDirectoryPath("/my/directory/path");
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default checkpoint SPI.
 * cfg.setCheckpointSpi(checkpointSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridSharedFsCheckpointSpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *     ...
 *     &lt;property name="checkpointSpi"&gt;
 *         &lt;bean class="org.gridgain.grid.spi.checkpoint.sharedfs.GridSharedFsCheckpointSpi"&gt;
 *             &lt;!-- Change to shared directory path in your environment. --&gt;
 *             &lt;property name="directoryPath" value="/my/directory/path"/&gt;
 *         &lt;/bean&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridCheckpointSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridSharedFsCheckpointSpi extends GridSpiAdapter implements GridCheckpointSpi,
    GridSharedFsCheckpointSpiMBean {
    /** Default checkpoint directory (value is <tt>work/checkpoint/sharedfs</tt>). */
    public static final String DFLT_DIRECTORY_PATH = "work/checkpoint/sharedfs";

    /** Checkpoint directory. */
    private static final String CHECKPOINT_DIRECTORY = "gridgain:checkpoint:directory";

    /** */
    private static final String CODES = "0123456789QWERTYUIOPASDFGHJKLZXCVBNM";

    /** */
    private static final int CODES_LEN = CODES.length();

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** Checkpoint directory where all files are stored. */
    private String dirPath = DFLT_DIRECTORY_PATH;

    /**
     * Either {@link #dirPath} value if it is absolute
     * path or @{GRID_GAIN_HOME}/{@link #dirPath} if one above was not found.
     */
    private File folder = null;

    /** Local host name. */
    private String host = null;

    /** Task that takes care about outdated files. */
    private GridSharedFsTimeoutTask timeoutTask = null;

    /**
     * {@inheritDoc}
     */
    public String getDirectoryPath() {
        return dirPath;
    }

    /**
     * Sets path to a shared directory where checkpoints will be stored. The
     * path can either be absolute or relative to <tt>GRIDGAIN_HOME</tt> system
     * or environment variable.
     * <p>
     * If not provided, default value is {@link #DFLT_DIRECTORY_PATH}.
     *
     * @param dirPath Absolute or GridGain installation home folder relative path where checkpoints
     *      will be stored.
     */
    @GridSpiConfiguration(optional = true)
    public void setDirectoryPath(String dirPath) {
        this.dirPath = dirPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(CHECKPOINT_DIRECTORY), getDirectoryPath());
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(dirPath != null, "dirPath != null");

        if (new File(dirPath).exists() == true) {
            folder = new File(dirPath);
        }
        else {
            folder = GridUtils.resolveGridGainPath(dirPath);

            if (folder == null) {
                // Create relative by default.
                folder = new File(getGridGainHome(), dirPath);

                if (folder.mkdirs() == false) {
                    if (folder.exists() == false) {
                        throw (GridSpiException)new GridSpiException("Checkpoint directory does not exist and cannot be created : " +
                            folder).setData(191, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");

                }
                }

                if (log.isInfoEnabled() == true) {
                    log.info("Created shared filesystem checkpoint folder: " + folder.getAbsolutePath());
                }
            }
        }

        assert folder != null : "ASSERTION [line=203, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        if (folder.isDirectory() == false) {
            throw (GridSpiException)new GridSpiException("Checkpoint directory path is not a valid directory: " + dirPath).setData(206, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");
        }

        registerMBean(gridName, this, GridSharedFsCheckpointSpiMBean.class);

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("folder", folder));
        }

        try {
            host = GridNetworkHelper.getLocalHost().getHostName();
        }
        catch (IOException e) {
            throw (GridSpiException)new GridSpiException("Failed to get localhost address.", e).setData(220, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");
        }

        Map<File, GridSharedFsTimeData> files = new HashMap<File, GridSharedFsTimeData>();

        // Track expiration for only those files that are made by this node
        // to avoid file access conflicts.
        for (File file : getFiles()) {
            if (file.exists() == true) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Checking checkpoint file: " + file.getAbsolutePath());
                }

                try {
                    GridSharedFsCheckpointData data = GridSharedFsUtils.read(file, marshaller, log);

                    if (data.getHost().equals(host) == true) {
                        files.put(file, new GridSharedFsTimeData(data.getExpireTime(), file.lastModified()));

                        if (log.isDebugEnabled() == true) {
                            log.debug("Registered existing checkpoint from: " + file.getAbsolutePath());
                        }
                    }
                }
                catch (GridException e) {
                    log.error("Failed to marshal/unmarshal objects in checkpoint file (ignoring): " +
                        file.getAbsolutePath(), e);
                }
                catch (IOException e) {
                    log.error("IO error reading checkpoint file (ignoring): " + file.getAbsolutePath(), e);
                }
            }
        }

        timeoutTask = new GridSharedFsTimeoutTask(gridName, marshaller, log);

        timeoutTask.add(files);

        timeoutTask.start();

        // Ack ok start.
        if (log.isInfoEnabled() ==true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        if (timeoutTask != null) {
            GridUtils.interrupt(timeoutTask);
            GridUtils.join(timeoutTask, log);
        }

        unregisterMBean();

        // Clean resources.
        folder = null;
        host = null;

        // Ack ok stop.
        if (log.isInfoEnabled() ==true) {
            log.info(stopInfo());
        }
    }

    /**
     * Returns new file name for the given key. Since fine name is based on the key,
     * the key must be unique. This method converts string key into hexadecimal-based
     * string to avoid conflicts of special characters in filenames.
     *
     * @param key Unique checkpoint key.
     * @return Unique checkpoint file name.
     */
    private String getUniqueFileName(String key) {
        assert key != null : "ASSERTION [line=296, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        StringBuilder buf = new StringBuilder();

        // To be overly safe we'll limit file name size
        // to 128 characters (124 characters name + 4 character extension).
        // We also limit file name to upper case only to avoid surprising
        // behavior between Windows and Unix file systems.
        for (int i = 0; i < key.length() && i < 124; i++) {
            buf.append(CODES.charAt(key.charAt(i) % CODES_LEN));
        }

        return buf.append(".gcp").toString();
    }

    /**
     * {@inheritDoc}
     */
    public byte[] loadCheckpoint(String key) throws GridSpiException {
        assert key != null : "ASSERTION [line=315, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        File file = new File(folder, getUniqueFileName(key));

        if (file.exists() == true) {
            try {
                GridSharedFsCheckpointData data = GridSharedFsUtils.read(file, marshaller, log);

                if (data != null) {
                    return data.getExpireTime() == 0 ? data.getState() : data.getExpireTime() >
                        System.currentTimeMillis() ? data.getState() : null;
                }
            }
            catch (GridException e) {
                throw (GridSpiException)new GridSpiException("Failed to marshal/unmarshal objects in checkpoint file: " +
                    file.getAbsolutePath(), e).setData(329, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to read checkpoint file: " + file.getAbsolutePath(), e).setData(333, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(String key, byte[] state, long timeout) throws GridSpiException {
        assert key != null : "ASSERTION [line=344, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        long expireTime = 0;

        if (timeout > 0) {
            expireTime = System.currentTimeMillis() + timeout;

            if (expireTime < 0) {
                expireTime = Long.MAX_VALUE;
            }
        }

        File file = new File(folder, getUniqueFileName(key));

        if (file.exists() == true) {
            if (log.isDebugEnabled() == true) {
                log.debug("Rewriting existing file: " + file.getAbsolutePath());
            }
        }

        try {
            GridSharedFsUtils.write(file, new GridSharedFsCheckpointData(state, expireTime, host), marshaller, log);
        }
        catch (IOException e) {
            throw (GridSpiException)new GridSpiException("Failed to write checkpoint data into file [path=" +
                file.getAbsolutePath() + ", state=" + Arrays.toString(state) + ']', e).setData(368, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");
        }
        catch (GridException e) {
            throw (GridSpiException)new GridSpiException("Failed to marshal checkpoint data into file [path=" +
                file.getAbsolutePath() + ", state=" + Arrays.toString(state) + ']', e).setData(372, "src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java");
        }

        if (timeout > 0) {
            timeoutTask.add(file, new GridSharedFsTimeData(expireTime, file.lastModified()));
        }
    }

    /**
     * Returns list of files in checkpoint directory.
     * All subdirectories and their files are skipped.
     *
     * @return Array of open file descriptors.
     */
    private File[] getFiles() {
        assert folder != null : "ASSERTION [line=388, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        return folder.listFiles(new FileFilter() {
            /**
             * {@inheritDoc}
             */
            public boolean accept(File pathname) {
                return pathname.isDirectory() == false;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCheckpoint(String key) {
        assert key != null : "ASSERTION [line=404, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        File file = new File(folder, getUniqueFileName(key));

        timeoutTask.remove(file);

        return delete(file);
    }

    /**
     * Tries to physically delete file if it's possible.
     *
     * @param file File to be deleted.
     * @return <tt>true</tt> if file was deleted successfully and <tt>false</tt> if not.
     */
    private boolean delete(File file) {
        assert file != null : "ASSERTION [line=420, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointSpi.java]";

        return file.delete() == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(3);

        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));
        attrs.add(createSpiAttributeName(CHECKPOINT_DIRECTORY));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridSharedFsCheckpointSpi.class, this);
    }
}
