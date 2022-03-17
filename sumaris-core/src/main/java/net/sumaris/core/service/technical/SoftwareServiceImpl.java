package net.sumaris.core.service.technical;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.SoftwareDao;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.model.technical.configuration.Software;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;


@Component("softwareService")
@Slf4j
public class SoftwareServiceImpl implements SoftwareService {

    @Autowired
    private SoftwareDao dao;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public SoftwareVO get(int id) {
        return dao.get(id);
    }

    @Override
    public SoftwareVO getByLabel(String label) {
        Preconditions.checkNotNull(label);

        return dao.getByLabel(label);
    }

    @Override
    public SoftwareVO save(SoftwareVO source) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());

        boolean isNew = source.getId() == null;

        SoftwareVO target = dao.save(source);

        // Emit event
        if (isNew) {
            publisher.publishEvent(new EntityInsertEvent(target.getId(), Software.class.getSimpleName(), target));
        } else {
            publisher.publishEvent(new EntityUpdateEvent(target.getId(), Software.class.getSimpleName(), target));
        }

        return target;
    }


}
