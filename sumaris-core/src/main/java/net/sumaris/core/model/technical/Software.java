package net.sumaris.core.model.technical;

import lombok.Data;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "software")
public class Software implements IItemReferentialEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "APP_CONF_SEQ")
    @SequenceGenerator(name = "APP_CONF_SEQ", sequenceName="APP_CONF_SEQ")
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


//    @OneToMany(fetch = FetchType.EAGER)
//    @JoinColumn(name = "software_fk", nullable = false)
//    private List<SoftwareProperty> configProperties;

}
