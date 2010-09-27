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
 * Annotates a special methods inside injected user-defined resource {@link GridUserResource}.
 * It can be used in any injected resource for grid tasks and grid jobs. Typically the method with this
 * annotation will be used for initialization of the injectable resource such as opening database connection,
 * network connection or reading configuration settings. Note that this method is called after the resource
 * itself has been injected with all its resources, if any.
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
 *     &#64;GridUserResourceOnDeployed                  
 *     private void deploy() {                      
 *         log.info("Deploying resource: " + this);
 *     }                                            
 *     ...                                          
 * }  
 * </pre>
 * <p>
 * See also {@link GridUserResourceOnUndeployed} for undeployment callbacks.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Apache20LicenseCompatible
public @interface GridUserResourceOnDeployed {
    // No-op.
}
