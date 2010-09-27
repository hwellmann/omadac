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

package org.gridgain.grid.resources;

import java.lang.annotation.*;
import org.gridgain.apache.*;

/**
 * Annotates a special method inside injected user-defined resources {@link GridUserResource}. This 
 * annotation is typically used to de-initialize user-defined resource. For example, the method with 
 * this annotation can close database connection, or perform certain cleanup. Note that this method
 * will be called before any injected resources on this user-defined resource are cleaned up.
 * <p>
 * Here is how annotation would typically happen:
 * <pre name="code" class="java">
 * public class MyUserResource {                    
 *     ...                                          
 *     &#64;GridLoggerResource                          
 *     private GridLogger log = null;               
 *                                                  
 *     &#64;GridSpringApplicationContextResource        
 *     private ApplicationContext springCtx = null; 
 *     ...                                          
 *     &#64;GridUserResourceOnUndeployed                  
 *     private void deploy() {                      
 *         log.info("Deploying resource: " + this);
 *     }                                            
 *     ...                                          
 * }  
 * </pre>
 * <p>
 * See also {@link GridUserResourceOnDeployed} for deployment callbacks.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Apache20LicenseCompatible
public @interface GridUserResourceOnUndeployed {
    // No-op.
}
