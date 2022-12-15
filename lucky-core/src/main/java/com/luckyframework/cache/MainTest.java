package com.luckyframework.cache;

import com.luckyframework.cache.impl.FIFOCache;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/10 15:59
 */
public class MainTest {

    public static void main(String[] args) {
        Cache<String, String> cacheMap = new FIFOCache<>(3);

        cacheMap.put("1", "111");
        cacheMap.put("2", "222");
        cacheMap.put("3", "333");

        System.out.println(cacheMap);

        cacheMap.get("1");
        cacheMap.get("1");
        cacheMap.get("1");
        cacheMap.get("2");
        cacheMap.get("2");
        cacheMap.get("3");
        cacheMap.put("4", "444");
        System.out.println(cacheMap);

    }
}
