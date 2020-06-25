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
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import javax.persistence.LockModeType;
import java.io.Serializable;
import java.util.List;

/**
 * @author peck7 on 03/04/2020.
 */
@NoRepositoryBean
public interface ReferentialRepository<
    E extends IItemReferentialEntity,
    V extends IReferentialVO,
    F extends Serializable
    >
    extends SumarisJpaRepository<E, Integer, V> {

    List<V> findAll(@Nullable F filter);

    Page<V> findAll(@Nullable F filter, Pageable pageable);

    List<V> findAll(@Nullable F filter, @Nullable DataFetchOptions fetchOptions);

    Page<V> findAll(@Nullable F filter, Pageable pageable, @Nullable DataFetchOptions fetchOptions);

    Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions);

    Page<V> findAll(F filter, int offset, int size, String sortAttribute,
                    SortDirection sortDirection, DataFetchOptions fetchOptions);

    List<V> findAllAsVO(@Nullable Specification<E> spec);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable);
    List<V> findAllAsVO(@Nullable Specification<E> spec, DataFetchOptions fetchOptions);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable, DataFetchOptions fetchOptions);

    long count(F filter);

    V get(int id);

    V get(int id, DataFetchOptions fetchOptions);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    V save(V vo);

    Specification<E> toSpecification(@Nullable F filter);
}
