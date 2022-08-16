/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.core.util.reactive;

import io.reactivex.Observable;
import lombok.NonNull;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Observables {

    protected Observables() {
        // helper class does not instantiate
    }

    /**
     * Will aply a filter, that compute a hashcode to detected changes
     * @param observable
     * @param <V>
     * @return
     */
    public static <V> Observable<V> distinctUntilChanged(Observable<V> observable) {
        return distinctUntilChanged(observable, new AtomicReference<>());
    }

    public static <V> Observable<V> distinctUntilChanged(@NonNull Observable<V> observable,
                                                         @NonNull final AtomicReference<Integer> previousHashCode) {
        return observable.filter(value -> {
            int hash = value.hashCode();
            if (previousHashCode.get() == null || previousHashCode.get() != hash) {
                previousHashCode.set(hash);
                return true; // OK, changed
            }
            return false;
        });
    }

    public static <K extends Serializable,
        D extends Date,
        V extends IUpdateDateEntity<K, D>>
    Observable<V> latest(@NonNull Observable<V> observable) {
        return latest(observable, new AtomicReference<>());
    }

    public static <K extends Serializable,
        D extends Date,
        V extends IUpdateDateEntity<K, D>>
    Observable<V> latest(@NonNull Observable<V> observable,
                         @NonNull final AtomicReference<D> previousUpdateDate) {
        return observable.filter(entity -> {
            if (previousUpdateDate.get() != null && previousUpdateDate.get().before(entity.getUpdateDate())) {
                previousUpdateDate.set(entity.getUpdateDate());
                return true;
            }
            return false;
        });
    }

    public static <V> Callable<Optional<V>> distinctUntilChanged(Callable<Optional<V>> loader) {
        return distinctUntilChanged(loader, new AtomicReference<>());
    }

    public static <V> Callable<Optional<V>> distinctUntilChanged(@NonNull Callable<Optional<V>> loader,
                                                                 @NonNull final AtomicReference<Integer> previousHashCode) {
        return () -> loader.call()
            .flatMap(value -> {
                int hash = value.hashCode();
                if (previousHashCode.get() == null || previousHashCode.get() != hash) {
                    previousHashCode.set(hash);
                    return Optional.of(value); // OK, changed
                }
                return Optional.empty(); // Skip
            });
    }

    public static <T, V> Callable<Optional<V>> flatMap(
        @NonNull Callable<Optional<T>> first,
        @NonNull Function<T, Optional<V>> second) {
        return () -> first.call().flatMap(second::apply);
    }
}
