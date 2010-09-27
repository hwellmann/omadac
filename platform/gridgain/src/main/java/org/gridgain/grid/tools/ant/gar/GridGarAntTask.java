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

package org.gridgain.grid.tools.ant.gar;

import java.io.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.zip.*;

/**
 * Ant task for generating GAR file. This task extends standard <tt>zip</tt> Ant task and
 * has two parameters:
 * <ul>
 * <li><tt>basedir</tt> - Base directory for GAR archive.</li>
 * <li>
 *      <tt>descrdir</tt> - Directory where descriptor {@link #DESCR_NAME} file is located.
 *      If not specified, it is assumed that GridGain descriptor will be searched in base directory
 *      (see {@link #setBasedir(File)}). <b>Note</b> further that GAR descriptor file is fully optional
 *      itself for GAR archive.
 * </li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridGarAntTask extends Zip {
    /** GAR descriptor name. Its value is <tt>gridgain.xml</tt>. */
    public static final String DESCR_NAME = "gridgain.xml";

    /**  Default descriptor path. */
    private static final String DESCR_PATH = "META-INF";

    /** Descriptor directory. */
    private File descrDir = null;

    /** Descriptor file name. */
    private File descrFile = null;

    /** Base directory of Ant task. */
    private File baseDir = null;

    /**
     * Creates ant task with default values.
     */
    public GridGarAntTask() {
        archiveType = "gar";
        emptyBehavior = "create";
    }

    /**
     * Sets the directory where descriptor is located. This parameter is optional and if not set Ant task
     * will search for descriptor file in base directory. <b>Note</b> further that GAR descriptor file is fully optional
     * itself for GAR archive.
     *
     * @param descrDir Descriptor directory.
     */
    public void setDescrdir(File descrDir) {
        assert descrDir != null : "ASSERTION [line=78, file=src/java/org/gridgain/grid/tools/ant/gar/GridGarAntTask.java]";

        this.descrDir = descrDir;
    }

    /**
     * Sets base directory for the archive.
     *
     * @param baseDir Base archive directory to set.
     */
    @Override
    public void setBasedir(File baseDir) {
        super.setBasedir(baseDir);

        this.baseDir = baseDir;
    }

    /**
     * Executes the Ant task.
     */
    @Override
    public void execute() {
        setEncoding("UTF8");

        // Otherwise super method will throw exception.
        if (baseDir != null && baseDir.isDirectory() == true) {
            File descr = null;

            File[] files = baseDir.listFiles(new FileFilter() {
                /**
                 * {@inheritDoc}
                 */
                public boolean accept(File pathname) {
                    return pathname.isDirectory() == true && pathname.getName().equals(DESCR_PATH) == true;
                }
            });

            if (files.length == 1) {
                files = files[0].listFiles(new FileFilter() {
                    /**
                     * {@inheritDoc}
                     */
                    public boolean accept(File pathname) {
                        return pathname.isDirectory() == false && pathname.getName().equals(DESCR_NAME) == true;
                    }
                });
            }

            if (files.length == 1) {
                descr = files[0];
            }

            // File was defined in source.
            if (descr != null) {
                if (descrDir != null) {
                    throw new BuildException("GridGain descriptor '" + DESCR_NAME + "' is already " +
                        "defined in source folder.");
                }
            }
            // File wasn't defined in source and must be defined using 'descrdir' attribute.
            else {
                if (descrDir == null) {
                    throw new BuildException(
                        "GridGain descriptor must be defined either in source folder or using 'descrdir' attribute.");
                }
                else if (descrDir.isDirectory() == false) {
                    throw new BuildException("'descrdir' attribute isn't folder [dir=" + descrDir.getAbsolutePath() +
                        ']');
                }

                descrFile = new File(getFullPath(descrDir.getAbsolutePath(), DESCR_NAME));

                if (descrFile.exists() == false) {
                    throw new BuildException("Folder doesn't contain GridGain descriptor [path=" +
                        descrDir.getAbsolutePath() + ']');
                }
            }
        }

        super.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initZipOutputStream(ZipOutputStream zOut) throws IOException {
        if (descrFile != null) {
            zipFile(descrFile, zOut, getFullPath(DESCR_PATH, DESCR_NAME), ZipFileSet.DEFAULT_FILE_MODE);
        }
    }

    /**
     * Constructs full path given two other paths.
     *
     * @param subPath1 1st path.
     * @param subPath2 2nd path.
     * @return Full path.
     */
    private static String getFullPath(String subPath1, String subPath2) {
        assert subPath1 != null : "ASSERTION [line=178, file=src/java/org/gridgain/grid/tools/ant/gar/GridGarAntTask.java]";
        assert subPath2 != null : "ASSERTION [line=179, file=src/java/org/gridgain/grid/tools/ant/gar/GridGarAntTask.java]";

        char c = subPath1.charAt(subPath1.length() - 1);

        boolean b1 = c == '/' || c == '\\';

        c = subPath2.charAt(0);

        boolean b2 = c == '/' || c == '\\';

        if (b1 != b2) {
            return subPath1 + subPath2;
        }
        else if (b1 == false) { // b2 == false
            return subPath1 + '/' + subPath2;
        }
        else { // b1 == b2 == true
            return subPath1.substring(0, subPath1.length() - 1) + File.separatorChar + subPath2.substring(1);
        }
    }
}
