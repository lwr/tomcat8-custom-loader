package com.github.lwr.tomcat;

import java.net.*;
import java.util.regex.Pattern;

/**
 * Copied from {@link org.apache.tomcat.util.buf.UriUtil}
 */
class UriUtil {

    private static final String WAR_SEPARATOR;
    private static final Pattern PATTERN_CUSTOM;

    private UriUtil() { }

    static {
        String custom = System.getProperty("org.apache.tomcat.util.buf.UriUtil.WAR_SEPARATOR");
        if (custom == null) {
            WAR_SEPARATOR = "*/";
            PATTERN_CUSTOM = null;
        } else {
            WAR_SEPARATOR = custom + "/";
            PATTERN_CUSTOM = Pattern.compile(Pattern.quote(WAR_SEPARATOR));
        }
    }

    /**
     * Convert a URL of the form <code>war:file:...</code> to
     * <code>jar:file:...</code>.
     *
     * @param warUrl The WAR URL to convert
     *
     * @return The equivalent JAR URL
     *
     * @throws MalformedURLException If the conversion fails
     */
    static URL warToJar(URL warUrl) throws MalformedURLException {
        // Assumes that the spec is absolute and starts war:file:/...
        String file = warUrl.getFile();
        if (file.contains("*/")) {
            file = file.replaceFirst("\\*/", "!/");
        } else if (file.contains("^/")) {
            file = file.replaceFirst("\\^/", "!/");
        } else if (PATTERN_CUSTOM != null) {
            file = file.replaceFirst(PATTERN_CUSTOM.pattern(), "!/");
        }

        return new URL("jar", warUrl.getHost(), warUrl.getPort(), file);
    }
}
