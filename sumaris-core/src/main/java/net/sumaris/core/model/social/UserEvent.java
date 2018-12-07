package net.sumaris.core.model.social;

import lombok.Data;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;

import javax.persistence.*;
import java.util.Date;


@Data
@Entity
@Table(name = "user_event")
@Cacheable
/**
 * TODO: complete this entity class
 */
public class UserEvent implements IUpdateDateEntityBean<Integer, Date> {

    public static final String PROPERTY_ISSUER = "issuer";

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(name = "issuer", nullable = false)
    private Date issuer;

    public String toString() {
        return new StringBuilder().append(super.toString()).append(",issuer=").append(this.issuer).toString();
    }
}