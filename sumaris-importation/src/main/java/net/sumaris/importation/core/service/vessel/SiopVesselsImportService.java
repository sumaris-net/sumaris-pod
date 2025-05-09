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

package net.sumaris.importation.core.service.vessel;

import net.sumaris.core.model.IProgressionModel;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportContextVO;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportResultVO;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.concurrent.Future;


public interface SiopVesselsImportService {


    /**
     * Import SIOP vessels file into the database
     *
     * @param context the importation context
     * @param progressionModel a progression model
     * @return
     * @throws IOException
     */
    @Transactional
    SiopVesselImportResultVO importFromFile(SiopVesselImportContextVO context, @Nullable IProgressionModel progressionModel) throws IOException;


    @Async("jobTaskExecutor")
    Future<SiopVesselImportResultVO> asyncImportFromFile(SiopVesselImportContextVO context,
                                                         @Nullable IProgressionModel progressionModel);
}
