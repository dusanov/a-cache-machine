package a.cache.machine.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;

class PerformanceTest {
    private static final int MAX_SIZE_IN_BYTES = 100;
    private static final int MAX_ITEMS = 10000;
    private LRUCache<String, Object> lruCache;
    private LFUCachePQueue<String, Object> lfuCache;

    @BeforeEach
    void setUp() {
        // Initialize a cache with a maximum size of 100 bytes for testing
        lruCache = new LRUCache<>(MAX_SIZE_IN_BYTES);
        lfuCache = new LFUCachePQueue<>(MAX_SIZE_IN_BYTES);
    }

    @Test
    void testLRU() {
        
        long startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS).forEach((i) -> {
            lruCache.put("key" + i, "value" + i);
        });
        assertEquals(7, lruCache.size());
        long endTime = System.nanoTime();
        System.out.println("LRU put execution time for "+MAX_ITEMS+" items: " + (endTime - startTime) / 1000 + " ms");
    }

    @Test
    void testLFU() {
        
        long startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS).forEach((i) -> {
            try {
                lfuCache.put("key" + i, "value" + i);
            } catch (CacheException e) {
                e.printStackTrace();
            }
        });
        assertEquals(7, lfuCache.size());
        long endTime = System.nanoTime();
        System.out.println("LFU put execution time for "+MAX_ITEMS+" items: " + (endTime - startTime) / 1000 + " ms");
    }
}