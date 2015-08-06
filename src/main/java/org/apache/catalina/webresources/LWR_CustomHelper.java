package org.apache.catalina.webresources;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ExpandWar;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * hacks for CustomWebappClassLoader.
 *
 * @author <a href="mailto:williamleung2006@gmail.com">William Leung</a>
 */
public class LWR_CustomHelper {


    private LWR_CustomHelper() {

    }


    public static boolean expandJarWarResources(StandardContext ctx, StandardRoot root) {
        List<WebResourceSet> classResources;
        try {
            Field f = StandardRoot.class.getDeclaredField("classResources");
            f.setAccessible(true);
            @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
            List<WebResourceSet> x = (List<WebResourceSet>) f.get(root);
            classResources = x;
        } catch (Exception e) {
            ctx.getLogger().warn("Failed accessing StandardRoot.classResources", e);
            return false;
        }

        if (classResources == null) {
            return false;
        }

        File libDir = new File(ctx.getWorkPath(), "WEB-INF/lib");
        ExpandWar.delete(libDir);

        boolean expanded = false;
        for (ListIterator<WebResourceSet> it = classResources.listIterator(); it.hasNext(); ) {
            WebResourceSet resource = it.next();
            if (!(resource instanceof JarWarResourceSet)) {
                continue;
            }

            JarWarResourceSet r1 = (JarWarResourceSet) resource;
            String archivePath;
            try {
                Field f = JarWarResourceSet.class.getDeclaredField("archivePath");
                f.setAccessible(true);
                archivePath = (String) f.get(r1);
            } catch (Exception e) {
                ctx.getLogger().warn("Failed retrieving JarWarResourceSet.archivePath", e);
                return false;
            }

            File file = new File(ctx.getWorkPath(), archivePath);
            ExpandWar.delete(file);
            try (JarFile warFile = new JarFile(r1.getBase())) {
                JarEntry jarFileInWar = warFile.getJarEntry(archivePath);
                try (InputStream jarFileIs = warFile.getInputStream(jarFileInWar)) {
                    // noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                    try (OutputStream jarFileOs = new FileOutputStream(file)) {
                        doCopy(jarFileIs, jarFileOs);
                    }
                }
            } catch (IOException e) {
                ctx.getLogger().warn(
                        String.format("Failed expanding resource [%s] to [%s]", archivePath, file), e
                );
            }

            JarResourceSet r2 = new JarResourceSet(root,
                    unOptimisePath(r1.getWebAppMount()),
                    file.getAbsolutePath(),
                    unOptimisePath(r1.getInternalPath()));
            try {
                r1.stop();
                r1.destroy();
            } catch (LifecycleException e) {
                // ignore
            }
            it.set(r2);
            expanded = true;
        }
        return expanded;
    }


    private static void doCopy(InputStream is, OutputStream os) throws IOException {
        int bufSize = 16384;
        byte[] b = new byte[bufSize];
        int n;
        while ((n = is.read(b, 0, bufSize)) > 0) {
            os.write(b, 0, n);
        }
    }


    private static String unOptimisePath(String path) {
        return (path == null || path.length() == 0) ? "/" : path;
    }
}
