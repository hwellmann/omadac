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

package org.gridgain.grid.kernal;

import java.lang.reflect.*;
import java.util.*;
import javassist.util.proxy.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.*;
import org.springframework.beans.*;

/**
 * Class creates proxy objects based on JavaAssist framework proxies.
 * <p>
 * Every proxy object has an interceptor which is notified about
 * method calls.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridProxyFactory {
    /** */
    private final List<GridProxyListener> listeners = new ArrayList<GridProxyListener>();

    /** */
    private GridLogger log = null;

    /**
     * Creates new instance of proxy factory.
     *
     * @param log Grid logger.
     */
    public GridProxyFactory(GridLogger log) {
        this.log = log;
    }

    /**
     * Gets proxy object for given super class and constructor arguments.
     *
     * @param <T> Type of super class.
     * @param superCls Super class.
     * @param args Super class constructor arguments.
     * @return Proxy object what extends given super class.
     * @throws GridException If any instantiation error occurred.
     */
    @SuppressWarnings({"unchecked"})
    private <T> T makeProxyFor(Class<T> superCls, Object... args) throws GridException {
        assert superCls != null : "ASSERTION [line=67, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";

        ProxyFactory fac = new ProxyFactory();

        fac.setSuperclass(superCls);

        Class<T> cls = fac.createClass();

        T res = null;

        try {
            res = createInstance(cls, args);
        }
        catch (Exception e) {
            throw (GridException)new GridException("Failed to instantiate proxy for " + superCls.getSimpleName(), e).setData(81, "src/java/org/gridgain/grid/kernal/GridProxyFactory.java");
        }

        assert isProxy(res) == true : "ASSERTION [line=84, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";

        // Set handler if it is traceable.
        ((ProxyObject)res).setHandler(new GridMethodHandler(superCls));

        return res;
    }

    /**
     * Creates new proxy object which has given one as a "super object".
     * Proxy has the same super class as a given object class and all
     * properties copied from given object.
     * <p>
     * If object has no constructor with given arguments the
     * same instance is returned.
     *
     * @param <T> Type of the object to get proxy for.
     * @param obj Object instance to be wrapped.
     * @param args Super class constructor arguments.
     * @return Proxy object what extends given super class.
     * @throws GridException If any instantiation error occurred.
     */
    @SuppressWarnings({"unchecked"})
    public <T> T getProxy(T obj, Object... args) throws GridException {
        assert obj != null : "ASSERTION [line=108, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";

        if (isProxy(obj) == true) {
            throw (GridException)new GridException("Failed to create proxy object for another proxy object: " + obj).setData(111, "src/java/org/gridgain/grid/kernal/GridProxyFactory.java");
        }

        T res = null;

        try {
            res = makeProxyFor((Class<T>)obj.getClass(), args);
        }
        catch (GridException e) {
            log.warning("Failed to proxy given instance: " + obj, e);

            return obj;
        }

        BeanUtils.copyProperties(obj, res, obj.getClass());

        return res;
    }

    /**
     * Creates new instance of given class by calling either constructor
     * without parameters if there are no arguments or with given arguments.
     *
     * @param <T> Class type.
     * @param cls Class to be instantiated.
     * @param args Constructor arguments. Note that odd are classes of the
     *      arguments.
     * @return Created instance.
     * @throws Exception Thrown in case of any errors.
     */
    @SuppressWarnings({"MethodWithTooExceptionsDeclared"})
    private <T> T createInstance(Class<T> cls, Object... args) throws Exception {
        T res;

        if (args.length > 0) {
            // Create classes and objects array from arguments.
            Class<?>[] argCls = new Class[args.length/2];
            Object[] argVal = new Object[args.length/2];

            for (int i = 0; i < args.length; i += 2) {
                argVal[i/2] = args[i];
                argCls[i/2] = (Class<?>)args[i + 1];
            }

            Constructor<T> ctor = cls.getConstructor(argCls);

            if (ctor == null) {
                throw new IllegalArgumentException("Failed to instantiate proxy (no matching constructor found " +
                    "in class): " + cls.getName());
            }

            res = ctor.newInstance(argVal);
        }
        else {
            res = cls.newInstance();
        }
        return res;
    }

    /**
     * Tests whether given specified object is a proxy one.
     *
     * @param obj Object to test.
     * @return <tt>true</tt> if given object is a proxy one
     *      created by {@link GridProxyFactory} class.
     */
    public boolean isProxy(Object obj) {
        return obj instanceof ProxyObject == true;
    }

    /**
     * Adds new interception listener.
     *
     * @param l New listener.
     */
    public void addListener(GridProxyListener l) {
        listeners.add(l);
    }

    /**
     * Removes interception listener. If there is no such listener
     * defined than silently ignores it.
     *
     * @param l Listener to be removed.
     */
    public void removeListener(GridProxyListener l) {
        listeners.remove(l);
    }

    /**
     * Internal method invocation handler. It is called by JavaAssist proxy.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private final class GridMethodHandler implements MethodHandler {
        /** Original object class. */
        private final Class<?> origCls;

        /**
         * Creates new instance of method interceptor.
         *
         * @param origCls Original class.
         */
        private GridMethodHandler(Class<?> origCls) {
            this.origCls = origCls;
        }

        /**
         * Receives all notification about methods completion.
         *
         * @param cls Callee class.
         * @param methodName Callee method name.
         * @param args Callee method call parameters.
         * @param res Call result. Might be <tt>null</tt> if call
         *      returned <tt>null</tt> or if exception happened.
         * @param exc Exception thrown by given method call if any.
         */
        private void afterCall(Class<?> cls, String methodName, Object[] args, Object res, Throwable exc) {
            assert cls != null : "ASSERTION [line=230, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";
            assert methodName != null : "ASSERTION [line=231, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";

            for (GridProxyListener l : listeners) {
                l.afterCall(cls, methodName, args, res, exc);
            }
        }

        /**
         * Called right before any traced method call.
         *
         * @param cls Callee class.
         * @param methodName Callee method name.
         * @param args Callee method parameters.
         */
        private void beforeCall(Class<?> cls, String methodName, Object[] args) {
            assert cls != null : "ASSERTION [line=246, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";
            assert methodName != null : "ASSERTION [line=247, file=src/java/org/gridgain/grid/kernal/GridProxyFactory.java]";

            for (GridProxyListener l : listeners) {
                l.beforeCall(cls, methodName, args);
            }
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"ProhibitedExceptionDeclared", "CatchGenericClass", "ProhibitedExceptionThrown"})
        public Object invoke(Object proxy, Method origMethod, Method proxyMethod, Object[] args) throws Throwable {
            beforeCall(origCls, origMethod.getName(), args);

            // Somehow proxy method can be null.
            // We were also confused but this could happen in some cases - just ignore them.
            if (proxyMethod != null) {
                Object res = null;

                Throwable exc = null;

                try {
                    res = proxyMethod.invoke(proxy, args);
                }
                catch (Throwable t) {
                    exc = t;

                    throw t;
                }
                finally {
                    afterCall(origCls, origMethod.getName(), args, res, exc);
                }

                return res;
            }

            return null;
        }
    }
}
