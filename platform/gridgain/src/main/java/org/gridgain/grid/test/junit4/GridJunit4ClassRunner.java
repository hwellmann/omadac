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
import java.lang.reflect.*;
import java.util.*;

import org.gridgain.grid.util.test.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit4ClassRunner extends GridJunit4Runner {
    /** */
    private final Class<?> cls;

    /** */
    private final Map<String, GridJunit4Result> resMap = new HashMap<String, GridJunit4Result>();

    /** */
    private transient List<Method> testMtds = null;

    /** */
    private transient Description desc = null;

    /**
     *
     * @param cls Runner class.
     */
    GridJunit4ClassRunner(Class<?> cls) {
        this.cls = cls;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Description getDescription() {
        if (desc == null) {
            desc = createDescription();
        }

        return desc;
    }

    /**
     *
     * @return Description for test class.
     */
    protected Description createDescription() {
        Description desc = Description.createSuiteDescription(cls.getName(), cls.getAnnotations());

        for (Method mtd : getTestMethods()) {
            desc.addChild(getDescription(mtd));
        }

        return desc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(RunNotifier notifier) {
        if (getTestMethods().isEmpty() == false) {
            for (Method mtd : getTestMethods()) {
                Description desc = getDescription(mtd);

                GridJunit4Result res = null;

                notifier.fireTestStarted(desc);

                try {
                    // Wait for results.
                    synchronized (resMap) {
                        while (resMap.isEmpty() == true) {
                            try {
                                resMap.wait();
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();

                                notifier.fireTestFailure(new Failure(desc, e));

                                return;
                            }
                        }

                        res = resMap.get(desc.getDisplayName());
                    }

                    try {
                        if (res.getStdOut() != null) {
                            GridTestPrintStreamFactory.getStdOut().write(res.getStdOut());
                        }
                        
                        if (res.getStdErr() != null) {
                            GridTestPrintStreamFactory.getStdErr().write(res.getStdErr());
                        }

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
        synchronized (resMap) {
            GridJunit4ClassRunner resRunner = (GridJunit4ClassRunner)runner;

            resMap.putAll(resRunner.resMap);

            resMap.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean setResult(GridJunit4Result res) {
        for (Method testMtd : getTestMethods()) {
            if (getDescription(testMtd).getDisplayName().equals(res.getName()) == true) {
                addResult(res.getName(), res);

                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean setResult(List<GridJunit4Result> res) {
        synchronized(resMap) {
            for (GridJunit4Result result : res) {
                if (setResult(result) == false) {
                    return false;
                }
            }

            resMap.notifyAll();
        }

        return true;
    }

    /**
     *
     * @param name  FIXDOC
     * @param res FIXDOC
     * @return Previous result.
     */
    private GridJunit4Result addResult(String name, GridJunit4Result res) {
        return resMap.put(name, res);
    }

    /**
     *
     * @return Methods annotated with given annotation.
     */
    private List<Method> getTestMethods() {
        if (testMtds == null) {
            testMtds = GridJunit4Utils.getAnnotatedMethods(cls, Test.class);
        }

        return testMtds;
    }

    /**
     *
     * @param mtd Method to get description for.
     * @return Method description.
     */
    protected Description getDescription(Method mtd) {
        return Description.createTestDescription(cls, mtd.getName(), mtd.getAnnotations());
    }
}
