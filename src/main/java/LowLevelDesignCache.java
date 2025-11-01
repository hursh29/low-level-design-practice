import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum EvictionPolicy {
    FIFO, LRU, LFU
}

class CacheEntry<T> {
    String key;
    T value;
    private final Long expirationTime;

    public CacheEntry(final String inputKey, final T inputValue, final Long expirationTime) {
        this.key = inputKey;
        this.value = inputValue;
        this.expirationTime = System.currentTimeMillis() + expirationTime;
    }

    public Boolean isExpired() {
        return System.currentTimeMillis() > this.expirationTime;
    }
}

interface EvictionStrategy<T> {

    EvictionPolicy policyType();

    T get(final String key);

    void put(final String key, final T value);
}

class LruEvictionStrategy<T> implements EvictionStrategy<T> {
    private final LinkedList<CacheEntry<T>> orderedEntries;
    private final Map<String, ListIterator<CacheEntry<T>>> keyToLocation;
    private final Long expiration;
    private final Integer capacity;

    LruEvictionStrategy(final Integer capacity, final Long cacheEvictDuration) {
        this.capacity = capacity;
        this.orderedEntries = new LinkedList<>();
        this.keyToLocation = new HashMap<>();
        this.expiration = cacheEvictDuration;
    }

    private void resetEntryToBegin(final String key) {
        final var currentLocation = keyToLocation.get(key);
        final var value = currentLocation.next().value;

        currentLocation.remove();
        orderedEntries.addFirst(new CacheEntry<T>(key, value, this.expiration));

        keyToLocation.put(key, orderedEntries.listIterator(0));
    }

    private void resetEntryToBegin(final String key, final T newValue) {
        final var currentLocation = keyToLocation.get(key);

        currentLocation.remove();
        orderedEntries.addFirst(new CacheEntry<T>(key, newValue, this.expiration));

        keyToLocation.put(key, orderedEntries.listIterator(0));
    }

    private void evictLast() {
        final var lastKey = orderedEntries.getLast().key;

        orderedEntries.removeLast();
        keyToLocation.remove(lastKey);
    }

    public T get(final String key) {
        if (!keyToLocation.containsKey(key)) {
            return null;
        }

        resetEntryToBegin(key);
        return keyToLocation.get(key).next().value;
    }

    public void put(final String key, final T newValue) {
        if (!keyToLocation.containsKey(key)) {
            orderedEntries.addFirst(new CacheEntry<T>(key, newValue, this.expiration));
            keyToLocation.put(key, orderedEntries.listIterator(0));
            if (orderedEntries.size() > this.capacity) {
                evictLast();
            }
        } else {
            resetEntryToBegin(key, newValue);
        }
    }

    @Override
    public EvictionPolicy policyType() {
        return EvictionPolicy.LRU;
    }
}

class FifoEvictionStrategy<T> implements EvictionStrategy<T> {
    private final LinkedList<CacheEntry<T>> orderedList;
    private final Long expiration;
    private final Integer capacity;

    FifoEvictionStrategy(final Integer capacity, final Long expiration) {
        this.expiration = expiration;
        this.capacity = capacity;
        this.orderedList = new LinkedList<>();
    }

    public T get(final String key) {
        return null;
    }

    public void put(final String key, final T newValue) {
    }

    @Override
    public EvictionPolicy policyType() {
        return EvictionPolicy.FIFO;
    }
}

class Cache<T> {
    private final EvictionStrategy strategy;

    public Cache(final EvictionPolicy userEvictionPolicy,
                 final Long userExipration,
                 final Integer capacity) {

        final var policyToStrategy =
            Stream.of(
                new LruEvictionStrategy<>(capacity, userExipration),
                new FifoEvictionStrategy<>(capacity, userExipration))
            .collect(Collectors.toUnmodifiableMap(EvictionStrategy::policyType, t -> t));

        this.strategy = Optional.ofNullable(policyToStrategy.get(userEvictionPolicy))
            .orElseThrow(() -> new IllegalStateException("Strategy not found"));
    }

    T get(final String key) {
        return (T) strategy.get(key);
    }

    void put(final String key, final T incomingValue) {
        strategy.put(key, incomingValue);
    }
}

public class LowLevelDesignCache {

    public static void main(String[] args) {
        final var cacheSystem = new Cache<Long>(EvictionPolicy.LRU, 2000L, 4);

        cacheSystem.put("1", 1L);
        cacheSystem.put("2", 2L);
        cacheSystem.put("3", 3L);
        cacheSystem.put("4", 4L);
        cacheSystem.put("5", 5L);

        System.out.println(cacheSystem.get("1"));
        System.out.println(cacheSystem.get("2"));
        System.out.println(cacheSystem.get("3"));
        System.out.println(cacheSystem.get("4"));
        System.out.println(cacheSystem.get("5"));

    }
}
