package net.sumaris.core.config;

/*-
 * #%L
 * Quadrige3 Core :: Shared
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfiguration {

    // default task executor
    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    @ConditionalOnMissingBean(name = {"applicationTaskExecutor", "taskExecutor"})
    @Lazy
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
        return builder
                .threadNamePrefix("task-")
                .build();
    }

    // task executor for job
    @Bean(name = "jobTaskExecutor")
    @Lazy
    public ThreadPoolTaskExecutor jobTaskExecutor(TaskExecutorBuilder builder) {
        return builder
            .threadNamePrefix("job-")
            .corePoolSize(1)
            .maxPoolSize(10)
            .queueCapacity(100)
            .build();
    }

    /*@Bean(name = "applicationEventMulticaster")
    @Lazy
    public ApplicationEventMulticaster simpleApplicationEventMulticaster(TaskExecutor taskExecutor) {
        SimpleApplicationEventMulticaster eventMulticaster =
                new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(taskExecutor);
        return eventMulticaster;
    }*/

}
