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

package org.gridgain.grid.spi.deployment.uri.util;

import java.util.regex.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridUriDeploymentUtils {
    /**
     * Enforces singleton.
     */
    private GridUriDeploymentUtils() {
        // No-op.
    }

    /**
     * Replace password in URI string with character '*'.
     * <p>
     * Parses given URI by applying ".*[:][/](.*[:].*)[@]{1}.*"
     * regular expression pattern and than if URI matches it
     * replaces password strings between '/' and '@' with '*'.
     *
     * @param uri URI which password should be replaced.
     * @return Converted URI string
     */
    public static String hidePassword(String uri) {
        if (uri == null) {
            return null;
        }

        if (Pattern.matches(".*[:][/](.*[:].*)[@]{1}.*", uri) == true) {
            int userInfoLastIdx = uri.indexOf('@');

            assert userInfoLastIdx != -1 : "ASSERTION [line=58, file=src/java/org/gridgain/grid/spi/deployment/uri/util/GridUriDeploymentUtils.java]";

            String str = uri.substring(0, userInfoLastIdx);

            int userInfoStartIdx = str.lastIndexOf('/');

            str = str.substring(userInfoStartIdx + 1);

            String[] params = str.split(";");

            int idx = -1;

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < params.length; i++) {
                if ((idx = params[i].indexOf(':')) != -1) {
                    params[i] = params[i].substring(0, idx + 1) + '*';
                }

                builder.append(params[i]);

                if (i != params.length - 1) {
                    builder.append(';');
                }
            }

            return new StringBuilder(uri).replace(userInfoStartIdx, userInfoLastIdx, builder.toString()).toString();
        }

        return uri;
    }
}
