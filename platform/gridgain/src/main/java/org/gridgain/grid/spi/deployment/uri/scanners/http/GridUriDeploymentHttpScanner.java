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

package org.gridgain.grid.spi.deployment.uri.scanners.http;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.uri.scanners.*;
import org.gridgain.grid.spi.deployment.uri.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.w3c.dom.*;
import org.w3c.tidy.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridUriDeploymentHttpScanner extends GridUriDeploymentScanner {
    /** Secure socket protocol to use. */
    private static final String PROTOCOL = "TLS";

    /** */
    @GridToStringExclude
    private URL scanDir = null;

    /** Cache of found files to check if any of it has been updated. */
    private Map<String, Long> tstampCache = new HashMap<String, Long>();

    /** */
    private final Tidy tidy;

    /** Outgoing data SSL socket factory. */
    private SSLSocketFactory sockFactory = null;

    /**
     *
     * @param gridName Grid instance name.
     * @param uri HTTP URI.
     * @param deployDir Deployment directory.
     * @param freq Scanner frequency.
     * @param filter Filename filter.
     * @param listener Deployment listener.
     * @param log Logger to use.
     * @throws GridSpiException Thrown in case of any error.
     */
    public GridUriDeploymentHttpScanner(
        String gridName,
        URI uri,
        File deployDir,
        long freq,
        FilenameFilter filter,
        GridUriDeploymentScannerListener listener,
        GridLogger log) throws GridSpiException {
        super(gridName, uri, deployDir, freq, filter, listener, log);

        initialize(uri);

        tidy = new Tidy();

        tidy.setQuiet(true);
        tidy.setOnlyErrors(true);
        tidy.setShowWarnings(false);
        tidy.setCharEncoding(Configuration.UTF8);
   }

    /**
     *
     * @param uri HTTP URI.
     * @throws GridSpiException Thrown in case of any error.
     */
    private void initialize(URI uri) throws GridSpiException {
        assert "http".equals(uri.getScheme()) == true || "https".equals(uri.getScheme()) == true : "ASSERTION [line=99, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java]";

        try {
            scanDir = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath());
        }
        catch (MalformedURLException e) {
            scanDir = null;

            throw (GridSpiException)new GridSpiException("Wrong value for scanned HTTP directory with URI: " + uri, e).setData(107, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java");
        }

        try {
            if ("https".equals(uri.getScheme()) == true) {
                // Set up socket factory to do authentication.
                SSLContext ctx = SSLContext.getInstance(PROTOCOL);

                ctx.init(null, getTrustManagers(), null);

                sockFactory = ctx.getSocketFactory();
            }
        }
        catch (NoSuchAlgorithmException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize SSL context. URI: " + uri, e).setData(121, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java");
        }
        catch (KeyManagementException e) {
            throw (GridSpiException)new GridSpiException("Failed to initialize SSL context. URI:" + uri, e).setData(124, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() {
        Set<String> foundFiles = new HashSet<String>(tstampCache.size());

        long start = System.currentTimeMillis();

        processHttp(foundFiles);

        if (getLogger().isDebugEnabled() == true) {
            getLogger().debug("HTTP scanner time in ms: " + (System.currentTimeMillis() - start));
        }

        if (isFirstScan() == false) {
            Set<String> deletedFiles = new HashSet<String>(tstampCache.keySet());

            deletedFiles.removeAll(foundFiles);

            if (deletedFiles.size() > 0) {
                List<String> uris = new ArrayList<String>();

                for (String file : deletedFiles) {
                    uris.add(getFileUri(getFileName(file)));
                }

                tstampCache.keySet().removeAll(deletedFiles);

                getListener().onDeletedFiles(uris);
            }
        }
    }

    /**
     *
     * @param files Files to process.
     */
    private void processHttp(Set<String> files) {
        Set<String> urls = getUrls(scanDir);

        for (String url : urls) {
            String fileName = getFileName(url);

            if (getFilter().accept(null, fileName) == true) {
                files.add(url);

                Long lastModified = tstampCache.get(url);

                InputStream in = null;
                OutputStream out = null;

                File file = null;

                try {
                    URLConnection conn = new URL(url).openConnection();

                    if (conn instanceof HttpsURLConnection == true) {
                        HttpsURLConnection httpsConn = (HttpsURLConnection)conn;

                        httpsConn.setHostnameVerifier(new DeploymentHostnameVerifier());

                        assert sockFactory != null : "ASSERTION [line=190, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java]";

                        // Initialize socket factory.
                        httpsConn.setSSLSocketFactory(sockFactory);
                    }

                    if (lastModified != null) {
                        conn.setIfModifiedSince(lastModified);
                    }

                    in = conn.getInputStream();

                    long rcvLastModified = conn.getLastModified();

                    if (in == null || lastModified != null && (lastModified == rcvLastModified ||
                        conn instanceof HttpURLConnection == true &&
                        ((HttpURLConnection)conn).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED)) {
                        continue;
                    }

                    tstampCache.put(url, rcvLastModified);

                    lastModified = rcvLastModified;

                    if (getLogger().isDebugEnabled() == true) {
                        getLogger().debug("Discovered deployment file or directory: " +
                            GridUriDeploymentUtils.hidePassword(url));
                    }

                    file = createTempFile(fileName, getDeployDirectory());

                    // Delete file when JVM stopped.
                    file.deleteOnExit();

                    out = new FileOutputStream(file);

                    GridUtils.copy(in, out);
                }
                catch (IOException e) {
                    getLogger().error("Failed to save file: " + fileName, e);
                }
                finally {
                    GridUtils.close(in, getLogger());
                    GridUtils.close(out, getLogger());
                }

                if (file != null && file.exists() == true && file.length() > 0) {
                    getListener().onNewOrUpdatedFile(file, getFileUri(fileName), lastModified);
                }
            }
        }
    }

    /**
     *
     * @param node XML element node.
     * @param res Set of URLs in string format to populate.
     * @param baseUrl Base URL.
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    private void findReferences(org.w3c.dom.Node node, Set<String> res, URL baseUrl) {
        if (node instanceof Element == true && "a".equals(node.getNodeName().toLowerCase()) == true) {
            Element element = (Element)node;

            String href = element.getAttribute("href");

            if (href != null && href.length() > 0) {
                URL url = null;

                try {
                    url = new URL(href);
                }
                catch (MalformedURLException e) {
                    try {
                        url = new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(),
                            href.charAt(0) == '/' ? href : baseUrl.getFile() + '/' + href);
                    }
                    catch (MalformedURLException e1) {
                        getLogger().error("Skipping bad URL: " + url, e1);
                    }
                }

                if (url != null) {
                    res.add(url.toString());
                }
            }
        }

        NodeList childNodes = node.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            findReferences(childNodes.item(i), res, baseUrl);
        }
    }

    /**
     *
     * @param url Base URL.
     * @return Set of referenced URLs in string format.
     */
    private Set<String> getUrls(URL url) {
        assert url != null : "ASSERTION [line=291, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java]";

        InputStream in = null;

        Set<String> urls = new HashSet<String>();

        Document dom = null;

        try {
            URLConnection conn = url.openConnection();

            if (conn instanceof HttpsURLConnection == true) {
                HttpsURLConnection httpsConn = (HttpsURLConnection)conn;

                httpsConn.setHostnameVerifier(new DeploymentHostnameVerifier());

                assert sockFactory != null : "ASSERTION [line=307, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java]";

                // Initialize socket factory.
                httpsConn.setSSLSocketFactory(sockFactory);
            }

            in = conn.getInputStream();

            if (in == null) {
                throw new IOException("Failed to open connection: " +
                    GridUriDeploymentUtils.hidePassword(url.toString()));
            }

            dom = tidy.parseDOM(in, null);
        }
        catch (IOException e) {
            getLogger().error("Failed to get HTML page: " + GridUriDeploymentUtils.hidePassword(url.toString()), e);
        }
        finally{
            GridUtils.close(in, getLogger());
        }

        if (dom != null) {
            findReferences(dom, urls, url);
        }

        return urls;
    }

    /**
     *
     * @param url Base URL string format.
     * @return File name extracted from <tt>url</tt> string format.
     */
    private String getFileName(String url) {
        assert url != null : "ASSERTION [line=342, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/http/GridUriDeploymentHttpScanner.java]";

        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * Construct array with one trust manager which don't reject input certificates.
     *
     * @return Array with one X509TrustManager implementation of trust manager.
     */
    private TrustManager[] getTrustManagers() {
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
                    StringBuilder buf = new StringBuilder();

                    buf.append("Trust manager handle client certificates [authType=");
                    buf.append(authType);
                    buf.append(", certificates=");

                    for (X509Certificate cert : certs) {
                        buf.append("{type=");
                        buf.append(cert.getType());
                        buf.append(", principalName=");
                        buf.append(cert.getSubjectX500Principal().getName());
                        buf.append('}');
                    }

                    buf.append(']');

                    if (getLogger().isDebugEnabled() == true) {
                        getLogger().debug(buf.toString());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    StringBuilder buf = new StringBuilder();

                    buf.append("Trust manager handle server certificates [authType=");
                    buf.append(authType);
                    buf.append(", certificates=");

                    for (X509Certificate cert : certs) {
                        buf.append("{type=");
                        buf.append(cert.getType());
                        buf.append(", principalName=");
                        buf.append(cert.getSubjectX500Principal().getName());
                        buf.append('}');
                    }

                    buf.append(']');

                    if (getLogger().isDebugEnabled() == true) {
                        getLogger().debug(buf.toString());
                    }
                }
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentHttpScanner.class, this,
            "scanDir", scanDir != null ? GridUriDeploymentUtils.hidePassword(scanDir.toString()) : null);
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
