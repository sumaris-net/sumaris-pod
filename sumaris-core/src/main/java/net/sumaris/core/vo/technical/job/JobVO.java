package net.sumaris.core.vo.technical.job;

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

import fr.ifremer.quadrige3.core.model.enumeration.JobStatusEnum;
import fr.ifremer.quadrige3.core.model.enumeration.JobTypeEnum;
import fr.ifremer.quadrige3.core.vo.IValueObjectWithUpdateDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;
import java.util.Date;

@Data
@FieldNameConstants
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class JobVO implements IValueObjectWithUpdateDate<Integer>, Comparable<JobVO> {

    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;
    @ToString.Include
    private String name;
    @ToString.Include
    private JobTypeEnum type;
    @EqualsAndHashCode.Include
    private JobStatusEnum status;
    @EqualsAndHashCode.Include
    private Date startDate;
    @EqualsAndHashCode.Include
    private Date endDate;
    private String log;
    private String configuration;
    private String report;

    @ToString.Include
    private Integer userId;

    private Timestamp updateDate;

    @Override
    public int compareTo(JobVO o) {
        return this.hashCode() - o.hashCode();
    }
}
