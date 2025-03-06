package a.cache.machine.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;

class PerformanceTest {
    private static final int EXPECTED = 2_000_001;
    private static final int MAX_SIZE_IN_BYTES = 1_060_000;
    private static final int MAX_ITEMS = 2_000_000;
    private LRUCache<String, Object> lruCache;
    private LFUCache<String, Object> lfuCache;

    @BeforeEach
    void setUp() {
        // Initialize a cache with a maximum size of 100 bytes
        // for testing
        lruCache = new LRUCache<>(MAX_SIZE_IN_BYTES);
        lfuCache = new LFUCache<>(MAX_SIZE_IN_BYTES);
    }

    @Test
    void testLRU_multi_thread() {
        long startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS)
                .parallel()
                .forEach((i) -> {
                    lruCache.put("key" + i, "value" + i);
                });
        // assertEquals(EXPECTED, lruCache.size());
        long endTime = System.nanoTime();
        System.out
                .println("LRU put execution time for " + MAX_ITEMS
                        + " items: "
                        + (endTime - startTime) / 1_000_000
                        + " ms");

        startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS)
                .parallel()
                .forEach((i) -> {
                    lruCache.get("key" + i);
                });
        // assertEquals(EXPECTED, lruCache.size());
        endTime = System.nanoTime();
        System.out
                .println("LRU get execution time for " + MAX_ITEMS
                        + " items: "
                        + (endTime - startTime) / 1_000_000
                        + " ms");
        System.out.println("\nLRU cache metrics:");
        System.out.println("Current size in bytes: " + lruCache.currentSizeInBytes());
        System.out.println("Max size in bytes: " + MAX_SIZE_IN_BYTES);
        System.out.println("Number of items: " + lruCache.size());
        System.out.println("Number of hits: " + lruCache.getMetrics().getHits());
        System.out.println("Number of misses: " + lruCache.getMetrics().getMisses());
        System.out.println("Number of evictions: " + lruCache.getMetrics()
                .getEvictions());
    }

    @Test
    // @Disabled
    void testLRU() {

        long startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS).forEach((i) -> {
            lruCache.put("key" + i, "value" + i);
        });
        // assertEquals(EXPECTED, lruCache.size());
        long endTime = System.nanoTime();
        System.out
                .println("LRU put execution time for " + MAX_ITEMS
                        + " items: "
                        + (endTime - startTime) / 1_000_000
                        + " ms");

        startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS).forEach((i) -> {
            lruCache.get("key" + i);
        });
        // assertEquals(EXPECTED, lruCache.size());
        endTime = System.nanoTime();
        System.out
                .println("LRU get execution time for " + MAX_ITEMS
                        + " items: "
                        + (endTime - startTime) / 1_000_000
                        + " ms");
        System.out.println("\nLRU cache metrics:");
        System.out.println("Current size in bytes: " + lruCache.currentSizeInBytes());
        System.out.println("Max size in bytes: " + MAX_SIZE_IN_BYTES);
        System.out.println("Number of items: " + lruCache.size());
        System.out.println("Number of hits: " + lruCache.getMetrics().getHits());
        System.out.println("Number of misses: " + lruCache.getMetrics().getMisses());
        System.out.println("Number of evictions: " + lruCache.getMetrics()
                .getEvictions());
    }

    @Test
    // @Disabled
    void testLFU() {

        long startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS).forEach((i) -> {
            lfuCache.put("key" + i, "value" + i);
        });
        // assertEquals(EXPECTED, lfuCache.size());
        long endTime = System.nanoTime();
        System.out
                .println("LFU put execution time for " + MAX_ITEMS
                        + " items: "
                        + (endTime - startTime) / 1_000_000
                        + " ms");

        startTime = System.nanoTime();
        IntStream.rangeClosed(0, MAX_ITEMS).forEach((i) -> {
            lfuCache.get("key" + i);
        });
        // assertEquals(EXPECTED, lfuCache.size());
        endTime = System.nanoTime();
        System.out
                .println("LFU get execution time for " + MAX_ITEMS
                        + " items: "
                        + (endTime - startTime) / 1_000_000
                        + " ms");
        System.out.println("\nLFU cache metrics:");
        System.out.println("Current size in bytes: " + lfuCache.currentSizeInBytes());
        System.out.println("Max size in bytes: " + MAX_SIZE_IN_BYTES);
        System.out.println("Number of items: " + lfuCache.size());
        System.out.println("Number of hits: " + lfuCache.getMetrics().getHits());
        System.out.println("Number of misses: " + lfuCache.getMetrics().getMisses());
        System.out.println("Number of evictions: " + lfuCache.getMetrics()
                .getEvictions());
    }
}
