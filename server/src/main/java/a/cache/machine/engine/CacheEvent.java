package a.cache.machine.engine;

public class CacheEvent {
    private String key;
    private Object value;
    private EventType type;

    public enum EventType {
        HIT, MISS, EVICTION
    }
    // Constructor, getters, and setters

    public CacheEvent(String key, Object value, EventType type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public EventType getType() {
        return type;
    }
}