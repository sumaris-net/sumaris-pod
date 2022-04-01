package net.sumaris.core.model.social;

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
import lombok.NonNull;

import java.io.Serializable;
import java.util.Arrays;

public enum InteractionTypeEnum implements Serializable {

    LIKE(1, "LIKE"),
    DISLIKE(2, "DISLIKE"),
    FOLLOW(3, "FOLLOW"),
    REPORT_ABUSE(4, "REPORT_ABUSE"),
    REPORT_ERROR(5, "REPORT_ERROR"),
    COMMENT(6, "COMMENT")
    ;

    public static InteractionTypeEnum valueOf(final int id) {
        return Arrays.stream(values())
                .filter(level -> level.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown InteractionTypeEnum: " + id));
    }

    public static InteractionTypeEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
                .filter(level -> label.equals(level.label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown InteractionTypeEnum: " + label));
    }

    private int id;
    private String label;

    InteractionTypeEnum(int id, String label) {
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
