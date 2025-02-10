package a.cache.machine.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LFUCachePQueueTest {
    private LFUCachePQueue<String, Object> cache;

    @BeforeEach
    void setUp() {
        // Initialize a cache with a maximum size of 100 bytes for testing
        cache = new LFUCachePQueue<>(100);
    }

    @Test
    void testPutAndGet() throws CacheException {
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
    }

    @Test
    void testGetNonExistentKey() {
        assertNull(cache.get("nonexistent"));
    }

    @Test
    void testEvictionPolicy() throws CacheException {
        // Add items until the cache size is exceeded
        cache.put("key1", "value1"); // Assume size = 13
        cache.put("key2", "value2"); // Assume size = 13
        cache.put("key3", "value3"); // Assume size = 13
        cache.put("key4", "value4"); // Assume size = 13
        cache.put("key5", "value5"); // Assume size = 13
        cache.put("key6", "value6"); // Assume size = 13
        cache.put("key7", "value7"); // Assume size = 13

        // Access keys to update their frequency
        cache.get("key1");
        cache.get("key1");
        cache.get("key2");
        cache.get("key3");
        cache.get("key4");
        cache.get("key5");

        // Add one more item to trigger eviction
        cache.put("key8", "value8"); // Assume size = 13

        // key6 and key7 should be evicted (least frequently used)
        assertNull(cache.get("key7"));

        // Add one more item to trigger eviction
        cache.put("key9", "value9"); // Assume size = 13
        assertNull(cache.get("key6"));

        assertEquals("value1", cache.get("key1"));
        assertEquals("value8", cache.get("key8"));
    }

    @Test
    void testSize() throws CacheException {
        cache.put("key1", "value1"); // Assume size = 20
        cache.put("key2", "value2"); // Assume size = 20

        assertEquals(2, cache.size());
    }

    @Test
    void testClear() throws CacheException {
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testMetrics() throws CacheException {
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.get("key1");
        cache.get("key1");
        cache.get("key2");
        cache.get("nonexistent");

        CacheMetrics metrics = cache.getMetrics();
        assertEquals(3, metrics.getHits());
        assertEquals(1, metrics.getMisses());
    }

    @Test
    void testNullKeyOrValue() {
        assertThrows(CacheException.class, () -> cache.put(null, "value"));
        assertThrows(CacheException.class, () -> cache.put("key", null));
    }

    @Test
    void testShutdownAndPersistence() throws CacheException {
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.shutdown();

        // Simulate restart by creating a new cache and loading from disk
        LFUCacheTSet<String, Object> newCache = new LFUCacheTSet<>(100);
        newCache.loadFromDisk();

        assertEquals("value1", newCache.get("key1"));
        assertEquals("value2", newCache.get("key2"));
    }

    @Test
    void testEventListener() throws CacheException {
        TestCacheEventListener listener = new TestCacheEventListener();
        cache.addEventListener(listener);

        cache.put("key1", "value1");
        cache.get("key1");
        cache.get("nonexistent");

        assertEquals(1, listener.getHitCount());
        assertEquals(1, listener.getMissCount());
    }

    // Helper class to test event listeners
    private static class TestCacheEventListener implements ICacheEventListener {
        private int hitCount = 0;
        private int missCount = 0;

        @Override
        public void onHit(String key) {
            hitCount++;
        }

        @Override
        public void onMiss(String key) {
            missCount++;
        }

        @Override
        public void onEviction(String key, Object value) {
            // Not used in this test
        }

        public int getHitCount() {
            return hitCount;
        }

        public int getMissCount() {
            return missCount;
        }
    }
}