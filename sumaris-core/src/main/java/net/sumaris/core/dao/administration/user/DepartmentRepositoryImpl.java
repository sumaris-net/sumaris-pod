package net.sumaris.core.dao.administration.user;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.cache.CacheNames;
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
    extends ReferentialRepositoryImpl<Department, DepartmentVO, DepartmentFilterVO>
    implements DepartmentRepositoryExtend {

    public DepartmentRepositoryImpl(EntityManager entityManager) {
        super(Department.class, DepartmentVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id", unless="#result==null")
    public DepartmentVO get(int id) {
        return super.get(id);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.DEPARTMENT_BY_LABEL, key = "#label", unless="#result==null")
    public DepartmentVO getByLabel(String label) {
        return super.getByLabel(label);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id"),
        @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_LABEL, allEntries = true)
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
        @CachePut(cacheNames= CacheNames.DEPARTMENT_BY_ID, key="#vo.id", condition = "#vo != null && #vo.id != null"),
        @CachePut(cacheNames= CacheNames.DEPARTMENT_BY_LABEL, key="#vo.label", condition = "#vo != null && #vo.id != null && #vo.label != null")
    })
    public DepartmentVO save(DepartmentVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getLabel());
        Preconditions.checkNotNull(vo.getName());
        Preconditions.checkNotNull(vo.getSiteUrl());

        return super.save(vo);
    }

    @Override
    protected Specification<Department> toSpecification(DepartmentFilterVO filter) {
        return super.toSpecification(filter)
            .and(withLogo(filter.getWithLogo()));
    }

}
