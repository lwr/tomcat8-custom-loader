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
                + ": DocRoot              : " + loader.getResources().getResource("/").getURL() + "\n"
                + ": Context load time    : " + (end - start) + "\n"
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
                        return new Resource(new java.io.StringBufferInputStream("<x/>"), getURI(name));
                    }

                    @Override
                    public URI getURI(String name) {
                        return new File(name).toURI();
                    }
                }
        );

        StandardContext ctx = new StandardContext();
        ctx.setOverride(true);

        ctx.addLifecycleListener(new Tomcat.FixContextListener());
        ctx.addLifecycleListener(new ContextConfig());

        String appBase = home + "/tmp/tomcat-webapp-loader-test/WebApps";
        // noinspection ResultOfMethodCallIgnored
        new File(appBase).mkdirs();

        ctx.setParent(new StandardHost());
        ctx.getParent().setParent(new StandardEngine());
        ((StandardHost) ctx.getParent()).setAppBase(appBase);
        ((StandardHost) ctx.getParent()).setUnpackWARs(unpackWARs);
        ((Engine) ctx.getParent().getParent()).setService(new StandardService());
        ((Engine) ctx.getParent().getParent()).getService().setServer(new StandardServer());
        ((Engine) ctx.getParent().getParent()).getService().getServer().setCatalinaHome(new File(home));
        ((Engine) ctx.getParent().getParent()).getService().getServer().setCatalinaBase(new File(home));

        ctx.setName(warPath.replaceAll(".*/|\\.war$", ""));
        ctx.setPath("/" + ctx.getName());
        ctx.setDocBase(warPath);
        ctx.setWorkDir(home + "/tmp/tomcat-webapp-loader-test/work/" + ctx.getName());

        if (loaderClass != null) {
            ctx.setLoader(new WebappLoader());
            ((WebappLoader) ctx.getLoader()).setLoaderClass(loaderClass);
        }
        return ctx;
    }
}
