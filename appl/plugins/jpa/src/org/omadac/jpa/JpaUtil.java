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
package org.omadac.jpa;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;

public class JpaUtil
{
    private static ScopedEntityManagerFactory factory;

    public void setEntityManagerFactory(EntityManagerFactory emf)
    {
        JpaUtil.factory = new ScopedEntityManagerFactory(emf);
    }

    public static EntityManagerFactory getEntityManagerFactory()
    {
        return factory;
    }

    public static EntityManager getCurrentEntityManager()
    {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        if (!transaction.isActive())
        {
            transaction.begin();
        }
        return entityManager;
    }

    public static EntityManager getCurrentEntityManager(EntityManagerFactory emf)
    {
        EntityManager entityManager = emf.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        if (!transaction.isActive())
        {
            transaction.begin();
        }
        return entityManager;
    }

    public static EntityManager getNewEntityManager()
    {
        EntityManager entityManager = factory.getDelegate().createEntityManager();

        EntityTransaction transaction = entityManager.getTransaction();
        if (!transaction.isActive())
        {
            transaction.begin();
        }
        return entityManager;
    }

    public static Connection getConnection()
    {
        EntityManager entityManager = getCurrentEntityManager();
        return getConnection(entityManager);
    }

    public static Connection getConnection(EntityManager entityManager)
    {
        entityManager.clear();
        OpenJPAEntityManager em = OpenJPAPersistence.cast(entityManager);
        Connection connection = (Connection) em.getConnection();

        return connection;
    }

    public static MetadataInspector getMetadataInspector(EntityManagerFactory emf)
    {
        Connection dbc = getConnection();
        return new MetadataInspector(dbc);
    }

    public static MetadataInspector getMetadataInspector(EntityManager em)
    {
        Connection dbc = getConnection(em);
        return new MetadataInspector(dbc);
    }

    public static void commit(Connection dbc)
    {
        try
        {
            dbc.commit();
        }
        catch (SQLException e)
        {
            throw new JpaException();
        }
    }

    public static void commit()
    {
        EntityManager entityManager = getCurrentEntityManager();
        entityManager.getTransaction().commit();
    }

    public static void rollback(Connection dbc)
    {
        try
        {
            dbc.rollback();
        }
        catch (SQLException e)
        {
            throw new JpaException();
        }
    }

    public static <T> T executeTransaction(EntityManagerFactory emf, TxCallable<T> work)
    {
        EntityManager em = emf.createEntityManager();
        return executeTransaction(em, work);
    }

    public static void executeTransaction(EntityManagerFactory emf, TxRunnable work)
    {
        EntityManager em = emf.createEntityManager();
        executeTransaction(em, work);
    }

    public static <T> T executeTransaction(EntityManager em, TxCallable<T> work)
    {
        EntityTransaction transaction = em.getTransaction();
        try
        {
            transaction.begin();

            return work.run(em);
        }
        // CHECKSTYLE:OFF
        catch (RuntimeException exc)
        // CHECKSTYLE:ON
        {
            transaction.rollback();
            throw exc;
        } 
        finally
        {
            finishTransaction(em, transaction);
        }
    }

    public static void executeTransaction(EntityManager em, TxRunnable work)
    {

        EntityTransaction transaction = em.getTransaction();
        try
        {
            transaction.begin();
            work.run(em);
        }
        // CHECKSTYLE:OFF
        catch (RuntimeException exc)
        // CHECKSTYLE:ON
        {
            //transaction.rollback();
            throw exc;
        } 
        finally
        {
            finishTransaction(em, transaction);
        }
    }

    private static void finishTransaction(EntityManager em, EntityTransaction transaction)
    {
        if (transaction.isActive() && !transaction.getRollbackOnly())
        {
            try
            {
                transaction.commit();
            }
            // CHECKSTYLE:OFF
            catch (RuntimeException exc)
            // CHECKSTYLE:ON
            {
                transaction.rollback();
                throw exc;
            }
        }
        em.close();
    }
}