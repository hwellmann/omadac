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
package org.omadac.make;

import javax.persistence.EntityManagerFactory;

/**
 * The execution context for a target provides external resources to be used by target
 * implementations.
 * 
 * @author hwellmann
 *
 */
public interface ExecutionContext
{
    /**
     * Returns the entity manager factory for the product persistence unit. 
     * @return product EMF
     */
    EntityManagerFactory getProductEntityManagerFactory();

    /**
     * Returns the entity manager factory for the engine persistence unit.
     * For internal use within the make engine only. 
     * @return engine EMF
     */
    EntityManagerFactory getEngineEntityManagerFactory();
    
    <T> T as(Class<T> klass);
}
