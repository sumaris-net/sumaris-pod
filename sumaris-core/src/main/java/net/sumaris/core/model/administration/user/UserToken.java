package net.sumaris.core.model.administration.user;

import lombok.Data;
import net.sumaris.core.dao.technical.model.IEntityBean;

import javax.persistence.*;
import java.util.Date;

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

    @Column(name="pubkey", nullable = false, unique = true)
    private String pubkey;

    @Column(name="token", nullable = false, unique = true)
    private String token;

}
