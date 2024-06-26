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

package net.sumaris.core.service.technical;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Slf4j
public class SoftwareServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private SoftwareService service;

    @Test
    public void save() {

        SoftwareVO software = new SoftwareVO();
        software.setLabel("test");
        software.setName("test");
        software.setStatusId(StatusEnum.ENABLE.getId());

        Map<String, String> props = Maps.newHashMap();
        props.put("property.1", "value1");
        props.put("property.2", "value2");
        software.setProperties(props);

        // Insert
        SoftwareVO savedSoftware = service.save(software);
        Assert.assertNotNull(savedSoftware);
        Assert.assertNotNull(savedSoftware.getId());
        Assert.assertNotNull(savedSoftware.getUpdateDate());
        Assert.assertNotNull(savedSoftware.getCreationDate());

        // Reload
        SoftwareVO reloadSoftware = service.get(software.getId());
        Assert.assertNotNull(savedSoftware);
        Assert.assertNotNull(savedSoftware.getProperties());
        Assert.assertEquals(savedSoftware.getId(), reloadSoftware.getId());
        Assert.assertEquals(props.size(), reloadSoftware.getProperties().size());

        // Update
        software.setName("new name");
        props = Maps.newHashMap();
        props.put("property.2", "value22");
        props.put("property.3", "value3");
        software.setProperties(props);
        savedSoftware = service.save(software);

        // Reload (by label)
        reloadSoftware = service.getByLabel(software.getLabel());
        Assert.assertNotNull(savedSoftware);
        Assert.assertNotNull(savedSoftware.getProperties());
        Assert.assertEquals(savedSoftware.getId(), reloadSoftware.getId());
        Assert.assertEquals(software.getName(), reloadSoftware.getName());
        Assert.assertEquals(props.size(), reloadSoftware.getProperties().size());
    }
}
