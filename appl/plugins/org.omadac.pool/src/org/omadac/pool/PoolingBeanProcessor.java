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
package org.omadac.pool;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.blueprint.BeanProcessor;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.commons.pool.ObjectPool;
import org.omadac.pool.impl.PoolingAdapter;
import org.omadac.pool.impl.PoolingInvocationHandler;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PoolingBeanProcessor implements BeanProcessor {

    private static Logger log = LoggerFactory.getLogger(PoolingBeanProcessor.class);

    /** Maps bean names to object pools. */
    private Map<String, ObjectPool> poolMap = new HashMap<String, ObjectPool>();

    public Object beforeInit(Object bean, String beanName, BeanCreator beanCreator,
            BeanMetadata beanData) {
        return bean;
    }

    public Object afterInit(Object bean, String beanName, BeanCreator beanCreator,
            BeanMetadata beanData) {
        if (! bean.getClass().isAnnotationPresent(Pooled.class)) {
            return bean;
        }
        
        log.debug("creating pooled proxy for {}", beanName);
        ExecutionContext context = ExecutionContext.Holder.getContext();
        
        PoolingAdapter poolingWrapper = new PoolingAdapter(beanCreator, context);
        ObjectPool objectPool = poolingWrapper.getObjectPool();
        poolMap.put(beanName, objectPool);
        PoolingInvocationHandler ih = new PoolingInvocationHandler(objectPool);
        Object proxy = Proxy.newProxyInstance(bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(), ih);
        return proxy;
    }

    public void beforeDestroy(Object bean, String beanName) {
        ObjectPool objectPool = poolMap.remove(beanName);
        if (objectPool == null) {
            return;
        }
        try {
            log.debug("closing pool for {}", beanName);
            objectPool.close();
        }
        catch (Exception exc) {
            log.error("cannot close ObjectPool", exc);
        }
    }

    public void afterDestroy(Object bean, String beanName) {
        // empty
    }
}
