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

package org.gridgain.grid.util;

/**
 * Per-thread cache of {@link StringBuilder} instances.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridStringBuilderFactory {
    /** Cache string builders per thread for better performance. */
    private static ThreadLocal<CachedBuilder> builders = new ThreadLocal<CachedBuilder>() {
        /**
         * {@inheritDoc}
         */
        @Override
        protected CachedBuilder initialValue() {
            return new CachedBuilder();
        }
    };

    /**
     * Acquires a cached instance of {@link StringBuilder} if
     * current thread is not using it yet. Otherwise a new
     * instance of string builder is returned.
     *
     * @return Cached instance of {@link StringBuilder}.
     */
    public static StringBuilder acquire() {
        return builders.get().acquire();
    }

    /**
     * Releases {@link StringBuilder} back to cache.
     *
     * @param builder String builder to release.
     */
    public static void release(StringBuilder builder) {
        builders.get().release(builder);
    }

    /**
     * No-op constructor to ensure singleton.
     */
    private GridStringBuilderFactory() {
        // No-op.
    }

    /**
     * Cached builder.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    static class CachedBuilder {
        /** Cached builder. */
        private StringBuilder builder = new StringBuilder();

        /** <tt>True</tt> if already used by a thread. */
        private boolean used = false;

        /**
         * @return The cached builder.
         */
        public StringBuilder acquire() {
            // If cached instance is already used, then we don't optimize.
            // Simply return a new StringBuilder in such case.
            if (used == true) {
                return new StringBuilder();
            }

            used = true;

            return builder;
        }

        /**
         * Releases builder for reuse.
         *
         * @param builder Builder to release.
         */
        @SuppressWarnings({"ObjectEquality"})
        public void release(StringBuilder builder) {
            if (this.builder == builder) {
                builder.setLength(0);

                used = false;
            }
        }
    }
}
