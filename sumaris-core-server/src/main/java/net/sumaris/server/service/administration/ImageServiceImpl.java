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

import net.sumaris.core.dao.data.ImageAttachmentDao;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.rest.RestPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("imageService")
public class ImageServiceImpl implements ImageService {

    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

    private final static String GRAVATAR_URL = "https://www.gravatar.com/avatar/%s";

    private String personAvatarUrl;
    private String departmentLogoUrl;
    private SumarisServerConfiguration config;

    @Autowired
    private ImageAttachmentDao dao;


    @Autowired
    public ImageServiceImpl(SumarisServerConfiguration config) {
        this.config = config;

        // Prepare URL for String formatter
        personAvatarUrl = config.getServerUrl() + RestPaths.PERSON_AVATAR_PATH;
        departmentLogoUrl = config.getServerUrl() + RestPaths.DEPARTMENT_LOGO_PATH;
    }

    @Override
    public ImageAttachmentVO get(int id) {
        return dao.get(id);
    }

    public void fillAvatar(PersonVO person) {
        if (person == null) return;
        if (person.isHasAvatar() && org.apache.commons.lang3.StringUtils.isNotBlank(person.getPubkey())) {
            person.setAvatar(personAvatarUrl.replace("{pubkey}", person.getPubkey()));
        }
        // Use gravatar URL
        else if (org.apache.commons.lang3.StringUtils.isNotBlank(person.getEmail())){
            person.setAvatar(String.format(GRAVATAR_URL, MD5Util.md5Hex(person.getEmail())));
        }
    }

    public void fillLogo(DepartmentVO department) {
        if (department == null) return;
        if (department.isHasLogo() && org.apache.commons.lang3.StringUtils.isNotBlank(department.getLabel())) {
            department.setLogo(departmentLogoUrl.replace("{label}", department.getLabel()));
        }
    }
}
