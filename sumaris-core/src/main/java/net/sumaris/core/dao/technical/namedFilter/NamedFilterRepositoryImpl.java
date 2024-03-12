package net.sumaris.core.dao.technical.namedFilter;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.technical.namedFilter.NamedFilter;
import net.sumaris.core.vo.administration.user.PersonFetchOptions;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFetchOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFilterVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class NamedFilterRepositoryImpl
        extends SumarisJpaRepositoryImpl<NamedFilter, Integer, NamedFilterVO>
        implements NamedFilterSpecifications {

    private final PersonRepository personRepository;

    public NamedFilterRepositoryImpl(EntityManager entityManager, PersonRepository personRepository) {
        super(NamedFilter.class, NamedFilterVO.class,  entityManager);
        this.personRepository = personRepository;
    }

    public Optional<NamedFilterVO> findById(int id, NamedFilterFetchOptions fetchOptions) {
        return super.findById(id).map(entity -> toVO(entity, fetchOptions));
    }

    public List<NamedFilterVO> findAll(
            NamedFilterFilterVO filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection,
            NamedFilterFetchOptions fetchOptions
    ) {
        Specification<NamedFilter> spec = filter != null ? toSpecification(filter) : null;
        TypedQuery<NamedFilter> query = getQuery(spec, offset, size, sortAttribute, sortDirection, getDomainClass());

        try (Stream<NamedFilter> stream = streamQuery(query)) {
            return stream.map(entity -> toVO(entity, fetchOptions)).toList();
        }
    }

    public void deleteById(int id) {
        // TODO
        // - can only delete own filter
        // - manage mange departement NamedFilter
        super.deleteById(id);
    }

    @Override
    public void toEntity(NamedFilterVO source, NamedFilter target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        if (target.getId() == null || target.getCreationDate() == null) {
            target.setCreationDate(new Date());
        }

        // TODO Department filter not yet implemented
        // Integer recorderDepartmentId = source.getRecorderDepartmentId() != null ? source.getRecorderDepartmentId() : (source.getRecorderDepartment() != null ? source.getRecorderDepartment().getId() : null);
        // if (copyIfNull || (recorderDepartmentId != null)) {
        //     if (recorderDepartmentId == null) {
        //         target.setRecorderDepartment(null);
        //     } else {
        //         target.setRecorderDepartment(getReference(Department.class, recorderDepartmentId));
        //     }
        // }

        // Recorder person
        Integer recorderPersonId = source.getRecorderPersonId() != null ? source.getRecorderPersonId() : (source.getRecorderPerson() != null ? source.getRecorderPerson().getId() : null);
        if (copyIfNull || (recorderPersonId != null)) {
            if (recorderPersonId == null) {
                target.setRecorderPerson(null);
            } else {
                target.setRecorderPerson(getReference(Person.class, recorderPersonId));
            }
        }
    }

    public NamedFilterVO toVO(NamedFilter source) {
        return toVO(source, null);
    }

    public NamedFilterVO toVO(NamedFilter source, NamedFilterFetchOptions fetchOptions) {
        if (source == null) return null;
        NamedFilterVO target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    public void toVO(NamedFilter source, NamedFilterVO target, NamedFilterFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

        // Recorder person
        PersonFetchOptions personFetchOptions = PersonFetchOptions.builder()
                .withDepartment(true)
                .withUserProfiles(false)
                .build();
        PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson(), personFetchOptions);
        target.setRecorderPerson(recorderPerson);
        target.setRecorderDepartmentId(recorderPerson.getId());

        // TODO recorderDepartment
    }

    protected final Specification<NamedFilter> toSpecification(NamedFilterFilterVO filter) {
        return toSpecification(filter, null);
    }

    protected Specification<NamedFilter> toSpecification(NamedFilterFilterVO filter, NamedFilterFetchOptions fetchOptions) {
        // TODO recorderDepartment
        // .where(hasRecorderDepartmentId(filter.getRecorderDepartmentId()))
        return BindableSpecification
                .where(hasRecorderPersonId(filter.getRecorderPersonId()))
                .and(hasEntityName(filter.getEntityName()))
                .and(searchText(filter.getSearchText()));
    }
}
