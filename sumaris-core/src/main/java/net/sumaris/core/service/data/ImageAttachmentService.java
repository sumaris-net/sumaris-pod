package net.sumaris.core.service.data;

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


import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Image service
 * 
 */
@Transactional
public interface ImageAttachmentService {

	@Transactional(readOnly = true)
	ImageAttachmentVO find(int id, @Nullable ImageAttachmentFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ImageAttachmentVO> findAllByFilter(ImageAttachmentFilterVO filter, Page page, ImageAttachmentFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ImageAttachmentVO> findAllByObject(int objectId, ObjectTypeEnum objectType, ImageAttachmentFetchOptions fetchOptions);

	ImageAttachmentVO save(ImageAttachmentVO trip);

	void delete(int id);

}
