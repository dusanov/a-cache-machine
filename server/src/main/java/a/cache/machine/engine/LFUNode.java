package a.cache.machine.engine;

import java.io.Serializable;

public class LFUNode<K,V> implements Serializable {
    private static final long serialVersionUID = 123L;
    K key;
    V value;
    int frequencyCount;
    LFUNode<K,V> next;
    LFUNode<K,V> prev;

    public LFUNode(K key, V val) {
        this.key = key;
        this.value = val;
        // Initial frequency is 1
        frequencyCount = 1;
    }
}
