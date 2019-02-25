package com.github.lwr.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.webresources.LWR_CustomHelper;
import org.apache.catalina.webresources.StandardRoot;

import java.io.*;
import java.net.*;

/**
 * A custom webapp ClassLoader to resolve Tomcat8's low performance issue.
 * <p/>
 * Check <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=57251">the discussion</a> for more details.
 *
 * @author <a href="mailto:williamleung2006@gmail.com">William Leung</a>
 */
public class CustomWebappClassLoader extends WebappClassLoaderBase {

    public CustomWebappClassLoader() {
        super();
    }


    public CustomWebappClassLoader(ClassLoader parent) {
        super(parent);
    }


    @Override
    public CustomWebappClassLoader copyWithoutTransformers() {

        CustomWebappClassLoader result = new CustomWebappClassLoader(getParent());

        super.copyStateWithoutTransformers(result);

        try {
            result.start();
        } catch (LifecycleException e) {
            throw new IllegalStateException(e);
        }

        return result;
    }


    @Override
    protected Object getClassLoadingLock(String className) {
        return this;
    }


    private boolean jarWarExpanded;

    @Override
    public void start() throws LifecycleException {
        super.start();
        jarWarExpanded = false;
        if (getResources() instanceof StandardRoot && getResources().getContext() instanceof StandardContext) {
            jarWarExpanded = LWR_CustomHelper.expandJarWarResources(
                    (StandardContext) getResources().getContext(),
                    (StandardRoot) getResources());
        }
    }


    @Override
    public URL[] getURLs() {
        URL[] urls = super.getURLs();
        if (jarWarExpanded) {
            // file:/... .../xxx.war!/
            String base = getResources().getResource("/").getURL().getFile();
            for (int i = 0; i < urls.length; i++) {
                URL url = urls[i];
                if (url.toString().startsWith("war:file:")) {
                    try {
                        // https://github.com/apache/tomcat80/commit/7e767cc6efe79cdd367213da3c1f88711a29ad7a
                        // a new uri schema "war:file:/.../xxx.war*/..." was introduced in tomcat 8.0.38
                        // it should be translated to jar schema first, and then to unpacked file URLs
                        urls[i] = url = UriUtil.warToJar(url);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                if ("jar".equals(url.getProtocol()) && url.getFile().endsWith(".jar") && url.getFile().startsWith(base)) {
                    File file = new File(((StandardContext) getResources().getContext()).getWorkPath(),
                            url.getFile().substring(base.length()));
                    if (file.isFile()) {
                        try {
                            urls[i] = file.toURI().toURL();
                        } catch (java.net.MalformedURLException e) {
                            // never happened
                        }
                    }
                }
            }
        }
        return urls;
    }


    static boolean profiling;
    long hostSpotTime;


    @Override
    public Class<?> findClassInternal(String name) {
        if (profiling) {
            long start = System.currentTimeMillis();
            try {
                return super.findClassInternal(name);
            } finally {
                hostSpotTime += (System.currentTimeMillis() - start);
            }
        } else {
            return super.findClassInternal(name);
        }
    }
}
