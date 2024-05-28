package net.sumaris.core.vo.data;

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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Date;

@Data
@EqualsAndHashCode
@FieldNameConstants
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
public class ImageAttachmentVO implements IDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO> {

    @EqualsAndHashCode.Exclude
    @ToString.Include
    private Integer id;
    private String comments;
    @EqualsAndHashCode.Exclude
    private Date creationDate;
    @EqualsAndHashCode.Exclude
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Date dateTime;
    private String contentType;

    @EqualsAndHashCode.Exclude
    private String content;
    private String path;

    private Integer objectTypeId;
    private Integer objectId;

    @EqualsAndHashCode.Exclude
    private String url;

    @JsonGetter
    public String getDataUrl() {
        if (content == null || contentType == null) return null;
        return new StringBuffer().append("data:")
            .append(contentType).append(";base64,")
            .append(content)
            .toString();
    }

    @JsonSetter
    public void setDataUrl(String dataUrl) {
        if (dataUrl == null) return;
        int separatorIndex = dataUrl.indexOf(";base64,");
        if (!dataUrl.startsWith("data:") || separatorIndex == -1) throw new IllegalArgumentException("Invalid 'dataUrl'. Should be a base64 data URL.");

        this.contentType = dataUrl.substring(5, separatorIndex);
        this.content = dataUrl.substring(separatorIndex + 8);
    }
}
