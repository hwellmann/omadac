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

package org.gridgain.grid.gridify.aop.spring;

import org.gridgain.grid.gridify.*;
import org.springframework.aop.framework.*;
import org.springframework.aop.support.*;

/**
 * Spring AOP enhancer. Use it to grid-enable methods annotated with
 * {@link Gridify} annotation.
 * <p>
 * Note, that Spring AOP requires that all grid-enabled methods must
 * be <tt>enhanced</tt> because it is proxy-based. Other AOP implementations, 
 * such as JBoss or AspectJ don't require special handling.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * <tt>gridified</tt> methods. 
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see Gridify
 */
public final class GridifySpringEnhancer {
    /** Spring aspect. */
    private static final GridifySpringAspect aspect = new GridifySpringAspect();
    
    /**
     * Enforces singleton.
     */
    private GridifySpringEnhancer() {
        // No-op.
    }

    /**
     * Enhances the object on load.
     * 
     * @param <T> Type of the object to enhance.
     * @param obj Object to augment/enhance.
     * @return Enhanced object.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T enhance(T obj) {
        ProxyFactory proxyFactory = new ProxyFactory(obj);

        proxyFactory.addAdvice(aspect);

        while (proxyFactory.getAdvisors().length > 0) {
            proxyFactory.removeAdvisor(0);
        }

        proxyFactory.addAdvisor(new DefaultPointcutAdvisor(new GridifySpringPointcut(), aspect));

        return (T)proxyFactory.getProxy();
    }
}
