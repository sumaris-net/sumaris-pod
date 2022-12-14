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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.job.JobProgressionVO;
import net.sumaris.core.jms.JmsConfiguration;
import net.sumaris.core.jms.JmsJobEventProducer;
import net.sumaris.core.model.social.EventLevelEnum;
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.model.social.SystemRecipientEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.util.Assert;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.social.UserEventVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.server.security.ISecurityContext;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

@ConditionalOnProperty(name = "sumaris.job.service.enabled", havingValue = "true", matchIfMissing = true)
@Component
@Slf4j
public class JobExecutionServiceImpl implements JobExecutionService {

    private final ObjectMapper objectMapper;
    private final JobService jobService;
    private final ISecurityContext<PersonVO> securityContext;
    private final UserEventService userEventService;

    private final Map<Integer, List<Consumer<JobProgressionVO>>> jobProgressionConsumerMap = new ConcurrentHashMap<>();
    private final Map<Integer, Future<?>> jobFutureMap = new ConcurrentHashMap<>();

    public JobExecutionServiceImpl(JobService jobService,
                                   UserEventService userEventService,
                                   ObjectMapper objectMapper,
                                   Optional<ISecurityContext<PersonVO>> securityContext) {
        this.jobService = jobService;
        this.userEventService = userEventService;
        this.objectMapper = objectMapper;
        this.securityContext = securityContext.orElse(null);
    }

    @PostConstruct
    protected void init() {
        log.info("Job Execution Service started");
    }

    @Override
    public JobVO run(JobVO job, Function<JobVO, Future<?>> asyncMethod) {
        Assert.notBlank(job.getName());
        Assert.notNull(job.getType());

        // Affect basic properties
        if (job.getIssuer() == null) {
            String issuer = securityContext.getAuthenticatedUser()
                .map(PersonVO::getPubkey)
                .orElse(SystemRecipientEnum.SYSTEM.getLabel());
            job.setIssuer(issuer);
        }
        job.setStatus(JobStatusEnum.PENDING);
        job.setStartDate(new Date());

        // Save job
        job = jobService.save(job);

        // Notify user
        sendUserEvent(EventLevelEnum.INFO, job);

        // Execute async method
        Future<?> future = asyncMethod.apply(job);
        jobFutureMap.put(job.getId(), future);

        return job;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void waitJobFuture() {
        synchronized (jobFutureMap) {
            if (!jobFutureMap.isEmpty()) {
                // Get finished task
                List<Map.Entry<Integer, Future<?>>> doneJobs = jobFutureMap.entrySet().stream()
                    .filter(entry -> entry.getValue().isDone() || entry.getValue().isCancelled())
                    .collect(Collectors.toList());
                // Mark jobs as finished
                for (Map.Entry<Integer, Future<?>> doneJob: doneJobs) {
                    // Get the future
                    Integer jobId = doneJob.getKey();
                    boolean cancelled = false;
                    Exception exception = null;
                    try {
                        doneJob.getValue().get();
                    } catch (Exception e) {
                        exception = e;
                        cancelled = e instanceof CancellationException || e instanceof InterruptedException;
                    }
                    // If an exception was thrown
                    if (exception != null) {
                        unregisterListeners(jobId);
                        // Update job
                        JobVO job = jobService.get(jobId);
                        job.setEndDate(new Date());
                        job.setStatus(cancelled ? JobStatusEnum.CANCELLED : JobStatusEnum.FATAL);
                        job.setLog(exception.toString());
                        log.error(t("sumaris.core.job.failed", job.getName()), exception);
                        jobService.save(job);
                        // Send notification
                        sendUserEvent(cancelled ? EventLevelEnum.WARNING : EventLevelEnum.ERROR, job);
                    }
                    // Remove from map
                    jobFutureMap.remove(jobId);
                }
            }
        }
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS, initialDelay = 1)
    @Transactional
    public void cleanJobs() {
        // Get pending or running jobs started 24 hours ago
        Timestamp startedBefore = new Timestamp(Dates.addDays(new Date(), -1).getTime());
        List<JobVO> oldPendingJobs = jobService.findAll(
            JobFilterVO.builder()
                .status(new JobStatusEnum[]{JobStatusEnum.PENDING, JobStatusEnum.RUNNING})
                .startedBefore(startedBefore)
                .build()
        );
        oldPendingJobs.forEach(job -> {
            if (jobFutureMap.containsKey(job.getId())) {
                // Cancel by Future
                jobFutureMap.get(job.getId()).cancel(job.getStatus().equals(JobStatusEnum.RUNNING));
            } else {
                // Just update the job
                job.setEndDate(new Date());
                job.setStatus(JobStatusEnum.CANCELLED);
                job.setLog("Cancelled by system");
                jobService.save(job);
            }

        });

    }

    public Observable<JobProgressionVO> watchJobProgression(Integer id) {

        JobVO job = jobService.findById(id).orElse(null);
        if (job == null) {
            log.debug("Job not found (id={})", id);
            return Observable.empty();
        }
        if (job.getStatus() != null && JobStatusEnum.isFinished(job.getStatus())) {
            log.debug("Job is finished (id={})", id);
            return Observable.empty();
        }

        JobProgressionVO startProgression = new JobProgressionVO(id, job.getName(), t("sumaris.core.job.progression.pending"), 0, 0);

        Observable<JobProgressionVO> observable = Observable.create(emitter -> {
            Consumer<JobProgressionVO> listener = emitter::onNext;
            registerListener(id, listener);
            emitter.setCancellable(() -> unregisterListener(id, listener));
        });

        return observable
            .observeOn(Schedulers.io())
            .startWithItem(startProgression)
            .takeUntil(Observable.interval(1, TimeUnit.SECONDS).filter(o -> !hasListeners(id))) // use a timer to watch listeners exists
            .doOnLifecycle(
                (subscription) -> log.debug("Watching job progression (id={})", id),
                () -> log.debug("Stop watching job progression (id={})", id)
            );

    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'start'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobStartEvent(JobVO job, Message message) {

        if (job == null) {
            log.warn("The JobStartEvent send empty payload. Message: {}", message);
            return;
        }
        log.debug("Receiving job start event for job {}", job);

        Integer jobId = job.getId();
        if (jobId == null || jobService.findById(jobId).isEmpty()) {
            // todo: Job not exists, create as external job ?
            log.warn("This job doesn't exists: {}", job);
            return;
        }

        // Get the job entity
        JobVO jobToUpdate = jobService.get(jobId);
        // Update start date
        jobToUpdate.setStartDate(new Date());
        jobToUpdate.setStatus(JobStatusEnum.RUNNING);
        // set Context
        jobToUpdate.setConfiguration(job.getConfiguration());

        // Save and Notify user
        jobService.save(jobToUpdate);
        sendUserEvent(EventLevelEnum.INFO, jobToUpdate);

    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'end'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobEndEvent(JobVO job, Message message) {

        if (job == null) {
            log.warn("The JobEndEvent send empty payload. Message: {}", message);
            return;
        }
        log.debug("Receiving job end event for job {}", job);

        Integer jobId = job.getId();
        if (jobId == null || jobService.findById(jobId).isEmpty()) {
            log.warn("This job doesn't exists: {}", job);
            return;
        }

        JobVO target = jobService.get(jobId);
        // Set end date
        target.setEndDate(new Date());
        target.setStatus(job.getStatus());
        // Update context if changed
        target.setConfiguration(job.getConfiguration());
        // Set report
        target.setReport(job.getReport());

        // Unregister listeners
        unregisterListeners(jobId);

        // Save and Notify user
        jobService.save(target);

        EventLevelEnum eventLevel;
        switch (job.getStatus()) {

            case ERROR:
            case FATAL:
                eventLevel = EventLevelEnum.ERROR;
                break;
            case WARNING:
            case CANCELLED:
                eventLevel = EventLevelEnum.WARNING;
                break;
            default:
                eventLevel = EventLevelEnum.INFO;
                break;
        }

        sendUserEvent(eventLevel, target);

    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'progress'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobProgressEvent(JobProgressionVO progression, Message message) {

        if (progression == null) {
            log.warn("The JobProgressionEvent send empty payload. Message: {}", message);
            return;
        }

        int jobId = progression.getId();
        log.debug("Receiving job progression event for job {}", progression);

        // Notify listeners
        List<Consumer<JobProgressionVO>> listeners = getListeners(jobId);
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.debug("Consume job progression event for job id={} (listener count: {}}", jobId, listeners.size());
            listeners.forEach(listener -> listener.accept(progression));
        }
    }

    protected UserEventVO sendUserEvent(@NonNull EventLevelEnum level, @NonNull JobVO job) {
        Preconditions.checkNotNull(job.getId());
        Preconditions.checkNotNull(job.getIssuer());

        // Prepare content
        String content = toJsonString(job);

        // Build events
        UserEventVO userEvent = UserEventVO.builder()
                .source(job.asSource()) // Link to job
                .issuer(SystemRecipientEnum.SYSTEM.getLabel())
                .recipient(job.getIssuer())
                .level(level)
                .type(EventTypeEnum.JOB)
                .content(content)
                .build();

        if (log.isDebugEnabled()) {
            log.debug("Will notify user {} for job changes: {}", job.getIssuer(), job);
        }

        // Save
        return userEventService.save(userEvent);
    }

    private List<Consumer<JobProgressionVO>> getListeners(Integer jobId) {
        return jobProgressionConsumerMap.get(jobId);
    }

    private boolean hasListeners(Integer jobId) {
        return jobProgressionConsumerMap.containsKey(jobId);
    }

    private void registerListener(Integer jobId, Consumer<JobProgressionVO> listener) {
        jobProgressionConsumerMap.computeIfAbsent(jobId, id -> new CopyOnWriteArrayList<>()).add(listener);
    }

    private void unregisterListener(Integer jobId, Consumer<JobProgressionVO> listener) {
        jobProgressionConsumerMap.computeIfPresent(jobId, (id, listeners) -> {
            listeners.remove(listener);
            return listeners.isEmpty() ? null : listeners;
        });
    }

    private void unregisterListeners(Integer jobId) {
        if (hasListeners(jobId)) {
            jobProgressionConsumerMap.remove(jobId);
        }
    }

    private String toJsonString(Object object) {
        String content = null;
        if (object != null) {
            if (object instanceof String) {
                content = (String) object;
            } else {
                try {
                    content = objectMapper.writeValueAsString(object);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Can't serialize content: %s", object), e);
                }
            }
        }
        return content;
    }
}
