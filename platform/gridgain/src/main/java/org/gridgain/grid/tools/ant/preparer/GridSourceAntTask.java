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

package org.gridgain.grid.tools.ant.preparer;

import java.io.*;
import java.util.regex.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

/**
 * Ant task preparing source code for distribution.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridSourceAntTask extends MatchingTask {
    /** .txt extension. */
    private static final String TXT_EXTENSION = ".txt";

    /** .java extension. */
    private static final String JAVA_EXTENSION = ".java";

    /** .css extension. */
    private static final String CSS_EXTENSION = ".css";

    /** .html extension. */
    private static final String HTML_EXTENSION = ".html";

    /** .js extension. */
    private static final String JS_EXTENSION = ".js";

    /** .xml extension. */
    private static final String XML_EXTENSION = ".xml";

    /** .dtd extension. */
    private static final String DTD_EXTENSION = ".dtd";

    /** .sh extension. */
    private static final String SH_EXTENSION = ".sh";

    /** .bat extension. */
    private static final String BAT_EXTENSION = ".bat";

    /** .cpp extension. */
    private static final String CPP_EXTENSION = ".cpp";

    /** .h extension. */
    private static final String H_EXTENSION = ".h";

    /** System line separator. */
    static final String NL = System.getProperty("line.separator");

    /** Regular expression for commented line. */
    private static final String COMMENTED_LINE_REGEX = "\\s*\\/\\/.*";

    /** Assertion text banner. */
    private static final String ASSERTION_TEXT = "ASSERTION";

    /** Java license substitution regular expression. */
    private final Pattern javaLicenseRegex = Pattern.compile("\\/\\*@ANT_JAVA_COPYRIGHT_LICENSE@\\*\\/");

    /** C++ license substitution regular expression. */
    private final Pattern cppLicenseRegex = Pattern.compile("\\/\\*@ANT_CPP_COPYRIGHT_LICENSE@\\*\\/");

    /** C++/Header license substitution regular expression. */
    private final Pattern hLicenseRegex = Pattern.compile("\\/\\*@ANT_H_COPYRIGHT_LICENSE@\\*\\/");

    /** Text license substitution regular expression. */
    private final Pattern txtLicenseRegex = Pattern.compile("<!--@ANT_TXT_COPYRIGHT_LICENSE@-->");

    /** CSS license substitution regular expression. */
    private final Pattern cssLicenseRegex = Pattern.compile("\\/\\*@ANT_CSS_COPYRIGHT_LICENSE@\\*\\/");

    /** JavaScript license substitution regular expression.*/
    private final Pattern jsLicenseRegex = Pattern.compile("\\/\\*@ANT_JS_COPYRIGHT_LICENSE@\\*\\/");

    /** XML license substitution regular expression.*/
    private final Pattern xmlLicenseRegex = Pattern.compile("<!--@ANT_XML_COPYRIGHT_LICENSE@-->");

    /** HTML license substitution regular expression.*/
    private final Pattern htmlLicenseRegex = Pattern.compile("<!--@ANT_HTML_COPYRIGHT_LICENSE@-->");

    /** DTD license substitution regular expression.*/
    private final Pattern dtdLicenseRegex = Pattern.compile("<!--@ANT_DTD_COPYRIGHT_LICENSE@-->");

    /** UNIX shell license substitution regular expression.*/
    private final Pattern shLicenseRegex = Pattern.compile("# @ANT_SH_COPYRIGHT_LICENSE@");

    /** Windows bat license substitution regular expression.*/
    private final Pattern batLicenseRegex = Pattern.compile(":: @ANT_BAT_COPYRIGHT_LICENSE@");

    /** Assert keyword regular expression. */
    private final Pattern assertRegex = Pattern.compile("\\s*assert .*;\\s*");

    /** Exception throw regular expression. */
    private final Pattern exRegex = Pattern.compile("\\s+new\\s+Grid\\w*Exception\\(");

    /** Directory. */
    private File dir = null;

    /** License file. */
    private File licenseFile = null;

    /** */
    private List<String> license = new ArrayList<String>();

    /** Input reader wrapper. */
    private GridSourceInput srcIn = null;

    /** Output  writer wrapper.  */
    private GridSourceOutput srcOut = null;
    
    /** Whether or not to process Java's exceptions and asserts. */
    private boolean procJava = true;

    /**
     * Sets directory.
     *
     * @param dir Directory to set.
     */
    public void setDir(File dir) {
        assert dir != null : "ASSERTION [line=142, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

        this.dir = dir;
    }
    
    /** 
     * 
     * @param procJava Whether or not to process Java's exceptions and asserts.
     */
    public void setProcJava(boolean procJava) {
        this.procJava = procJava;
    }

    /**
     * Sets license file.
     *
     * @param licenseFile License file to set.
     */
    public void setLicenseFile(File licenseFile) {
        this.licenseFile = licenseFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (dir == null) {
            throw new BuildException("'dir' attribute must be specified.");
        }

        if (licenseFile == null) {
            throw new BuildException("'licenseFile' attribute must be specified.");
        }

        log("dir=" + dir, Project.MSG_DEBUG);
        log("licenseFile=" + licenseFile, Project.MSG_DEBUG);

        // Read license file.
        try {
            BufferedReader reader = new BufferedReader(new FileReader(licenseFile));

            try {
                for (String line = null; (line = reader.readLine()) != null;) {
                    license.add(line);
                }
            }
            finally {
                close(reader);
            }
        }
        catch (IOException e) {
            throw new BuildException("Error while loading license file: " + licenseFile, e);
        }

        DirectoryScanner scanner = getDirectoryScanner(dir);

        int n = 0;

        for (String fileName : scanner.getIncludedFiles()) {
            try {
                processFile(fileName.replace('\\', '/'));

                log("Processed source for: " + fileName);

                n++;
            }
            catch (IOException e) {
                throw new BuildException("Error while processing file: " + fileName, e);
            }
        }

        log("Processed " + n + " file(s)");
    }

    /**
     * Processes (prepares) given file.
     *
     * @param fileName Name of the file to process.
     * @throws IOException Thrown in case of any error.
     */
    private void processFile(String fileName) throws IOException {
        String fullFileName = dir.getAbsolutePath() + '/' + fileName;

        // Read file.
        File file = new File(fullFileName);

        String text = new String(readFile(file));

        Writer writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(fullFileName));

            if (fileName.endsWith(JAVA_EXTENSION)) {
                processJavaContent(text, fileName, writer);
            }
            else if (fileName.endsWith(CSS_EXTENSION)) {
                processCssContent(text, writer);
            }
            else if (fileName.endsWith(JS_EXTENSION)) {
                processJsContent(text, writer);
            }
            else if (fileName.endsWith(TXT_EXTENSION)) {
                processTxtContent(text, writer);
            }
            else if (fileName.endsWith(XML_EXTENSION)) {
                processXmlContent(text, writer);
            }
            else if (fileName.endsWith(HTML_EXTENSION)) {
                processHtmlContent(text, writer);
            }
            else if (fileName.endsWith(DTD_EXTENSION)) {
                processDtdContent(text, writer);
            }
            else if (fileName.endsWith(CPP_EXTENSION)) {
                processCppContent(text, writer);
            }
            else if (fileName.endsWith(H_EXTENSION)) {
                processHContent(text, writer);
            }
            else if (fileName.endsWith(SH_EXTENSION)) {
                processShContent(text, writer);
            }
            else if (fileName.endsWith(BAT_EXTENSION)) {
                processBatContent(text, writer);
            }
        }
        finally {
            close(writer);
        }
    }

    /**
     * Replaces license token with license text.
     *
     * @param regex License regular expression.
     * @param text Text in which to replace.
     * @param commPrefix Optional comment's first line.
     * @param commToken Optional comment's line start.
     * @param commSuffix Optional comment's last line.
     * @return Replaced text.
     */
    private String replaceLicense(Pattern regex, String text, String commPrefix, String commToken, String commSuffix) {
        StringBuilder buf = new StringBuilder();

        buf.append(commPrefix == null ? "" : commPrefix);
        buf.append(NL);

        String tok = commToken == null ? "" : commToken;

        for (String line : license) {
            buf.append(tok);
            buf.append(line);
            buf.append(NL);
        }

        buf.append(commSuffix == null ? "" : commSuffix);
        buf.append(NL);

        return regex.matcher(text).replaceAll(buf.toString());
    }

    /**
     * Processes (prepares) text file content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processTxtContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(txtLicenseRegex, text, null, null, null));
    }

    /**
     * Processes (prepares) XML file content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processXmlContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(xmlLicenseRegex, text, "<!--", "    ", "-->"));
    }

    /**
     * Processes (prepares) HTML file content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processHtmlContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(htmlLicenseRegex, text, "<!--", "    ", "-->"));
    }

    /**
     * Processes (prepares) DTD file content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processDtdContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(dtdLicenseRegex, text, "<!--", "    ", "-->"));
    }

    /**
     * Processes (prepares) C++ file content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processCppContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(cppLicenseRegex, text, "//", "// ", "//"));
    }

    /**
     * Processes (prepares) C++ Header file content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processHContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(hLicenseRegex, text, "//", "// ", "//"));
    }

    /**
     * Processes (prepares) UNIX shell content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processShContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(shLicenseRegex, text, "#", "# ", "#"));
    }

    /**
     * Processes (prepares) Windows bat content.
     *
     * @param text File text.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processBatContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(batLicenseRegex, text, "::", ":: ", "::"));
    }

    /**
     * Processes (prepares) JavaScript file content.
     *
     * @param text File content.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processJsContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(jsLicenseRegex, text, "/*", " * ", " */"));
    }

    /**
     * Processes (prepares) CSS file content.
     *
     * @param text File content.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processCssContent(String text, Writer writer) throws IOException {
        // Replace copyright.
        writer.write(replaceLicense(cssLicenseRegex, text, "/*", " * ", " */"));
    }

    /**
     * Processes (prepares) Java file content.
     *
     * @param content File content.
     * @param fileName Name of the file containing source content.
     * @param writer Writer for the result.
     * @throws IOException Thrown in case of any error.
     */
    private void processJavaContent(String content, String fileName, Writer writer) throws IOException {
        // Replace copyright.
        String processed = replaceLicense(javaLicenseRegex, content, "/*", " * ", " */");

        if (procJava == true) {
            // After all changes add source information to exceptions and write file.
            BufferedReader reader = null;
    
            try {
                reader = new BufferedReader(new StringReader(processed));
    
                srcIn = new GridSourceInput(reader, fileName);
                srcOut = new GridSourceOutput(writer);
    
                for (String line = null; (line = srcIn.readLine()) != null;) {
                    if (processException(line) == false) {
                        if (processAssert(line) == false) {
                            srcOut.writeLine(line);
                        }
                    }
                }
            }
            finally {
                close(reader);
    
                srcIn = null;
                srcOut = null;
            }
        }
        else {
            writer.write(processed);
        }
    }

    /**
     * Processes exception throwing.
     *
     * @param line Source code line with exception throwing.
     * @return Whether or not processing succeeded.
     * @throws IOException Thrown in case of any errors.
     */
    private boolean processException(String line) throws IOException {
        assert line != null : "ASSERTION [line=475, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

        // Process only not commented lines.
        if (line.matches(COMMENTED_LINE_REGEX) == true) {
            return false;
        }

        String[] arr = exRegex.split(line);

        switch (arr.length) {
            case 0:
            case 1: {
                break;
            }

            case 2: {
                // Store current line.
                String currentLine = Integer.toString(srcOut.getLine());

                // Extract exception name.
                String ex = line.substring(arr[0].length(), line.length() - arr[1].length() - 1).trim();

                assert ex.startsWith("new") == true : "ASSERTION [line=497, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

                ex = ex.substring(3).trim();

                int endPos = findNotCommentedChar(arr[1], ";");

                if (endPos == -1) {
                    // Multi-line exception.
                    writeLine(arr[0] + " (" + ex + ')', "new " + ex + '(', arr[1]);

                    while ((line = srcIn.readLine()) != null) {
                        endPos = findNotCommentedChar(line, ";");

                        if (endPos == -1) {
                            writeLine(line);
                        }
                        else {
                            writeLine(line.substring(0, endPos) + ".setData(", currentLine + ", ",
                                '"' + srcIn.getFileName() + "\")" + line.substring(endPos));

                            break;
                        }
                    }
                }
                else {
                    // Single line exception.
                    writeLine(arr[0] + " (" + ex + ')', "new " + ex + '(', arr[1].substring(0, endPos) + ".setData(",
                        currentLine + ", ", '"' + srcIn.getFileName() + "\")" + arr[1].substring(endPos));
                }

                return true;
            }

            default: {
                throw new BuildException("Too many exceptions in the file [file='" + srcIn.getFileName() +
                    "', line=" + srcIn.getLine() + ']');
            }
        }

        // No exceptions found.
        return false;
    }

    /**
     * Processes assertion.
     *
     * @param line Source code line assertion.
     * @return Whether or not processing succeeded.
     * @throws IOException Thrown in case of any errors.
     */
    private boolean processAssert(String line) throws IOException {
        assert line != null : "ASSERTION [line=548, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

        // Process only not commented lines.
        if (line.matches(COMMENTED_LINE_REGEX) == true || assertRegex.matcher(line).matches() == false) {
            return false;
        }

        String assertionMsg = ASSERTION_TEXT + " [line=" + srcOut.getLine() + ", file=" + srcIn.getFileName() + ']';

        int pos;

        // Find assertion message or assertion's end.
        while ((pos = findNotCommentedChar(line, ":;")) < 0) {
            writeLine(line);
        }

        if (line.charAt(pos) == ';') {
            // Assertion without message.
            writeLine(line.substring(0, pos) + " : ", '"' + assertionMsg + '"' + line.substring(pos));
        }
        else {
            // Assertion with message.
            int msgPos = pos + 1;

            while (msgPos < line.length() && line.charAt(msgPos) == ' ') {
                msgPos++;
            }

            String existsMsg = line.substring(msgPos);

            writeLine(line.substring(0, pos) + ": ", '"' + assertionMsg + ". \" + ",
                existsMsg);

            if (findNotCommentedChar(existsMsg, ";") == -1) {
                // Assertion not ended yet.
                while ((line = srcIn.readLine()) != null) {
                    writeLine(line);

                    if (findNotCommentedChar(line, ";") >= 0) {
                        break;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Utility routine looking up 1st uncommented character in given string.
     *
     * @param s String to look up in.
     * @param chars Characters to find.
     * @return Index of found character.
     */
    private int findNotCommentedChar(String s, String chars) {
        assert chars != null : "ASSERTION [line=604, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";
        assert chars.length() > 0 : "ASSERTION [line=605, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\"' || c == '\'') {
                // Skip content of strings.
                i++;

                while (i < s.length() && (s.charAt(i) != c || s.charAt(i - 1) == '\\')) {
                    i++;
                }

                if (i == s.length()) {
                    throw new BuildException("Cannot parse line [file='" + srcIn.getFileName() + "', line=" +
                        srcIn.getLine() + ']');
                }
            }

            if (chars.indexOf(c) >= 0) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Reads the file into character array.
     *
     * @param file File to read.
     * @return File's content as character array.
     * @throws IOException Thrown in case of any error.
     */
    private char[] readFile(File file) throws IOException {
        assert file != null : "ASSERTION [line=640, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

        char[] res = new char[(int)file.length()];

        Reader reader = new BufferedReader(new FileReader(file));

        try {
            for (int i = 0, off = 0; (i = reader.read(res, off, res.length - off)) > 0; off += i) {
                // No-op.
            }
        }
        finally {
            close(reader);
        }

        return res;
    }

    /**
     * Closes resource handling <tt>nulL</tt> values and exceptions.
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
     * Writes parts space delimited and breaks long line.
     * @param parts Parts to be written out.
     *
     * @throws IOException Thrown in case of any error.
     */
    private void writeLine(String ... parts) throws IOException {
        assert srcOut != null : "ASSERTION [line=682, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";
        assert parts.length > 0 : "ASSERTION [line=683, file=src/java/org/gridgain/grid/tools/ant/preparer/GridSourceAntTask.java]";

        StringBuilder line = new StringBuilder();

        for (String s : parts) {
            line.append(s);
        }

        srcOut.writeLine(line.toString());
    }
}
