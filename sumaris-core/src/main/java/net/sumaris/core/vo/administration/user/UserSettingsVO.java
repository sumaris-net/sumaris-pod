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
import net.sumaris.core.vo.IValueObject;

import java.sql.Timestamp;
import java.util.Date;

@Data
public class UserSettingsVO implements IUpdateDateEntityBean<Integer, Date>, IValueObject<Integer> {

    public static final String PROPERTY_ISSUER = "issuer";
    public static final String PROPERTY_CONTENT = "content";
    public static final String PROPERTY_NONCE = "nonce";
    public static final String PROPERTY_LOCALE = "locale";
    public static final String PROPERTY_LAT_LONG_FORMAT= "latLongFormat";

    private Integer id;
    private Date updateDate;

    private String issuer;

    // public settings
    private String locale;
    private String latLongFormat;

    // private settings (encrypted content, with nonce)
    private String content;
    private String nonce;
}
