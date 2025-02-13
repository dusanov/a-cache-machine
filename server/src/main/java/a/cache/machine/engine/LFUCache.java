package a.cache.machine.engine;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFUCache<K,V> implements ICache<K,V> {

    private static final Logger logger = LoggerFactory.getLogger(LFUCache.class);

    // Maps key to the Node
    private Map<K, LFUNode<K,V>> cache;

    // Maps frequency to doubly linked list
    //(head, tail) of Nodes with that frequency
    private Map<Integer, LFUFrequencyPair<LFUNode<K,V>, LFUNode<K,V>> > frequencyMap;

    // Tracks the minimum frequency
    private int minFrequency;

    // Capacity of the LFU cache
    // private int capacity;
    private final long maxSizeInBytes;
    private final AtomicLong currentSizeInBytes;
    private final List<ICacheEventListener> listeners;
    private final CacheMetrics metrics;    

    // Constructor to initialize LFUCache with a given capacity
    public LFUCache(long maxSizeInBytes) {
        // this.capacity = capacity;

        // Initial minimum frequency is 0
        minFrequency = 0;
        cache = new ConcurrentHashMap<>();
        frequencyMap = new ConcurrentHashMap<>();
        this.maxSizeInBytes = maxSizeInBytes;
        this.currentSizeInBytes = new AtomicLong(0);
        this.listeners = new CopyOnWriteArrayList<>();
        this.metrics = new CacheMetrics();        
    }

    @Override
    public V get(K key) {
        if (!cache.containsKey(key)) {
            listeners.forEach(listener -> listener.onMiss(key.toString()));
            metrics.incrementMisses();            
            return null;
        }
        LFUNode<K,V> node = cache.get(key);
        V res = node.value;
        updateFreq(node);
        listeners.forEach(listener -> listener.onHit(key.toString()));
        metrics.incrementHits();        
        return res;
    }

    // Function to put a key-value pair into the cache
    @Override
    public V put(K key, V value) throws CacheException {

        if (key == null || value == null) {
            throw new CacheException("Key or value cannot be null");
        }
        if (cache.containsKey(key)) {
            LFUNode<K,V> node = cache.get(key);
            currentSizeInBytes.addAndGet(-estimateSize(node.value));
            node.value = value;
            updateFreq(node);
            currentSizeInBytes.addAndGet(estimateSize(value));
            return value;
        } else {
            if (currentSizeInBytes.get() >= maxSizeInBytes) {
                LFUNode<K,V> node = frequencyMap.get(minFrequency).second.prev;
                cache.remove(node.key);
                currentSizeInBytes.addAndGet(-estimateSize(node.value));
                listeners.forEach(listener -> listener.onEviction(key.toString(), value));
                metrics.incrementEvictions();                
                remove(node);
                // Remove the frequency list if it's empty
                if (frequencyMap.get(minFrequency).first.next == frequencyMap.get(minFrequency).second) {
                    frequencyMap.remove(minFrequency);
                }
            }

            // Create a new node for the key-value pair
            LFUNode<K,V> node = new LFUNode<>(key, value);
            cache.put(key, node);
            currentSizeInBytes.addAndGet(estimateSize(value));
            // Reset minimum frequency to 1
            minFrequency = 1;
            add(node, 1);
            return value;
        }
    }

    // Add a node right after the head
    void add(LFUNode<K,V> node, int freq) {

        // Initialize the frequency list if it doesn't exist
        if (!frequencyMap.containsKey(freq)) {

            // Dummy head node
            LFUNode<K,V> head = new LFUNode<>(null,null);

            // Dummy tail node
            LFUNode<K,V> tail = new LFUNode<>(null,null);
            head.next = tail;
            tail.prev = head;
            frequencyMap.put(freq, new LFUFrequencyPair<>(head, tail));
        }

        // Insert the node right after the head
        LFUNode<K,V> head = frequencyMap.get(freq).first;
        LFUNode<K,V> temp = head.next;
        node.next = temp;
        node.prev = head;
        head.next = node;
        temp.prev = node;
    }

    // Remove a node from the list
    void remove(LFUNode<K,V> node) {

        // Update pointers to exclude the node
        LFUNode<K,V> delprev = node.prev;
        LFUNode<K,V> delnext = node.next;
        delprev.next = delnext;
        delnext.prev = delprev;
    }

    // Update the frequency of a node
    void updateFreq(LFUNode<K,V> node) {

        // Get the current frequency
        int oldFreq = node.frequencyCount;

        // Increment the frequency
        node.frequencyCount++;

        // Remove the node from the current frequency list
        remove(node);
        if (frequencyMap.get(oldFreq).first.next
            == frequencyMap.get(oldFreq).second) {
            frequencyMap.remove(oldFreq);

            // Update minimum frequency if needed
            if (minFrequency == oldFreq) {
                minFrequency++;
            }
        }

        // Add the node to the updated frequency list
        add(node, node.frequencyCount);
    }

    private long estimateSize(V value) {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);) {
            oos.writeObject(value);
            return baos.size();
        } catch (IOException e) {
            // Fallback to a rough estimation if serialization fails
            return value.toString().length() * 2; // Rough estimate assuming String chars are 2 bytes
        }
    }
    // Other methods (remove, clear, shutdown, etc.)

    @Override
    public synchronized void shutdown() throws CacheException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("cache.dat"))) {
            oos.writeObject(cache);
        } catch (IOException e) {
            throw new CacheException("Failed to persist cache to disk", e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void loadFromDisk() throws CacheException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("cache.dat"))) {
            ConcurrentHashMap<K, V> cacheFromDisk = (ConcurrentHashMap<K, V>) ois.readObject();
            cacheFromDisk.forEach((k, v) -> {
                try {
                    this.put(k, (V) ((LFUNode)v).value);
                } catch (CacheException e) {
                    logger.error(e.getLocalizedMessage());
                }
            });
        } catch (IOException | ClassNotFoundException e) {
            throw new CacheException("Failed to load cache from disk", (Throwable) e);
        }
    }

    @Override
    public V remove(K key) throws CacheException {
        frequencyMap.remove(key);
        return cache.remove(key).value;
    }

    @Override
    public void clear() throws CacheException {
        frequencyMap.clear();
        cache.clear();
    }
    @Override
    public int size() {
        return cache.size();
    }

    public long currentSizeInBytes() {
        return currentSizeInBytes.get();
    }

    @Override
    public void addEventListener(ICacheEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(ICacheEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public CacheMetrics getMetrics() {
        return metrics;
    }
}