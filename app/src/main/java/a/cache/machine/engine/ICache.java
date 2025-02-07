package a.cache.machine.engine;

public interface ICache<K, V> {
	V get(K key) throws CacheException;
	void put(K key, V value) throws CacheException;
	void remove(K key) throws CacheException;
	void clear() throws CacheException;
	long size();
	void shutdown() throws CacheException;
	void addEventListener(ICacheEventListener listener);
	void removeEventListener(ICacheEventListener listener);
	CacheMetrics getMetrics();
}