package net.sumaris.core.dao.data;

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

import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author peck7 on 31/08/2020.
 */
public interface ImageAttachmentRepository
        extends DataRepository<ImageAttachment, ImageAttachmentVO, ImageAttachmentFilterVO, ImageAttachmentFetchOptions>,
        ImageAttachmentSpecifications {

    @Query("select id from ImageAttachment where objectId=:objectId and objectType.id=:objectTypeId")
    List<Integer> getIdsFromObject(@Param("objectId") int objectId, @Param("objectTypeId") int objectTypeId);

    @Query("from ImageAttachment where objectId=:objectId and objectType.id=:objectTypeId")
    List<ImageAttachment> findAllByObject(@Param("objectId") int objectId, @Param("objectTypeId") int objectTypeId);
}
