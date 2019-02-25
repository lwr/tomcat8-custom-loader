/*
 * Copyright (c) 2019 Mailtech.cn, Ltd. All Rights Reserved.
 */

package com.github.lwr.tomcat;

import org.junit.Ignore;
import org.junit.Test;

import java.net.*;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * TestLoaderWithSpringBootTestWar.
 *
 * @author <a href="mailto:lwr@coremail.cn">William Leung</a>
 */
public class TestLoaderWithSpringBootTestWar extends TestCustomWebappClassLoader {

    public TestLoaderWithSpringBootTestWar() {
        warPath = home + "/.m2/repository/org/springframework/boot"
                + "/spring-boot-deployment-test-tomcat/1.5.7.RELEASE/spring-boot-deployment-test-tomcat-1.5.7.RELEASE.war";
    }


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
    public void testCustomLoaderUnpackWAR() throws Exception {
        unpackWARs = true;
        loaderClass = CustomWebappClassLoader.class.getName();
        testWebappClassLoader();
    }


    @Test
    public void testCustomLoaderForWAR() throws Exception {
        unpackWARs = false;
        loaderClass = CustomWebappClassLoader.class.getName();
        testWebappClassLoader();

        String unzippedDir = home + "/tmp/tomcat-webapp-loader-test/work/spring-boot-deployment-test-tomcat-1.5.7.RELEASE";
        assertEquals(Arrays.asList(
                new URL("jar:file:" + warPath + "!/WEB-INF/classes/"),
                new URL("file:" + unzippedDir + "/WEB-INF/lib/spring-beans-4.3.11.RELEASE.jar"),
                new URL("file:" + unzippedDir + "/WEB-INF/lib/jcl-over-slf4j-1.7.25.jar")
        ), Arrays.asList(loader.getURLs()).subList(0, 3));
    }
}
