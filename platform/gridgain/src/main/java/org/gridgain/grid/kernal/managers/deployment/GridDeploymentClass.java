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

package org.gridgain.grid.kernal.managers.deployment;

import java.lang.annotation.*;
import java.util.concurrent.*;
import java.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.*;

/**
 * Represents single class deployment.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDeploymentClass {
    /** */
    private final Class<?> cls;

    /** Deployment mode. */
    private final GridDeploymentMode depMode;

    /** Class loader. */
    private final ClassLoader clsLdr;

    /** Class loader ID. */
    private final UUID clsLdrId;

    /** User version. */
    private final String userVer;

    /** Sequence number. */
    private final long seqNum;

    /** Class alias. */
    private final String alias;

    /** Flag indicating local (non-p2p) deployment. */
    private final boolean local;

    /** */
    private final ConcurrentMap<Class<?>, GridMutable<Annotation>> anns =
        new ConcurrentHashMap<Class<?>, GridMutable<Annotation>>(1);

    /**
     * @param cls Deployed class.
     * @param alias Class alias.
     * @param depMode Deployment mode.
     * @param clsLdr Class loader.
     * @param clsLdrId Class loader ID.
     * @param seqNum Sequence number.
     * @param userVer User version.
     * @param local <tt>True</tt> if local deployment.
     */
    public GridDeploymentClass(Class<?> cls, String alias, GridDeploymentMode depMode, ClassLoader clsLdr,
        UUID clsLdrId, long seqNum, String userVer, boolean local) {
        assert cls != null : "ASSERTION [line=78, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";
        assert depMode != null : "ASSERTION [line=79, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";
        assert clsLdr != null : "ASSERTION [line=80, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";
        assert clsLdrId != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";
        assert userVer != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";
        assert alias != null : "ASSERTION [line=83, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";

        this.cls = cls;
        this.clsLdr = clsLdr;
        this.clsLdrId = clsLdrId;
        this.seqNum = seqNum;
        this.userVer = userVer;
        this.depMode = depMode;
        this.alias = alias;
        this.local = local;
    }

    /**
     * Gets property cls.
     *
     * @return Property cls.
     */
    public Class<?> getDeployedClass() {
        return cls;
    }

    /**
     * Gets property depMode.
     *
     * @return Property depMode.
     */
    public GridDeploymentMode getDeploymentMode() {
        return depMode;
    }

    /**
     * Gets property seqNum.
     *
     * @return Property seqNum.
     */
    public long getSequenceNumber() {
        return seqNum;
    }

    /**
     *
     * @return Class loader.
     */
    public ClassLoader getClassLoader() {
        return clsLdr;
    }

    /**
     * Gets property clsLdrId.
     *
     * @return Property clsLdrId.
     */
    public UUID getClassLoaderId() {
        return clsLdrId;
    }

    /**
     * Gets property userVer.
     *
     * @return Property userVer.
     */
    public String getUserVersion() {
        return userVer;
    }

    /**
     * Gets property alias.
     *
     * @return Property alias.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param name Either class name or alias.
     * @return <tt>True</tt> if name is equal to either class name or alias.
     */
    public boolean hasName(String name) {
        assert name != null : "ASSERTION [line=162, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentClass.java]";

        return cls.getName().equals(name) == true || alias.equals(name) == true;
    }

    /**
     * Gets property local.
     *
     * @return Property local.
     */
    public boolean isLocal() {
        return local;
    }

    /**
     *
     * @param annCls Annotation class.
     * @return Annotation value.
     * @param <T> Annotation class.
     */
    @SuppressWarnings({"unchecked"})
    public <T extends Annotation> T getAnnotation(Class<T> annCls) {
        GridMutable<T> ann = (GridMutable<T>)anns.get(annCls);

        if (ann == null) {
            ann = new GridMutable<T>(GridUtils.getAnnotation(cls, annCls));

            anns.putIfAbsent(annCls, (GridMutable<Annotation>)ann);
        }

        return ann.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentClass.class, this);
    }
}
