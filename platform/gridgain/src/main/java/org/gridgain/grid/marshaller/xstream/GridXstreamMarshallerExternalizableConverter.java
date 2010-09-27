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

import com.thoughtworks.xstream.converters.reflection.*;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.mapper.*;
import com.thoughtworks.xstream.io.*;
import com.thoughtworks.xstream.core.util.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * This converter is needed, because default XStream converter does not work
 * with non-public classes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridXstreamMarshallerExternalizableConverter extends ExternalizableConverter {
    /** Mapper. */
    private final Mapper mapper;

    /**
     * @param mapper Xstream mapper.
     */
    GridXstreamMarshallerExternalizableConverter(Mapper mapper) {
        super(mapper);

        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext ctx) {
        final Class type = ctx.getRequiredType();

        try {
            Constructor constructor = type.getConstructor();

            // Override non-public access.
            constructor.setAccessible(true);

            final Externalizable obj = (Externalizable)constructor.newInstance();

            CustomObjectInputStream.StreamCallback callback = new CustomObjectInputStream.StreamCallback() {
                /**
                 * {@inheritDoc}
                 */
                public Object readFromStream() {
                    reader.moveDown();

                    Object streamItem = ctx.convertAnother(obj, mapper.realClass(reader.getNodeName()));

                    reader.moveUp();

                    return streamItem;
                }

                /**
                 * {@inheritDoc}
                 */
                public Map readFieldsFromStream() {
                    throw new UnsupportedOperationException();
                }

                /**
                 * {@inheritDoc}
                 */
                public void defaultReadObject() {
                    throw new UnsupportedOperationException();
                }

                /**
                 * {@inheritDoc}
                 */
                public void registerValidation(ObjectInputValidation validation, int priority)
                    throws NotActiveException {
                    throw new NotActiveException("Stream is inactive.");
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    throw new UnsupportedOperationException("Objects are not allowed to call ObjectInput.close() " +
                        "from readExternal()");
                }
            };

            CustomObjectInputStream in = CustomObjectInputStream.getInstance(ctx, callback);

            obj.readExternal(in);

            in.popCallback();

            return obj;
        }
        catch (InstantiationException e) {
            throw new ConversionException("Cannot construct " + type.getClass(), e);
        }
        catch (IllegalAccessException e) {
            throw new ConversionException("Cannot construct " + type.getClass(), e);
        }
        catch (IOException e) {
            throw new ConversionException("Cannot externalize " + type.getClass(), e);
        }
        catch (ClassNotFoundException e) {
            throw new ConversionException("Cannot externalize " + type.getClass(), e);
        }
        catch (NoSuchMethodException e) {
            throw new ConversionException("Cannot externalize " + type.getClass(), e);
        }
        catch (InvocationTargetException e) {
            throw new ConversionException("Cannot externalize " + type.getClass(), e);
        }
    }
}
