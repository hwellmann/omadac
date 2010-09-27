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
import java.util.*;

import junit.framework.*;

import org.gridgain.grid.util.test.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit38ClassRunner extends GridJunit4Runner {
    /** */
    private final String name;

    /** */
    private final Class<?> cls;

    /** */
    private GridJunit4Result res = null;

    /** */
    private transient TestCase test = null;

    /** */
    private transient Description desc = null;

    /** */
    private final transient Object mux = new Object();

    /**
     * @param cls Runner class.
     */
    GridJunit38ClassRunner(Class<?> cls) {
        this.cls = cls;

        name = cls.getName();
    }

    /**
     *
     * @param test Test.
     */
    GridJunit38ClassRunner(TestCase test) {
        cls = test.getClass();

        name = Description.createTestDescription(test.getClass(), test.getName()).getDisplayName();

        this.test = test;
    }

    /**
     *
     * @return Gets test case for given class.
     */
    private TestCase getTest() {
        if (test == null) {
            test = (TestCase)GridJunit4Utils.createJunit3Test(name, getTestClass());
        }

        return test;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Class<?> getTestClass() {
        return cls;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void copyResults(GridJunit4Runner runner) {
        GridJunit38ClassRunner resRunner =  (GridJunit38ClassRunner)runner;

        if (resRunner.name.equals(name) == true) {
            synchronized (mux) {
                res = resRunner.res;

                mux.notifyAll();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean setResult(GridJunit4Result res) {
        if (name.equals(res.getName()) == true) {
            this.res = res;

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean setResult(List<GridJunit4Result> res) {
        assert res.size() == 1 : "ASSERTION [line=134, file=src/java/org/gridgain/grid/test/junit4/GridJunit38ClassRunner.java]";

        for (GridJunit4Result result : res) {
            if (setResult(result) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Description getDescription() {
        if (desc == null) {
            desc = GridJunit4Utils.createJunit3Description(getTest());
        }

        return desc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(RunNotifier notifier) {
        notifier.fireTestStarted(desc);

        try {
            // Wait for results.
            synchronized (mux) {
                while (true) {
                    // This condition is taken out of the loop to avoid
                    // potentially wrong optimization by the compiler of
                    // moving field access out of the loop causing this loop
                    // to never exit.
                    if (res != null) {
                        break;
                    }

                    try {
                        mux.wait();
                    }
                    catch (InterruptedException e) {
                        notifier.fireTestFailure(new Failure(desc, e));

                        return;
                    }
                }
            }

            try {
                GridTestPrintStreamFactory.getStdOut().write(res.getStdOut());
                GridTestPrintStreamFactory.getStdErr().write(res.getStdErr());

                if (res.getFailure() != null) {
                    notifier.fireTestFailure(new Failure(desc, res.getFailure()));
                }

                if (res.isIgnored() == true) {
                    notifier.fireTestIgnored(desc);
                }
            }
            catch (IOException e) {
                notifier.fireTestFailure(new Failure(desc, e));
            }
        }
        finally {
            notifier.fireTestFinished(desc);
        }
    }
}
