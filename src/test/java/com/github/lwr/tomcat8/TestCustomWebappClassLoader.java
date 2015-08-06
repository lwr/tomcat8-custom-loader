package com.github.lwr.tomcat8;

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
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.*;

/**
 * TestTomcat8ClassLoader.
 *
 * @author <a href="mailto:williamleung2006@gmail.com">William Leung</a>
 */
public class TestCustomWebappClassLoader {


    String home = new File(System.getProperty("user.home")).getAbsolutePath();
    String warPath = home + "/.m2/repository/org/springframework/boot"
            + "/spring-boot-deployment-test-tomcat/1.2.5.RELEASE/spring-boot-deployment-test-tomcat-1.2.5.RELEASE.war";
    String loaderClass;
    boolean unpackWARs;


    PrintStream out = System.out;


    @Test
    @Ignore
    public void testTomcatLoaderUnpackWAR() throws Exception {
        unpackWARs = true;
        testWebappClassLoader();
    }


    @Test
    @Ignore
    public void testTomcatLoaderForWAR() throws Exception {
        unpackWARs = false;
        testWebappClassLoader();
    }


    @Test
    @Ignore
    public void testCustomLoaderUnpackWAR() throws Exception {
        unpackWARs = true;
        loaderClass = CustomWebappClassLoader.class.getName();
        testWebappClassLoader();
    }


    @Test
    @Ignore
    public void testCustomLoaderForWAR() throws Exception {
        unpackWARs = false;
        loaderClass = CustomWebappClassLoader.class.getName();
        testWebappClassLoader();
    }


    void testWebappClassLoader() throws Exception {
        CustomWebappClassLoader.profiling = true;

        long start = System.currentTimeMillis();
        StandardContext ctx = newContext();
        ctx.start();
        long end = System.currentTimeMillis();

        WebappClassLoaderBase loader = (WebappClassLoaderBase) ctx.getLoader().getClassLoader();

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
        StandardContext ctx = new StandardContext();

        ctx.addLifecycleListener(new Tomcat.FixContextListener());
        ctx.addLifecycleListener(new ContextConfig());

        String appBase = home + "/tmp/tomcat8-webapp-loader-test/test-host-webapps";
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

        ctx.setName(warPath.replaceAll(".*/", ""));
        ctx.setPath("/" + ctx.getName());
        ctx.setDocBase(warPath);
        ctx.setWorkDir(home + "/tmp/tomcat8-webapp-loader-test/test-host-work");

        if (loaderClass != null) {
            ctx.setLoader(new WebappLoader());
            ((WebappLoader) ctx.getLoader()).setLoaderClass(loaderClass);
        }
        return ctx;
    }
}
