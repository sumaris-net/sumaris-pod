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
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("imageAttachmentService")
public class ImageAttachmentServiceImpl implements ImageAttachmentService {

	private static final Logger log = LoggerFactory.getLogger(ImageAttachmentServiceImpl.class);

	@Autowired
	protected ImageAttachmentRepository imageAttachmentRepository;

	@Override
	public ImageAttachmentVO find(int id) {
		return imageAttachmentRepository.findById(id).orElse(null);
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
