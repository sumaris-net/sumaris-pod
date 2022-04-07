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

package net.sumaris.server.util.social;

import lombok.NonNull;
import net.sumaris.core.model.social.EventTypeEnum;

import java.util.Arrays;

public enum MessageTypeEnum {
    EMAIL(EventTypeEnum.EMAIL),
    INBOX_MESSAGE(EventTypeEnum.INBOX_MESSAGE),
    FEED(EventTypeEnum.FEED),
    DEBUG_DATA(EventTypeEnum.DEBUG_DATA),
    ;


    public static MessageTypeEnum nullToDefault(MessageTypeEnum type, MessageTypeEnum defaultType) {
        return type != null ? type : defaultType;
    }

    public static MessageTypeEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
            .filter(item -> label.equals(item.label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown MessageTypeEnum: " + label));
    }

    private String label;
    private EventTypeEnum eventType;

    MessageTypeEnum(EventTypeEnum eventType) {
        this(eventType.getLabel());
        this.eventType = eventType;
    }

    MessageTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public EventTypeEnum toEventType() {
        return eventType;
    }
}
