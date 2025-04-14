package net.sumaris.core.util.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extension de HashMap qui calcule le hashCode en fonction du contenu exact
 * de la map, en utilisant les valeurs et les clés de manière plus stricte.
 * @param <K> Type des clés
 * @param <V> Type des valeurs
 */
public class ContentAwareHashMap<K, V> extends HashMap<K, V> {

    public ContentAwareHashMap() {
        super();
    }

    public ContentAwareHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public ContentAwareHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ContentAwareHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    public int hashCode() {
        // Calculer un hash personnalisé basé sur chaque entrée de la map
        int hash = 0;
        for (Entry<K, V> entry : entrySet()) {
            // Utiliser un multiplicateur premier pour créer un hash plus unique
            hash = 31 * hash + Objects.hashCode(entry.getKey());
            hash = 37 * hash + Objects.hashCode(entry.getValue());
        }

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        // Deux instances de ContentAwareHashMap ne sont égales que si 
        // c'est le même objet (même référence)
        return this == o;
    }
}