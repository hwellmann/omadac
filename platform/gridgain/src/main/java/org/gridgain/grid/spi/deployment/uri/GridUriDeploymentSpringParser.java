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

import java.io.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.logger.*;
import org.springframework.beans.*;
import org.springframework.beans.factory.xml.*;
import org.springframework.core.io.*;

/**
 * Workaround for {@link InputStreamResource}. Converts input stream with XML
 * to <tt>GridUriDeploymentSpringDocument</tt> with {@link ByteArrayResource}
 * instead of {@link InputStreamResource}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridUriDeploymentSpringParser {
    /**
     * Enforces singleton.
     */
    private GridUriDeploymentSpringParser() {
        // No-op.
    }

    /**
     * Converts given input stream expecting XML inside to
     * {@link GridUriDeploymentSpringDocument}.
     * <p>
     * This is a workaround for the {@link InputStreamResource} which does
     * not work properly.
     *
     * @param in Input stream with XML.
     * @param log Logger
     * @return Grid wrapper for the input stream.
     * @throws GridSpiException Thrown if incoming input stream could not be
     *      read or parsed by <tt>Spring</tt> {@link XmlBeanFactory}.
     * @see XmlBeanFactory
     */
    static GridUriDeploymentSpringDocument parseTasksDocument(InputStream in, GridLogger log) throws
        GridSpiException {
        assert in != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringParser.java]";

        // Note: use ByteArrayResource instead of InputStreamResource because InputStreamResource doesn't work.
        ByteArrayOutputStream out  = new ByteArrayOutputStream();

        try {
            GridUtils.copy(in, out);

            XmlBeanFactory factory = new XmlBeanFactory(new ByteArrayResource(out.toByteArray()));

            return new GridUriDeploymentSpringDocument(factory);
        }
        catch (BeansException e) {
            throw (GridSpiException)new GridSpiException("Failed to parse spring XML file.", e).setData(77, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringParser.java");
        }
        catch (IOException e) {
            throw (GridSpiException)new GridSpiException("Failed to parse spring XML file.", e).setData(80, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringParser.java");
        }
        finally{
            GridUtils.close(out, log);
        }
    }
}
