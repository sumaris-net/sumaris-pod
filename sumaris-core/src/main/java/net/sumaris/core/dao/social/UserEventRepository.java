package net.sumaris.core.dao.social;

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

import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.vo.social.UserEventVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

public interface UserEventRepository
    extends SumarisJpaRepository<UserEvent, Integer, UserEventVO>, UserEventSpecifications {

    @Query("select max(creationDate) from UserEvent where recipient in (:recipients)")
    Timestamp getMaxCreationDateByRecipient(@Param("recipients") Collection<String> recipients);

    @Query("select max(readDate) from UserEvent where recipient in (:recipients)")
    Timestamp getMaxReadDateByRecipient(@Param("recipients") Collection<String> recipients);

    @Query("from UserEvent where source = :source")
    UserEvent getBySource(@Param("source") String source);

    List<UserEvent> findAllByIssuer(String issuer);

    List<UserEvent> findAllByRecipient(String recipient);

    @Modifying
    @Query("update UserEvent e set e.issuer = :newPubkey where e.issuer = :oldPubkey")
    void updateIssuerPubkey(@Param("oldPubkey") String oldPubkey, @Param("newPubkey") String newPubkey);

    @Modifying
    @Query("update UserEvent e set e.recipient = :newPubkey where e.recipient = :oldPubkey")
    void updateRecipientPubkey(@Param("oldPubkey") String oldPubkey, @Param("newPubkey") String newPubkey);
}

