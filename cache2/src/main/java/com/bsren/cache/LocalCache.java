package com.bsren.cache;

import com.bsren.cache.queue.AccessQueue;
import com.bsren.cache.queue.WriteQueue;
import com.google.common.base.Ticker;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1.构造参数：cache最大数量
 * 2.实现Cache中的方法,get and put
 * 3.expand
 * 4.cache其他方法
 * 5.读超时和写超时，读超时和写超时队列，读队列
 */
public class LocalCache<K, V> {

    static final int MAXIMUM_CAPACITY = 1 << 30;

    Ticker ticker = Ticker.systemTicker();

    int maxWeight;

    int segmentShift;

    int segmentMask;

    Segment<K, V>[] segments;

    public LocalCache(int maxWeight) {
        this.maxWeight = Math.min(MAXIMUM_CAPACITY, maxWeight);
        int segmentShift = 0;
        int segmentCount = 1;
        while (segmentCount * 20 < maxWeight) {
            segmentShift++;
            segmentCount <<= 1;
        }
        this.segments = new Segment[segmentCount];
        this.segmentShift = 32 - segmentShift;
        this.segmentMask = segmentCount - 1;
        int segmentCapacity = (this.maxWeight + segmentCount - 1) / segmentCount;
        int segmentSize = 1;
        while (segmentSize < segmentCapacity) {
            segmentSize <<= 1;
        }
        for (int i = 0; i < segments.length; i++) {
            segments[i] = createSegment(segmentSize);
        }
    }

    private Segment<K, V> createSegment(int segmentSize) {
        return new Segment<>(this, segmentSize);
    }

    static class Segment<K, V> extends ReentrantLock {

        LocalCache<K, V> map;

        int segmentWeight;

        int threshold;

        int count;

        int modCount;

        @GuardedBy("this")
        Queue<Entry<K,V>> writeQueue;

        @GuardedBy("this")
        Queue<Entry<K,V>> accessQueue;

        Queue<Entry<K,V>> recencyQueue;

        AtomicInteger readCount = new AtomicInteger();


        AtomicReferenceArray<Entry<K, V>> table;

        Segment(LocalCache<K, V> map, int initialCapacity) {
            this.map = map;
            this.segmentWeight = initialCapacity;
            initTable(initialCapacity);
            writeQueue = new WriteQueue<>();
            accessQueue = new AccessQueue<>();
            recencyQueue = new ConcurrentLinkedDeque<>();
        }

        private void initTable(int initialCapacity) {
            AtomicReferenceArray<Entry<K, V>> newTable = new AtomicReferenceArray<>(initialCapacity);
            this.threshold = newTable.length() * 3 / 4;
            this.table = newTable;
        }


        Entry<K,V> getLiveEntry(Object key,int hash, long now){
            Entry<K,V> e = getEntry(key,hash);
            if(e==null){
                return null;
            }else if(map.isExpired(e,now)){
                tryExpireEntries(now);
                return null;
            }
            return e;
        }

        private void tryExpireEntries(long now) {
            if(tryLock()){
                try {
                    expireEntries(now);
                }finally {
                    unlock();
                }
            }
        }

        @GuardedBy("this")
        private void expireEntries(long now) {
            drainRecencyQueue();
            Entry<K,V> e;
            while ((e = writeQueue.peek())!=null && map.isExpired(e,now)){
                if(!removeEntry(e)){
                    throw new AssertionError();
                }
            }
            while ((e = accessQueue.peek())!=null && map.isExpired(e,now)){
                if(!removeEntry(e)){
                    throw new AssertionError();
                }
            }
        }

        @GuardedBy("this")
        private boolean removeEntry(Entry<K,V> entry) {
            int newCount = this.count-1;
            AtomicReferenceArray<Entry<K,V>> table = this.table;
            int index = entry.getHash() & (table.length()-1);
            Entry<K,V> first = table.get(index);
            for (Entry<K,V> e = first;e !=null;e = e.getNext()){
                if(e==entry){    //比较的地址相等
                    modCount++;
                    Entry<K,V> newFirst = removeValueFromChain(first,e,e.getKey(),entry.getHash(),e.getValueReference());
                    newCount = this.count-1;
                    table.set(index,newFirst);
                    this.count = newCount;
                    return true;
                }
            }
            return false;
        }

        private Entry<K,V> removeValueFromChain(Entry<K,V> first, Entry<K,V> e, K key, int hash, Object valueReference) {
            return null;
        }

        Entry<K,V> getEntry(Object key,int hash){
            AtomicReferenceArray<Entry<K,V>> table = this.table;
            int index = hash & (table.length()-1);
            Entry<K,V> first = table.get(index);
            for (Entry<K,V> e = first;e!=null;e = e.getNext()){
                K entryKey = e.getKey();
                if(equalsKey(entryKey,key)){
                    return e;
                }
            }
            return null;
        }



        public V get(Object key, int hash) {
            try {
                if (count == 0) {
                    return null;
                }
                AtomicReferenceArray<Entry<K, V>> table = this.table;
                Entry<K, V> first = table.get(hash & (table.length() - 1));
                for (Entry<K, V> e = first; e != null; e = e.getNext()) {
                    if (e.getHash() != hash) {
                        continue;
                    }
                    K entryKey = e.getKey();
                    if (equalsKey(entryKey, key)) {
                        Object valueReference = e.getValueReference();
                        long now = map.ticker.read();
                        if (!(valueReference instanceof Value)) {
                            recordRead(e,now);
                            return (V) valueReference;
                        }
                        V value = ((Value<K, V>) valueReference).get();
                        if (value != null) {
                            recordRead(e,now);
                            return value;
                        }
                    }
                }
                return null;
            } finally {
                postRead();
            }
        }

        private void postRead() {
            this.readCount.incrementAndGet();
        }

        private void recordRead(Entry<K, V> e,long time) {
            e.setAccessTime(time);
            recencyQueue.add(e);
        }

        private boolean equalsKey(K entryKey, Object key) {
            return entryKey.equals(key);
        }

        public V put(K key, int hash, V value) {
            lock();
            try {
                int newCount = this.count;
                if (newCount + 1 > threshold) {
                    expand();
                }
                AtomicReferenceArray<Entry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                Entry<K, V> first = table.get(index);
                long now = map.ticker.read();
                for (Entry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (equalsKey(entryKey, key)) {
                        Object valueReference = e.getValueReference();
                        V entryValue = null;
                        if (!(valueReference instanceof Value)) {
                            entryValue = (V) valueReference;
                        } else {
                            entryValue = (V) ((Value) valueReference).get();
                        }
                        modCount++;
                        setValue(e, key, value,now);
                        this.count = newCount;
                        return entryValue;
                    }
                }
                modCount++;
                Entry<K, V> entry = new StrongEntry<>(key, hash, first);
                setValue(entry, key, value,now);
                table.set(index, entry);
                newCount = this.count + 1;
                this.count = newCount;
                return null;
            } finally {
                unlock();
                postWrite();
            }
        }

        @GuardedBy("this")
        private void setValue(Entry<K, V> e, K key, V value,long now) {
            e.setValue(value);
            recordWrite(e,now);
        }

        @GuardedBy("this")
        private void recordWrite(Entry<K,V> e, long now) {
            drainRecencyQueue();
            e.setAccessTime(now);
            e.setWriteTime(now);
            accessQueue.add(e);
            writeQueue.add(e);
        }

        @GuardedBy("this")
        private void drainRecencyQueue() {
            Entry<K,V> e;
            while ((e = recencyQueue.poll())!=null){
                accessQueue.add(e);
            }
        }

        @GuardedBy("this")
        private void expand() {
            AtomicReferenceArray<Entry<K, V>> oldTable = this.table;
            int oldCapacity = oldTable.length();
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                return;
            }
            int newCount = this.count;
            AtomicReferenceArray<Entry<K, V>> newTable = new AtomicReferenceArray<>(oldCapacity << 1);
            this.threshold = newTable.length() * 3 / 4;
            int newMask = newTable.length() - 1;
            for (int oldIndex = 0; oldIndex < oldCapacity; oldIndex++) {
                Entry<K, V> head = oldTable.get(oldIndex);
                if (head == null) {
                    continue;
                }
                Entry<K, V> next = head.getNext();
                int headIndex = head.getHash() & newMask;
                if (next == null) {
                    newTable.set(headIndex, head);
                } else {
                    Entry<K, V> tail = head;
                    int tailIndex = headIndex;
                    for (Entry<K, V> e = next; e != null; e = e.getNext()) {
                        int newIndex = e.getHash() & newMask;
                        if (newIndex != tailIndex) {
                            tailIndex = newIndex;
                            tail = e;
                        }
                    }
                    newTable.set(tailIndex, tail);

                    for (Entry<K, V> e = head; e != tail; e = e.getNext()) {
                        int newIndex = e.getHash() & newMask;
                        Entry<K, V> newNext = newTable.get(newIndex);
                        Entry<K, V> newFirst = copyEntry(e, newNext);
                        newTable.set(newIndex, newFirst);
                    }
                }
            }
            table = newTable;
            this.count = newCount;
        }

        private Entry<K, V> copyEntry(Entry<K, V> original, Entry<K, V> newNext) {
            return new StrongEntry<>(original.getKey(), original.getHash(), newNext);
        }

        private void postWrite() {

        }

        public void clear() {

        }

        public V remove(@Nullable Object key, int hash) {
            lock();
            try {
                long now = map.ticker.read();
                preWrite(now);
                int newCount = this.count - 1;
                AtomicReferenceArray<Entry<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                Entry<K, V> first = table.get(index);
                for (Entry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if(equalsKey(entryKey,key)){
                        Object valueReference = e.getValueReference();
                        if(!(valueReference instanceof ValueReference)){
                            modCount++;
                        }
                    }
                }

            } finally {
                unlock();
                postWrite();
            }
            return null;
        }

        private void preWrite(long now) {

        }
    }

    private boolean isExpired(Entry<K, V> e, long now) {
        return false;
    }

    public V getIfPresent(Object key) {
        int hash = hash(key);
        return segmentFor(hash).get(key, hash);
    }

    public V put(K key, V value) {
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value);
    }


    public void cleanUp() {
        for (Segment<K, V> segment : segments) {
            segment.clear();
        }
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    private V remove(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash);
    }

    public void invalidateAll(Iterable<?> keys) {
        // TODO(fry): batch by segment
        for (Object key : keys) {
            remove(key);
        }
    }

    public void invalidate(Object key) {

    }

    public int size() {
        int ret = 0;
        for (Segment<K, V> segment : segments) {
            ret += segment.count;
        }
        return ret;
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
