/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.server.test.config;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public final class Suppressibles {

    public static <K, V> Map<K, V> unsuppressibleMap(Map<K, Suppressible<V>> suppressibleMap) {
        return new UnsuppressibleMap<>(suppressibleMap);
    }

    public static <E> Set<E> unsuppressibleSet(Set<Suppressible<E>> suppressibleCollection) {
        return new UnsuppressibleSet<>(suppressibleCollection);
    }

    public static <E> Collection<E> unsuppressibleCollection(Collection<Suppressible<E>> suppressibleCollection) {
        return new UnsuppressibleCollection<>(suppressibleCollection);
    }

    public static <K, V> Entry<K, V> unsuppressibleEntry(Entry<K, Suppressible<V>> suppressibleEntry) {
        return new UnsuppressibleEntry<>(suppressibleEntry);
    }

    public static <E> Iterator<E> unsuppressibleIterator(Iterator<Suppressible<E>> suppressibleIterator) {
        return new UnsuppressibleIterator<>(suppressibleIterator);
    }

    public static Set<Suppression> getDefaultSuppressions() {
        return EnumSet.of(Suppression.NONE);
    }

    public static <E> List<E> unsuppressibleList(List<Suppressible<E>> suppressibleList) {
        return new UnsuppressibleList<>(suppressibleList);
    }

    public static <E> ListIterator<E> unsuppressibleListIterator(ListIterator<Suppressible<E>> suppressibleIterator) {
        return new UnsuppressibleListIterator<>(suppressibleIterator);
    }

    private Suppressibles() {
        // utility class
    }

    private static final class UnsuppressibleEntry<K, V> implements Entry<K, V> {

        private Entry<K, Suppressible<V>> suppressible;

        private UnsuppressibleEntry(Entry<K, Suppressible<V>> suppressible) {
            this.suppressible = suppressible;
        }

        @Override
        public K getKey() {
            return suppressible.getKey();
        }

        @Override
        public V getValue() {
            Suppressible<V> value = suppressible.getValue();
            if (value == null) {
                return null;
            }
            return value.value();
        }

        @Override
        public V setValue(V value) {
            suppressible.setValue(new Suppressible<>(value));
            return getValue();
        }

    }

    private static final class UnsuppressibleMap<K, V> implements Map<K, V> {

        private final Map<K, Suppressible<V>> suppressible;
        private Collection<V> collectionValueCache;

        private UnsuppressibleMap(Map<K, Suppressible<V>> suppressible) {
            this.suppressible = suppressible;
        }

        @Override
        public int size() {
            return suppressible.size();
        }

        @Override
        public boolean isEmpty() {
            return suppressible.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return suppressible.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            if (collectionValueCache == null) {
                collectionValueCache = Suppressibles.unsuppressibleCollection(suppressible.values());
            }
            return collectionValueCache.contains(value);
        }

        @Override
        public V get(Object key) {
            Suppressible<V> value = suppressible.get(key);
            if (value == null) {
                return null;
            }
            return value.value();
        }

        @Override
        public V put(K key, V value) {
            Suppressible<V> returnValue = suppressible.put(key, new Suppressible<>(value));
            if (returnValue == null) {
                return null;
            }
            return returnValue.value();
        }

        @Override
        public V remove(Object key) {
            Suppressible<V> remove = suppressible.remove(key);
            if (remove == null) {
                return null;
            }
            return remove.value();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void clear() {
            suppressible.clear();
        }

        @Override
        public Set<K> keySet() {
            return suppressible.keySet();
        }

        @Override
        public Collection<V> values() {
            if (collectionValueCache == null) {
                collectionValueCache = Suppressibles.unsuppressibleCollection(suppressible.values());
            }
            return collectionValueCache;
        }

        /**
         * This is not right because it is not backed by the map TODO: FIX this by having a delegating set
         */
        // @Override
        // public Set<java.util.Map.Entry<K, V>> entrySet() {
        // Set<Entry<K, V>> result = new HashSet<Entry<K, V>>();
        // Set<java.util.Map.Entry<K, Suppressible<V>>> suppressableEntrySet = suppressible.entrySet();
        // for (java.util.Map.Entry<K, Suppressible<V>> suppressibleEntry : suppressableEntrySet) {
        // result.add(Suppressibles.unsuppressibleEntry(suppressibleEntry));
        // }
        // return result;
        // }
        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            return new DelegatingEntrySet<>(suppressible.entrySet());
        }
    }

    private static class DelegatingEntryIterator<E extends Entry<K, V>, K, V> implements Iterator<E> {

        private Iterator<Entry<K, Suppressible<V>>> suppressible;

        public DelegatingEntryIterator(Iterator<Entry<K, Suppressible<V>>> iterator) {
            this.suppressible = iterator;
        }

        @Override
        public boolean hasNext() {
            return suppressible.hasNext();
        }

        @Override
        public E next() {
            return (E) Suppressibles.unsuppressibleEntry(suppressible.next());
        }

        @Override
        public void remove() {
            suppressible.remove();
        }

    }

    private static class DelegatingEntrySet<E extends Entry<K, V>, K, V> implements Set<E> {

        private Set<Entry<K, Suppressible<V>>> entrySet;

        public DelegatingEntrySet(Set<Entry<K, Suppressible<V>>> entrySet) {
            this.entrySet = entrySet;
        }

        @Override
        public int size() {
            return entrySet.size();
        }

        @Override
        public boolean isEmpty() {
            return entrySet.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (!isEntryOfSametype(o)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Entry<K, V> comparedTo = (Entry<K, V>) o;
            for (Entry<K, Suppressible<V>> entry : entrySet) {
                if (entry.getKey().equals(comparedTo.getKey())) {
                    if (Suppressibles.unsuppressibleEntry(entry).getValue().equals(comparedTo.getValue())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return new DelegatingEntryIterator(entrySet.iterator());
        }

        @Override
        public Object[] toArray() {
            List<Object> result = new ArrayList<>();
            for (Entry<K, Suppressible<V>> entry : entrySet) {
                result.add(Suppressibles.unsuppressibleEntry(entry));
            }
            return result.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            List<Object> result = new ArrayList<>();
            for (Entry<K, Suppressible<V>> entry : entrySet) {
                result.add(Suppressibles.unsuppressibleEntry(entry));
            }
            return result.toArray(a);
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            if (isEntrySetOfSametype(c)) {
                @SuppressWarnings("unchecked")
                Set<Entry<K, V>> comparedTo = (Set<Entry<K, V>>) c;
                for (Entry<K, V> entry : comparedTo) {
                    if (!contains(entry)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            entrySet.clear();
        }

        @SuppressWarnings("unchecked")
        private boolean isEntryOfSametype(Object o) {
            if (!o.getClass().isInstance(Entry.class)) {
                return false;
            }
            @SuppressWarnings("unused")
            Entry<K, V> comparedTo;
            try {
                comparedTo = (Entry<K, V>) o;
            } catch (Exception e) {
                throw new ClassCastException();
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private boolean isEntrySetOfSametype(Object o) {
            if (!o.getClass().isInstance(Set.class)) {
                return false;
            }
            @SuppressWarnings("unused")
            Set<Entry<K, V>> comparedTo;
            try {
                comparedTo = (Set<Entry<K, V>>) o;
            } catch (Exception e) {
                throw new ClassCastException();
            }
            return true;
        }
    }

    private static final class UnsuppressibleSet<E> implements Set<E> {

        private final Set<Suppressible<E>> suppressible;
        private Collection<E> collectionCache;

        private UnsuppressibleSet(Set<Suppressible<E>> suppressible) {
            this.suppressible = suppressible;
        }

        private Collection<E> getCollectionCache() {
            if (collectionCache == null) {
                collectionCache = Suppressibles.unsuppressibleCollection(suppressible);
            }
            return collectionCache;
        }

        @Override
        public int size() {
            return suppressible.size();
        }

        @Override
        public boolean isEmpty() {
            return suppressible.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return getCollectionCache().contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return getCollectionCache().iterator();
        }

        @Override
        public Object[] toArray() {
            return getCollectionCache().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return getCollectionCache().toArray(a);
        }

        @Override
        public boolean add(E e) {
            return suppressible.add(new Suppressible<>(e));
        }

        @Override
        public boolean remove(Object o) {
            return getCollectionCache().remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return getCollectionCache().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean result = false;
            for (E value : c) {
                result |= add(value);
            }
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return getCollectionCache().retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return getCollectionCache().removeAll(c);
        }

        @Override
        public void clear() {
            suppressible.clear();
        }
    }

    private static final class UnsuppressibleListIterator<E> implements ListIterator<E> {

        private ListIterator<Suppressible<E>> suppressible;

        private UnsuppressibleListIterator(ListIterator<Suppressible<E>> suppressiblelistIterator) {
            this.suppressible = suppressiblelistIterator;
        }

        @Override
        public boolean hasNext() {
            return suppressible.hasNext();
        }

        @Override
        public E next() {
            Suppressible<E> next = suppressible.next();
            if (next == null) {
                return null;
            }
            return next.value();
        }

        @Override
        public boolean hasPrevious() {
            return suppressible.hasPrevious();
        }

        @Override
        public E previous() {
            Suppressible<E> previous = suppressible.previous();
            if (previous == null) {
                return null;
            }
            return previous.value();
        }

        @Override
        public int nextIndex() {
            return suppressible.nextIndex();
        }

        @Override
        public int previousIndex() {
            return suppressible.previousIndex();
        }

        @Override
        public void remove() {
            suppressible.remove();
        }

        @Override
        public void set(E e) {
            suppressible.set(new Suppressible<>(e, Suppression.NONE));
        }

        @Override
        public void add(E e) {
            suppressible.add(new Suppressible<>(e, Suppression.NONE));
        }

    }

    private static final class UnsuppressibleIterator<E> implements Iterator<E> {

        private final Iterator<Suppressible<E>> suppressible;

        private UnsuppressibleIterator(Iterator<Suppressible<E>> suppressible) {
            this.suppressible = suppressible;
        }

        @Override
        public boolean hasNext() {
            return suppressible.hasNext();
        }

        @Override
        public E next() {
            Suppressible<E> next = suppressible.next();
            if (next == null) {
                return null;
            }
            return next.value();
        }

        @Override
        public void remove() {
            suppressible.remove();
        }
    }

    private static final class UnsuppressibleList<E> implements List<E> {

        private final List<Suppressible<E>> suppressible;
        private Collection<E> collectionCache;

        private UnsuppressibleList(List<Suppressible<E>> suppressible) {
            this.suppressible = suppressible;
        }

        private Collection<E> getCollectionCache() {
            if (collectionCache == null) {
                collectionCache = Suppressibles.unsuppressibleCollection(suppressible);
            }
            return collectionCache;
        }

        @Override
        public int size() {
            return suppressible.size();
        }

        @Override
        public boolean isEmpty() {
            return suppressible.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return getCollectionCache().contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return getCollectionCache().iterator();
        }

        @Override
        public Object[] toArray() {
            return getCollectionCache().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return getCollectionCache().toArray(a);
        }

        @Override
        public boolean add(E e) {
            return suppressible.add(new Suppressible<>(e, Suppression.NONE));
        }

        @Override
        public boolean remove(Object o) {
            return getCollectionCache().remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return getCollectionCache().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean result = false;
            for (E e : c) {
                result |= add(e);
            }
            return result;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            Iterator<? extends E> iter = c.iterator();
            int i;
            for (i = 0; i < c.size(); i++) {
                add(index + i, iter.next());
            }
            return i > 0;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return getCollectionCache().removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return getCollectionCache().retainAll(c);
        }

        @Override
        public void clear() {
            suppressible.clear();
        }

        @Override
        public E get(int index) {
            return suppressible.get(index).value();
        }

        @Override
        public E set(int index, E element) {
            return suppressible.set(index, new Suppressible<>(element, Suppression.NONE)).value();
        }

        @Override
        public void add(int index, E element) {
            suppressible.add(index, new Suppressible<>(element, Suppression.NONE));
        }

        @Override
        public E remove(int index) {
            Suppressible<E> remove = suppressible.remove(index);
            if (remove == null) {
                return null;
            }
            return remove.value();
        }

        @Override
        public int indexOf(Object o) {
            for (int i = 0; i < suppressible.size(); i++) {
                if (suppressible.get(i).value().equals(o)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            int last = -1;
            for (int i = 0; i < suppressible.size(); i++) {
                if (suppressible.get(i).value().equals(o)) {
                    last = i;
                }
            }
            return last;
        }

        @Override
        public ListIterator<E> listIterator() {
            return Suppressibles.unsuppressibleListIterator(suppressible.listIterator());
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return Suppressibles.unsuppressibleListIterator(suppressible.listIterator(index));
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return Suppressibles.unsuppressibleList(suppressible.subList(fromIndex, toIndex));
        }

    }

    private static final class UnsuppressibleCollection<E> implements Collection<E> {

        private final Collection<Suppressible<E>> suppressible;

        private UnsuppressibleCollection(Collection<Suppressible<E>> suppressible) {
            this.suppressible = suppressible;
        }

        @Override
        public int size() {
            return suppressible.size();
        }

        @Override
        public boolean isEmpty() {
            return suppressible.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            for (Suppressible<E> suppressedEntry : suppressible) {
                if (suppressedEntry.value().equals(o)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return Suppressibles.unsuppressibleIterator(suppressible.iterator());
        }

        @Override
        public Object[] toArray() {
            List<Object> result = new ArrayList<>();
            for (Suppressible<E> suppressedEntry : suppressible) {
                result.add(suppressedEntry.value());
            }
            return result.toArray();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            if (a.length < size()) {
                T[] b = (T[]) Array.newInstance(a.getClass(), size());
                Iterator<Suppressible<E>> iter = suppressible.iterator();
                int i = 0;
                while (iter.hasNext()) {
                    b[i++] = (T) iter.next();
                }
                return b;
            } else {
                Iterator<Suppressible<E>> iter = suppressible.iterator();
                while (iter.hasNext()) {
                    int i = 0;
                    a[i++] = (T) iter.next();
                }
                return a;
            }
        }

        @Override
        public boolean add(E e) {
            return suppressible.add(new Suppressible<>(e));
        }

        @Override
        public boolean remove(Object o) {
            for (Suppressible<E> suppressedEntry : suppressible) {
                if (suppressedEntry.value().equals(o)) {
                    return suppressible.remove(suppressible);
                }
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Suppressible<E> suppressedEntry : suppressible) {
                if (c.contains(suppressedEntry.value())) {
                    continue;
                } else {
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean result = false;
            for (E e : c) {
                result |= add(e);
            }
            return result;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean result = false;
            for (Object e : c) {
                result |= remove(e);
            }
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean result = false;
            for (Suppressible<E> suppressedEntry : suppressible) {
                if (c.contains(suppressedEntry.value())) {
                    continue;
                } else {
                    result |= suppressible.remove(suppressedEntry);
                }
            }
            return result;
        }

        @Override
        public void clear() {
            suppressible.clear();
        }

    }
}
