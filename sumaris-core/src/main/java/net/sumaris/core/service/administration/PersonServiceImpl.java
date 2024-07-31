package net.sumaris.core.service.administration;

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


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.service.data.ImageAttachmentService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("personService")
@RequiredArgsConstructor
@Slf4j
public class PersonServiceImpl implements PersonService {


	protected final PersonRepository personRepository;

	protected final DepartmentRepository departmentRepository;

	private final ImageAttachmentService imageAttachmentService;



	@Override
	public List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return personRepository.findByFilter(PersonFilterVO.nullToEmpty(filter), offset, size, sortAttribute, sortDirection);
	}

	@Override
	public Page<PersonVO> findByFilter(PersonFilterVO filter, Pageable page) {
		return personRepository.findByFilter(filter, page);
	}

	@Override
	public Page<String> findEmailsByFilter(PersonFilterVO filter, Pageable page) {
		return findByFilter(filter, page)
			.map(PersonVO::getEmail);
	}

	@Override
	public Page<String> findPubkeysByFilter(PersonFilterVO filter, Pageable page) {
		return findByFilter(filter, page)
			.map(PersonVO::getEmail);
	}

	@Override
	public String findByEmailMD5(String emailMD5) {
		return personRepository.findByEmailMD5(emailMD5).getEmail();
	}

	@Override
	public Long countByFilter(PersonFilterVO filter) {
		return personRepository.countByFilter(PersonFilterVO.nullToEmpty(filter));
	}

	@Override
	public PersonVO getById(final int id) {
		// This find method was a find in PersonDaoImpl
		return personRepository.findVOById(id)
				.orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.person.notFound")));
	}

	@Override
	public String getEmailById(final int id) {
		return personRepository.getById(id).getEmail();
	}

	@Override
	public String getPubkeyById(int id) {
		return personRepository.getById(id).getPubkey();
	}

	@Override
	public PersonVO getByPubkey(@NonNull String pubkey) {
		return personRepository.findByPubkey(pubkey)
				.orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.person.notFound")));
	}

	@Override
	public Optional<PersonVO> findByPubkey(@NonNull String pubkey) {
		return personRepository.findByPubkey(pubkey);
	}

	@Override
	public PersonVO getByUsername(@NonNull String username) {
		return personRepository.findByUsername(username)
			.orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.person.notFound")));
	}

	@Override
	public PersonVO getByEmail(String email) {
		List<PersonVO> matches = findByFilter(
			PersonFilterVO.builder()
				.email(email)
				.build(), Pageable.unpaged()).getContent();
		if (CollectionUtils.size(matches) != 1) throw new DataNotFoundException(I18n.t("sumaris.error.person.notFound"));
		return matches.get(0);
	}

	@Override
	public Optional<PersonVO> findByUsername(@NonNull String username) {
		return personRepository.findByUsername(username);
	}

	@Override
    public boolean isExistsByEmailHash(final String hash) {
        Preconditions.checkArgument(StringUtils.isNotBlank(hash));
        return personRepository.existsByEmailMD5(hash);
    }

    @Override
	@Cacheable(cacheNames = CacheConfiguration.Names.PERSON_AVATAR_BY_PUBKEY, unless = "#result==null")
	public ImageAttachmentVO getAvatarByPubkey(@NonNull final String pubkey, ImageAttachmentFetchOptions fetchOptions) {
		Optional<Person> person = personRepository.findByPubkey(pubkey)
			.flatMap(vo -> personRepository.findById(vo.getId()));

		int avatarId = person
			.map(Person::getAvatar)
			.map(ImageAttachment::getId)
			.orElseThrow(() -> new DataRetrievalFailureException(I18n.t("sumaris.error.person.avatar.notFound")));

		return imageAttachmentService.find(avatarId, fetchOptions);
	}

	@Override
	public List<String> getEmailsByProfiles(UserProfileEnum... userProfiles) {
		Preconditions.checkNotNull(userProfiles);

		return personRepository.getEmailsByProfiles(
				ImmutableList.copyOf(userProfiles).stream().map(up -> up.id).collect(Collectors.toList()),
				ImmutableList.of(StatusEnum.ENABLE.getId())
		);
	}

	@Override
	public PersonVO save(PersonVO person) {
		checkValid(person);

		// Make sure to fill department, before saving, because of cache
		if (person.getDepartment().getLabel() == null) {
			DepartmentVO department = departmentRepository.get(person.getDepartment().getId());
			person.setDepartment(department);
		}

		return personRepository.save(person);
	}

	@Override
	public List<PersonVO> save(List<PersonVO> persons) {
		Preconditions.checkNotNull(persons);

		return persons.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		personRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);

		ids.forEach(this::delete);
	}

	/* -- protected methods -- */
	protected void checkValid(PersonVO person) {
		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(person.getEmail(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.email")));
		Preconditions.checkNotNull(person.getFirstName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.firstName")));
		Preconditions.checkNotNull(person.getLastName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.lastName")));
		Preconditions.checkNotNull(person.getDepartment(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.department")));
		Preconditions.checkNotNull(person.getDepartment().getId(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.department")));
	}
}
