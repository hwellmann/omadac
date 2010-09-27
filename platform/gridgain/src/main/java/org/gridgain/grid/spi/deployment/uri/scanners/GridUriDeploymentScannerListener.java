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

package org.gridgain.grid.spi.deployment.uri.scanners;

import java.io.*;
import java.util.*;

/**
 * Scanner listener interface. Whatever deployment scanner is used
 * (ftp, http, file and so on) following events happens:
 * <ul>
 * <li><tt>onNewOrUpdatedFile</tt> - happens when new file has been found or updated.</li>
 * <li><tt>onDeletedFiles</tt> - happens when file(s) has been removed.</li>
 * <li><tt>onFirstScanFinished</tt> - happens when scanner completed its first scan.</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridUriDeploymentScannerListener extends EventListener {
    /**
     * Notifies about new file or those one that was updated.
     *
     * @param file New or updated file.
     * @param uri File URI.
     * @param tstamp File modification date.
     */
    public void onNewOrUpdatedFile(File file, String uri, long tstamp);

    /**
     * Notifies about removed files.
     *
     * @param uris List of removed files.
     */
    public void onDeletedFiles(List<String> uris);

    /**
     * Notifies about first scan completion.
     */
    public void onFirstScanFinished();
}
