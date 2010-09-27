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

import java.io.Closeable;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.NamingException;

import org.gridgain.grid.GridTask;
import org.gridgain.grid.GridTaskName;
import org.gridgain.grid.logger.GridLogger;
import org.gridgain.grid.spi.GridSpi;
import org.gridgain.grid.util.mbean.GridStandardMBean;
import org.gridgain.grid.util.runnable.GridRunnable;

/**
 * Collection of utility methods used throughout the system.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"UnusedReturnValue"})
public final class GridUtils {
    /** JMX domain as 'xxx.gridgain'. */
    public static final String JMX_DOMAIN = GridUtils.class.getName().substring(0, GridUtils.class.getName().
        indexOf('.', GridUtils.class.getName().indexOf('.') + 1));

    /** Default buffer size = 4K. */
    private static final int BUF_SIZE = 4096;

    /** Byte bit-mask. */
    private static final int MASK = 0xf;

    /** Time span dividers. */
    private static final long[] SPAN_DIVS = new long[] {1000L, 60L, 60L, 60L};

    /** Date format pattern for log messages. */
    private static final String LOG_DATE_FORMAT_PATTERN = "MM/dd/yyyy HH:mm:ss.SSS";

    /**
     * Enforces singleton.
     */
    private GridUtils() {
        // No-op.
    }

    /**
     * Checks whether two objects are equal. Both references can be <tt>null</tt>.
     *
     * @param o1 Object 1 (duh!)
     * @param o2 Object 2 (duh!)
     * @return <tt>true</tt> if both objects are value equal, <tt>false</tt> otherwise. Two
     *      <tt>null</tt>s are considered equal.
     */
    public static boolean equalsWithNulls(Object o1, Object o2) {
        // Could be written shorter - but less readable.
        if (o1 != null) {
            return o1.equals(o2) == true;
        }
        else if (o2 != null) {
            return o2.equals(o1) == true;
        }

        return true;
    }

    /**
     * Formats system time in milliseconds for printing in logs.
     *
     * @param sysTime System time.
     * @return Formatted time string.
     */
    public static String format(long sysTime) {
        return new SimpleDateFormat(LOG_DATE_FORMAT_PATTERN).format(new java.util.Date(sysTime));
    }

    /**
     * Takes given collection, shuffles it and returns iterable instance.
     *
     * @param <T> Type of elements to create iterator for.
     * @param col Collection to shuffle.
     * @return Iterable instance over randomly shuffled collection.
     */
    public static <T> Iterable<T> randomIterable(Collection<T> col) {
        List<T> list = new ArrayList<T>(col);

        Collections.shuffle(list);

        return list;
    }

    /**
     * Converts enumeration to iterable so it can be used in <tt>foreach</tt> construct.
     *
     * @param <T> Types of instances for iteration.
     * @param e Enumeration to convert.
     * @return Iterable over the given enumeration.
     */
    public static <T> Iterable<T> asIterable(final Enumeration<T> e) {
        return e == null ? null : new Iterable<T>() {
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    /**
                     * {@inheritDoc}
                     */
                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings({"IteratorNextCanNotThrowNoSuchElementException"})
                    public T next() {
                        return e.nextElement();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Creates string presentation of given time <tt>span</tt> in hh:mm:ss:msec format.
     *
     * @param span Time span.
     * @return String presentation.
     */
    public static String timeSpan2String(long span) {
        long[] t = new long[4];

        long sp = span;

        for (int i = 0; i < SPAN_DIVS.length && sp > 0; sp /= SPAN_DIVS[i++]) {
            t[i] = sp % SPAN_DIVS[i];
        }

        return (t[3] < 10 ? "0" + t[3] : Long.toString(t[3])) + ':' +
            (t[2] < 10 ? "0" + t[2] : Long.toString(t[2])) + ':' +
            (t[1] < 10 ? "0" + t[1] : Long.toString(t[1])) + ':' +
            (t[0] < 10 ? "0" + t[0] : Long.toString(t[0]));
    }

    /**
     * Copy source file (or folder) to destination file (or folder). Supported source & destination:
     * <ul>
     * <li>File to File</li>
     * <li>File to Folder</li>
     * <li>Folder to Folder</li>
     * </ul>
     *
     * @param src Source file or folder.
     * @param dest Destination file or folder.
     * @param overwrite Whether or not overwrite existing files and folders.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static void copy(File src, File dest, boolean overwrite) throws IOException {
        assert src != null : "ASSERTION [line=190, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert dest != null : "ASSERTION [line=191, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        /*
         * Supported source & destination:
         * ===============================
         * 1. File -> File
         * 2. File -> Directory
         * 3. Directory -> Directory
         */

        // Source must exist.
        if (src.exists() == false) {
            throw new FileNotFoundException("Source can't be found: " + src);
        }

        // Check that source and destination are not the same.
        if (src.getAbsoluteFile().equals(dest.getAbsoluteFile()) == true) {
            throw new IOException("Source and destination are the same [src=" + src + ", dest=" + dest + ']');
        }

        if (dest.exists() == true) {
            if (dest.isDirectory() == false && overwrite == false) {
                throw new IOException("Destination already exists: " + dest);
            }

            if (dest.canWrite() == false) {
                throw new IOException("Destination is not writable:" + dest);
            }
        }
        else {
            File parent = dest.getParentFile();

            if (parent != null && parent.exists() == false) {
                // Ignore any errors here.
                // We will get errors when we'll try to open the file stream.
                parent.mkdirs();
            }

            // If source is a directory, we should create destination directory.
            if (src.isDirectory() == true) {
                dest.mkdir();
            }
        }

        if (src.isDirectory() == true) {
            // In this case we have Directory -> Directory.
            // Note that we copy the content of the directory and not the directory itself.

            File[] files = src.listFiles();

            for (File file : files) {
                if (file.isDirectory() == true) {
                    File dir = new File(dest, file.getName());

                    if (dir.exists() == false && dir.mkdirs() == false) {
                        throw new IOException("Can't create directory: " + dir);
                    }

                    copy(file, dir, overwrite);
                }
                else {
                    copy(file, dest, overwrite);
                }
            }
        }
        else {
            // In this case we have File -> File or File -> Directory.
            File file = null;

            if (dest.exists() == true && dest.isDirectory() == true) {
                // File -> Directory.
                file = new File(dest, src.getName());
            }
            else {
                // File -> File.
                file = dest;
            }

            if (overwrite == false && file.exists() == true) {
                throw new IOException("Destination already exists: " + file);
            }

            FileInputStream in = null;
            FileOutputStream out = null;

            try {
                in = new FileInputStream(src);
                out = new FileOutputStream(file);

                copy(in, out);
            }
            finally {
                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * Copies input byte stream to output byte stream.
     *
     * @param in Input byte stream.
     * @param out Output byte stream.
     * @return Number of the copied bytes.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        assert in != null : "ASSERTION [line=303, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert out != null : "ASSERTION [line=304, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        byte[] buf = new byte[BUF_SIZE];

        int cnt = 0;

        for (int n = 0; (n = in.read(buf)) > 0;) {
            out.write(buf, 0, n);

            cnt += n;
        }

        return cnt;
    }

    /**
     * Copies input character stream to output character stream.
     *
     * @param in Input character stream.
     * @param out Output character stream.
     * @return Number of the copied characters.
     * @throws IOException Thrown if an I/O error occurs.
     */
    public static int copy(Reader in, Writer out) throws IOException {
        assert in != null : "ASSERTION [line=328, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert out != null : "ASSERTION [line=329, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        char[] buf = new char[BUF_SIZE];

        int cnt = 0;

        for (int n = 0; (n = in.read(buf)) > 0;) {
            out.write(buf, 0, n);

            cnt += n;
        }

        return cnt;
    }

    /**
     * Deletes file or directory with all subdirectories and files.
     *
     * @param file File or directory to delete.
     * @return <tt>true</tt> if and only if the file or directory is successfully deleted,
     *      <tt>false</tt> otherwise
     */
    public static boolean delete(File file) {
        assert file != null : "ASSERTION [line=352, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        boolean res = true;

        if (file.isDirectory() == true) {
            File[] files = file.listFiles();

            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    if (file1.isDirectory() == true) {
                        res &= delete(file1);
                    }
                    else {
                        res &= file1.delete();
                    }
                }
            }

            res &= file.delete();
        }
        else {
            res = file.delete();
        }

        return res;
    }

    /**
     * Retrieves <tt>GRIDGAIN_HOME</tt> property. The property is retrieved from system
     * properties or from environment in that order. If path is not absolute and <tt>GRIDGAIN_HOME</tt>
     * has not been set, then <tt>null</tt> is returned.
     *
     * @return <tt>GRIDGAIN_HOME</tt> property.
     */
    public static String getGridGainHome() {
        String home = System.getProperty("GRIDGAIN_HOME");

        if (home == null) {
            home = System.getenv("GRIDGAIN_HOME");
        }

        if (home == null) {
            return null;
        }

        return home;
    }

    /**
     * Gets file representing the path passed in. First the check is made if path is absolute.
     * If not, then the check is made if path is relative to ${GRIDGAIN_HOME}. If both checks fail,
     * then <tt>null</tt> is returned, otherwise file representing path is returned.
     * <p>
     * See {@link #getGridGainHome()} for information on how <tt>GRIDGAIN_HOME</tt> is retrieved.
     *
     * @param path Path to resolve.
     * @return Resolved path, or <tt>null</tt> if file cannot be resolved.
     * @see #getGridGainHome()
     */
    public static File resolveGridGainPath(String path) {
        File file = new File(path);

        if (file.exists() == false) {
            String home = getGridGainHome();

            if (home == null) {
                return null;
            }

            file = new File(home, path);

            return file.exists() == true ? file : null;
        }

        return file;
    }

    /**
     * Converts byte array to formatted string. If calling:
     * <pre name="code" class="java">
     * byte[] data = {10, 20, 30, 40, 50, 60, 70, 80, 90};
     *
     * GridUtils.byteArray2String(data, "0x%02X", ",0x%02X")
     * </pre>
     * the result will be:
     * <pre name="code" class="java">
     *  0x0A, 0x14, 0x1E, 0x28, 0x32, 0x3C, 0x46, 0x50, 0x5A
     * </pre>
     *
     * @param arr Array of byte.
     * @param headerFmt C-style string format for the first element.
     * @param bodyFmt C-style string format for second and following elements, if any.
     * @return String with converted bytes.
     */
    public static String byteArray2String(byte[] arr, String headerFmt, String bodyFmt) {
        assert arr != null : "ASSERTION [line=447, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert headerFmt != null : "ASSERTION [line=448, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert bodyFmt != null : "ASSERTION [line=449, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        StringBuilder buf = new StringBuilder();

        buf.append('{');

        boolean first = true;

        for (byte b : arr) {
            if (first == true) {
                buf.append(String.format(headerFmt, b));

                first = false;
            }
            else {
                buf.append(String.format(bodyFmt, b));
            }
        }

        buf.append('}');

        return buf.toString();
    }

    /**
     * Converts byte array to hex string.
     *
     * @param arr Array of bytes.
     * @return Hex string.
     */
    public static String byteArray2HexString(byte[] arr) {
        StringBuilder buf = new StringBuilder(arr.length << 1);

        for (byte b : arr) {
            buf.append(Integer.toHexString(MASK & b >>> 4)).append(Integer.toHexString(MASK & b));
        }

        return buf.toString().toUpperCase();
    }

    /**
     * Converts primitive double to byte array.
     *
     * @param d Double to convert.
     * @return Byte array.
     */
    public static byte[] doubleToBytes(double d) {
        return longToBytes(Double.doubleToLongBits(d));
    }

    /**
     * Converts primitive <tt>double</tt> type to byte array and stores
     * it in the specified byte array.
     *
     * @param d Double to convert.
     * @param bytes Array of bytes.
     * @param off Offset.
     * @return New offset.
     */
    public static int doubleToBytes(double d, byte[] bytes, int off) {
        return longToBytes(Double.doubleToLongBits(d), bytes, off);
    }

    /**
     * Converts primitive float to byte array.
     *
     * @param f Float to convert.
     * @return Array of bytes.
     */
    public static byte[] floatToBytes(float f) {
        return intToBytes(Float.floatToIntBits(f));
    }

    /**
     * Converts primitive float to byte array.
     *
     * @param f Float to convert.
     * @param bytes Array of bytes.
     * @param off Offset.
     * @return New offset.
     */
    public static int floatToBytes(float f, byte[] bytes, int off) {
        return intToBytes(Float.floatToIntBits(f), bytes, off);
    }

    /**
     * Converts primitive <tt>long</tt> type to byte array.
     *
     * @param l Long value.
     * @return Array of bytes.
     */
    public static byte[] longToBytes(long l) {
        byte[] bytes = new byte[(Long.SIZE >> 3)];

        longToBytes(l, bytes, 0);

        return bytes;
    }

    /**
     * Converts primitive <tt>long</tt> type to byte array and stores it in specified
     * byte array.
     *
     * @param l Long value.
     * @param bytes Array of bytes.
     * @param off Offset in <tt>bytes</tt> array.
     * @return Number of bytes overwritten in <tt>bytes</tt> array.
     */
    public static int longToBytes(long l, byte[] bytes, int off) {
        int bytesCnt = Long.SIZE >> 3;

        for (int i = 0; i < bytesCnt; i++) {
            int shift = (bytesCnt - i - 1) << 3;

            bytes[off++] = (byte)(l >>> shift & 0xff);
        }

        return off;
    }

    /**
     * Converts primitive <tt>int</tt> type to byte array.
     *
     * @param i Integer value.
     * @return Array of bytes.
     */
    public static byte[] intToBytes(int i) {
        byte[] bytes = new byte[(Integer.SIZE >> 3)];

        intToBytes(i, bytes, 0);

        return bytes;
    }

    /**
     * Converts primitive <tt>int</tt> type to byte array and stores it in specified
     * byte array.
     *
     * @param i Integer value.
     * @param bytes Array of bytes.
     * @param off Offset in <tt>bytes</tt> array.
     * @return Number of bytes overwritten in <tt>bytes</tt> array.
     */
    public static int intToBytes(int i, byte[] bytes, int off) {
        int bytesCnt = Integer.SIZE >> 3;

        for (int j = 0; j < bytesCnt; j++) {
            int shift = (bytesCnt - j - 1) << 3;

            bytes[off++] = (byte)(i >>> shift & 0xff);
        }

        return off;
    }

    /**
     * Constructs <tt>int</tt> from byte array.
     *
     * @param bytes Array of bytes.
     * @param off Offset in <tt>bytes</tt> array.
     * @return Integer value.
     */
    public static int bytesToInt(byte[] bytes, int off) {
        assert bytes != null : "ASSERTION [line=612, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        int bytesCnt = Integer.SIZE >> 3;

        assert off + bytesCnt <= bytes.length : "ASSERTION [line=616, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        int res = 0;

        for (int i = 0; i < bytesCnt; i++) {
            int shift = (bytesCnt - i - 1) << 3;

            res |= (0xffL & bytes[off++]) << shift;
        }

        return res;
    }

    /**
     * Constructs <tt>long</tt> from byte array.
     *
     * @param bytes Array of bytes.
     * @param off Offset in <tt>bytes</tt> array.
     * @return Long value.
     */
    public static long bytesToLong(byte[] bytes, int off) {
        assert bytes != null : "ASSERTION [line=637, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        int bytesCnt = Long.SIZE >> 3;

        assert off + bytesCnt <= bytes.length : "ASSERTION [line=641, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        long res = 0;

        for (int i = 0; i < bytesCnt; i++) {
            int shift = (bytesCnt - i - 1) << 3;

            res |= (0xffL & bytes[off++]) << shift;
        }

        return res;
    }

    /**
     * Constructs double from byte array.
     *
     * @param bytes Byte array.
     * @param off Offset in <tt>bytes</tt> array.
     * @return Double value.
     */
    public static double bytesToDouble(byte[] bytes, int off) {
        return Double.longBitsToDouble(bytesToLong(bytes, off));
    }

    /**
     * Constructs float from byte array.
     *
     * @param bytes Byte array.
     * @param off Offset in <tt>bytes</tt> array.
     * @return Float value.
     */
    public static float bytesToFloat(byte[] bytes, int off) {
        return Float.intBitsToFloat(bytesToInt(bytes, off));
    }

    /**
     * Checks for containment of the value in the array.
     * Both array cells and value may be <tt>null</tt>. Two <tt>null</tt>s are considered equal.
     *
     * @param arr Array of objects.
     * @param val Value to check for containment inside of array.
     * @return <tt>true</tt> if contains object, <tt>false</tt> otherwise.
     */
    public static boolean containsObjectArray(Object[] arr, Object val) {
        assert arr != null : "ASSERTION [line=685, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        for (Object o : arr) {
            // If both are nulls, then they are equal.
            if (o == null && val == null) {
                return true;
            }

            // Only one is null and the other one isn't.
            if (o == null || val == null) {
                return false;
            }

            // Both are not nulls.
            if (o.equals(val) == true) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks for containment of given string value in the specified array.
     * Array's cells and string value can be <tt>null</tt>. Tow <tt>null</tt>s are considered equal.
     *
     * @param arr Array of strings.
     * @param val Value to check for containment inside of array.
     * @param ignoreCase Ignoring case if <tt>true</tt>.
     * @return <tt>true</tt> if contains string, <tt>false</tt> otherwise.
     */
    public static boolean containsStringArray(String[] arr, String val, boolean ignoreCase) {
        assert arr != null : "ASSERTION [line=717, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        for (String s : arr) {
            // If both are nulls, then they are equal.
            if (s == null && val == null) {
                return true;
            }

            // Only one is null and the other one isn't.
            if (s == null || val == null) {
                return false;
            }

            // Both are not nulls.
            if (ignoreCase == true) {
                if (s.equalsIgnoreCase(val) == true) {
                    return true;
                }
            }
            else {
                if (s.equals(val) == true) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks for containment of value matching given regular expression in the provided array.
     *
     * @param arr Array of strings.
     * @param regex Regular expression.
     * @return <tt>true</tt> if string matching given regular expression found, <tt>false</tt> otherwise.
     */
    public static boolean containsRegexArray(String[] arr, String regex) {
        assert arr != null : "ASSERTION [line=754, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert regex != null : "ASSERTION [line=755, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        for (String s : arr) {
            if (s != null && s.matches(regex) == true) {
                return true;
            }
        }

        return false;
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(Closeable rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(Socket rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     */
    public static void close(MulticastSocket rsrc) {
        if (rsrc != null) {
            rsrc.close();
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(ServerSocket rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(SelectableChannel rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(SelectionKey rsrc, GridLogger log) {
        if (rsrc != null) {
            // This call will automatically deregister the selection key as well.
            close(rsrc.channel(), log);
        }
    }

// hwellmann: Not needed.    
//    /**
//     * Closes given resource logging possible checked exception.
//     *
//     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
//     * @param purge Whether or not to purge mail inbox on closing.
//     * @param log Logger to use to log possible checked exception.
//     */
//    public static void close(GridMailInbox rsrc, boolean purge, GridLogger log) {
//        if (rsrc != null) {
//            try {
//                rsrc.close(purge);
//            }
//            catch (GridMailException e) {
//                if (log != null) {
//                    log.error("Failed to close inbox [inbox=" + rsrc + ", purge=" + purge + ']', e);
//                }
//                else {
//                    e.printStackTrace(System.err);
//                }
//            }
//        }
//    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(Reader rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(JarFile rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     */
    public static void close(DatagramSocket rsrc) {
        if (rsrc != null) {
            rsrc.close();
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(Selector rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                if (rsrc.isOpen() == true) {
                    rsrc.close();
                }
            }
            catch (IOException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes given resource logging possible checked exception.
     *
     * @param rsrc Resource to close. If it's <tt>null</tt> - it's no-op.
     * @param log Logger to use to log possible checked exception.
     */
    public static void close(Context rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (NamingException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes JDBC connection.
     *
     * @param rsrc JDBC connection to close. If connection is <tt>null</tt>, it's no-op.
     * @param log Logger to log errors.
     */
    public static void close(java.sql.Connection rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (SQLException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes JDBC statement.
     *
     * @param rsrc JDBC statement to close. If statement is <tt>null</tt>, it's no-op.
     * @param log Logger to log errors.
     */
    public static void close(Statement rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (SQLException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Closes JDBC resultset.
     *
     * @param rsrc JDBC resultset to close. If resultset is <tt>null</tt>, it's no-op.
     * @param log Logger to log errors.
     */
    public static void close(ResultSet rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.close();
            }
            catch (SQLException e) {
                if (log != null) {
                    log.error("Failed to close resource: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Rollbacks JDBC connection.
     *
     * @param rsrc JDBC connection to rollback. If connection is <tt>null</tt>, it's no-op.
     * @param log Logger to log errors.
     */
    public static void rollbackConnection(java.sql.Connection rsrc, GridLogger log) {
        if (rsrc != null) {
            try {
                rsrc.rollback();
            }
            catch (SQLException e) {
                if (log != null) {
                    log.error("Failed to rollback JDBC connection: " + rsrc, e);
                }
                else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Constructs JMX object name with given properties.
     * Map with ordered <tt>groups</tt> used for proper object name construction.
     *
     * @param gridName Grid name.
     * @param grp Name of the group.
     * @param name Name of mbean.
     * @return JMX object name.
     * @throws MalformedObjectNameException Thrown in case of any errors.
     */
    public static ObjectName makeMBeanName(String gridName, String grp, String name)
        throws MalformedObjectNameException {
        StringBuilder builder = new StringBuilder(JMX_DOMAIN + ':');

        if (gridName != null && gridName.length() > 0) {
            builder.append("grid=").append(gridName).append(',');
        }

        if (grp != null) {
            builder.append("group=").append(grp).append(',');
        }

        builder.append("name=").append(name);

        return new ObjectName(builder.toString());
    }

    /**
     * Registers MBean with the server.
     *
     * @param <T> Type of mbean.
     * @param mbeanSvr MBean server.
     * @param gridName Grid name.
     * @param grp Name of the group.
     * @param name Name of mbean.
     * @param impl MBean implementation.
     * @param itf MBean interface.
     * @return JMX object name.
     * @throws JMException If MBean creation failed.
     */
    public static <T> ObjectName registerMBean(MBeanServer mbeanSvr, String gridName, String grp,
        String name, T impl, Class<T> itf) throws JMException {
        GridStandardMBean mbean = new GridStandardMBean(impl, itf);

        mbean.getMBeanInfo();

        return mbeanSvr.registerMBean(mbean,
            makeMBeanName(gridName, grp, name)).getObjectName();
    }

    /**
     * Creates new map instance with one entry.
     *
     * @param n1 Key.
     * @param v1 Value.
     * @return Created map.
     */
    public static Map<String, Serializable> makeMap(String n1, Serializable v1) {
        Map<String, Serializable> map = new HashMap<String, Serializable>(1);

        map.put(n1, v1);

        return map;
    }

    /**
     * Create new map instance with given entries.
     *
     * @param n1 Key 1.
     * @param v1 Value 1.
     * @param n2 Key 2.
     * @param v2 Value 2.
     * @return Created map.
     */
    public static Map<String, Serializable> makeMap(String n1, Serializable v1, String n2, Serializable v2) {
        Map<String, Serializable> map = new HashMap<String, Serializable>(2);

        map.put(n1, v1);
        map.put(n2, v2);

        return map;
    }

    /**
     * Creates new map instance with given entries.
     *
     * @param n1 Key 1.
     * @param v1 Value 1.
     * @param n2 Key 2.
     * @param v2 Value 2.
     * @param n3 Key 3.
     * @param v3 Value 3.
     * @return Created map.
     */
    public static Map<String, Serializable> makeMap(String n1, Serializable v1, String n2, Serializable v2, String n3,
        Serializable v3) {
        Map<String, Serializable> map = new HashMap<String, Serializable>(3);

        map.put(n1, v1);
        map.put(n2, v2);
        map.put(n3, v3);

        return map;
    }

    /**
     * Creates new map instance with given entries.
     *
     * @param n1 Key 1.
     * @param v1 Value 1.
     * @param n2 Key 2.
     * @param v2 Value 2.
     * @param n3 Key 3.
     * @param v3 Value 3.
     * @param n4 Key 4.
     * @param v4 Value 4.
     * @return Created map.
     */
    public static Map<String, Serializable> makeMap(String n1, Serializable v1, String n2, Serializable v2, String n3,
        Serializable v3, String n4, Serializable v4) {
        Map<String, Serializable> map = new HashMap<String, Serializable>(3);

        map.put(n1, v1);
        map.put(n2, v2);
        map.put(n3, v3);
        map.put(n4, v4);

        return map;
    }

    /**
     * Convenience method that interrupts a given thread if it's not <tt>null</tt>.
     *
     * @param t Thread to interrupt.
     */
    public static void interrupt(Thread t) {
        if (t != null) {
            t.interrupt();
        }
    }

    /**
     * Convenience method that interrupts a given thread if it's not <tt>null</tt>.
     *
     * @param workers Threads to interrupt.
     */
    public static void interrupt(Collection<? extends Thread> workers) {
        if (workers != null) {
            for (Thread worker : workers) {
                worker.interrupt();
            }
        }
    }

    /**
     * Waits for completion of a given thread. If thread is <tt>null</tt> then
     * this method returns immediately returning <tt>true</tt>
     *
     * @param t Thread to join.
     * @param log Logger for logging errors.
     * @return <tt>true</tt> if thread has finished, <tt>false</tt> otherwise.
     */
    public static boolean join(Thread t, GridLogger log) {
        if (t != null) {
            try {
                t.join();

                return true;
            }
            catch (InterruptedException e) {
                log.warning("Got interrupted while waiting for completion of a thread: " + t, e);

                return false;
            }
        }

        return true;
    }

    /**
     * Waits for completion of a given threads. If thread is <tt>null</tt> then
     * this method returns immediately returning <tt>true</tt>
     *
     * @param workers Thread to join.
     * @param log Logger for logging errors.
     * @return <tt>true</tt> if thread has finished, <tt>false</tt> otherwise.
     */
    public static boolean joinThreads(Collection<? extends Thread> workers, GridLogger log) {
        boolean retval = true;

        if (workers != null) {
            for (Thread worker : workers) {
                if (join(worker, log) == false) {
                    retval = false;
                }
            }
        }

        return retval;
    }

    /**
     * Cancels given runnable.
     *
     * @param r Runnable to cancel - it's no-op if runnable is <tt>null</tt>.
     */
    public static void cancel(GridRunnable r) {
        if (r != null) {
            r.cancel();
        }
    }

    /**
     * Cancels collection of runnables.
     *
     * @param rs Collection of runnables - it's no-op if collection is <tt>null</tt>.
     */
    public static void cancel(Collection<? extends GridRunnable> rs) {
        if (rs != null) {
            for (GridRunnable r : rs) {
                r.cancel();
            }
        }
    }

    /**
     * Joins runnable.
     *
     * @param r Runnable to join.
     * @param log The logger to possible exception.
     * @return <tt>true</tt> if worker has not been interrupted, <tt>false</tt> if it was interrupted.
     */
    public static boolean join(GridRunnable r, GridLogger log) {
        if (r != null) {
            try {
                r.join();
            }
            catch (InterruptedException e) {
                log.warning("Got interrupted while waiting for completion of runnable: " + r, e);

                return false;
            }
        }

        return true;
    }

    /**
     * Joins given collection of runnables.
     *
     * @param rs Collection of workers to join.
     * @param log The logger to possible exceptions.
     * @return <tt>true</tt> if none of the worker have been interrupted,
     *      <tt>false</tt> if at least one was interrupted.
     */
    public static boolean join(Collection<? extends GridRunnable> rs, GridLogger log) {
        boolean retval = true;

        if (rs != null) {
            for (GridRunnable r : rs) {
                if (join(r, log) == false) {
                    retval = false;
                }
            }
        }

        return retval;
    }

    /**
     * Shutdowns given ExecutorSrvice and wait for executor service to stop.
     *
     * @param owner The ExecutorService owner.
     * @param exec ExecutorService to shutdown.
     * @param log The logger to possible exceptions and warnings.
     */
    public static void shutdownNow(Class<?> owner, ExecutorService exec, GridLogger log) {
        if (exec != null) {
            List<Runnable> tasks = exec.shutdownNow();

            if (tasks != null && tasks.isEmpty() == false) {
                log.warning("Runnable tasks outlived thread pool executor service [owner=" + getSimpleName(owner) +
                    ", tasks=" + tasks + ']');
            }

            try {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                log.warning("Got interrupted while waiting for executor service to stop.", e);
            }
        }
    }

    /**
     * Writes UUID to output stream. This method is meant to be used by
     * implementations of {@link Externalizable} interface.
     *
     * @param out Output stream.
     * @param uid UUID to write.
     * @throws IOException If write failed.
     */
    public static void writeUUID(ObjectOutput out, UUID uid) throws IOException {
        // Write null flag.
        out.writeBoolean(uid == null);

        if (uid != null) {
            out.writeLong(uid.getMostSignificantBits());
            out.writeLong(uid.getLeastSignificantBits());
        }
    }

    /**
     * Reads UUID from input stream. This method is meant to be used by
     * implementations of {@link Externalizable} interface.
     *
     * @param in Input stream.
     * @return Read UUID.
     * @throws IOException If read failed.
     */
    public static UUID readUUID(ObjectInput in) throws IOException {
        // If UUID is not null.
        if (in.readBoolean() == false) {
            long most = in.readLong();
            long least = in.readLong();

            return new UUID(most, least);
        }

        return null;
    }

    /**
     * Writes string to output stream accounting for <tt>null</tt> values.
     *
     * @param out Output stream to write to.
     * @param s String to write, possibly <tt>null</tt>.
     * @throws IOException If write failed.
     */
    public static void writeString(ObjectOutput out, String s) throws IOException {
        // Write null flag.
        out.writeBoolean(s == null);

        if (s != null) {
            out.writeUTF(s);
        }
    }

    /**
     * Reads string from input stream accounting for <tt>null</tt> values.
     *
     * @param in Stream to read from.
     * @return Read string, possibly <tt>null</tt>.
     * @throws IOException If read failed.
     */
    public static String readString(ObjectInput in) throws IOException {
        // If value is not null, then read it. Otherwise return null.
        return in.readBoolean() == false ? in.readUTF() : null;
    }

    /**
     * Gets annotation for a class.
     *
     * @param <T> Type of annotation to return.
     * @param cls Class to get annotation from.
     * @param annCls Annotation to get.
     * @return Instance of annotation, or <tt>null</tt> if not found.
     */
    public static <T extends java.lang.annotation.Annotation> T getAnnotation(Class<?> cls, Class<T> annCls) {
        for (Class<?> c = cls; c != null && c.equals(Object.class) == false; c = c.getSuperclass()) {
            T ann = c.getAnnotation(annCls);

            if (ann != null) {
                return ann;
            }
        }

        return null;
    }

    /**
     * Gets simple class name taking care of empty names.
     *
     * @param cls Class to get the name for.
     * @return Simple class name.
     */
    public static String getSimpleName(Class<?> cls) {
        return cls.getSimpleName().length() == 0 ? cls.getName() : cls.getSimpleName();
    }

    /**
     * Checks if the map passed in is contained in base map.
     *
     * @param base Base map.
     * @param map Map to check.
     * @return <tt>True</tt> if all entries within map are contained in base map,
     *      <tt>false</tt> otherwise.
     */
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public static boolean containsAll(Map<?,?> base, Map<?,?> map) {
        assert base != null : "ASSERTION [line=1490, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert map != null : "ASSERTION [line=1491, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (base.containsKey(entry.getKey()) == true) {
                Object val = base.get(entry.getKey());

                if (val == null && entry.getValue() == null) {
                    continue;
                }

                if (val == null || entry.getValue() == null || val.equals(entry.getValue()) == false) {
                    // Mismatch found.
                    return false;
                }
            }
            else {
                return false;
            }
        }

        // All entries in 'map' are contained in base map.
        return true;
    }

    /**
     * Copies all elements from source collection into new collection
     * except for elements in the exclude array.
     *
     * @param <T> Element type.
     * @param elems Source list.
     * @param exclude Exclude collection.
     * @return Copy of the passed in list without elements on exclude array.
     */
    public static <T> List<T> copy(List<T> elems, T... exclude) {
        return copy(elems, Arrays.asList(exclude));
    }

    /**
     * Copies all elements from source collection into new collection
     * except for elements in the exclude collection.
     *
     * @param <T> Element type.
     * @param elems Source list.
     * @param exclude Exclude collection.
     * @return Copy of the passed in list without elements on exclude collection.
     */
    public static <T> List<T> copy(List<T> elems, Collection<T> exclude) {
        List<T> copy = new ArrayList<T>(elems.size());

        for (T t : elems) {
            if (exclude.contains(t) == false) {
                copy.add(t);
            }
        }

        return copy;
    }

    /**
     * Gets task name for the given task class.
     *
     * @param taskCls Task class.
     * @return Either task name from class annotation (see {@link GridTaskName}})
     *      or task class name if there is no annotation.
     */
    public static String getTaskName(final Class<? extends GridTask<?, ?>> taskCls) {
        GridTaskName nameAnn = getAnnotation(taskCls, GridTaskName.class);

        return nameAnn == null ? taskCls.getName() : nameAnn.value();
    }

    /**
     * Creates SPI attribute name by adding prefix to the attribute name.
     * Prefix is an SPI name + '.'.
     *
     * @param spi SPI.
     * @param attrName attribute name.
     * @return SPI attribute name.
     */
    public static String createSpiAttributeName(GridSpi spi, String attrName) {
        assert spi != null : "ASSERTION [line=1571, file=src/java/org/gridgain/grid/util/GridUtils.java]";
        assert spi.getName() != null : "ASSERTION [line=1572, file=src/java/org/gridgain/grid/util/GridUtils.java]";

        return spi.getName() + '.' + attrName;
    }

    /**
     * Gets resource path for the class.
     *
     * @param clsName Class name.
     * @return Resource name for the class.
     */
    public static String classNameToResourceName(String clsName) {
        return clsName.replaceAll("\\.", "/") + ".class";
    }

    /**
     * Detects class loader for given class and returns class loaded by appropriate classloader.
     * This method will first check if {@link Thread#getContextClassLoader()} is appropriate.
     * If yes, then context class loader will be returned, otherwise
     * the {@link Class#getClassLoader()} will be returned.
     *
     * @param cls Class to find class loader for.
     * @return Class loader for given class.
     */
    public static ClassLoader detectClassLoader(Class<?> cls) {
        ClassLoader ldr = Thread.currentThread().getContextClassLoader();

        //noinspection ObjectEquality
        if (ldr == cls.getClassLoader()) {
            return ldr;
        }

        //noinspection UnusedCatchParameter
        try {
            Class<?> c = Class.forName(cls.getName(), true, ldr);

            //noinspection ObjectEquality
            if (c == cls) {
                return ldr;
            }
        }
        catch (ClassNotFoundException e) {
            // No-op.
        }

        return cls.getClassLoader();
    }
}
