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

package net.sumaris.core.model.referential.gradient;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IWithDescriptionAndCommentEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

/**
 * @author peck7 on 08/06/2020.
 */
@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "depth_gradient")
public class DepthGradient implements IItemReferentialEntity<Integer>, IWithDescriptionAndCommentEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "depth_gradient_seq")
    @SequenceGenerator(name = "depth_gradient_seq", sequenceName="depth_gradient_seq", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(nullable = false, length = LENGTH_LABEL)
    
    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;
}