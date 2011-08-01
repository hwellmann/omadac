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

import org.apache.commons.pool.BasePoolableObjectFactory;

public class PoolableServiceFactory extends BasePoolableObjectFactory {
    
    private Object prototype;

    public PoolableServiceFactory(Object prototype) {
        this.prototype = prototype;
    }

    @Override
    public Object makeObject() throws Exception {
        return prototype.getClass().newInstance();
    }
}
