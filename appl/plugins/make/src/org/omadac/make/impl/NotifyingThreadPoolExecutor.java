/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2010, Harald Wellmann and Contributors
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
package org.omadac.make.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NotifyingThreadPoolExecutor extends ThreadPoolExecutor
{
    private ThreadPoolJobManager manager;

    public NotifyingThreadPoolExecutor(int numThreads, ThreadPoolJobManager jobManager)
    {
        super(numThreads, numThreads, Long.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        this.manager = jobManager;
    }

    @Override
    protected synchronized void afterExecute(Runnable r, Throwable t)
    {
        manager.afterExecute(r, t);
    }
}
