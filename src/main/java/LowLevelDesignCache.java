import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

enum EvictionPolicy {
    FIFO, LRU, LFU
}

class CacheEntry<T> implements Comparable<CacheEntry<T>> {
    String key;
    T value;
    private final Integer operationOrder;
    final Long expirationTime;

    public CacheEntry(final String inputKey, final T inputValue,
                      final Integer operationOrder,
                      final Long expirationTime) {
        this.key = inputKey;
        this.value = inputValue;
        this.operationOrder = operationOrder;
        this.expirationTime = expirationTime;
    }

    public Boolean isExpired() {
        return System.currentTimeMillis() > this.expirationTime;
    }

    @Override
    public int compareTo(CacheEntry<T> o) {
        return Integer.compare(o.operationOrder, this.operationOrder);
    }
}

class LfuCacheEntry<T> implements Comparable<LfuCacheEntry<T>> {
    String key;
    T value;
    private final Integer operationOrder;
    final Long expirationTime;
    final Long frequency;

    public LfuCacheEntry(final String inputKey, final T inputValue,
                         final Integer operationOrder,
                         final Long expirationTime, final Long frequency) {
        this.key = inputKey;
        this.value = inputValue;
        this.operationOrder = operationOrder;
        this.expirationTime = expirationTime;
        this.frequency = frequency;
    }

    public Boolean isExpired() {
        return System.currentTimeMillis() > this.expirationTime;
    }

    @Override
    public String toString() {
        return "LfuCacheEntry{" +
            "key='" + key + '\'' +
            ", value=" + value +
            ", operationOrder=" + operationOrder +
            ", expirationTime=" + expirationTime +
            ", frequency=" + frequency +
            '}';
    }

    @Override
    public int compareTo(LfuCacheEntry<T> o) {
        final var frequencyComparison = Long.compare(o.frequency, this.frequency);

        if (frequencyComparison != 0) {
            return frequencyComparison;
        }

        final var orderComparison = Integer.compare(o.operationOrder, this.operationOrder);

        if (orderComparison != 0) {
            return orderComparison;
        }

        return o.key.compareTo(this.key);
    }
}

interface EvictionStrategy<T> {

    EvictionPolicy policyType();

    T get(final String key);

    void put(final String key, final T value);
}

class LfuEvictionStrategy<T> implements EvictionStrategy<T> {
    private final TreeSet<LfuCacheEntry<T>> cacheEntries;
    private final Map<String, LfuCacheEntry<T>> keyLookup;
    private int operationOrder;
    private final Integer capacity;
    private final Long expirationTime;

    LfuEvictionStrategy(final Integer capacity, final Long expirationTime) {
        operationOrder = 0;
        this.capacity = capacity;
        this.expirationTime = expirationTime;
        this.cacheEntries = new TreeSet<>();
        this.keyLookup = new HashMap<>();
    }

    @Override
    public EvictionPolicy policyType() {
        return EvictionPolicy.LFU;
    }

    private void reviseOrder(final String key) {
        final var retrievedEntry = keyLookup.get(key);
        cacheEntries.remove(retrievedEntry);

        final var newEntry = new LfuCacheEntry<>(key, retrievedEntry.value, operationOrder,
            retrievedEntry.expirationTime, retrievedEntry.frequency + 1L);

        cacheEntries.add(newEntry);
        keyLookup.replace(key, newEntry);
    }

    private void reviseOrder(final String key, final T newValue) {
        final var retrievedEntry = keyLookup.get(key);
        cacheEntries.remove(retrievedEntry);

        final var newEntry = new LfuCacheEntry<>(key, newValue, operationOrder,
            System.currentTimeMillis() + this.expirationTime, retrievedEntry.frequency + 1L);

        cacheEntries.add(newEntry);
        keyLookup.replace(key, newEntry);
    }

    private void evictAllRetired() {
        cacheEntries.stream()
            .filter(LfuCacheEntry::isExpired)
            .forEach(keyLookup::remove);

        this.cacheEntries.removeIf(LfuCacheEntry::isExpired);
    }

    @Override
    public T get(String key) {
        final var retrievedEntry = keyLookup.get(key);

        if (key == null) {
            return null;
        }
        if (retrievedEntry.isExpired()) {
            cacheEntries.remove(retrievedEntry);
            keyLookup.remove(key);

            return null;
        }

        operationOrder += 1;
        reviseOrder(key);

        return retrievedEntry.value;
    }

    @Override
    public void put(String key, T value) {
        operationOrder += 1;
        if (keyLookup.containsKey(key)) {
            reviseOrder(key, value);
            return;
        }

        final var newEntry = new LfuCacheEntry<>(
            key,
            value,
            operationOrder,
            System.currentTimeMillis() + this.expirationTime,
            1L);

        if (cacheEntries.size() == capacity) {
            evictAllRetired();

            if (cacheEntries.size() == capacity) {
                final var lastKey = cacheEntries.pollLast().key;

                keyLookup.remove(lastKey);
            }
        }

        cacheEntries.add(newEntry);
        keyLookup.put(key, newEntry);
    }
}

class LruEvictionStrategy<T> implements EvictionStrategy<T> {
    private final TreeSet<CacheEntry<T>> orderedEntries;
    private final Map<String, CacheEntry<T>> keyLookup;
    private final Long expiration;
    private final Integer capacity;
    private int operationOrder = 0;

    LruEvictionStrategy(final Integer capacity, final Long cacheEvictDuration) {
        this.capacity = capacity;
        this.orderedEntries = new TreeSet<>();
        this.keyLookup = new TreeMap<>();
        this.expiration = cacheEvictDuration;
    }

    private void resetEntryToBegin(final String key) {
        final var olderEntry = keyLookup.get(key);
        final var newEntry = new CacheEntry<>(
            key, olderEntry.value, operationOrder, olderEntry.expirationTime
        );
        orderedEntries.remove(olderEntry);
        orderedEntries.add(newEntry);
        keyLookup.replace(key, newEntry);
    }

    private void resetEntryToBegin(final String key, final T newValue) {
        final var olderEntry = keyLookup.get(key);
        final var newEntry = new CacheEntry<>(
            key, newValue, operationOrder, System.currentTimeMillis() + this.expiration
        );

        orderedEntries.remove(olderEntry);
        orderedEntries.add(newEntry);
        keyLookup.replace(key, newEntry);
    }

    private void evictRetiredEntries() {
        orderedEntries.stream()
            .filter(CacheEntry::isExpired)
            .forEach(keyLookup::remove);

        orderedEntries.removeIf(CacheEntry::isExpired);
    }

    @Override
    public T get(String key) {
        final var retrievedEntry = keyLookup.get(key);

        if (key == null) {
            return null;
        }
        if (retrievedEntry.isExpired()) {
            orderedEntries.remove(retrievedEntry);
            keyLookup.remove(key);

            return null;
        }

        operationOrder += 1;
        resetEntryToBegin(key);

        return retrievedEntry.value;
    }

    public void put(final String key, final T newValue) {
        operationOrder += 1;

        if (!keyLookup.containsKey(key)) {
            final var newCacheEntry = new CacheEntry<T>(
                key, newValue, operationOrder, System.currentTimeMillis() + this.expiration);

            if (orderedEntries.size() == capacity) {
                evictRetiredEntries();

                if (orderedEntries.size() == capacity) {
                    final var lastKey = orderedEntries.pollLast().key;

                    keyLookup.remove(lastKey);
                }
            }

            orderedEntries.add(newCacheEntry);
            keyLookup.put(key, newCacheEntry);
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
    private final TreeSet<CacheEntry<T>> orderedList;
    private final Map<String, CacheEntry<T>> keyLookUp;
    private final Long expiration;
    private final Integer capacity;
    private Integer operationOrder;

    FifoEvictionStrategy(final Integer capacity, final Long expiration) {
        this.operationOrder = 0;
        this.expiration = expiration;
        this.capacity = capacity;
        this.keyLookUp = new HashMap<>();
        this.orderedList = new TreeSet<>();
    }

    private void evictExpiredEntries() {
        while (!orderedList.isEmpty() && orderedList.last().isExpired()) {
            orderedList.pollLast();
        }
    }

    @Override
    public T get(final String key) {
        final CacheEntry<T> maybeEntry = keyLookUp.get(key);
        if (maybeEntry == null || maybeEntry.isExpired()) {
            return null;
        }

        return maybeEntry.value;
    }

    @Override
    public void put(final String key, final T newValue) {
        operationOrder += 1;
        final var cachedEntry = new CacheEntry<>(key, newValue, operationOrder,
            System.currentTimeMillis() + this.expiration);

        evictExpiredEntries();
        if (keyLookUp.containsKey(key)) {
            orderedList.remove(keyLookUp.get(key));
            keyLookUp.remove(key);
        }

        orderedList.add(cachedEntry);
        keyLookUp.put(key, cachedEntry);

        if (orderedList.size() > capacity) {
            final var toBeRemoved = orderedList.pollLast();

            keyLookUp.remove(toBeRemoved.key);
        }
    }

    @Override
    public EvictionPolicy policyType() {
        return EvictionPolicy.FIFO;
    }
}

class Cache<T> {

    private final EvictionStrategy<T> strategy;

    public Cache(final EvictionPolicy userEvictionPolicy,
                 final Long expirationWindow,
                 final Integer capacity) {
        if (userEvictionPolicy == EvictionPolicy.LRU) {
            strategy = new LruEvictionStrategy<>(capacity, expirationWindow);
        } else if (userEvictionPolicy == EvictionPolicy.FIFO) {
            strategy = new FifoEvictionStrategy<>(capacity, expirationWindow);
        } else if (userEvictionPolicy == EvictionPolicy.LFU) {
            strategy = new LfuEvictionStrategy<>(capacity, expirationWindow);
        } else {
            strategy = null;
            System.err.println("not able to find relevant strategy");
        }
    }

    T get(final String key) {
        return strategy.get(key);
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
