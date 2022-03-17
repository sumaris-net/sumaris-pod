package net.sumaris.core.dao.administration.user;

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

import com.google.common.base.Preconditions;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;

/**
 * @author peck7 on 20/08/2020.
 */
public class DepartmentRepositoryImpl
    extends ReferentialRepositoryImpl<Department, DepartmentVO, DepartmentFilterVO, ReferentialFetchOptions>
    implements DepartmentSpecifications {

    public DepartmentRepositoryImpl(EntityManager entityManager) {
        super(Department.class, DepartmentVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_ID, key = "#id", unless="#result==null")
    public DepartmentVO get(int id) {
        return super.get(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_LABEL, key = "#label", unless="#result==null")
    public DepartmentVO getByLabel(String label) {
        return super.getByLabel(label);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_ID, key = "#id"),
        @CacheEvict(cacheNames = CacheConfiguration.Names.DEPARTMENT_BY_LABEL, allEntries = true)
    })
    public void deleteById(Integer id) {
        super.deleteById(id);
    }

    @Override
    protected void toVO(Department source, DepartmentVO target, ReferentialFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Has logo
        target.setHasLogo(source.getLogo() != null);

    }

    @Override
    @Caching(put = {
        @CachePut(cacheNames= CacheConfiguration.Names.DEPARTMENT_BY_ID, key="#source.id", condition = "#source != null && #source.id != null"),
        @CachePut(cacheNames= CacheConfiguration.Names.DEPARTMENT_BY_LABEL, key="#source.label", condition = "#source != null && #source.id != null && #source.label != null")
    })
    public DepartmentVO save(DepartmentVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());
        Preconditions.checkNotNull(source.getName());
        Preconditions.checkNotNull(source.getSiteUrl());

        return super.save(source);
    }

    @Override
    protected Specification<Department> toSpecification(DepartmentFilterVO filter, ReferentialFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(withLogo(filter.getWithLogo()));
    }

}
