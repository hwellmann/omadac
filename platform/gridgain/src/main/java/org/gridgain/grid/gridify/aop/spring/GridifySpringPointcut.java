package org.gridgain.grid.gridify.aop.spring;

import java.lang.reflect.*;
import org.gridgain.grid.gridify.*;
import org.springframework.aop.*;

/**
 * Pointcut used by {@link GridifySpringAspect} aspect to find methods
 * annotated with {@link Gridify} annotation.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridifySpringPointcut implements Pointcut {
    /**
     * Class filter.
     *
     *  @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private static final ClassFilter filter = new ClassFilter() {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        public boolean matches(Class cls) {
            return true;
        }
    };

    /** Method matcher. */
    private static final MethodMatcher matcher = new MethodMatcher() {
        /**
         * {@inheritDoc}
         */
        // Warning suppression is due to Spring...
        @SuppressWarnings("unchecked")
        public boolean matches(Method method, Class cls) {
            return cls.isAnnotationPresent(Gridify.class) == true ||
                method.isAnnotationPresent(Gridify.class) == true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isRuntime() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        // Warning suppression is due to Spring...
        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        public boolean matches(Method method, Class aClass, Object[] objs) {
            // No-op.
            return false;
        }
    };

    /**
     * {@inheritDoc}
     */
    public ClassFilter getClassFilter() {
        return filter;
    }

    /**
     * {@inheritDoc}
     */
    public MethodMatcher getMethodMatcher() {
        return matcher;
    }
}
