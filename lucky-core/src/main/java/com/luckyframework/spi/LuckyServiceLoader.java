package com.luckyframework.spi;

import org.springframework.objenesis.instantiator.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Lucky的SPI（服务发现机制）
 * 规则：
 *  1.文件位置：META-INF/lucky/
 *  2.文 件 名：以接口的全限定名结尾即可
 *  3.文件内容：接口实现类全限定名(一行一个实现)
 * @author fk
 * @version 1.0
 * @date 2020/12/10 0010 9:52
 */
public class LuckyServiceLoader<S> implements Iterable<S> {

    private static final String PREFIX = "META-INF/lucky/";
    private final String SUFFIX;

    // The class or interface representing the service being loaded
    private final Class<S> service;

    // The class loader used to locate, load, and instantiate providers
    private final ClassLoader loader;

    private static Map<String,Set<Class<?>>> cacheServiceImpls;

    private LuckyServiceLoader(Class<S> svc, ClassLoader cl) {
        if(cacheServiceImpls==null)cacheServiceImpls=new HashMap<>();
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        SUFFIX = svc.getName();
        if(!cacheServiceImpls.containsKey(SUFFIX)){
            findResources();
        }
    }

    private void findResources(){
        try {
            Enumeration<URL> resources = loader.getResources(PREFIX);
            Set<Class<?>> impls=new HashSet<>();
            while (resources.hasMoreElements()){
                URL url = resources.nextElement();
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jarFile = conn.getJarFile();
                findJarFile(impls,jarFile);
            }
            cacheServiceImpls.put(SUFFIX,impls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findJarFile(Set<Class<?>> impls, JarFile jarFile){
        Enumeration<JarEntry> en = jarFile.entries();
        InputStream input = null;
        try {
            while (en.hasMoreElements()) {
                JarEntry jarEntry = en.nextElement();
                String name = jarEntry.getName();
                if(name.startsWith(PREFIX) && name.endsWith(SUFFIX)){
                    input=jarFile.getInputStream(jarEntry);
                    BufferedReader br=new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                    br.lines().map(str -> str.contains("#") ? str.substring(0,str.indexOf("#")) : str)
                            .filter(str -> !StringUtils.hasText(str)&&!str.startsWith("#"))
                            .forEach((str)->{
                                str = str.trim();
                                try {
                                    Class<?> aClass = loader.loadClass(str);
                                    if(service.isAssignableFrom(aClass)){
                                        impls.add(aClass);
                                    }
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            });
                    br.close();
                    input.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <S> LuckyServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new LuckyServiceLoader<>(service, loader);
    }


    public static <S> LuckyServiceLoader<S> load(Class<S> service) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return LuckyServiceLoader.load(service, cl);
    }

    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>() {
            final Set<Class<?>> serviceClassSet=cacheServiceImpls.get(SUFFIX);
            final Iterator<Class<?>> serviceClassIterator=serviceClassSet.iterator();

            @Override
            public boolean hasNext() {
                return serviceClassIterator.hasNext();
            }

            @Override
            @SuppressWarnings("unchecked")
            public S next() {
                Class<?> serviceClass = serviceClassIterator.next();
                return (S) ClassUtils.newInstance(serviceClass);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
