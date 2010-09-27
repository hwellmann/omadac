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
import java.net.*;
import java.security.cert.*;
import java.security.*;
import javax.net.ssl.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridUrlHelper {
    /** Secure socket protocol to use. */
    private static final String HTTPS_PROTOCOL = "TLS";

    /**
     * Enforces singleton.
     */
    private GridUrlHelper() {
        // No-op.
    }

    /**
     * Download resource by URL.
     *
     * @param url URL to download.
     * @param file File where downloaded resource should be stored.
     * @return File where downloaded resource should be stored.
     * @throws IOException If error occurred.
     */
    public static File downloadUrl(URL url, File file) throws IOException {
        assert url != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/util/GridUrlHelper.java]";
        assert file != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/util/GridUrlHelper.java]";

        InputStream in = null;
        OutputStream out = null;

        try {
            URLConnection conn = url.openConnection();

            if (conn instanceof HttpsURLConnection == true) {
                HttpsURLConnection httpsConn = (HttpsURLConnection)conn;

                httpsConn.setHostnameVerifier(new DeploymentHostnameVerifier());

                SSLContext ctx = SSLContext.getInstance(HTTPS_PROTOCOL);

                ctx.init(null, getTrustManagers(), null);

                SSLSocketFactory sockFactory = ctx.getSocketFactory();


                // Initialize socket factory.
                httpsConn.setSSLSocketFactory(sockFactory);
            }

            in = conn.getInputStream();

            if (in == null) {
                throw new IOException("Failed to open connection: " + url.toString());
            }

            out = new BufferedOutputStream(new FileOutputStream(file));

            GridUtils.copy(in, out);
        }
        catch (NoSuchAlgorithmException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new IOException("Failed to open https connection [url=" + url.toString() + ", msg=" + e + ']');
        }
        catch (KeyManagementException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new IOException("Failed to open https connection [url=" + url.toString() + ", msg=" + e + ']');
        }
        finally {
            GridUtils.close(in, null);
            GridUtils.close(out, null);
        }

        return file;
    }

    /**
     * Construct array with one trust manager which don't reject input certificates.
     *
     * @return Array with one X509TrustManager implementation of trust manager.
     */
    private static TrustManager[] getTrustManagers() {
        return new TrustManager[]{
            new X509TrustManager() {
                /**
                 * {@inheritDoc}
                 */
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                /**
                 * {@inheritDoc}
                 */
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // No-op.
                }

                /**
                 * {@inheritDoc}
                 */
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // No-op.
                }
            }
        };
    }


    /**
     * Verifier always return successful result for any host.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    static class DeploymentHostnameVerifier implements HostnameVerifier {
        /**
         * {@inheritDoc}
         */
        public boolean verify(String hostname, SSLSession session) {
            // Remote host trusted by default.
            return true;
        }
    }
}
