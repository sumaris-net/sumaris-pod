package net.sumaris.core.model.technical;

import lombok.Data;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "software")
public class Software implements IItemReferentialEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SOFTWARE_SEQ")
    @SequenceGenerator(name = "SOFTWARE_SEQ", sequenceName="SOFTWARE_SEQ")
    private Integer id;

    @Column(nullable = false )
    private String label;

    @Column(nullable = false )
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

    @OneToMany(fetch = FetchType.EAGER, targetEntity = SoftwareProperty.class, mappedBy = SoftwareProperty.PROPERTY_SOFTWARE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SoftwareProperty> properties = new ArrayList<>();

}
