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

package net.sumaris.server.service.technical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.server.service.administration.AccountServiceImpl;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.*;

@Component
public class EntityTrashService {

    private static final Logger log = LoggerFactory.getLogger(EntityTrashService.class);

    private boolean enable;
    private File trashDirectory;

    @Resource
    private ObjectMapper objectMapper;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.enable = event.getConfig().enableEntityTrash();
        this.trashDirectory = event.getConfig().getTrashDirectory();
    }

    @JmsListener(destination = "deleteTrip", containerFactory = "jmsListenerContainerFactory")
    protected void onDeleteTrip(Serializable event) throws IOException, JMSException {
        Preconditions.checkNotNull(event);

        if (!this.enable || !(event instanceof IValueObject)) return; // Skip

        IValueObject data = (IValueObject)event;
        String entityName = data.getClass().getSimpleName();
        if (entityName.lastIndexOf("VO") == entityName.length() - 2) {
            entityName = entityName.substring(0, entityName.length() - 2);
        }


        File directory = new File(trashDirectory, entityName);
        FileUtils.forceMkdir(directory);

        String filename = String.format("%s#%s.json",
                entityName.toLowerCase(),
                data.getId());


        FileWriter writer = new FileWriter(new File(directory, filename));
        try {
            objectMapper.writeValue(writer, data);
            writer.flush();
        } finally {
            writer.close();
        }
    }
}
