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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.annotation.EntityEnums;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.DataBeans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("imageAttachmentService")
@RequiredArgsConstructor
@Slf4j
public class ImageAttachmentServiceImpl implements ImageAttachmentService {

	protected final SumarisConfiguration configuration;
	protected final ImageAttachmentRepository imageAttachmentRepository;

	protected boolean enableTrash;
	protected boolean enableDataImagesDirectory;

	@PostConstruct
	@EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
	public void onConfigurationReady() {
		this.enableTrash = configuration.enableEntityTrash();
		this.enableDataImagesDirectory = configuration.enableDataImagesDirectory();
	}

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
	public ImageAttachmentVO save(ImageAttachmentVO image) {
		Preconditions.checkNotNull(image);

		return imageAttachmentRepository.save(image);
	}

	@Override
	public List<ImageAttachmentVO> saveAllByParent(List<ImageAttachmentVO> sources, @NonNull IEntity<Integer> parent, @NonNull ObjectTypeEnum objectTypeEnum) {
		EntityEnums.checkResolved(objectTypeEnum);

		List<Integer> existingIdsToRemove = imageAttachmentRepository.getIdsByObject(parent.getId(), objectTypeEnum.getId());
		List<ImageAttachmentVO> targets = Beans.getStream(sources)
			.filter(Objects::nonNull)
			.map(source -> {
				boolean exists = existingIdsToRemove.remove(source.getId());

				// Update only, when images already exists, and no content changes
				if (exists && source.getContent() == null) {
					// Update comments only
					log.debug("Update Image#{} comments", source.getId());
					imageAttachmentRepository.updateComments(source.getId(), source.getComments());
				}
				else {
					Preconditions.checkNotNull(source.getContent(), "Missing required image 'content' value");
					Preconditions.checkArgument(source.getId() == null, "Cannot change the content of an existing image");

					// Fill defaults
					fillDefaultProperties(source, parent, objectTypeEnum.getId());

					// Save with content in the DB
					source = imageAttachmentRepository.save(source);
				}
				return source;
			}).toList();

		// Remove
		if (CollectionUtils.isNotEmpty(existingIdsToRemove)) {
			// Remove files
			imageAttachmentRepository.deleteAllById(existingIdsToRemove);
		}

		return targets;
	}

	@Override
	public void delete(int id) {
		// TODO : should delete files also !
		imageAttachmentRepository.deleteById(id);
	}


	/* -- protected functions -- */

	protected void fillDefaultProperties(ImageAttachmentVO source,
										 IEntity<Integer> parent,
										 int objectTypeId) {
		if (source == null) return;

		// Set default recorder department
		if (parent instanceof IWithRecorderDepartmentEntity<?, ?> sourceParent) {
            DataBeans.setDefaultRecorderDepartment(source, (DepartmentVO)sourceParent.getRecorderDepartment());
		}

		// Fill date
		if (source.getDateTime() == null && parent instanceof IUpdateDateEntity<?, ?> sourceParent) {
			// Use update date
			source.setDateTime((Date)sourceParent.getUpdateDate());

			// Or creation date (when update date not yet defined)
			if (source.getDateTime() == null && parent instanceof IRootDataEntity<?> rootParent) {
				source.setDateTime(rootParent.getCreationDate());
			}
		}


		// Link to parent
		source.setObjectId(parent.getId());
		source.setObjectTypeId(objectTypeId);
	}
}
