package net.sumaris.core.vo.administration.user;

import io.leangen.graphql.annotations.GraphQLIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.IValueObject;

import java.sql.Timestamp;
import java.util.Date;

@Data
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTokenVO implements IUpdateDateEntity<Integer, Date>, IValueObject<Integer> {
    private Integer id;
    @GraphQLIgnore
    private Integer userId;
    private String pubkey; // Used only for compatibility with Sumaris API
    @EqualsAndHashCode.Include
    private String token;
    private String name;
    private Integer flags;
    private Timestamp expirationDate;
    private Timestamp lastUsedDate;

    private Date updateDate;
    private Date creationDate;
}
