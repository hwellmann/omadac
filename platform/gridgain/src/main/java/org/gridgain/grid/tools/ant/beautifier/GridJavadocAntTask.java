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

package org.gridgain.grid.tools.ant.beautifier;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import java.io.*;
import java.util.*;

/**
 * Ant task fixing known HTML issues for Javadoc.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJavadocAntTask extends MatchingTask {
    /** I/O buffer size. */
    private static final int BUF_SIZE = 1024 << 3;

    /** directory. */
    private File dir = null;

    /** CSS file name. */
    private String css = null;

    /** I/O buffer. */
    private final char[] ioBuf = new char[BUF_SIZE];

    /**
     * Sets directory.
     *
     * @param dir Directory to set.
     */
    public void setDir(File dir) {
        assert dir != null : "ASSERTION [line=54, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocAntTask.java]";

        this.dir = dir;
    }

    /**
     * Sets CSS file name.
     * @param css CSS file name to set.
     */
    public void setCss(String css) {
        assert css != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocAntTask.java]";

        this.css = css;
    }

    /**
     * Closes resource.
     *
     * @param closeable Resource to close.
     */
    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException e) {
                log("Failed closing [resource=" + closeable + ", message=" + e.getLocalizedMessage() + ']',
                    Project.MSG_WARN);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (dir == null) {
            throw new BuildException("'dir' attribute must be specified.");
        }

        if (css == null) {
            throw new BuildException("'css' attribute must be specified.");
        }

        log("dir=" + dir, Project.MSG_DEBUG);
        log("css=" + css, Project.MSG_DEBUG);

        DirectoryScanner scanner = getDirectoryScanner(dir);

        for (String fileName : scanner.getIncludedFiles()) {
            String file = dir.getAbsolutePath() + '/' + fileName;

            try {
                processFile(file);
            }
            catch (IOException e) {
                throw new BuildException("IO error while processing: " + file, e);
            }
        }
    }

    /**
     * Processes file (cleaning up Javadoc's HTML).
     *
     * @param file File to cleanup.
     * @throws IOException Thrown in case of any I/O error.
     */
    private void processFile(String file) throws IOException {
        CharArrayWriter writer = new CharArrayWriter();

        Reader reader = null;

        try {
            reader = new FileReader(file);

            for (int i = 0; (i = reader.read(ioBuf)) != -1; writer.write(ioBuf, 0, i)) {
                // No-op.
            }
        }
        finally {
            close(reader);
        }

        GridJavdocCharArrayLexReader lexer = new GridJavdocCharArrayLexReader(writer.toCharArray());

        List<GridJavadocToken> toks = new ArrayList<GridJavadocToken>();

        StringBuilder tokBuf = new StringBuilder();

        int ch = 0;

        while ((ch = lexer.read()) != GridJavdocCharArrayLexReader.EOF) {
            // Instruction, tag or comment.
            if (ch =='<') {
                if (tokBuf.length() > 0) {
                    toks.add(new GridJavadocToken(GridJavadocTokenType.TOKEN_TEXT, tokBuf.toString()));

                    tokBuf.setLength(0);
                }

                tokBuf.append('<');

                ch = lexer.read();

                if (ch == GridJavdocCharArrayLexReader.EOF) {
                    throw new IOException("Unexpected EOF: " + file);
                }

                // Instruction or comment.
                if (ch == '!') {
                    for (; ch != GridJavdocCharArrayLexReader.EOF && ch != '>'; ch = lexer.read()) {
                        tokBuf.append((char)ch);
                    }

                    if (ch == GridJavdocCharArrayLexReader.EOF) {
                        throw new IOException("Unexpected EOF: " + file);
                    }

                    assert ch == '>' : "ASSERTION [line=173, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocAntTask.java]";

                    tokBuf.append('>');

                    String value = tokBuf.toString();

                    toks.add(new GridJavadocToken(value.startsWith("<!--") == true ? GridJavadocTokenType.TOKEN_COMM :
                        GridJavadocTokenType.TOKEN_INSTR, value));

                    tokBuf.setLength(0);
                }
                // Tag.
                else {
                    for (; ch != GridJavdocCharArrayLexReader.EOF && ch != '>'; ch = lexer.read()) {
                        tokBuf.append((char)ch);
                    }

                    if (ch == GridJavdocCharArrayLexReader.EOF) {
                        throw new IOException("Unexpected EOF: " + file);
                    }

                    assert ch == '>' : "ASSERTION [line=194, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocAntTask.java]";

                    tokBuf.append('>');

                    if (tokBuf.length() <= 2) {
                        throw new IOException("Invalid HTML in [file=" + file + ", html=" + tokBuf.toString() + ']');
                    }

                    String value = tokBuf.toString();

                    toks.add(new GridJavadocToken(value.startsWith("</") == true ?
                        GridJavadocTokenType.TOKEN_CLOSE_TAG : GridJavadocTokenType.TOKEN_OPEN_TAG, value));

                    tokBuf.setLength(0);
                }
            }
            else {
                tokBuf.append((char)ch);
            }
        }

        if (tokBuf.length() > 0) {
            toks.add(new GridJavadocToken(GridJavadocTokenType.TOKEN_TEXT, tokBuf.toString()));
        }

        boolean inZone = false;
        boolean addCss = false;

        Stack<Boolean> stack = new Stack<Boolean>();

        for (GridJavadocToken tok : toks) {
            String value = tok.getValue();

            switch (tok.getType()) {
                case TOKEN_COMM: {
                    if (value.equals("<!-- ========= END OF TOP NAVBAR ========= -->") == true) {
                        inZone = true;
                    }
                    else if (value.equals("<!-- ======= START OF BOTTOM NAVBAR ====== -->") == true) {
                        inZone = false;
                    }

                    break;
                }

                case TOKEN_OPEN_TAG: {
                    if (inZone == true) {
                        if (value.startsWith("<TABLE") == true) {
                            addCss = !value.contains("BORDER=\"0\"");

                            stack.push(addCss);

                            if (addCss == true) {
                                tok.setUpdatedValue("<TABLE CLASS=\"" + css + '\"' + value.substring(6));
                            }
                        }
                        else if (value.startsWith("<TD") == true) {
                            if (addCss == true) {
                                tok.setUpdatedValue("<TD CLASS=\"" + css + '\"' + value.substring(3));
                            }
                        }
                        else if (value.startsWith("<TH") == true) {
                            if (addCss == true) {
                                tok.setUpdatedValue("<TH CLASS=\"" + css + '\"' + value.substring(3));
                            }
                        }
                    }

                    break;
                }

                case TOKEN_CLOSE_TAG: {
                    if (value.equalsIgnoreCase("</head>") == true) {
                        tok.setUpdatedValue(
                            "<link type=\"text/css\" rel=\"stylesheet\" href=\"http://www.gridgain.com/sh/SyntaxHighlighter.css\"></link>\n" +
                            "<script language=\"javascript\" src=\"http://www.gridgain.com/sh/shCore.js\"></script>\n" +
                            "<script language=\"javascript\" src=\"http://www.gridgain.com/sh/shBrushJava.js\"></script>\n" +
                            "<script language=\"javascript\" src=\"http://www.gridgain.com/sh/shBrushXml.js\"></script>\n" +
                            "</head>\n");
                    }
                    else if (value.equalsIgnoreCase("</body>") == true) {
                        tok.setUpdatedValue(
                            "<script language=\"javascript\">\n" +
                            "dp.SyntaxHighlighter.ClipboardSwf = 'http://www.gridgain.com/sh/clipboard.swf';\n" +
                            "dp.SyntaxHighlighter.HighlightAll('code');\n" +
                            "</script>\n" +
                            "</body>\n");
                    }
                    else if (inZone == true) {
                        if (value.startsWith("</TABLE") == true) {
                            stack.pop();

                            addCss = stack.isEmpty() == true || stack.peek() == true;
                        }
                    }

                    break;
                }

                case TOKEN_INSTR:
                case TOKEN_TEXT: {
                    // No-op.

                    break;
                }

                default: {
                    assert false : "ASSERTION [line=301, file=src/java/org/gridgain/grid/tools/ant/beautifier/GridJavadocAntTask.java]";
                }
            }
        }

        replaceFile(file, toks);
    }

    /**
     * Replaces file with given list of tokens.
     *
     * @param file File to replace.
     * @param toks List of token comprising the content of the file.
     * @throws IOException Thrown in case of any errors.
     */
    private void replaceFile(String file, List<GridJavadocToken> toks) throws IOException {
        OutputStream out = null;

        try {
            out = new BufferedOutputStream(new FileOutputStream(file));

            for (GridJavadocToken tok : toks) {
                out.write(tok.getValue().getBytes());
            }
        }
        finally {
            close(out);
        }
    }
}
