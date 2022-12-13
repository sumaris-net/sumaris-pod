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

package net.sumaris.server.config;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.service.ServiceLocator;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationStartupListener {

    private final String port;

    @Autowired
    public ApplicationStartupListener(@Value("${server.port}") String port) {
        this.port = port;
    }

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        ServiceLocator.init(event.getApplicationContext());
    }

    @EventListener
    public void onConfigurationReadyEvent(final ConfigurationReadyEvent event) {
        log.info(I18n.t("sumaris.server.started", this.port));
    }
}
