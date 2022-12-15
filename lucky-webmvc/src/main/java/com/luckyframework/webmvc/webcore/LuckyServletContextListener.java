package com.luckyframework.webmvc.webcore;

import com.luckyframework.context.ApplicationContext;
import com.luckyframework.scanner.Scanner;
import com.luckyframework.webmvc.applicationcontext.WebAnnotationScannerApplicationContext;
import com.luckyframework.webmvc.exceptions.WebApplicationContentInitializedException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;

public class LuckyServletContextListener implements ServletContextListener {

    public final static String WEB_APPLICATION_NAME     =   "luckyWebApplicationContext";
    public final static String SCANNER_ROOT_CLASS_NAMES =   "lucky.scanner.rootClassNames";
    public final static String SCANNER_PACKAGES         =   "lucky.scanner.packages";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        ApplicationContext webAppContext= initializeApplicationContext(servletContext);
        setContextInitParameterToEnvironment(webAppContext.getEnvironment(),servletContext);
        servletContext.setAttribute(WEB_APPLICATION_NAME,webAppContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            ((ApplicationContext)sce.getServletContext().getAttribute(WEB_APPLICATION_NAME)).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 根据配置初始化ApplicationContext
    private ApplicationContext initializeApplicationContext(ServletContext servletContext){
//
//        // 1.ServletContext中的[lucky.scanner.packages]配置优先生效
//        String contextPackage = servletContext.getInitParameter(SCANNER_PACKAGES);
//        if(StringUtils.hasText(contextPackage)){
//            return initializeApplicationContextByPackages(contextPackage.split(","));
//        }
//
//        // 2.ServletContext中的[lucky.scanner.rootClassNames]配置其次生效
//        String contextClassNames = servletContext.getInitParameter(SCANNER_ROOT_CLASS_NAMES);
//        if(StringUtils.hasText(contextClassNames)){
//            return initializeApplicationContextByRootClassNames(contextClassNames.split(","));
//        }
//
//        // 3.lucky环境变量中的[lucky.scanner.packages]配置生效
//        org.springframework.core.env.Environment readOnlyEnvironment = Environment.defaultEnvironment();
//        if(readOnlyEnvironment.containsKey(SCANNER_PACKAGES)){
//            Object luckyPackage = readOnlyEnvironment.getProperty(SCANNER_PACKAGES);
//            if(luckyPackage instanceof String){
//                return initializeApplicationContextByPackages(luckyPackage.toString().split(","));
//            }
//            Class<?> luckyPackageClass = luckyPackage.getClass();
//            if((luckyPackageClass.isArray() || Collection.class.isAssignableFrom(luckyPackageClass))
//                    && luckyPackageClass.getComponentType()==String.class){
//                return initializeApplicationContextByPackages(readOnlyEnvironment.getProperty(SCANNER_PACKAGES,String[].class));
//            }
//            throw new WebApplicationContentInitializedException("Web Application Content failed to initialize, '"+SCANNER_PACKAGES+"' configuration error!");
//        }
//
//        // 5.lucky环境变量中的[lucky.scanner.rootClassNames]配置生效
//        if(readOnlyEnvironment.containsKey(SCANNER_ROOT_CLASS_NAMES)){
//            Object luckyRootClassNames = readOnlyEnvironment.getProperty(SCANNER_ROOT_CLASS_NAMES);
//            if(luckyRootClassNames instanceof String){
//                return initializeApplicationContextByRootClassNames(luckyRootClassNames.toString().split(","));
//            }
//            Class<?> luckyPackageClass = luckyRootClassNames.getClass();
//            if((luckyPackageClass.isArray() || Collection.class.isAssignableFrom(luckyPackageClass))
//                    && luckyPackageClass.getComponentType()==String.class){
//                return initializeApplicationContextByRootClassNames(readOnlyEnvironment.getProperty(SCANNER_PACKAGES,String[].class));
//            }
//            throw new WebApplicationContentInitializedException("Web Application Content failed to initialize, '"+SCANNER_ROOT_CLASS_NAMES+"' configuration error!");
//        }
//
//        // 6.没有任何配置，则使用默认的初始化方案
//        return initializeDefaultApplicationContext();
        return null;
    }

    // 初始化一个默认的ApplicationContext
    private ApplicationContext initializeDefaultApplicationContext(){
        try {
            Resource[] resources = Scanner.PM.getResources("classpath:/**/*.class");
            return WebAnnotationScannerApplicationContext.create(new HashSet<>(Arrays.asList(resources)));
        }catch (IOException e){
            throw new WebApplicationContentInitializedException("Web Application Content failed to initialize, an exception occurred while getting the class resource.",e);
        }
    }

    // 使用一组class全路径来初始化ApplicationContext
    private ApplicationContext initializeApplicationContextByRootClassNames(String... rootClassNames){
        Class<?>[] rootClasses = new Class[rootClassNames.length];
        for (int i = 0; i < rootClassNames.length; i++) {
            try {
                rootClasses[i] = Class.forName(rootClassNames[i]);
            } catch (ClassNotFoundException e) {
                throw new WebApplicationContentInitializedException("Web Application Content failed to initialize, no class file matching the configuration item '"+rootClassNames[i]+"'] was found.",e);
            }
        }
        return WebAnnotationScannerApplicationContext.create(rootClasses);
    }

    // 使用一组包名来初始化ApplicationContext
    private ApplicationContext initializeApplicationContextByPackages(String...packages){
        return WebAnnotationScannerApplicationContext.create(packages);
    }

    // 将Servlet上下文中的初始化参数设置到Lucky的环境变量中
    private void setContextInitParameterToEnvironment(Environment environment, ServletContext servletContext){
        Enumeration<String> initParameterNames = servletContext.getInitParameterNames();
        while (initParameterNames.hasMoreElements()){
            String key = initParameterNames.nextElement();
//            environment.setProperty(key,servletContext.getInitParameter(key));
        }
    }
}
