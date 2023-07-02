package com.bsren.cache;

import org.junit.Test;

public class TestCache {

    @Test
    public void test1(){
        LocalCache<String,String> localCache = new LocalCache<>(100);
        localCache.put("rsb","24");
        System.out.println(localCache.getIfPresent("rsb"));
    }
}
