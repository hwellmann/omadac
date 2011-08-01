/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2011, Harald Wellmann and Contributors
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.omadac.pool.impl;

import org.apache.aries.blueprint.BeanProcessor.BeanCreator;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.commons.pool.BasePoolableObjectFactory;

public class PoolableBeanCreator extends BasePoolableObjectFactory {
    
    private BeanCreator beanCreator;
    private ExecutionContext context;

    public PoolableBeanCreator(BeanCreator beanCreator, ExecutionContext context) {
        this.beanCreator = beanCreator;
        this.context = context;
    }

    @Override
    public Object makeObject() throws Exception {
        ExecutionContext.Holder.setContext(context);
        return beanCreator.getBean();
    }
}
