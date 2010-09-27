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

package org.gridgain.grid.marshaller.xstream;

import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.converters.*;
import java.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Marshaller that usese <a href="http://xstream.codehaus.org/">XStream</a>
 * to marshal objects. This marshaller does not require objects to implement
 * {@link Serializable}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridXstreamMarshaller implements GridMarshaller {
    /** XStream instance to use with system class loader. */
    @GridToStringExclude
    private final XStream dfltXstream;

    /** Non-serializable grid resources converter. */
    @GridToStringExclude
    private final Converter rsrcConverter = new GridXstreamMarshallerResourceConverter();

    /** Object converter. */
    @GridToStringExclude
    private final Converter objConverter = new GridXstreamMarshallerObjectConverter();

    /**
     * Initializes <tt>XStream</tt> marshaller.
     */
    public GridXstreamMarshaller() {
        dfltXstream = createXstream(getClass().getClassLoader());
    }

    /**
     *
     * @param ldr Class loader for created XStream object.
     * @return created Xstream object.
     */
    private XStream createXstream(ClassLoader ldr) {
        XStream res = new XStream();

        res.registerConverter(new GridXstreamMarshallerExternalizableConverter(res.getMapper()));

        res.registerConverter(rsrcConverter);
        res.registerConverter(objConverter);

        res.setClassLoader(ldr);

        return res;
    }

    /**
     * {@inheritDoc}
     */
    public void marshal(Object obj, OutputStream out) throws GridException {
        dfltXstream.toXML(obj, out);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public <T> T unmarshal(InputStream in, ClassLoader clsLoader) throws GridException {
        if (getClass().getClassLoader().equals(clsLoader) == true) {
            return (T)dfltXstream.fromXML(in);
        }

        XStream xstream = createXstream(clsLoader);

        return (T)xstream.fromXML(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridXstreamMarshaller.class, this);
    }
}
