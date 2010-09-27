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

/**
 * Provides functionality for determining current Operating System and JDK.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridOs {
    /** OS JDK string. */
    private static String osJdkStr = null;

    /** OS string. */
    private static String osStr = null;

    /** JDK string. */
    private static String jdkStr = null;

    /** Indicates whether current OS is Windows 95. */
    private static boolean win95 = false;

    /** Indicates whether current OS is Windows 98. */
    private static boolean win98 = false;

    /** Indicates whether current OS is Windows NT. */
    private static boolean winNt = false;

    /** Indicates whether current OS is Windows Vista. */
    private static boolean winVista = false;

    /** Indicates whether current OS is Windows 2000. */
    private static boolean win2k = false;

    /** Indicates whether current OS is Windows XP. */
    private static boolean winXp = false;

    /** Indicates whether current OS is Windows Server 2003. */
    private static boolean win2003 = false;

    /** Indicates whether current OS is UNIX flavor. */
    private static boolean unix = false;

    /** Indicates whether current OS is Solaris. */
    private static boolean solaris = false;

    /** Indicates whether current OS is Linux flavor. */
    private static boolean linux = false;

    /** Indicates whether current OS is NetWare. */
    private static boolean netware = false;

    /** Indicates whether current OS is Mac OS. */
    private static boolean mac = false;

    /** Indicates whether current OS architecture is Sun Sparc. */
    private static boolean sparc = false;

    /** Indicates whether current OS architecture is Intel X86. */
    private static boolean x86 = false;

    /** Name of the underlying OS. */
    private static String osName = null;

    /** Version of the underlying OS. */
    private static String osVersion = null;

    /** CPU architecture of the underlying OS. */
    private static String osArch = null;

    /** Name of the Java Runtime. */
    private static String javaRuntimeName = null;

    /** Name of the Java Runtime version. */
    private static String javaRuntimeVersion = null;

    /** Name of the JDK vendor. */
    private static String jdkVendor = null;

    /** Name of the JDK. */
    private static String jdkName = null;

    /** Version of the JDK. */
    private static String jdkVersion = null;

    /** Name of JVM specification. */
    private static String jvmSpecName = null;

    /** Version of JVM implementation. */
    private static String jvmImplVersion = null;

    /** Vendor's name of JVM implementation. */
    private static String jvmImplVendor = null;

    /** Name of the JVM implementation. */
    private static String jvmImplName = null;

    /**
     * Enforces singleton.
     */
    private GridOs() {
        // No-op.
    }

    /**
     * Detects the underlying OS and initializes local variables.
     */
    static {
        String osName = System.getProperty("os.name");

        String osLow = osName.toLowerCase();

        // OS type detection.
        if (osLow.contains("win") == true) {
            if(osLow.contains("95")) {
                win95 = true;
            }
            else if(osLow.contains("98") == true) {
                win98 = true;
            }
            else if (osLow.contains("nt") == true) {
                winNt = true;
            }
            else if (osLow.contains("2000") == true) {
                win2k = true;
            }
            else if (osLow.contains("vista") == true) {
                winVista = true;
            }
            else if (osLow.contains("xp") == true) {
                winXp = true;
            }
            else if (osLow.contains("2003") == true) {
                win2003 = true;
            }
        }
        else if (osLow.contains("netware") == true) {
            netware = true;
        }
        else if (osLow.contains("mac os") == true) {
            mac = true;
        }
        else {
            // UNIXs flavors tokens.
            for (String os : new String[] {"ix", "inux", "olaris", "un", "ux", "sco", "bsd", "att"}) {
                if (osLow.contains(os) == true) {
                    unix = true;

                    break;
                }
            }

            // UNIX name detection.
            if (osLow.contains("olaris") == true) {
                solaris = true;
            }
            else if (osLow.contains("inux") == true) {
                linux = true;
            }
        }

        String osArch = System.getProperty("os.arch");

        String archStr = osArch.toLowerCase();

        // OS architecture detection.
        if (archStr.contains("x86") == true) {
            x86 = true;
        }
        else if (archStr.contains("sparc") == true) {
            sparc = true;
        }

        String javaRuntimeName = System.getProperty("java.runtime.name");
        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String jdkVendor = System.getProperty("java.specification.vendor");
        String jdkName = System.getProperty("java.specification.name");
        String jdkVersion = System.getProperty("java.specification.version");
        String osVersion = System.getProperty("os.version");
        String jvmSpecName = System.getProperty("java.vm.specification.name");
        String jvmImplVersion = System.getProperty("java.vm.version");
        String jvmImplVendor = System.getProperty("java.vm.vendor");
        String jvmImplName = System.getProperty("java.vm.name");

        String jdkStr = javaRuntimeName + ' ' + javaRuntimeVersion + ' ' + jvmImplVendor + ' ' + jvmImplName + ' ' +
            jvmImplVersion;

        osStr = osName + ' ' + osVersion  + ' ' + osArch;
        osJdkStr = osLow + ", " + jdkStr;

        // Copy auto variables to static ones.
        GridOs.osName = osName;
        GridOs.jdkName = jdkName;
        GridOs.jdkVendor = jdkVendor;
        GridOs.jdkVersion = jdkVersion;
        GridOs.jdkStr = jdkStr;
        GridOs.osVersion = osVersion;
        GridOs.osArch = osArch;
        GridOs.jvmSpecName = jvmSpecName;
        GridOs.jvmImplVersion = jvmImplVersion;
        GridOs.jvmImplVendor = jvmImplVendor;
        GridOs.jvmImplName = jvmImplName;
        GridOs.javaRuntimeName = javaRuntimeName;
        GridOs.javaRuntimeVersion = javaRuntimeVersion;
    }

    /**
     * Gets OS JDK string.
     *
     * @return OS JDK string.
     */
    public static String getOsJdkString() {
        return osJdkStr;
    }

    /**
     * Gets OS string.
     *
     * @return OS string.
     */
    public static String getOsString() {
        return osStr;
    }

    /**
     * Gets JDK string.
     *
     * @return JDK string.
     */
    public static String getJdkString() {
        return jdkStr;
    }

    /**
     * Indicates whether current OS is Linux flavor.
     *
     * @return <tt>true</tt> if current OS is Linux - <tt>false</tt> otherwise.
     */
    public static boolean isLinux() {
        return linux == true;
    }

    /**
     * Gets JDK name.
     * @return JDK name.
     */
    public static String getJdkName() {
        return jdkName;
    }

    /**
     * Gets JDK vendor.
     *
     * @return JDK vendor.
     */
    public static String getJdkVendor() {
        return jdkVendor;
    }

    /**
     * Gets JDK version.
     *
     * @return JDK version.
     */
    public static String getJdkVersion() {
        return jdkVersion;
    }

    /**
     * Gets OS CPU-architecture.
     *
     * @return OS CPU-architecture.
     */
    public static String getOsArchitecture() {
        return osArch;
    }

    /**
     * Gets underlying OS name.
     *
     * @return Underlying OS name.
     */
    public static String getOsName() {
        return osName;
    }

    /**
     * Gets underlying OS version.
     *
     * @return Underlying OS version.
     */
    public static String getOsVersion() {
        return osVersion;
    }

    /**
     * Indicates whether current OS is Mac OS.
     *
     * @return <tt>true</tt> if current OS is Mac OS - <tt>false</tt> otherwise.
     */
    public static boolean isMacOs() {
        return mac == true;
    }

    /**
     * Indicates whether current OS is Netware.
     *
     * @return <tt>true</tt> if current OS is Netware - <tt>false</tt> otherwise.
     */
    public static boolean isNetWare() {
        return netware == true;
    }

    /**
     * Indicates whether current OS is Solaris.
     *
     * @return <tt>true</tt> if current OS is Solaris (SPARC or x86) - <tt>false</tt> otherwise.
     */
    public static boolean isSolaris() {
        return solaris == true;
    }

    /**
     * Indicates whether current OS is Solaris on Spark box.
     *
     * @return <tt>true</tt> if current OS is Solaris SPARC - <tt>false</tt> otherwise.
     */
    public static boolean isSolarisSparc() {
        return solaris == true && sparc == true;
    }

    /**
     * Indicates whether current OS is Solaris on x86 box.
     *
     * @return <tt>true</tt> if current OS is Solaris x86 - <tt>false</tt> otherwise.
     */
    public static boolean isSolarisX86() {
        return solaris == true && x86 == true;
    }

    /**
     * Indicates whether current OS is UNIX flavor.
     *
     * @return <tt>true</tt> if current OS is UNIX - <tt>false</tt> otherwise.
     */
    public static boolean isUnix() {
        return unix == true;
    }

    /**
     * Indicates whether current OS is Windows.
     *
     * @return <tt>true</tt> if current OS is Windows (any versions) - <tt>false</tt> otherwise.
     */
    public static boolean isWindows() {
        return winXp == true || win95 == true || win98 == true || winNt == true || win2k == true ||
            win2003 == true || winVista == true;
    }

    /**
     * Indicates whether current OS is Windows Vista.
     *
     * @return <tt>true</tt> if current OS is Windows Vista - <tt>false</tt> otherwise.
     */
    public static boolean isWindowsVista() {
        return winVista == true;
    }

    /**
     * Indicates whether current OS is Windows 2000.
     *
     * @return <tt>true</tt> if current OS is Windows 2000 - <tt>false</tt> otherwise.
     */
    public static boolean isWindows2k() {
        return win2k == true;
    }

    /**
     * Indicates whether current OS is Windows Server 2003.
     *
     * @return <tt>true</tt> if current OS is Windows Server 2003 - <tt>false</tt> otherwise.
     */
    public static boolean isWindows2003() {
        return win2003 == true;
    }

    /**
     * Indicates whether current OS is Windows 95.
     *
     * @return <tt>true</tt> if current OS is Windows 95 - <tt>false</tt> otherwise.
     */
    public static boolean isWindows95() {
        return win95 == true;
    }

    /**
     * Indicates whether current OS is Windows 98.
     *
     * @return <tt>true</tt> if current OS is Windows 98 - <tt>false</tt> otherwise.
     */
    public static boolean isWindows98() {
        return win98 == true;
    }

    /**
     * Indicates whether current OS is Windows NT.
     *
     * @return <tt>true</tt> if current OS is Windows NT - <tt>false</tt> otherwise.
     */
    public static boolean isWindowsNt() {
        return winNt == true;
    }

    /**
     * Indicates that GridGain has been sufficiently tested on the current OS.
     *
     * @return <tt>true</tt> if current OS was sufficiently tested - <tt>false</tt> otherwise.
     */
    public static boolean isSufficientlyTestedOs() {
        return
            win2k == true ||
            winXp == true ||
            winVista == true ||
            mac == true ||
            linux == true ||
            solaris == true;
    }

    /**
     * Indicates whether current OS is Windows XP.
     *
     * @return <tt>true</tt> if current OS is Windows XP- <tt>false</tt> otherwise.
     */
    public static boolean isWindowsXp() {
        return winXp == true;
    }

    /**
     * Gets JVM specification name.
     *
     * @return JVM specification name.
     */
    public static String getJvmSpecificationName() {
        return jvmSpecName;
    }

    /**
     * Gets JVM implementation version.
     *
     * @return JVM implementation version.
     */
    public static String getJvmImplementationVersion() {
        return jvmImplVersion;
    }

    /**
     * Gets JVM implementation vendor.
     *
     * @return JVM implementation vendor.
     */
    public static String getJvmImplementationVendor() {
        return jvmImplVendor;
    }

    /**
     * Gets JVM implementation name.
     *
     * @return JVM implementation name.
     */
    public static String getJvmImplementationName() {
        return jvmImplName;
    }

    /**
     * Gets Java Runtime name.
     *
     * @return Java Runtime name.
     */
    public static String getJavaRuntimeName() {
        return javaRuntimeName;
    }

    /**
     * Gets Java Runtime version.
     *
     * @return Java Runtime version.
     */
    public static String getJavaRuntimeVersion() {
        return javaRuntimeVersion;
    }
}
