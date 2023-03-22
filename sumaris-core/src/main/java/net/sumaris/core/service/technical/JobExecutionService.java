package net.sumaris.core.service.technical;

/*-
 * #%L
 * Quadrige3 Core :: Server API
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
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


import io.reactivex.rxjava3.core.Observable;
import net.sumaris.core.event.job.JobProgressionVO;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.vo.technical.job.JobVO;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface JobExecutionService {
    <R> JobVO run(JobVO job,
                  Callable<Object> configurationLoader,
                  Function<IProgressionModel, Future<R>> asyncMethod);

    <R> JobVO run(JobVO job,
                  Function<IProgressionModel, Future<R>> callableFuture);

    Observable<JobProgressionVO> watchJobProgression(Integer id);

}
