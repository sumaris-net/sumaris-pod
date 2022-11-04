package net.sumaris.core.model.technical.job;

/*-
 * #%L
 * Quadrige3 Core :: Model Shared
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import net.sumaris.core.model.referential.ProcessingStatusEnum;

import java.io.Serializable;
import java.util.Arrays;

public enum JobStatusEnum implements Serializable {

    PENDING("PENDING", ProcessingStatusEnum.WAITING_EXECUTION),
    SUCCESS("SUCCESS", ProcessingStatusEnum.SUCCESS),
    ERROR("ERROR", ProcessingStatusEnum.ERROR),
    WARNING("WARNING", ProcessingStatusEnum.WARNING),

    RUNNING("RUNNING", ProcessingStatusEnum.WAITING_EXECUTION), // TODO: add CANCELLED in ProcessingStatusEnum ?
    FATAL("FATAL", ProcessingStatusEnum.ERROR), // TODO: add FATAL in ProcessingStatusEnum ?
    CANCELLED("CANCELLED", ProcessingStatusEnum.ERROR) // TODO: add CANCELLED in ProcessingStatusEnum ?
    ;

    private final String id;

    private final ProcessingStatusEnum processingStatus;

    JobStatusEnum(String id, ProcessingStatusEnum processingStatus) {
        this.id = id;
        this.processingStatus = processingStatus;
    }

    public String getId() {
        return id;
    }

    public ProcessingStatusEnum getProcessingStatus() {
        return processingStatus;
    }

    public static JobStatusEnum byLabel(final String label) {
        return Arrays.stream(values()).filter(enumValue -> enumValue.getId().equals(label)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown JobStatusEnum: " + label));
    }

    public static JobStatusEnum byProcessingStatus(final ProcessingStatusEnum processingStatus) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.processingStatus == processingStatus)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ProcessingStatusEnum: " + processingStatus.name()));
    }
}
