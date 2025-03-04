package a.cache.machine;

public interface IClient<K, V> {
    V put(K key, V value) throws Exception;
    V get(K key) throws Exception;
    V remove(K key) throws Exception;
    void clear();
    int size();
}
