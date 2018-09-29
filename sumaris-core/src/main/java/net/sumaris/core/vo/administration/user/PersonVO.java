package net.sumaris.core.vo.administration.user;

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

import lombok.Data;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.vo.referential.UserProfileVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class PersonVO implements IUpdateDateEntityBean<Integer, Date> {

    public static final String PROPERTY_PUBKEY = "pubkey";
    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_AVATAR = "avatar";

    private Integer id;
    private Date updateDate;
    private Date creationDate;

    private String firstName;
    private String lastName;
    private String email;

    private String pubkey;

    private Integer statusId;

    private DepartmentVO department;

    private List<String> profiles;

    private boolean hasAvatar;

    private String avatar;

}
