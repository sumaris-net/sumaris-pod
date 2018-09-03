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
import net.sumaris.core.dao.administration.PersonDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("personService")
public class PersonServiceImpl implements PersonService {

	private static final Log log = LogFactory.getLog(PersonServiceImpl.class);

	@Autowired
	protected PersonDao personDao;

	@Override
	public List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
	    if (filter == null) filter = new PersonFilterVO();
		return personDao.findByFilter(filter, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public Long countByFilter(final PersonFilterVO filter) {
		return personDao.countByFilter(filter);
	}

	@Override
	public PersonVO get(final int personId) {
		return personDao.get(personId);
	}

	@Override
	public PersonVO getByPubkey(final String pubkey) {
		Preconditions.checkNotNull(pubkey);
		PersonVO person = personDao.getByPubkeyOrNull(pubkey);
		if (person == null) {
			throw new DataRetrievalFailureException(I18n.t("sumaris.error.person.notFound"));
		}
		return person;
	}

    @Override
    public boolean isExistsByEmailHash(final String hash) {
        Preconditions.checkArgument(StringUtils.isNotBlank(hash));
        return personDao.isExistsByEmailHash(hash);
    }

    @Override
	public ImageAttachmentVO getAvatarByPubkey(final String pubkey) {
		Preconditions.checkNotNull(pubkey);
		return personDao.getAvatarByPubkey(pubkey);
	}

	@Override
	public PersonVO save(PersonVO person) {
		checkValid(person);
		return personDao.save(person);
	}

	@Override
	public void delete(int id) {
		personDao.delete(id);
	}

	/* -- protected methods -- */

	protected void checkValid(PersonVO person) {
		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(person.getEmail(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.email")));
		Preconditions.checkNotNull(person.getFirstName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.firstName")));
		Preconditions.checkNotNull(person.getLastName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.person.lastName")));

	}
}
