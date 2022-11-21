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


import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("imageAttachmentService")
@RequiredArgsConstructor
@Slf4j
public class ImageAttachmentServiceImpl implements ImageAttachmentService {

	protected final ImageAttachmentRepository imageAttachmentRepository;

	@Override
	public List<ImageAttachmentVO> findAllByObject(int objectId, ObjectTypeEnum objectType, ImageAttachmentFetchOptions fetchOptions) {
		return imageAttachmentRepository.findAllByObject(objectId, objectType.getId())
			.stream()
			.map(image -> imageAttachmentRepository.toVO(image, fetchOptions))
			.collect(Collectors.toList());
	}

	@Override
	public ImageAttachmentVO find(int id, ImageAttachmentFetchOptions fetchOptions) {
		return imageAttachmentRepository.findById(id, fetchOptions).orElse(null);
	}

	@Override
	public List<ImageAttachmentVO> findAllByFilter(ImageAttachmentFilterVO filter, Page page, ImageAttachmentFetchOptions fetchOptions) {
		return imageAttachmentRepository.findAll(filter, page, fetchOptions);
	}

	@Override
	public ImageAttachmentVO save(ImageAttachmentVO imageAttachment) {
		Preconditions.checkNotNull(imageAttachment);

		return imageAttachmentRepository.save(imageAttachment);
	}

	@Override
	public void delete(int id) {
		imageAttachmentRepository.deleteById(id);
	}

}
