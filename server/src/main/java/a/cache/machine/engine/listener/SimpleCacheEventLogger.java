package a.cache.machine.engine.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import a.cache.machine.engine.ICacheEventListener;

public class SimpleCacheEventLogger implements ICacheEventListener {
    private static final Logger logger = LoggerFactory.getLogger(SimpleCacheEventLogger.class);
    private int hitCount = 0;
    private int missCount = 0;
    private int evictionCount = 0;

    @Override
    public void onHit(String key) {
        hitCount++;
        logger.info("Cache hit for key: " + key);
    }

    @Override
    public void onMiss(String key) {
        missCount++;
        logger.info("Cache miss for key: " + key);
    }

    @Override
    public void onEviction(String key, Object value) {
        evictionCount++;
        logger.info("Eviction for key: " + key + ", value: " + value);
    }

    public int getHitCount() {
        return hitCount;
    }

    public int getMissCount() {
        return missCount;
    }

    public int getEvictionCount() {
        return evictionCount;
    }
}
