package net.sumaris.core.model.technical.versionning;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.IWithDescriptionAndCommentEntity;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "system_version")
@NamedQueries({
        @NamedQuery(name = "SystemVersion.last", query = "SELECT \n" +
                "      label as schemaVersion\n" +
                "    FROM \n" +
                "      SystemVersion\n" +
                "    WHERE\n" +
                "      id = (select max(id) from SystemVersion)" )
})
public class SystemVersion implements IReferentialEntity<Integer>, IWithDescriptionAndCommentEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SYSTEM_VERSION_SEQ")
    @SequenceGenerator(name = "SYSTEM_VERSION_SEQ", sequenceName="SYSTEM_VERSION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String label;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @Column(name="creation_date", nullable = false)
    private Date creationDate;

    @Column(name="update_date")
    private Date updateDate;


}
