package a.cache.machine.engine;

public interface ICache<K, V> {
	V get(K key) throws CacheException;
	V put(K key, V value) throws CacheException;
	V remove(K key) throws CacheException;
	void clear() throws CacheException;
	int size();
	void shutdown() throws CacheException;
	void addEventListener(ICacheEventListener listener);
	void removeEventListener(ICacheEventListener listener);
	CacheMetrics getMetrics();
}