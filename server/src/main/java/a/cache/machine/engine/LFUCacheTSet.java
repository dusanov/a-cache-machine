package a.cache.machine.engine;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFUCacheTSet<K, V> implements ICache<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(LFUCacheTSet.class);
    private final ConcurrentHashMap<K, V> cache;
    private final ConcurrentHashMap<K, Integer> frequencyMap;
    // private final TreeSet<CacheEntry<K>> evictionSet;
    private final TreeSet<K> evictionSet;
    private final long maxSizeInBytes;
    private final AtomicLong currentSizeInBytes;
    private final List<ICacheEventListener> listeners;
    private final CacheMetrics metrics;

    public LFUCacheTSet(long maxSizeInBytes) {
        this.cache = new ConcurrentHashMap<>();
        this.frequencyMap = new ConcurrentHashMap<>();
        // this.evictionSet = new TreeSet<>(Comparator
        //     .comparingInt((CacheEntry<K> entry) -> frequencyMap.get(entry.getKey()))
        //     .thenComparingLong(CacheEntry::getInsertionTime)
        // );        
        this.evictionSet = new TreeSet<>(Comparator
                                            .comparing(K::toString)    
                                            .thenComparingInt(frequencyMap::get)
                                        );                                        
        this.maxSizeInBytes = maxSizeInBytes;
        this.currentSizeInBytes = new AtomicLong(0);
        this.listeners = new CopyOnWriteArrayList<>();
        this.metrics = new CacheMetrics();
    }

    @Override
    public V get(K key) {
        if (cache.containsKey(key)) {
            frequencyMap.put(key, frequencyMap.get(key) + 1);
            // Refresh the eviction set
            // evictionSet.remove(new CacheEntry<>(key));
            // evictionSet.add(new CacheEntry<>(key));            
            evictionSet.remove(key);
            evictionSet.add(key);            
            listeners.forEach(listener -> listener.onHit(key.toString()));
            metrics.incrementHits();
            return cache.get(key);
        } else {
            listeners.forEach(listener -> listener.onMiss(key.toString()));
            metrics.incrementMisses();
            return null;
        }
    }

    @Override
    public V put(K key, V value) throws CacheException {
        if (key == null || value == null) {
            throw new CacheException("Key or value cannot be null");
        }

        long objectSize = estimateSize(value); 
        while (currentSizeInBytes.get() + objectSize > maxSizeInBytes) {
            evict();
        }

        frequencyMap.put(key, 1);
        currentSizeInBytes.addAndGet(objectSize);
        // evictionSet.add(new CacheEntry<>(key));
        evictionSet.add(key);
        return cache.put(key, value);
    }

    private void evict() {
        if (!evictionSet.isEmpty()) {
            // CacheEntry<K> entryToEvict = evictionSet.pollFirst();
            K entryToEvict = evictionSet.pollFirst();
            if (entryToEvict != null) {
                // K key = entryToEvict.getKey();
                V value = cache.remove(entryToEvict);
                frequencyMap.remove(entryToEvict);
                currentSizeInBytes.addAndGet(-estimateSize(value));
                listeners.forEach(listener -> listener.onEviction(entryToEvict.toString(), value));
                metrics.incrementEvictions();
                logger.info("Evicted key: {}, current queue size in bytes: {}", entryToEvict, currentSizeInBytes);
            }
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
               evictionSet.add(k);
           });
       } catch (IOException | ClassNotFoundException e) {
           throw new CacheException("Failed to load cache from disk", (Throwable) e);
       }
   }

    @Override
    public V remove(K key) throws CacheException {
        frequencyMap.remove(key);
        evictionSet.remove(key);
        return cache.remove(key);
    }


    @Override
    public void clear() throws CacheException {
        frequencyMap.clear();
        evictionSet.clear();
        cache.clear();
    }

    @Override
    public int size() {
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

    // Helper class for cache entries
    private static class CacheEntry<K> {
        private final K key;
        private final long insertionTime;

        public CacheEntry(K key) {
            this.key = key;
            this.insertionTime = System.nanoTime();
        }

        public K getKey() {
            return key;
        }

        public long getInsertionTime() {
            return insertionTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheEntry<?> that = (CacheEntry<?>) o;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }    
}