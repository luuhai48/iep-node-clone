/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package xin.env;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RuntimeEnvironment {

    public static final String RUNTIME_MODE_ARG = "xin.runtime.mode";
    public static final String DIRPROVIDER_ARG = "xin.runtime.dirProvider";

    private static final String osname = System.getProperty("os.name").toLowerCase();
    private static final String javaSpecVendor = System.getProperty("java.specification.vendor");
    private static final boolean isHeadless;
    private static final boolean hasJavaFX;

    static {
    	boolean isHeadless_;

        try {
            // Load by reflection to prevent exception in case java.awt does not exist
            Class graphicsEnvironmentClass = Class.forName("java.awt.GraphicsEnvironment");
            Method isHeadlessMethod = graphicsEnvironmentClass.getMethod("isHeadless");
            isHeadless_ = (Boolean) isHeadlessMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        	isHeadless_ = true;
        }
        isHeadless = isHeadless_;
        
        boolean hasJavaFX_;
        try {
            Class.forName("javafx.application.Application");
            hasJavaFX_ = true;
        } catch (ClassNotFoundException e) {
            System.out.println("javafx not supported: "+e.getMessage());
            hasJavaFX_ = false;
        }
        hasJavaFX = hasJavaFX_;
    }

    private static boolean isWindowsRuntime() {
        return osname.startsWith("windows");
    }

    private static boolean isUnixRuntime() {
        return osname.contains("nux") || osname.contains("nix") || osname.contains("aix") || osname.contains("bsd") || osname.contains("sunos");
    }

    private static boolean isMacRuntime() {
        return osname.contains("mac");
    }

    private static boolean isWindowsService() {
        return "service".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG)) && isWindowsRuntime();
    }

    private static boolean isHeadless() {
        return isHeadless;
    }

    private static boolean isDesktopEnabled() {
        return "desktop".equalsIgnoreCase(System.getProperty(RUNTIME_MODE_ARG)) && !isHeadless(); 
    }

    public static boolean isDesktopApplicationEnabled() {
        boolean isDesktopApplicationEnabled = isDesktopEnabled() && hasJavaFX;
    	System.out.println("isDesktopApplicationEnabled=" + isDesktopApplicationEnabled);
        return isDesktopApplicationEnabled;
    }

    public static RuntimeMode getRuntimeMode() {
        System.out.println("isHeadless=" + isHeadless());
        if (isDesktopEnabled()) {
            return new DesktopMode();
        } else if (isWindowsService()) {
            return new WindowsServiceMode();
        } else {
            return new CommandLineMode();
        }
    }

    public static DirProvider getDirProvider() {
        String dirProvider = System.getProperty(DIRPROVIDER_ARG);
        if (dirProvider != null) {
            try {
                return (DirProvider)Class.forName(dirProvider).newInstance();
            } catch (ReflectiveOperationException e) {
                System.out.println("Failed to instantiate dirProvider " + dirProvider);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        if (isDesktopEnabled()) {
            if (isWindowsRuntime()) {
                return new WindowsUserDirProvider();
            }
            if (isUnixRuntime()) {
                return new UnixUserDirProvider();
            }
            if (isMacRuntime()) {
                return new MacUserDirProvider();
            }
        }
        return new DefaultDirProvider();
    }
}
