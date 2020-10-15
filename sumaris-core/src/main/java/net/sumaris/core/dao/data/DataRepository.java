package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.filter.IDataFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface DataRepository<
    E extends IDataEntity<Integer>,
    V extends IDataVO<Integer>,
    F extends IDataFilter,
    O extends IFetchOptions
    >
    extends SumarisJpaRepository<E, Integer, V> {

    // From a filter
    List<V> findAll(F filter);

    Page<V> findAll(F filter, Pageable pageable);

    List<V> findAll(F filter, @Nullable O fetchOptions);

    List<V> findAll(F filter, net.sumaris.core.dao.technical.Page page, @Nullable O fetchOptions);

    Page<V> findAll(F filter, Pageable pageable, @Nullable O fetchOptions);

    Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, O fetchOptions);

    Page<V> findAll(F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, O fetchOptions);

    // From a specification
    List<V> findAllVO(@Nullable Specification<E> spec);

    Page<V> findAllVO(@Nullable Specification<E> spec, Pageable pageable);

    List<V> findAllVO(@Nullable Specification<E> spec, O fetchOptions);

    Page<V> findAllVO(@Nullable Specification<E> spec, Pageable pageable, O fetchOption);

    long count(F filter);

    Optional<V> findById(int id);

    Optional<V> findById(int id, O fetchOptions);

    V get(Integer id);

    V get(Integer id, O fetchOptions);

    V control(V vo);

    V validate(V vo);

    V unvalidate(V vo);

    V qualify(V vo);

    V toVO(E source, O fetchOptions);

}
