package a.cache.machine.engine;

public interface ICacheEventListener {
    void onHit(String key);
    void onMiss(String key);
    void onEviction(String key, Object value);
}
