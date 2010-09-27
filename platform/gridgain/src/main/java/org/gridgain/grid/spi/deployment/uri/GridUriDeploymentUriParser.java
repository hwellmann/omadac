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

package org.gridgain.grid.spi.deployment.uri;

import org.gridgain.grid.util.tostring.*;

/**
 * Helper class which encodes given string.
 * It replaces all occurrences of space with '%20', percent sign
 * with '%25' and semicolon with '%3B' if given string corresponds to
 * expected format.
 * <p>
 * Expected format is (schema):(//)URL(?|#)(parameters)
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentUriParser {
    /** Input string which should be parsed and encoded. */
    private final String input;

    /** Encoded string. */
    @GridToStringExclude
    private String encoded = null;

    /**
     * Creates new instance of parser for the given input string.
     *
     * @param input Input string which will be parsed.
     */
    GridUriDeploymentUriParser(String input) {
        assert input != null : "ASSERTION [line=51, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentUriParser.java]";

        this.input = input;

        encoded = input;
    }

    /**
     * Parses {@link #input} by extracting URL without schema and parameters
     * and than encodes this URL.
     * <p>
     * Expected {@link #input} format is (schema):(//)URL(?|#)(parameters)
     *
     * @return Either encoded string or unchanged if it does not match format.
     */
    String parse() {
        int n = input.length();

        // Scheme.
        int p = scan(0, n, "/?#", ":");

        if (p > 0 && at(p, n, ':') == true) {
            p++;            // Skip ':'

            if (at(p, n, '/') == true) {
                if (at(p, n, '/') == true && at(p + 1, n, '/') == true) {
                    p += 2;

                    // Seek authority.
                    int q = scan(p, n, "", "/?#");

                    if (q > p) {
                        p = q;
                    }
                }

                int q = scan(p, n, "", "?#");

                StringBuilder buf = new StringBuilder(input.substring(0, p));

                buf.append(encodePath(input.substring(p, q)));
                buf.append(input.substring(q, n));

                encoded = buf.toString();
            }
        }

        return encoded;
    }

    /**
     * Scan forward from the given start position.  Stop at the first char
     * in the err string (in which case -1 is returned), or the first char
     * in the stop string (in which case the index of the preceding char is
     * returned), or the end of the input string (in which case the length
     * of the input string is returned).  May return the start position if
     * nothing matches.
     *
     * @param start Start scan position.
     * @param end End scan position.
     * @param err Error characters.
     * @param stop Stoppers.
     * @return <tt>-1</tt> if character from the error characters list was found;
     *      index of first character occurence is on stop character list; end
     *      position if {@link #input} does not contain any characters
     *      from <tt>error</tt> or <tt>stop</tt>; start if start > end.
     */
    private int scan(int start, int end, String err, String stop) {
        int p = start;

        while (p < end) {
            char c = input.charAt(p);

            if (err.indexOf(c) >= 0) {
                return -1;
            }

            if (stop.indexOf(c) >= 0) {
                break;
            }

            p++;
        }

        return p;
    }

    /**
     * Tests whether {@link #input} contains <tt>c</tt> at position <tt>start</tt>
     * and <tt>start</tt> less than <tt>end</tt>.
     *
     * @param start Start position.
     * @param end End position.
     * @param c Character {@link #input} is tested against
     * @return <tt>true</tt> only if {@link #input} contains <tt>c</tt> at position
     *      <tt>start</tt> and <tt>start</tt> less than <tt>end</tt>.
     */
    private boolean at(int start, int end, char c) {
        return start < end && input.charAt(start) == c;
    }

    /**
     * Encodes given path by replacing all occurences of space with '%20',
     * percent sign with '%25' and semicolon with '%3B'.
     *
     * @param path Path to be encoded.
     * @return Encoded path.
     */
    private String encodePath(String path) {
        StringBuilder buf = new StringBuilder(path.length());

        for (int i = 0; i < path.length() ; i++) {
            char c = path.charAt(i);

            switch(c) {
                case ' ': {
                    buf.append("%20"); break;
                }

                case '%': {
                    buf.append("%25"); break;
                }
                case ';':{
                    buf.append("%3B"); break;
                }

                default: {
                    buf.append(c);
                }
            }
        }

        return  buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentUriParser.class, this);
    }
}
