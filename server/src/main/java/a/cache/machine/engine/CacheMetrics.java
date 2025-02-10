package a.cache.machine.engine;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class CacheMetrics implements Serializable {
    private static final long serialVersionUID = 1L;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public void incrementHits() { hits.incrementAndGet(); }
    public void incrementMisses() { misses.incrementAndGet(); }
    public void incrementEvictions() { evictions.incrementAndGet(); }

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }
    public long getEvictions() { return evictions.get(); }
}