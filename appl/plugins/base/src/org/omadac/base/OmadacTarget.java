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
package org.omadac.base;

import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.make.Target;

/**
 * Base class for Omadac targets. Decorates a Target with Omadac runtime configuration.
 * @author hwellmann
 *
 */
public abstract class OmadacTarget extends Target
{
    private static final long serialVersionUID = 1L;

    public OmadacSettings getConfiguration()
    {
        ExecutionContextImpl ctxImpl = getExecutionContext().as(ExecutionContextImpl.class);
        OmadacSettings config = ctxImpl.getConfigManager().getConfiguration();
        return config;
    }
}
