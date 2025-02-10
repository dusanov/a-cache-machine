package a.cache.machine.engine;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

//TODO: this is not concurrent
public class LRUCache<K,V> extends LinkedHashMap<K, V> implements ICache<K,V> {

    private final int maxCapacity;
    private final AtomicLong currentSizeInBytes;
    private final List<ICacheEventListener> listeners;
    private final CacheMetrics metrics;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.maxCapacity = capacity;
        this.currentSizeInBytes = new AtomicLong(0);
        this.listeners = new CopyOnWriteArrayList<>();
        this.metrics = new CacheMetrics();        
    }

    @Override
    public V get(Object key) {
        @SuppressWarnings("unchecked")
        K typedKey = (K) key;
        if (super.containsKey(typedKey)) {
            listeners.forEach(listener -> listener.onHit(typedKey.toString()));
            metrics.incrementHits();
            return super.get(typedKey);
        } else {
            listeners.forEach(listener -> listener.onMiss(key.toString()));
            metrics.incrementMisses();
            return null;
        }
    }    

    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key or value cannot be null");
        }

        long objectSize = estimateSize(value);
        // while (currentSizeInBytes.get() + objectSize > maxSizeInBytes) {
        //     evict();
        // }

        currentSizeInBytes.addAndGet(objectSize);
        return super.put(key, value);
    }    

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() > maxCapacity;
    }

    private long estimateSize(V value) {
        try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
        ) {
            oos.writeObject(value);
            return baos.size();
        } catch (IOException e) {
            // Fallback to a rough estimation if serialization fails
            return value.toString().length() * 2; // Rough estimate assuming String chars are 2 bytes
        }
    }    

    @Override
    public void shutdown() throws CacheException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("cache.dat"))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new CacheException("Failed to persist cache to disk", e);
        }
    }
    
    public void loadFromDisk() throws CacheException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("cache.dat"))) {
            @SuppressWarnings("unchecked")
            LRUCache<K, V> cacheFromDisk = (LRUCache<K, V>) ois.readObject();
            cacheFromDisk.forEach((k, v) -> {
                this.put(k, v);
                currentSizeInBytes.addAndGet(estimateSize(v));
            });
        } catch (IOException | ClassNotFoundException e) {
            throw new CacheException("Failed to load cache from disk", (Throwable) e);
        }
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