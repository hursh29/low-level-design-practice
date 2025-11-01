import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum EvictionPolicy {
    FIFO, LRU, LFU
}

class CacheEntry<T> implements Comparable<CacheEntry<T>> {
    String key;
    T value;
    private final Integer operationOrder;
    final Long expirationTime;
    private final EvictionPolicy policy;

    public CacheEntry(final String inputKey, final T inputValue,
                      final Integer operationOrder,
                      final Long expirationTime) {
        this.key = inputKey;
        this.value = inputValue;
        this.operationOrder = operationOrder;
        this.expirationTime = expirationTime;
        this.policy = EvictionPolicy.LRU;
    }

    public Boolean isExpired() {
        return System.currentTimeMillis() > this.expirationTime;
    }

    @Override
    public int compareTo(CacheEntry<T> o) {
        return Integer.compare(o.operationOrder, this.operationOrder);
    }
}

interface EvictionStrategy<T> {

    EvictionPolicy policyType();

    T get(final String key);

    void put(final String key, final T value);
}

class LruEvictionStrategy<T> implements EvictionStrategy<T> {
    private final TreeSet<CacheEntry<T>> orderedEntries;
    private final Map<String, CacheEntry<T>> keyLookUp;
    private final Long expiration;
    private final Integer capacity;
    private int operationOrder = 0;

    LruEvictionStrategy(final Integer capacity, final Long cacheEvictDuration) {
        this.capacity = capacity;
        this.orderedEntries = new TreeSet<>();
        this.keyLookUp = new TreeMap<>();
        this.expiration = cacheEvictDuration;
    }

    private void resetEntryToBegin(final String key) {
        final var olderEntry = keyLookUp.get(key);
        final var newEntry = new CacheEntry<>(
            key, olderEntry.value, operationOrder, olderEntry.expirationTime
        );
        orderedEntries.remove(olderEntry);
        orderedEntries.add(newEntry);
        keyLookUp.replace(key, newEntry);
    }

    private void resetEntryToBegin(final String key, final T newValue) {
        final var olderEntry = keyLookUp.get(key);
        final var newEntry = new CacheEntry<>(
            key, newValue, operationOrder, System.currentTimeMillis() + this.expiration
        );

        orderedEntries.remove(olderEntry);
        orderedEntries.add(newEntry);
        keyLookUp.replace(key, newEntry);
    }

    private void evictLast() {
        assert !orderedEntries.isEmpty();

        // see if we can clear all retired items
        final var allRetiredEntries = orderedEntries.stream()
            .filter(CacheEntry::isExpired)
            .collect(Collectors.toSet());

        if (allRetiredEntries.isEmpty()) {
            final var lastKey = orderedEntries.pollLast().key;
            keyLookUp.remove(lastKey);
        } else {
            orderedEntries.removeAll(allRetiredEntries);
            orderedEntries.forEach((entry) -> keyLookUp.remove(entry.key));
        }
    }

    public T get(final String key) {
        if (!keyLookUp.containsKey(key)) {
            return null;
        }
        operationOrder += 1;

        // check if its expired
        final var savedEntry = keyLookUp.get(key);
        if (savedEntry.isExpired()) {
            orderedEntries.remove(savedEntry);
            return null;
        }

        resetEntryToBegin(key);
        return keyLookUp.get(key).value;
    }

    public void put(final String key, final T newValue) {
        operationOrder += 1;
        if (!keyLookUp.containsKey(key)) {
            final var newCacheEntry = new CacheEntry<T>(
                key, newValue, operationOrder, System.currentTimeMillis() + this.expiration);

            orderedEntries.add(newCacheEntry);
            keyLookUp.put(key, newCacheEntry);
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
