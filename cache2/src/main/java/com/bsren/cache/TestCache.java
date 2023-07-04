package com.bsren.cache;

import org.junit.Test;

public class TestCache {

    @Test
    public void testGetAndSet(){
        LocalCache<String,String> localCache = new LocalCache<>(100);
        localCache.put("rsb","24");
        System.out.println(localCache.getIfPresent("rsb"));
    }

    @Test
    public void testExpand(){
        LocalCache<String,String> localCache = new LocalCache<>(10);
        for (int i=0;i<100;i++){
            localCache.put("case"+i,i+"0");
        }
        System.out.println(localCache.getIfPresent("case"+99));
    }
}
