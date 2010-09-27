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

package org.gridgain.grid.test.junit4;

import java.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.test.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"ParameterNameDiffersFromOverriddenParameter"})
class GridJunit4Job extends GridJobAdapter<GridJunit4Runner> {
    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /**
     *
     * @param arg FIXDOC
     */
    GridJunit4Job(GridJunit4Runner arg) {
        super(arg);

        assert arg != null : "ASSERTION [line=51, file=src/java/org/gridgain/grid/test/junit4/GridJunit4Job.java]";
    }

    /**
     * {@inheritDoc}
     */
    public GridJunit4Runner execute() throws GridException {
        final GridJunit4Runner runner = getArgument();

        JUnitCore core = new JUnitCore();

        core.addListener(new RunListener() {
            /** */
            private GridTestPrintStream out = null;

            /** */
            private GridTestPrintStream err = null;

            /** */
            private Throwable failure = null;

            /**
             * {@inheritDoc}
             */
            @Override
            public void testStarted(Description desc) throws Exception {
                GridTestPrintStreamFactory.getStdOut().println("Distributed test started: " + desc);

                out = GridTestPrintStreamFactory.acquireOut();
                err = GridTestPrintStreamFactory.acquireErr();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void testFinished(Description desc) throws Exception {
                try {
                    runner.setResult(new GridJunit4Result(desc.getDisplayName(), getBytes(out), getBytes(err),
                        failure));
                }
                catch (IOException e) {
                    log.error("Error resetting output.", e);
                }

                GridTestPrintStreamFactory.releaseOut();
                GridTestPrintStreamFactory.releaseErr();

                out = null;
                err = null;
                failure = null;

                GridTestPrintStreamFactory.getStdOut().println("Distributed test finished: " + desc);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void testFailure(Failure failure) throws Exception {
                this.failure = failure.getException();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void testIgnored(Description desc) throws Exception {
                GridJunit4Result res = new GridJunit4Result(desc.getDisplayName());

                res.setIgnored(true);

                runner.setResult(res);
            }

            /**
             *
             * @param out FIXDOC
             * @return Output bytes.
             * @throws IOException FIXDOC
             */
            private byte[] getBytes(GridTestPrintStream out) throws IOException {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

                out.purge(byteOut);

                return byteOut.toByteArray();
            }
        });

        core.run(runner.getTestClass());

        return runner;
    }
}
