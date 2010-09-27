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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.*;

/**
 * Implementation of {@link GridSpiThread} that takes care about outdated files.
 * Every checkpoint has expiration date after which it makes no sense to
 * keep it. This class periodically compares files last access time with given
 * expiration time.
 * <p>
 * If this file was not accessed then it is deleted. If file access time is
 * different from modification date new expiration date is set.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridSharedFsTimeoutTask extends GridSpiThread {
    /** Map of files to their access and expiration date. */
    private Map<File, GridSharedFsTimeData> files = new HashMap<File, GridSharedFsTimeData>();

    /** Messages logger. */
    private GridLogger log = null;

    /** Messages marshaller. */
    private GridMarshaller marshaller = null;

    /** */
    private final Object mux = new Object();

    /**
     * Creates new instance of task that looks after files.
     *
     * @param gridName Grid name.
     * @param marshaller Messages marshaller.
     * @param log Messages logger.
     */
    GridSharedFsTimeoutTask(String gridName, GridMarshaller marshaller, GridLogger log) {
        super(gridName, "grid-sharedfs-timeout-worker", log);

        assert marshaller != null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";
        assert log != null : "ASSERTION [line=67, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";

        this.marshaller = marshaller;
        this.log = log.getLogger(getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void body() throws InterruptedException {
        Map<File, GridSharedFsTimeData> snapshot = null;

        long nextTime = 0;

        while (isInterrupted() == false) {
            synchronized (mux) {
                long delay = System.currentTimeMillis() - nextTime;

                if (nextTime != 0 && delay > 0) {
                    mux.wait(delay);
                }

                snapshot = new HashMap<File, GridSharedFsTimeData>(files);

                long now = System.currentTimeMillis();

                nextTime = -1;

                // check files one by one and physically remove
                // if (now - last modification date) > expiration time
                for (Map.Entry<File, GridSharedFsTimeData> entry : snapshot.entrySet()) {
                    File file = entry.getKey();

                    GridSharedFsTimeData timeData = entry.getValue();

                    try {
                        if (timeData.getLastAccessTime() != file.lastModified()) {
                            timeData.setExpireTime(GridSharedFsUtils.read(file, marshaller, log).getExpireTime());
                        }
                    }
                    catch (GridException e) {
                        log.error("Failed to marshal/unmarshal in checkpoint file: " + file.getAbsolutePath(), e);

                        continue;
                    }
                    catch (IOException e) {
                        if (file.exists() == false) {
                            files.remove(file);
                        }
                        else {
                            log.error("Failed to read checkpoint file: " + file.getAbsolutePath(), e);
                        }

                        continue;
                    }

                    if (timeData.getExpireTime() > 0) {
                        if (timeData.getExpireTime() <= now) {
                            if (file.delete() == false && file.exists() == true) {
                                log.error("Failed to delete check point file by timeout: " + file.getAbsolutePath());
                            }
                            else {
                                files.remove(file);

                                if (log.isDebugEnabled() == true) {
                                    log.debug("File was deleted by timeout: " + file.getAbsolutePath());
                                }
                            }
                        }
                        else {
                            if (timeData.getExpireTime() < nextTime || nextTime == -1) {
                                nextTime = timeData.getExpireTime();
                            }
                        }
                    }
                }
            }
        }

        files.clear();
    }

    /**
     * Adds file to a list of files this task should look after.
     *
     * @param file      File being watched.
     * @param timeData  File expiration and access information.
     */
    void add(File file, GridSharedFsTimeData timeData) {
        assert file != null : "ASSERTION [line=157, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";
        assert timeData != null : "ASSERTION [line=158, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";

        synchronized (mux) {
            files.put(file, timeData);

            mux.notifyAll();
        }
    }

    /**
     * Adds list of files this task should looks after.
     *
     * @param newFiles List of files.
     */
    void add(Map<File, GridSharedFsTimeData> newFiles) {
        assert newFiles != null : "ASSERTION [line=173, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";

        synchronized (mux) {
            files.putAll(newFiles);

            mux.notifyAll();
        }
    }

    /**
     * Stops watching file.
     *
     * @param file File that task should not look after anymore.
     */
    void remove(File file) {
        assert file != null : "ASSERTION [line=188, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";

        synchronized (mux) {
            files.remove(file);
        }
    }

    /**
     * Stops watching file list.
     *
     * @param delFiles List of files this task should not look after anymore.
     */
    void remove(List<File> delFiles) {
        assert delFiles != null : "ASSERTION [line=201, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeoutTask.java]";

        synchronized (mux) {
            for (File file : delFiles) {
                files.remove(file);
            }
        }
    }
}
