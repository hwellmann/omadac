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

package org.gridgain.jsr305;

import java.lang.annotation.*;

import org.gridgain.apache.*;

/**
 * The presence of this annotation on a method parameter indicates that <tt>null</tt> is an 
 * acceptable value for that parameter. See <a href="http://google-collections.googlecode.com/svn/trunk/javadoc/com/google/common/base/Nullable.html">Google Collections</a>
 * or <a href="http://jcp.org/en/jsr/detail?id=305">JSR-305</a> for more information. 
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Apache20LicenseCompatible
public @interface Nullable {
    // No-op.
}
