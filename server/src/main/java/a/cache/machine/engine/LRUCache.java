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

public class LRUCache<K, V> extends LinkedHashMap<K, V> implements ICache<K, V> {

    private final int maxCapacityInBytes;
    private final AtomicLong currentSizeInBytes;
    private final List<ICacheEventListener> listeners;
    private final CacheMetrics metrics;

    public LRUCache(int maxCapacityInBytes) {
        super(16, 0.75f, true);
        this.maxCapacityInBytes = maxCapacityInBytes;
        this.currentSizeInBytes = new AtomicLong(0);
        this.listeners = new CopyOnWriteArrayList<>();
        this.metrics = new CacheMetrics();
    }

    @Override
    public synchronized V get(Object key) {
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
    public synchronized V put(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key or value cannot be null");
        }
        long objectSize = estimateSize(value);
        currentSizeInBytes.addAndGet(objectSize);
        return super.put(key, value);
    }

    @Override
    public synchronized V remove(Object key) {
        return super.remove(key);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    public long currentSizeInBytes() {
        return currentSizeInBytes.get();
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (currentSizeInBytes() > maxCapacityInBytes) {
            currentSizeInBytes.addAndGet(-estimateSize(eldest.getValue()));
            listeners.forEach(listener -> listener.onEviction(eldest.getKey().toString(), eldest.getValue()));
            metrics.incrementEvictions();
            return true;
        }
        return false;
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

    @Override
    public synchronized void shutdown() throws CacheException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("cache.dat"))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new CacheException("Failed to persist cache to disk", e);
        }
    }

    public synchronized void loadFromDisk() throws CacheException {
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
    public synchronized void addEventListener(ICacheEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void removeEventListener(ICacheEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public CacheMetrics getMetrics() {
        return metrics;
    }

}
