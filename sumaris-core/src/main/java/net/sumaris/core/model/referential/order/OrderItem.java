package net.sumaris.core.model.referential.order;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.referential.IReferentialEntity;

import javax.persistence.*;
import java.util.Date;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "order_item")
public class OrderItem implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "ORDER_ITEM_SEQ")
    @SequenceGenerator(name = "ORDER_ITEM_SEQ", sequenceName="ORDER_ITEM_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(name = "object_id", nullable = false)
    @ToString.Include
    private Integer objectId;

    @Column(name="rank", nullable = false)
    @ToString.Include
    private Integer rankOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_type_fk", nullable = false)
    @ToString.Include
    private OrderType type;
}
