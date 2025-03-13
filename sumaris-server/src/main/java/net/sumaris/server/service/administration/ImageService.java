package net.sumaris.server.service.administration;

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

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

public interface ImageService {

    String URI_IMAGE_SUFFIX = "image:";

    List<ImageAttachmentVO> getImagesForObject(int objectId, ObjectTypeEnum objectType);

    List<ImageAttachmentVO> findAllByFilter(ImageAttachmentFilterVO filter, Page page, @Nullable ImageAttachmentFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    ImageAttachmentVO find(int id, @Nullable ImageAttachmentFetchOptions fetchOptions);

    void fillAvatar(PersonVO person);

    void fillLogo(DepartmentVO department);

    String getImageUrl(ImageAttachmentVO image);

    String getImageUrlByUri(String imageUri);

    String getImageUrlById(int id);

    /**
     * Generates the URL for an image based on the provided file path.
     *
     * @param filename the path or name of the image file for which the URL is to be generated
     * @return a string representing the full URL of the image
     */
    String getImageUrlByPath(String filename);

}
