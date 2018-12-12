package net.sumaris.core.model.administration.user;

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
import net.sumaris.core.dao.technical.model.IEntityBean;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Data
@Entity
@Table(name = "user_token")
public class UserToken implements IEntityBean<Integer> {

    public static final String PROPERTY_PUBKEY = "pubkey";
    public static final String PROPERTY_TOKEN = "token";

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    private Date creationDate;

    @Column(name="pubkey", nullable = false)
    private String pubkey;

    @Column(name="token", nullable = false, unique = true)
    private String token;

    public int hashCode() {
        return Objects.hash(token);
    }

}
