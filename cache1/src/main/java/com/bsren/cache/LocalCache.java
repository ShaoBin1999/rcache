package com.bsren.cache;
import com.google.common.base.Ticker;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1.构造参数：cache最大数量
 * 2.实现Cache中的方法
 */
public class LocalCache<K,V>{

    static final int MAXIMUM_CAPACITY = 1 << 30;

    Ticker ticker = Ticker.systemTicker();

    int maxWeight;

    int segmentShift;

    int segmentMask;

    Segment<K, V>[] segments;

    public LocalCache(int maxWeight){
        this.maxWeight = Math.min(MAXIMUM_CAPACITY,maxWeight);
        int segmentShift = 0;
        int segmentCount = 1;
        while (segmentCount*20<maxWeight){
            segmentShift++;
            segmentCount<<=1;
        }
        this.segments = new Segment[segmentCount];
        this.segmentShift = 32 - segmentShift;
        this.segmentMask = segmentCount-1;
        int segmentCapacity = (this.maxWeight+segmentCount-1)/segmentCount;
        int segmentSize = 1;
        while (segmentSize<segmentCapacity){
            segmentSize<<=1;
        }
        for (int i=0;i<segments.length;i++){
            segments[i] = createSegment(segmentSize);
        }
    }

    private Segment<K,V> createSegment(int segmentSize) {
        return new Segment<>(this, segmentSize);
    }

    static class Segment<K, V> extends ReentrantLock {

        LocalCache<K,V> map;

        int segmentWeight;

        int threshold;

        int count;

        int modCount;

        AtomicInteger readCount = new AtomicInteger();


        AtomicReferenceArray<Entry<K, V>> table;

        Segment(LocalCache<K,V> map,int initialCapacity){
            this.map = map;
            this.segmentWeight = initialCapacity;
            initTable(initialCapacity);
        }

        private void initTable(int initialCapacity) {
            AtomicReferenceArray<Entry<K,V>> newTable = new AtomicReferenceArray<>(initialCapacity);
            this.threshold = newTable.length() * 3/4;
            this.table = newTable;
        }

        public V get(Object key, int hash) {
            try {
                if(count==0){
                    return null;
                }
                AtomicReferenceArray<Entry<K,V>> table = this.table;
                Entry<K, V> first = table.get(hash & (table.length() - 1));
                for (Entry<K,V> e = first; e!=null; e = e.getNext()){
                    if(e.getHash() != hash){
                        continue;
                    }
                    K entryKey = e.getKey();
                    if(equalsKey(entryKey,key)){
                        Object valueReference = e.getValueReference();
                        if(!(valueReference instanceof Value)){
                            recordRead(e);
                            return (V) valueReference;
                        }
                        V value = ((Value<K,V>) valueReference).get();
                        if(value!=null){
                            recordRead(e);
                            return value;
                        }
                    }
                }
                return null;
            }finally {
                postRead();
            }
        }

        private void postRead() {
            this.readCount.incrementAndGet();
        }

        private void recordRead(Entry<K, V> e) {

        }

        private boolean equalsKey(K entryKey, Object key) {
            return entryKey == key;
        }

        public V put(K key, int hash, V value) {
            lock();
            try {
                int newCount = this.count;
                if(newCount+1>threshold){
                    expand();
                }
                AtomicReferenceArray<Entry<K,V>> table = this.table;
                int index = hash & (table.length()-1);
                Entry<K,V> first = table.get(index);
                for (Entry<K,V> e = first;e!=null;e = e.getNext()){
                    K entryKey = e.getKey();
                    if(equalsKey(entryKey,key)){
                        Object valueReference = e.getValueReference();
                        V entryValue = null;
                        if(!(valueReference instanceof Value)){
                            entryValue = (V) valueReference;
                        } else {
                            entryValue = (V) ((Value)valueReference).get();
                        }
                        modCount++;
                        setValue(e,key,value);
                        this.count = newCount;
                        return entryValue;
                    }
                }
                modCount++;
                Entry<K,V> entry = new StrongEntry<>(key,hash,first);
                setValue(entry,key,value);
                table.set(index,entry);
                newCount = this.count+1;
                this.count = newCount;
                return null;
            }finally {
                unlock();
                postWrite();
            }
        }

        @GuardedBy("this")
        private void setValue(Entry<K,V> e, K key, V value) {
            e.setValue(value);
        }

        private void expand() {

        }

        private void postWrite() {

        }
    }

    public V getIfPresent(Object key){
        int hash = hash(key);
        return segmentFor(hash).get(key,hash);
    }

    public V put(K key,V value){
        int hash = hash(key);
        return segmentFor(hash).put(key,hash,value);
    }


    Segment<K, V> segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    private int hash(Object key) {
        return rehash(key.hashCode());
    }

    static int rehash(int h) {
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }
}
