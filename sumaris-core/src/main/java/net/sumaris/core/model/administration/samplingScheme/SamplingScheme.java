/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.model.administration.samplingScheme;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "sampling_scheme")
@Cacheable
public class SamplingScheme implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SAMPLING_SCHEME_SEQ")
    @SequenceGenerator(name = "SAMPLING_SCHEME_SEQ", sequenceName="SAMPLING_SCHEME_SEQ", allocationSize = IItemReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    private String label;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Program.class)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

}
