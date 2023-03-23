package com.github.lwr.tomcat;

import org.apache.catalina.Engine;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;

import java.io.*;
import java.net.*;

/**
 * TestCustomWebappClassLoader.
 *
 * @author <a href="mailto:williamleung2006@gmail.com">William Leung</a>
 */
abstract class TestCustomWebappClassLoader {


    String home = new File(System.getProperty("user.home")).getAbsolutePath();
    String warPath;
    String loaderClass;
    boolean unpackWARs;
    WebappClassLoaderBase loader;

    private final PrintStream out = System.out;


    void testWebappClassLoader() throws Exception {
        CustomWebappClassLoader.profiling = true;

        long start = System.currentTimeMillis();
        StandardContext ctx = newContext();
        ctx.start();
        long end = System.currentTimeMillis();

        loader = (WebappClassLoaderBase) ctx.getLoader().getClassLoader();

        out.print(""
                + "\n"
                + "=== Test Result ==================================\n"
                + ": DocRoot              : " + ctx.getResources().getResource("/").getURL() + "\n"
                + ": Context start time   : " + (end - start) + "\n"
                + ": HostSpot (loadClass) : " + ((loader instanceof CustomWebappClassLoader)
                ? ((CustomWebappClassLoader) loader).hostSpotTime : "unknown") + "\n"
                + "=== Dump JARs   ==================================\n"
                + "");
        for (URL url : loader.getURLs()) {
            out.println(url);
        }
        out.print(""
                + "=== Test End    ==================================\n"
                + "");
    }


    private StandardContext newContext() {
        org.apache.tomcat.util.file.ConfigFileLoader.setSource(
                new org.apache.tomcat.util.file.ConfigurationSource() {
                    @Override
                    @SuppressWarnings("deprecation")
                    public Resource getResource(String name) {
                        return new Resource(new StringBufferInputStream("<x/>"), getURI(name));
                    }

                    @Override
                    public URI getURI(String name) {
                        return new File(name).toURI();
                    }
                }
        );

        Engine engine = new StandardEngine();
        StandardHost host = new StandardHost();
        StandardContext ctx = new StandardContext();
        ctx.setOverride(true);
        ctx.addLifecycleListener(new Tomcat.FixContextListener());
        ctx.addLifecycleListener(new ContextConfig());
        ctx.setParent(host);

        String appBase = home + "/tmp/tomcat-webapp-loader-test/WebApps";
        // noinspection ResultOfMethodCallIgnored
        new File(appBase).mkdirs();

        host.setName("localhost");
        host.setUnpackWARs(unpackWARs);
        host.setAppBase(appBase);
        host.setParent(engine);

        engine.setName("Catalina");
        engine.setService(new StandardService());
        engine.getService().setServer(new StandardServer());
        engine.getService().getServer().setCatalinaHome(new File(home));
        engine.getService().getServer().setCatalinaBase(new File(home));

        ctx.setName(warPath.replaceAll(".*/|\\.war$", ""));
        ctx.setPath("/" + ctx.getName());
        ctx.setDocBase(warPath);
        ctx.setWorkDir(home + "/tmp/tomcat-webapp-loader-test/work/" + ctx.getName());

        if (loaderClass != null) {
            // workarounds this exception with <JarScanner scanClassPath="false" /> since tomcat 9.0.16
            // > Caused by: java.lang.IllegalArgumentException: More than one fragment with the name [spring_web] was found. ...
            // >         at org.apache.tomcat.util.descriptor.web.WebXml.orderWebFragments(WebXml.java:2275)
            // >         ... ...
            ((StandardJarScanner) ctx.getJarScanner()).setScanClassPath(false);
            ctx.setLoader(new WebappLoader());
            ((WebappLoader) ctx.getLoader()).setLoaderClass(loaderClass);
        }
        return ctx;
    }
}
