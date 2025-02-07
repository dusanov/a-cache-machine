package a.cache.machine.engine;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFUCache<K, V> implements ICache<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(LFUCache.class);
    private final ConcurrentHashMap<K, V> cache;
    private final ConcurrentHashMap<K, Integer> frequencyMap;
    private final PriorityQueue<K> evictionQueue;
    private final long maxSizeInBytes;
    private final AtomicLong currentSizeInBytes;
    private final List<ICacheEventListener> listeners;
    private final CacheMetrics metrics;

    public LFUCache(long maxSizeInBytes) {
        this.cache = new ConcurrentHashMap<>();
        this.frequencyMap = new ConcurrentHashMap<>();
        this.evictionQueue = new PriorityQueue<>(Comparator.comparingInt(frequencyMap::get));
        this.maxSizeInBytes = maxSizeInBytes;
        this.currentSizeInBytes = new AtomicLong(0);
        this.listeners = new CopyOnWriteArrayList<>();
        this.metrics = new CacheMetrics();
    }

    @Override
    public V get(K key) {
        if (cache.containsKey(key)) {
            frequencyMap.put(key, frequencyMap.get(key) + 1);
            listeners.forEach(listener -> listener.onHit(key.toString()));
            metrics.incrementHits();
            evictionQueue.remove(key);
            evictionQueue.offer(key);
            return cache.get(key);
        } else {
            listeners.forEach(listener -> listener.onMiss(key.toString()));
            metrics.incrementMisses();
            return null;
        }
    }

    @Override
    public void put(K key, V value) throws CacheException {
        if (key == null || value == null) {
            throw new CacheException("Key or value cannot be null");
        }

        long objectSize = estimateSize(value);
        while (currentSizeInBytes.get() + objectSize > maxSizeInBytes) {
            evict();
        }

        cache.put(key, value);
        frequencyMap.put(key, 1);
        currentSizeInBytes.addAndGet(objectSize);
        evictionQueue.offer(key);
    }

    private void evict() {
        K keyToEvict = evictionQueue.poll();
        if (keyToEvict != null) {
            V value = cache.remove(keyToEvict);
            frequencyMap.remove(keyToEvict);
            currentSizeInBytes.addAndGet(-estimateSize(value));
            listeners.forEach(listener -> listener.onEviction(keyToEvict.toString(), value));
            metrics.incrementEvictions();
            logger.info("Evicted key: {}, current queue size in bytes: {}", keyToEvict, currentSizeInBytes);
        }
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
   // Other methods (remove, clear, shutdown, etc.)

   @Override
   public void shutdown() throws CacheException {
       try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("cache.dat"))) {
           oos.writeObject(cache);
       } catch (IOException e) {
           throw new CacheException("Failed to persist cache to disk", e);
       }
   }
   
   public void loadFromDisk() throws CacheException {
       try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("cache.dat"))) {
           @SuppressWarnings("unchecked")
           ConcurrentHashMap<K, V> cacheFromDisk = (ConcurrentHashMap<K, V>) ois.readObject();
           cacheFromDisk.forEach((k, v) -> {
               cache.put(k, v);
               frequencyMap.put(k, 1);
               currentSizeInBytes.addAndGet(estimateSize(v));
               evictionQueue.add(k);
           });
       } catch (IOException | ClassNotFoundException e) {
           throw new CacheException("Failed to load cache from disk", (Throwable) e);
       }
   }

    @Override
    public void remove(K key) throws CacheException {
        cache.remove(key);
    }


    @Override
    public void clear() throws CacheException {
        cache.clear();
    }

    @Override
    public long size() {
        return cache.size();
    }

    public long currentSizeInBytes(){
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