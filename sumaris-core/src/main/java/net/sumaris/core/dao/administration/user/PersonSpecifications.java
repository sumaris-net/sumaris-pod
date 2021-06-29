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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.UserProfile;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author peck7 on 20/08/2020.
 */
public interface PersonSpecifications extends ReferentialSpecifications<Person> {

    String USER_PROFILES_PARAMETER = "userProfiles";
    String USER_PROFILES_SET_PARAMETER = "userProfilesSet";
    String PUBKEY_PARAMETER = "pubkey";
    String EMAIL_PARAMETER = "email";
    String FIRST_NAME_PARAMETER = "firstName";
    String LAST_NAME_PARAMETER = "lastName";
    String USERNAME_PARAMETER = "username";

    default Specification<Person> hasUserProfileIds(PersonFilterVO filter) {
        // Prepare user profile ids
        Collection<Integer> userProfileIds;
        if (ArrayUtils.isNotEmpty(filter.getUserProfiles())) {
            userProfileIds = Arrays.stream(filter.getUserProfiles())
                .map(UserProfileEnum::valueOf)
                .map(profile -> profile.id)
                .collect(Collectors.toList());
        }
        else if (ArrayUtils.isNotEmpty(filter.getUserProfileIds())) {
            userProfileIds = ImmutableList.copyOf(filter.getUserProfileIds());
        }
        else if (filter.getUserProfileId() != null) {
            userProfileIds = ImmutableList.of(filter.getUserProfileId());
        }
        else {
            userProfileIds = null;
        }

        BindableSpecification<Person> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Collection> userProfileParam = criteriaBuilder.parameter(Collection.class, USER_PROFILES_PARAMETER);
            ParameterExpression<Boolean> userProfileSetParam = criteriaBuilder.parameter(Boolean.class, USER_PROFILES_SET_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isFalse(userProfileSetParam),
                criteriaBuilder.in(root.join(Person.Fields.USER_PROFILES, JoinType.LEFT).get(UserProfile.Fields.ID)).value(userProfileParam)
            );
        });
        specification.addBind(USER_PROFILES_SET_PARAMETER, CollectionUtils.isNotEmpty(userProfileIds));
        specification.addBind(USER_PROFILES_PARAMETER, userProfileIds);
        return specification;
    }

    default Specification<Person> hasPubkey(String pubkey) {
        BindableSpecification<Person> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, PUBKEY_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(root.get(Person.Fields.PUBKEY), parameter)
            );
        });
        specification.addBind(PUBKEY_PARAMETER, pubkey);
        return specification;
    }

    default Specification<Person> hasUsername(String username) {
        BindableSpecification<Person> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, USERNAME_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(root.get(Person.Fields.USERNAME), parameter),
                criteriaBuilder.equal(root.get(Person.Fields.USERNAME_EXTRANET), parameter)
            );
        });
        specification.addBind(USERNAME_PARAMETER, username);
        return specification;
    }

    default Specification<Person> hasEmail(String email) {
        BindableSpecification<Person> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, EMAIL_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(root.get(Person.Fields.EMAIL), parameter)
            );
        });
        specification.addBind(EMAIL_PARAMETER, email);
        return specification;
    }

    default Specification<Person> hasFirstName(String firstName) {
        BindableSpecification<Person> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, FIRST_NAME_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(criteriaBuilder.upper(root.get(Person.Fields.FIRST_NAME)), criteriaBuilder.upper(parameter))
            );
        });
        specification.addBind(FIRST_NAME_PARAMETER, firstName);
        return specification;
    }

    default Specification<Person> hasLastName(String lastName) {
        BindableSpecification<Person> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, LAST_NAME_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(parameter),
                criteriaBuilder.equal(criteriaBuilder.upper(root.get(Person.Fields.LAST_NAME)), criteriaBuilder.upper(parameter))
            );
        });
        specification.addBind(LAST_NAME_PARAMETER, lastName);
        return specification;
    }

    Optional<PersonVO> findById(int id);

    Optional<PersonVO> findByPubkey(String pubkey);

    Optional<PersonVO> findByUsername(String username);

    List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    long countByFilter(PersonFilterVO filter);

    List<String> getEmailsByProfiles(List<Integer> userProfileIds, List<Integer> statusIds);

    void clearCache();

}
