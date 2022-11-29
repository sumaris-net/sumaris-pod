package net.sumaris.core.service.technical;

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

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationEventListener;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;

@Transactional(readOnly = true)
public interface ConfigurationService {

    SumarisConfiguration getConfiguration();

    SoftwareVO getCurrentSoftware();

    boolean isReady();

    @Transactional(readOnly = false, noRollbackFor = PersistenceException.class)
    void applySoftwareProperties();

    void addListener(ConfigurationEventListener listener);

    void removeListener(ConfigurationEventListener listener);


}


