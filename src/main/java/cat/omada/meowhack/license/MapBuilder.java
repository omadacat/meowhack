/*
 * Originally from fabric-license-check by Flowey (GPL-3.0).
 * https://github.com/FloweyTheFlower/fabric-license-check
 * Integrated into meowhack, also GPL-3.0.
 */
package cat.omada.meowhack.license;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {
    private final Map<K, V> map = new HashMap<>();

    public MapBuilder() {

    }

    public MapBuilder<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    public Map<K, V> get() {
        return map;
    }

    public Map<K, V> getImmutable() {
        return Collections.unmodifiableMap(map);
    }
}
