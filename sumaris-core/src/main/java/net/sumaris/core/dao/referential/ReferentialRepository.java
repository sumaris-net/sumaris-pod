package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 03/04/2020.
 */
@NoRepositoryBean
public interface ReferentialRepository<
    ID extends Serializable,
    E extends IItemReferentialEntity<ID>,
    V extends IReferentialVO<ID>,
    F extends IReferentialFilter,
    O extends IFetchOptions
    >
    extends SumarisJpaRepository<E, ID, V> {

    List<V> findAll(F filter);

    List<V> findAll(F filter, @Nullable O fetchOptions);

    List<V> findAll(F filter, net.sumaris.core.dao.technical.Page page, @Nullable O fetchOptions);

    /**
     * @deprecated use Page instead
     */
    @Deprecated
    Page<V> findAll(F filter, Pageable pageable);

    /**
     * @deprecated use Page instead
     */
    @Deprecated
    Page<V> findAll(F filter, Pageable pageable, @Nullable O fetchOptions);

    Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, @Nullable O fetchOptions);

    Page<V> findAll(F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, @Nullable O fetchOptions);

    List<V> findAllAsVO(@Nullable Specification<E> spec);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable);
    List<V> findAllAsVO(@Nullable Specification<E> spec, @Nullable O fetchOptions);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable, @Nullable O fetchOptions);

    long count(F filter);

    V get(ID id);

    V get(ID id, O fetchOptions);

    V getByLabel(String label);

    V getByLabel(String label, O fetchOption);

    Optional<V> findVOById(ID id);

    Optional<V> findVOById(ID id, O fetchOptions);

    Optional<V> findByLabel(String label);

    Optional<V> findByLabel(String label, O fetchOptions);

}
