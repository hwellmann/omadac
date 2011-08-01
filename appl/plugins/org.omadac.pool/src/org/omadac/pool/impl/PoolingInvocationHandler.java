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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.pool.ObjectPool;

public final class PoolingInvocationHandler implements InvocationHandler {
    private ObjectPool objectPool;

    public PoolingInvocationHandler(ObjectPool objectPool) {
        this.objectPool = objectPool;
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        boolean inInvoke = false;
        try {
            Object target = objectPool.borrowObject();
            inInvoke = true;
            result = method.invoke(target, args);
            inInvoke = false;
            objectPool.returnObject(target);
        }
        catch (Throwable exc) {
            Throwable exceptionToRethrow = null;
            if (!inInvoke) {
                exceptionToRethrow = exc;
            }
            else {
                if (exc instanceof InvocationTargetException) {
                    exc = ((InvocationTargetException) exc).getTargetException();
                }

                if (!(exc instanceof RuntimeException)) {
                    exceptionToRethrow = exc;
                }
            }
            if (exceptionToRethrow == null) {
                exceptionToRethrow = exc;
            }
            throw exceptionToRethrow;
        }
        return result;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
            return null;
        }

        return doInvoke(proxy, method, args);
    }
}
