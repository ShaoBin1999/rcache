package com.bsren.cache;


public interface Entry<K,V> {


    Entry<K,V> getNext();

    int getHash();

    K getKey();

    Object getValueReference();

    void setValue(Object value);
    
    void setAccessTime(long time);
    
    long getAccessTime();
    
    void setWriteTime(long time);
    
    long getWriteTime();

    Entry<K, V> getNextInAccessQueue();
    
    void setNextInAccessQueue(Entry<K, V> next);

    Entry<K, V> getPreviousInAccessQueue();
    
    void setPreviousInAccessQueue(Entry<K, V> previous);

    Entry<K, V> getNextInWriteQueue();

    void setNextInWriteQueue(Entry<K, V> next);

    Entry<K, V> getPreviousInWriteQueue();

    void setPreviousInWriteQueue(Entry<K, V> previous);
    
}
