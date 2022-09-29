package net.sumaris.core.service.technical;

/*-
 * #%L
 * Quadrige3 Core :: Server API
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

import fr.ifremer.quadrige3.core.model.option.NoFetchOptions;
import fr.ifremer.quadrige3.core.model.option.SaveOptions;
import fr.ifremer.quadrige3.core.model.system.Job;
import fr.ifremer.quadrige3.core.service.AbstractEntityService;
import fr.ifremer.quadrige3.core.vo.system.JobFilterVO;
import fr.ifremer.quadrige3.core.vo.system.JobVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.apache.activemq.broker.scheduler.Job;

import java.util.Optional;

public interface JobService {

    Optional<JobVO> find(int id);
}
