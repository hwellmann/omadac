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

package org.gridgain.grid.loaders.cmdline;

import java.util.concurrent.*;
import org.gridgain.grid.*;
import static org.gridgain.grid.GridFactoryState.*;
import org.gridgain.grid.loaders.*;

/**
 * This class defines command-line GridGain loader. This loader can be used to start GridGain
 * outside of any hosting environment from command line. This loader is a Java application with
 * {@link #main(String[])} method that accepts command line arguments. It accepts just one
 * parameter which is Spring XML configuration file path. You can run this class from command
 * line without parameters to get help message.
 * <p>
 * Note that scripts <tt>[GRIDGAIN_HOME]/bin/gridgain.{sh|bat}</tt> shipped with GridGain use
 * this loader and you can use them as an example.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"CallToSystemExit"})
@GridLoader(description = "Command line loader")
public final class GridCommandLineLoader {
    /** Name of the system property defining name of command line program. */
    private static final String GRIDGAIN_PROG_NAME = "GRIDGAIN_PROG_NAME";

    /** Copyright text. Ant processed. */
    private static final String COPYRIGHT = "Copyright (C) 2005-2009 GridGain Systems.";

    /** Version. Ant processed. */
    private static final String VER = "2.1.1";

    /** Support email. Ant processed. */
    private static final String EMAIL = "support@gridgain.com";

    /** Latch. */
    private static CountDownLatch latch = null;

    /**
     * Enforces singleton.
     */
    private GridCommandLineLoader() {
        // No-op.
    }

    /**
     * Echos the given messages.
     *
     * @param msg Message to echo.
     */
    private static void echo(String msg) {
        assert msg != null : "ASSERTION [line=73, file=src/java/org/gridgain/grid/loaders/cmdline/GridCommandLineLoader.java]";

        System.out.println(msg);
    }

    /**
     * Echos exception stack trace.
     *
     * @param e Exception to print.
     */
    private static void echo(GridException e) {
        assert e != null : "ASSERTION [line=84, file=src/java/org/gridgain/grid/loaders/cmdline/GridCommandLineLoader.java]";

        System.err.println(e);
    }

    /**
     * Exists with optional error message, usage show and exit code.
     *
     * @param errMsg Optional error message.
     * @param showUsage Whether or not to show usage information.
     * @param exitCode Exit code.
     */
    private static void exit(String errMsg, boolean showUsage, int exitCode) {
        if (errMsg != null) {
            echo("ERROR: " + errMsg);
        }

        String runner = System.getProperty(GRIDGAIN_PROG_NAME, "gridgain.{sh|bat}");

        int space = runner.indexOf(' ');

        runner = runner.substring(0, space == -1 ? runner.length() : space);

        if (showUsage == true) {
            echo("Usage:");
            echo("    " + runner + " <arg>");
            echo("    Where <arg> is:");
            echo("    ?, /help, -help  - show this message.");
            echo("    path             - path to Spring XML configuration file.");
            echo("                       Path can be absolute or relative to GRIDGAIN_HOME.");
            echo("Spring file should contain one bean definition of Java type");
            echo("org.gridgain.grid.GridConfiguration. Note that bean will be");
            echo("fetched by the type and its ID is not used.");
            echo("For support send email to: " + EMAIL);
        }

        System.exit(exitCode);
    }

    /**
     * Prints logo.
     */
    private static void logo() {
        echo("GridGain Command Line Loader, ver. " + VER);
        echo(COPYRIGHT);
        echo("");
    }

    /**
     * Tests whether argument is help argument.
     *
     * @param arg Command line argument.
     * @return <tt>true</tt> if given argument is a help argument, <tt>false</tt> otherwise.
     */
    private static boolean isHelp(String arg) {
        String s = null;

        //noinspection SingleCharacterStartsWith
        if (arg.startsWith("-") == true || arg.startsWith("/") == true || arg.startsWith("\\") == true) {
            s = arg.substring(1);
        }
        else if (arg.startsWith("--") == true) {
            s = arg.substring(2);
        }
        else {
            s = arg;
        }

        return s.equals("?") == true || s.equalsIgnoreCase("help") == true || s.equalsIgnoreCase("h") == true;
    }

    /**
     * Main entry point.
     *
     * @param args Command line arguments.
     */
    @SuppressWarnings({"unchecked"})
    public static void main(String[] args) {
        logo();

        if (args.length < 1) {
            exit("Too few arguments.", true, -1);
        }

        if (args.length > 1) {
            exit("Too many arguments.", true, -1);
        }

        if (isHelp(args[0]) == true) {
            exit(null, true, 0);
        }

        GridFactory.addListener(new GridFactoryListener() {
            /**
             * {@inheritDoc}
             */
            public void onStateChange(String name, GridFactoryState state) {
                if (state == STOPPED && latch != null) {
                    latch.countDown();
                }
            }
        });

        try {
            GridFactory.start(args[0]);
        }
        catch (GridException e) {
            echo(e);

            exit("Failed to start grid: " + e.getMessage(), false, -1);
        }

        latch = new CountDownLatch(GridFactory.getAllGrids().size());

        try {
            while (latch.getCount() > 0) {
                latch.await();
            }
        }
        catch (InterruptedException e) {
            echo("Loader was interrupted (exiting): " + e.getMessage());
        }

        System.exit(0);
    }
}
