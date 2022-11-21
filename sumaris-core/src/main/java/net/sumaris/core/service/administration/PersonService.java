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


import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional(isolation = Isolation.READ_COMMITTED)
public interface PersonService {

	@Transactional(readOnly = true)
	List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

	@Transactional(readOnly = true)
	Page<PersonVO> findByFilter(PersonFilterVO filter, Pageable page);

	@Transactional(readOnly = true)
	Page<String> findEmailsByFilter(PersonFilterVO filter, Pageable page);

	@Transactional(readOnly = true)
	Page<String> findPubkeysByFilter(PersonFilterVO filter, Pageable page);

	@Transactional(readOnly = true)
	Long countByFilter(PersonFilterVO filter);

	@Transactional(readOnly = true)
	PersonVO getById(int id);

	@Transactional(readOnly = true)
	String getEmailById(int id);

	@Transactional(readOnly = true)
	String getPubkeyById(int id);

	@Transactional(readOnly = true)
	PersonVO getByPubkey(String pubkey);

	@Transactional(readOnly = true)
	Optional<PersonVO> findByPubkey(String pubkey);

	@Transactional(readOnly = true)
	PersonVO getByUsername(String username);

	@Transactional(readOnly = true)
	PersonVO getByEmail(String email);

	@Transactional(readOnly = true)
	Optional<PersonVO> findByUsername(String username);

	@Transactional(readOnly = true)
	boolean isExistsByEmailHash(String hash);

	@Transactional(readOnly = true)
	ImageAttachmentVO getAvatarByPubkey(String pubkey, ImageAttachmentFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<String> getEmailsByProfiles(UserProfileEnum... userProfiles);

	List<PersonVO> save(List<PersonVO> persons);

	PersonVO save(PersonVO person);

	void delete(int id);

	void delete(List<Integer> ids);
}
