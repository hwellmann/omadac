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

package org.gridgain.grid.test.junit3;

import junit.framework.*;

/**
 * Proxy interface for local tests.
 * <p>
 * Note that this interface must be declared <tt>public</tt> in order for
 * JavaAssist to work.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
/*@HIDE_FROM_JAVADOC@*/public interface GridJunit3TestCaseProxy {
    /*
     * This class public only due to limitation/design of JUnit3 framework.
     */

    /**
     *
     * @return Original test case.
     */
    TestCase getGridGainJuni3OriginalTestCase();

    /**
     *
     * @param stdOut Standard output in serialized form.
     * @param errOut Standard error output in serialized form.
     * @param error Optional error.
     * @param failure Optional failure.
     */
    void setGridGainJunit3Result(byte[] stdOut, byte[] errOut, Throwable error, Throwable failure);
}
