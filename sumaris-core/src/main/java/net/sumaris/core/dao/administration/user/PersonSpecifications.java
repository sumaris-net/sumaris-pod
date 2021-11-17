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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.UserProfile;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

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

    String USER_PROFILE_IDS_PARAMETER = "userProfiles";
    String PUBKEY_PARAMETER = "pubkey";
    String EMAIL_PARAMETER = "email";
    String FIRST_NAME_PARAMETER = "firstName";
    String LAST_NAME_PARAMETER = "lastName";
    String USERNAME_PARAMETER = "username";

    default Specification<Person> hasUserProfileIds(PersonFilterVO filter) {
        // Prepare user profile ids
        Collection<Integer> userProfileIds = null;
        if (ArrayUtils.isNotEmpty(filter.getUserProfiles())) {
            userProfileIds = Arrays.stream(filter.getUserProfiles())
                .map(UserProfileEnum::valueOf)
                .map(profile -> profile.id)
                .collect(Collectors.toList());
        }
        else if (ArrayUtils.isNotEmpty(filter.getUserProfileIds())) {
            userProfileIds = Arrays.asList(filter.getUserProfileIds());
        }
        else if (filter.getUserProfileId() != null) {
            userProfileIds = ImmutableList.of(filter.getUserProfileId());
        }

        // Stop if filter not need
        if (CollectionUtils.isEmpty(userProfileIds)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.distinct(true); // Avoid duplicate persons
            ParameterExpression<Collection> userProfileIdsParam = criteriaBuilder.parameter(Collection.class, USER_PROFILE_IDS_PARAMETER);
            return criteriaBuilder
                .in(Daos.composePath(root, StringUtils.doting(Person.Fields.USER_PROFILES, UserProfile.Fields.ID)))
                .value(userProfileIdsParam);
        })
        .addBind(USER_PROFILE_IDS_PARAMETER, userProfileIds);
    }

    default Specification<Person> hasPubkey(String pubkey) {
        if (pubkey == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, PUBKEY_PARAMETER);
            return criteriaBuilder.equal(root.get(Person.Fields.PUBKEY), parameter);
        }).addBind(PUBKEY_PARAMETER, pubkey);
    }

    default Specification<Person> hasUsername(String username) {
        if (StringUtils.isBlank(username)) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, USERNAME_PARAMETER);
            return criteriaBuilder.or(
                criteriaBuilder.equal(root.get(Person.Fields.USERNAME), parameter),
                criteriaBuilder.equal(root.get(Person.Fields.USERNAME_EXTRANET), parameter)
            );
        }).addBind(USERNAME_PARAMETER, username);
    }

    default Specification<Person> hasEmail(String email) {
        if (email == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, EMAIL_PARAMETER);
            return criteriaBuilder.equal(root.get(Person.Fields.EMAIL), parameter);
        }).addBind(EMAIL_PARAMETER, email);
    }

    default Specification<Person> hasFirstName(String firstName) {
        if (firstName == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, FIRST_NAME_PARAMETER);
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get(Person.Fields.FIRST_NAME)), parameter);
        }).addBind(FIRST_NAME_PARAMETER, firstName.toUpperCase());
    }

    default Specification<Person> hasLastName(String lastName) {
        if (lastName == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, LAST_NAME_PARAMETER);
            return criteriaBuilder.equal(criteriaBuilder.upper(root.get(Person.Fields.LAST_NAME)), parameter);
        }).addBind(LAST_NAME_PARAMETER, lastName.toUpperCase());
    }

    Optional<PersonVO> findById(int id);

    Optional<PersonVO> findByPubkey(String pubkey);

    Optional<PersonVO> findByUsername(String username);

    List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    long countByFilter(PersonFilterVO filter);

    List<String> getEmailsByProfiles(List<Integer> userProfileIds, List<Integer> statusIds);

    void clearCache();

}
