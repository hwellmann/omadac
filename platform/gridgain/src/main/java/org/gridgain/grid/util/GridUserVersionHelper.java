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

package org.gridgain.grid.util;

import java.io.*;
import java.util.concurrent.atomic.*;
import org.springframework.beans.factory.xml.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.*;
import org.springframework.core.io.*;
import org.gridgain.grid.logger.*;

/**
 * Helper to get user version for given classloader.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridUserVersionHelper {
    /** Path to <tt>gridgain.xml</tt> file. */
    public static final String GRIDGAIN_XML_PATH = "META-INF/gridgain.xml";

    /** Default user version. */
    public static final String DFLT_USER_VERSION = "0";

    /** System class loader user version. */
    private static final AtomicReference<String> SYS_LDR_VER = new AtomicReference<String>(null);

    /**
     * Gets user version for given class loader by checking
     * <tt>META-INF/gridgain.xml</tt> file for <tt>userVersion</tt> attribute. If
     * <tt>gridgain.xml</tt> file is not found, or user version is not specified there,
     * then default version (empty string) is returned.
     *
     * @param ldr Class loader.
     * @param log Logger.
     * @return User version for given class loader or empty string if no version
     *      was explicitly specified.
     */
    public static String getUserVersion(ClassLoader ldr, GridLogger log) {
        assert ldr != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/util/GridUserVersionHelper.java]";

        // For system class loader return cached version.
        //noinspection ObjectEquality
        if (ldr == GridUserVersionHelper.class.getClassLoader() && SYS_LDR_VER.get() != null) {
            return SYS_LDR_VER.get();
        }

        String usrVer = DFLT_USER_VERSION;

        InputStream in = ldr.getResourceAsStream(GRIDGAIN_XML_PATH);

        if (in != null) {
            // Note: use ByteArrayResource instead of InputStreamResource because InputStreamResource doesn't work.
            ByteArrayOutputStream out  = new ByteArrayOutputStream();

            try {
                GridUtils.copy(in, out);

                XmlBeanFactory factory = new XmlBeanFactory(new ByteArrayResource(out.toByteArray()));

                usrVer = (String)factory.getBean("userVersion");

                usrVer = usrVer == null ? DFLT_USER_VERSION : usrVer.trim();
            }
            catch (NoSuchBeanDefinitionException ignored) {
                if (log.isInfoEnabled() == true) {
                    log.info("User version is not explicitly defined (will use default version) [file=" +
                        GRIDGAIN_XML_PATH + ", clsLdr=" + ldr + ']');
                }

                usrVer = DFLT_USER_VERSION;
            }
            catch (BeansException e) {
                String msg = "Failed to parse spring XML file (will use default user version) [file=" +
                    GRIDGAIN_XML_PATH + ", clsLdr=" + ldr + ']';

                if (log == null) {
                    System.err.println(msg);

                    e.printStackTrace();
                }
                else {
                    log.error(msg, e);
                }

                usrVer = DFLT_USER_VERSION;
            }
            catch (IOException e) {
                String msg = "Failed to read spring XML file (will use default user version) [file=" +
                    GRIDGAIN_XML_PATH + ", clsLdr=" + ldr + ']';

                if (log == null) {
                    System.err.println(msg);

                    e.printStackTrace();
                }
                else {
                    log.error(msg, e);
                }

                usrVer = DFLT_USER_VERSION;
            }
            finally{
                GridUtils.close(out, log);
            }
        }

        // For system class loader return cached version.
        //noinspection ObjectEquality
        if (ldr == GridUserVersionHelper.class.getClassLoader()) {
            SYS_LDR_VER.compareAndSet(null, usrVer);
        }

        return usrVer;
    }

    /**
     * Ensure singleton.
     */
    private GridUserVersionHelper() {
        // No-op.
    }
}
