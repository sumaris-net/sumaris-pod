package net.sumaris.core.model.technical.configuration;

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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "software_property")
public class SoftwareProperty implements IItemReferentialEntity<Integer>  {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SOFTWARE_PROPERTY_SEQ")
    @SequenceGenerator(name = "SOFTWARE_PROPERTY_SEQ", sequenceName="SOFTWARE_PROPERTY_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = 255)
    
    private String label;

    @Column(nullable = false, length = 2000) // Increase, need to large URL in images
    
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_fk", nullable = false)
    private Software software;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ObjectType.class)
    @JoinColumn(name = "object_type_fk", referencedColumnName = "id")
    private ObjectType objectType;

    @Column(name = "object_id")
    private Integer objectId;

}
