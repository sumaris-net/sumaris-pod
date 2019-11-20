package net.sumaris.server.http.graphql.administration;

/*-
 * #%L
 * SUMARiS:: Server
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

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.*;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.service.administration.DepartmentService;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.rest.RestPaths;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsGuest;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.technical.ChangesPublisherService;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AdministrationGraphQLService {

    private static final Logger log = LoggerFactory.getLogger(AdministrationGraphQLService.class);

    private String personAvatarUrl;
    private String departmentLogoUrl;

    private final static String GRAVATAR_URL = "https://www.gravatar.com/avatar/%s";

    @Autowired
    private PersonService personService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ChangesPublisherService changesPublisherService;

    @Autowired
    public AdministrationGraphQLService(SumarisServerConfiguration config) {
        super();

        // Prepare URL for String formatter
        personAvatarUrl = config.getServerUrl() + RestPaths.PERSON_AVATAR_PATH;
        departmentLogoUrl = config.getServerUrl() + RestPaths.DEPARTMENT_LOGO_PATH;
    }

    /* -- Person / department -- */

    @GraphQLQuery(name = "persons", description = "Search in persons")
    @Transactional(readOnly = true)
    @IsUser
    public List<PersonVO> findPersonsByFilter(@GraphQLArgument(name = "filter") PersonFilterVO filter,
                                              @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                              @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                              @GraphQLArgument(name = "sortBy", defaultValue = PersonVO.Fields.PUBKEY) String sort,
                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                              @GraphQLEnvironment() Set<String> fields
    ) {
        List<PersonVO> result = personService.findByFilter(filter, offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);

        // Fill avatar Url
        if (fields.contains(PersonVO.Fields.AVATAR)) {
            result.forEach(this::fillAvatar);
        }

        return result;
    }

    @GraphQLMutation(name = "savePersons", description = "Create or update many persons")
    @IsAdmin
    public List<PersonVO> savePersons(
            @GraphQLArgument(name = "persons") List<PersonVO> persons) {
        return personService.save(persons);
    }

    @GraphQLMutation(name = "deletePersons", description = "Delete many person (by ids)")
    @IsAdmin
    public void deletePersons(
            @GraphQLArgument(name = "ids") List<Integer> ids) {
        personService.delete(ids);
    }

    @GraphQLQuery(name = "isEmailExists", description = "Check if email exists (from a md5 hash)")
    @Transactional(readOnly = true)
    public boolean isEmailExists(@GraphQLArgument(name = "hash") String hash,
                                 @GraphQLArgument(name = "email") String email) {
        if (StringUtils.isBlank(hash) && StringUtils.isBlank(email)) {
            throw new SumarisTechnicalException("required 'meil' or 'hash' argument");
        }
        if (StringUtils.isNotBlank(email)) {
            hash = MD5Util.md5Hex(email);
            log.debug(String.format("Checking if email {%s} exists from hash {%s}...", email, hash));
        }
        else {
            log.debug(String.format("Checking if email exists from hash {%s}...", hash));
        }
        boolean result = personService.isExistsByEmailHash(hash);
        if (result) {
            log.warn(String.format("Email hash {%s} already used !", hash));
        }
        else {
            log.debug(String.format("Email hash {%s} not used.", hash));
        }
        return result;
    }

    /* TODO: enable when pagination will be manage in the client app
    @GraphQLQuery(name = "countPersons", description = "Search in persons")
    @Transactional(readOnly = true)
    public Long countPersonsByFilter(@GraphQLArgument(name = "tripFilter") PersonFilterVO tripFilter) {
        return personService.countByFilter(tripFilter);
    }*/

    @GraphQLQuery(name = "account", description = "Load a user account")
    @Transactional(readOnly = true)
    @IsGuest
    @PreAuthorize("#pubkey == authentication.name")
    public AccountVO loadAccount(@P("pubkey") @GraphQLArgument(name = "pubkey") String pubkey) {

        AccountVO result = accountService.getByPubkey(pubkey);
        fillAvatar(result);
        return result;
    }

    @GraphQLQuery(name = "departments", description = "Search in departments")
    @Transactional(readOnly = true)
    public List<DepartmentVO> findDepartments(@GraphQLArgument(name = "filter") DepartmentFilterVO filter,
                                              @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                              @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                              @GraphQLArgument(name = "sortBy", defaultValue = ReferentialVO.Fields.NAME) String sort,
                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                              @GraphQLEnvironment() Set<String> fields) {
        List<DepartmentVO> result = departmentService.findByFilter(filter, offset, size, sort, direction != null ? SortDirection.valueOf(direction.toUpperCase()) : null);

        // Fill logo Url (if need)
        if (fields.contains(DepartmentVO.Fields.LOGO)) {
            result.forEach(this::fillLogo);
        }

        return result;
    }

    @GraphQLQuery(name = "department", description = "Get a person")
    @Transactional(readOnly = true)
    public DepartmentVO getDepartmentById(@GraphQLArgument(name = "id") int id,
                                         @GraphQLEnvironment() Set<String> fields
    ) {
        DepartmentVO result = departmentService.get(id);

        // Fill avatar Url
        if (result != null && fields.contains(DepartmentVO.Fields.LOGO)) {
            this.fillLogo(result);
        }

        return result;
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "createAccount", description = "Create an account")
    public AccountVO createAccount(@GraphQLArgument(name = "account") AccountVO account) {
        return accountService.createAccount(account);
    }

    @GraphQLMutation(name = "saveAccount", description = "Create or update an account")
    @IsGuest
    @PreAuthorize("hasRole('ROLE_ADMIN') or #account.pubkey == authentication.name")
    public AccountVO saveAccount(@P("account") @GraphQLArgument(name = "account") AccountVO account) {
        return accountService.saveAccount(account);
    }

    @GraphQLMutation(name = "confirmAccountEmail", description = "Confirm an account email")
    public boolean confirmEmail(@GraphQLArgument(name="email") String email,
                                @GraphQLArgument(name="code") String signatureHash) {
        accountService.confirmEmail(email, signatureHash);
        return true;
    }

    @GraphQLMutation(name = "sendAccountConfirmationEmail", description = "Resent confirmation email")
    @IsGuest
    public boolean sendConfirmationEmail(@GraphQLArgument(name="email") String email,
                                         @GraphQLArgument(name="locale", defaultValue = "en_GB") String locale) {
        accountService.sendConfirmationEmail(email, locale);
        return true;
    }

    @GraphQLMutation(name = "saveDepartment", description = "Create or update a department")
    @IsAdmin
    public DepartmentVO saveDepartment(@GraphQLArgument(name = "department") DepartmentVO department) {
        return departmentService.save(department);
    }


    /* -- Subscriptions -- */

    @GraphQLSubscription(name = "updateAccount", description = "Subscribe to an account update")
    @IsGuest
    @PreAuthorize("hasRole('ROLE_ADMIN') or #pubkey == authentication.name")
    public Publisher<AccountVO> updateAccount(
            @P("pubkey") @GraphQLArgument(name = "pubkey") final String pubkey,
            @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to get changes, in seconds.") final Integer minIntervalInSecond) {

        Preconditions.checkNotNull(pubkey, "Missing pubkey");
        Preconditions.checkArgument(pubkey.length() > 6, "Invalid pubkey");

        PersonVO person = personService.getByPubkey(pubkey);

        return changesPublisherService.getPublisher(Person.class, AccountVO.class, person.getId(), minIntervalInSecond, true);
    }

    public DepartmentVO fillLogo(DepartmentVO department) {
        if (department != null && department.getHasLogo() != null && department.getHasLogo().booleanValue() && StringUtils.isBlank(department.getLogo()) && StringUtils.isNotBlank(department.getLabel())) {
            department.setLogo(departmentLogoUrl.replace("{label}", department.getLabel()));
        }
        return department;
    }

    /* -- Protected methods -- */



    protected void fillAvatar(PersonVO person) {
        if (person == null) return;
        if (person.getHasAvatar() != null && person.getHasAvatar().booleanValue() && StringUtils.isNotBlank(person.getPubkey())) {
            person.setAvatar(personAvatarUrl.replace("{pubkey}", person.getPubkey()));
        }
        // Use gravatar URL
        else if (StringUtils.isNotBlank(person.getEmail())){
            person.setAvatar(String.format(GRAVATAR_URL, MD5Util.md5Hex(person.getEmail())));
        }
    }


}
