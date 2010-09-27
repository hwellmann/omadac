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

import java.util.*;

import junit.framework.*;

import org.gridgain.grid.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.internal.runners.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit4SuiteRunner extends GridJunit4Runner {
    /** */
    private final Class<?> cls;

    /** */
    private List<GridJunit4Runner> children;

    /** */
    private transient Description desc = null;

    /** */
    private transient Set<GridJunit4Runner> locRunners = new HashSet<GridJunit4Runner>();

    /**
     * Constructor required by JUnit4.
     *
     * @param cls Suite class.
     */
    GridJunit4SuiteRunner(Class<?> cls) {
        assert cls != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/test/junit4/GridJunit4SuiteRunner.java]";

        this.cls = cls;
    }

    /**
     *
     * @return Annotated classes.
     * @throws InitializationError FIXDOC
     */
    private Class<?>[] getTestClasses() throws InitializationError {
        SuiteClasses ann = cls.getAnnotation(SuiteClasses.class);

        if (ann == null) {
            throw new InitializationError(String.format("class '%s' must have a SuiteClasses annotation",
                cls.getName()));
        }

        return ann.value();
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
        Description desc = Description.createSuiteDescription(cls.getName());

        for (Runner child : getChildren()) {
            desc.addChild(child.getDescription());
        }

        return desc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(RunNotifier notifier) {
        for (GridJunit4Runner child : getChildren()) {
            child.run(notifier);
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
    boolean setResult(GridJunit4Result res) {
        for (GridJunit4Runner child : getChildren()) {
            if (child.setResult(res) == true) {
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
    void copyResults(GridJunit4Runner runner) {
        GridJunit4SuiteRunner resRunner = (GridJunit4SuiteRunner)runner;

        for (int i = 0; i < getChildren().size(); i++) {
            getChildren().get(i).copyResults(resRunner.getChildren().get(i));
        }
    }

    /**
     *
     * @return Child runners.
     */
    final List<GridJunit4Runner> getChildren() {
        if (children == null) {
            children = createChildren();
        }

        return children;
    }

    /**
     *
     * @return Child runners.
     */
    protected List<GridJunit4Runner> createChildren() {
        List<GridJunit4Runner> children = new ArrayList<GridJunit4Runner>();

        try {
            for (Class<?> testCls : getTestClasses()) {
                children.add(getRunner(testCls));
            }
        }
        catch (InitializationError e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to create suite children.", e).setData(191, "src/java/org/gridgain/grid/test/junit4/GridJunit4SuiteRunner.java");
        }

        return children;
    }

    /**
     *
     * @param runner FIXDOC
     * @return <tt>True</tt> if runner is local.
     */
    protected boolean isLocal(GridJunit4Runner runner) {
        return locRunners.contains(runner) == true;
    }

    /**
     *
     * @param testCls FIXDOC
     * @return Runner for the class.
     * @throws InitializationError FIXDOC
     */
    protected GridJunit4Runner getRunner(Class<?> testCls) throws InitializationError {
        if (testCls.getAnnotation(Ignore.class) != null) {
            return new GridJunit4IgnoredClassRunner(testCls);
        }

        RunWith runWith= testCls.getAnnotation(RunWith.class);

        if (runWith != null) {
            Class<?> runnerCls = runWith.value();

            if (runnerCls.equals(GridJunit4LocalSuite.class) == true) {
                GridJunit4Runner runner = new GridJunit4SuiteRunner(testCls);

                locRunners.add(runner);

                return runner;
            }
            else if (runnerCls.equals(Suite.class) == true || runnerCls.equals(GridJunit4Suite.class)) {
                return new GridJunit4SuiteRunner(testCls);
            }
            else if (runnerCls.equals(AllTests.class) == true) {
                return new GridJunit38SuiteRunner(testCls);
            }
            else if (runnerCls.equals(Parameterized.class) == true) {
                return new GridJunit4ParametarizedRunner(testCls);
            }
            else if (runnerCls.equals(Enclosed.class) == true) {
                return new GridJunit4EnclosedRunner(testCls);
            }

            throw new InitializationError("Runners other than 'Suite' are not supported yet: " + runWith.value());
        }
        else if (GridJunit4Utils.hasSuiteMethod(testCls) == true) {
            return new GridJunit38SuiteRunner(testCls);
        }
        else if (isPre4Test(testCls) == true) {
            return new GridJunit38SuiteRunner(testCls);
        }
        else {
            // According to Junit4 code, this is the best we can do here.
            return new GridJunit4ClassRunner(testCls);
        }
    }

    /**
     *
     * @param testClass FIXDOC
     * @return <tt>True</tt> if test is Junit3 test.
     */
    private boolean isPre4Test(Class<?> testClass) {
        return TestCase.class.isAssignableFrom(testClass);
    }
}
