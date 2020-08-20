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
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 03/04/2020.
 */
@NoRepositoryBean
public interface ReferentialRepository<
    E extends IItemReferentialEntity,
    V extends IReferentialVO,
    F extends ReferentialFilterVO
    >
    extends SumarisJpaRepository<E, Integer, V> {

    List<V> findAll(@Nullable F filter);

    // TODO: find usage of Page ??
    Page<V> findAll(@Nullable F filter, Pageable pageable);

    List<V> findAll(@Nullable F filter, @Nullable ReferentialFetchOptions fetchOptions);

    Page<V> findAll(@Nullable F filter, Pageable pageable, @Nullable ReferentialFetchOptions fetchOptions);

    Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, ReferentialFetchOptions fetchOptions);

    Page<V> findAll(F filter, int offset, int size, String sortAttribute,
                    SortDirection sortDirection, ReferentialFetchOptions fetchOptions);

    List<V> findAllAsVO(@Nullable Specification<E> spec);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable);
    List<V> findAllAsVO(@Nullable Specification<E> spec, ReferentialFetchOptions fetchOptions);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable, ReferentialFetchOptions fetchOptions);

    long count(F filter);

    V get(int id);

    V get(int id, ReferentialFetchOptions fetchOptions);

    V getByLabel(String label);

    Optional<V> findByLabel(String label);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    V save(V vo);

    Specification<E> toSpecification(@Nullable F filter);
}
