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

package net.sumaris.core.model.technical.extraction;

public interface IExtractionFormat {

    /**
     * If label was derived from another format, return the raw (original) format.
     * Example: "rdb-001" will return "RDB"
     *
     * @param label
     * @return
     */
    static String getRawFormatLabel(String label) {
        if (label == null) return null;
        int lastSeparatorIndex = label.lastIndexOf("-");
        if (lastSeparatorIndex == -1) return label;
        return label.substring(0, lastSeparatorIndex);
    }

    String getLabel();
    String getVersion();
    ExtractionCategoryEnum getCategory();


    String[] getSheetNames();

    default String getRawFormatLabel() {
        return getRawFormatLabel(getLabel());
    }
}
