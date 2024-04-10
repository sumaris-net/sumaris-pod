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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.job.JobEndEvent;
import net.sumaris.core.event.job.JobProgressionEvent;
import net.sumaris.core.event.job.JobProgressionVO;
import net.sumaris.core.event.job.JobStartEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.jms.JmsConfiguration;
import net.sumaris.core.jms.JmsJobEventProducer;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.referential.ProcessingTypeEnum;
import net.sumaris.core.model.social.EventLevelEnum;
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.model.social.SystemRecipientEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.util.Assert;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.social.UserEventVO;
import net.sumaris.core.vo.technical.job.IJobResultVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.server.security.ISecurityContext;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.nuiton.i18n.I18n.t;

@Component
@ConditionalOnProperty(name = "sumaris.job.service.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class JobExecutionServiceImpl implements JobExecutionService {

    private final ObjectMapper objectMapper;
    private final JobService jobService;
    private final ISecurityContext<PersonVO> securityContext;
    private final UserEventService userEventService;

    private final ApplicationEventPublisher publisher;
    private final Map<Integer, List<Consumer<JobProgressionVO>>> jobProgressionListeners = new ConcurrentHashMap<>();
    private final Map<Integer, Future<?>> runningJobsById = new ConcurrentHashMap<>();

    private final TaskExecutor taskExecutor;

    public JobExecutionServiceImpl(JobService jobService,
                                   UserEventService userEventService,
                                   ObjectMapper objectMapper,
                                   Optional<TaskExecutor> taskExecutor,
                                   Optional<ISecurityContext<PersonVO>> securityContext,
                                   ApplicationEventPublisher publisher) {
        this.jobService = jobService;
        this.userEventService = userEventService;
        this.securityContext = securityContext.orElse(null);
        this.publisher = publisher;
        this.taskExecutor = taskExecutor.orElse(null);

        // Use a cloned object mapper, to skip attributes with null value
        this.objectMapper = objectMapper.copy();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @PostConstruct
    protected void init() {
        log.info("Job Execution Service started");
    }

    @Override
    public <R> JobVO run(JobVO job, Function<IProgressionModel, Future<R>> callableFuture) {
        return run(job, null, callableFuture);
    }

    @Override
    public <R> JobVO run(JobVO job,
                         Callable<Object> configurationLoader,
                         Function<IProgressionModel, Future<R>> callableFuture) {
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

        // Load job configuration
        if (configurationLoader != null) {
            try {
                Object configuration = configurationLoader.call();
                // Store configuration into the job (as json)
                if (configuration != null) {
                    job.setConfiguration(writeValueAsString(configuration));
                }
            } catch (Exception e) {
                throw new SumarisTechnicalException(e);
            }
        }

        // Save job
        JobVO finalJob = jobService.save(job);

        // Notify user
        sendUserEvent(EventLevelEnum.INFO, finalJob);

        // Start async
        if (this.taskExecutor != null) {
            this.taskExecutor.execute(() -> start(finalJob, callableFuture));
        }
        else {
            start(finalJob, callableFuture);
        }

        return job;
    }

    public <R> Future<R> start(JobVO job,
                               Function<IProgressionModel,
                               Future<R>> callableFuture) {
        final int jobId = job.getId();

        // Publish job start event
        job.setStatus(JobStatusEnum.RUNNING);
        publisher.publishEvent(new JobStartEvent(job.getId(), job));

        // Create progression model and listener to throttle events
        ProgressionModel progressionModel = new ProgressionModel();

        // Create listeners to throttle events
        io.reactivex.rxjava3.core.Observable<JobProgressionVO> progressionObservable = Observable.create(emitter -> {

            // Create listener on bean property and emit the value
            PropertyChangeListener listener = evt -> {
                ProgressionModel progression = (ProgressionModel) evt.getSource();
                JobProgressionVO jobProgression = JobProgressionVO.fromModelBuilder(progression)
                    .id(jobId)
                    .name(job.getName())
                    .build();
                emitter.onNext(jobProgression);

                if (progression.isCompleted()) {
                    // complete observable
                    emitter.onComplete();
                }
            };

            // Add listener on current progression and message
            progressionModel.addPropertyChangeListener(ProgressionModel.Fields.CURRENT, listener);
            progressionModel.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, listener);
        });

        Disposable progressionSubscription = progressionObservable
            // throttle for 500ms to filter unnecessary flow
            .throttleLatest(500, TimeUnit.MILLISECONDS, true)
            // Publish job progression event
            .subscribe(jobProgressionVO -> publisher.publishEvent(new JobProgressionEvent(jobId, jobProgressionVO)));

        // Execute import
        try {
            R result = null;

            try {

                // Start job
                Future<R> resultFuture = callableFuture.apply(progressionModel);

                // Store as running job, to be able to cancelled it
                this.runningJobsById.put(jobId, resultFuture);

                // Wait job result
                result = resultFuture.get();

                // Extract the job status, from result
                if (result instanceof IJobResultVO) {
                    job.setStatus(((IJobResultVO)result).getStatus());
                }
                else {
                    log.warn("Cannot read {} job's status (job's result not implements {}). Will use SUCCESS", job.getType(), IJobResultVO.class.getSimpleName());
                    job.setStatus(JobStatusEnum.SUCCESS);
                }

            } catch (Exception e) {
                boolean cancelled = e instanceof CancellationException || e instanceof InterruptedException;
                // Set status (failed or cancelled)
                job.setStatus(cancelled ? JobStatusEnum.CANCELLED : JobStatusEnum.FATAL);
                // Add error to log
                if (!cancelled) job.appendToLog(e.getMessage());
            }

            // Store result as report
            if (result != null) {
                // Serialize result in job report (as json)
                job.setReport(writeValueAsString(result));
            }

            return new AsyncResult(result);

        } finally {
            // Publish job end event
            publisher.publishEvent(new JobEndEvent(jobId, job));
            Observables.dispose(progressionSubscription);
            runningJobsById.remove(jobId);
        }
    }

    @Scheduled(cron = "${sumaris.job.service.clean.hourly.cron:0 0 * * * ?}")
    public void cleanJobs() {

        // Get pending or running jobs started 24 hours ago
        Timestamp startedBefore = new Timestamp(Dates.addDays(new Date(), -1).getTime());
        String[] knownProcessingTypes = Arrays.stream(ProcessingTypeEnum.values())
            .filter(type -> type.getId() >= 0 && !ProcessingTypeEnum.UNKNOWN.equals(type))
            .map(ProcessingTypeEnum::getLabel)
            .toArray(String[]::new);

        List<JobVO> oldPendingJobs = jobService.findAll(
            JobFilterVO.builder()
                .status(new JobStatusEnum[]{JobStatusEnum.PENDING, JobStatusEnum.RUNNING})
                .types(knownProcessingTypes)
                .startedBefore(startedBefore)
                .build()
        );
        oldPendingJobs.forEach(job -> {
            if (runningJobsById.containsKey(job.getId())) {
                // Cancel by Future
                runningJobsById.get(job.getId()).cancel(job.getStatus().equals(JobStatusEnum.RUNNING));
            }
            else {
                // Make sure this kind of job is callable by the pod
                ProcessingTypeEnum processingType = ProcessingTypeEnum.byLabel(job.getType())
                    .orElse(ProcessingTypeEnum.UNKNOWN);
                if (!ProcessingTypeEnum.UNKNOWN.equals(processingType)) {

                    String cancelMessage = I18n.t("sumaris.job.cancel.message", SystemRecipientEnum.SYSTEM);
                    log.info("Job#{} - {}", job.getId(), cancelMessage);

                    // Just update the job end date, in the history table
                    job.setEndDate(new Date());
                    job.setStatus(JobStatusEnum.CANCELLED);
                    job.setLog(cancelMessage);
                    jobService.save(job);
                }
            }

        });

    }

    public Observable<JobProgressionVO> watchJobProgression(int id) {

        JobVO job = jobService.findById(id).orElse(null);
        if (job == null) {
            log.debug("Job not found (id={})", id);
            return Observable.empty();
        }
        if (job.getStatus() != null && JobStatusEnum.isFinished(job.getStatus())) {
            log.debug("Job is finished (id={})", id);
            return Observable.empty();
        }

        Observable<JobProgressionVO> observable = Observable.create(emitter -> {
            Consumer<JobProgressionVO> listener = emitter::onNext;
            registerProgressionListener(id, listener);
            emitter.setCancellable(() -> unregisterProgressionListener(id, listener));
        });

        Observable<JobProgressionVO> result = observable
            .observeOn(Schedulers.io());

        // Add an initiale progression, only if pending
        if (job.getStatus() == JobStatusEnum.PENDING) {
            JobProgressionVO firstProgression = new JobProgressionVO(id, job.getName(), t("sumaris.core.job.progression.pending"), 0, 0);
            result = result.startWithItem(firstProgression);
        }

        return result
            // Add a destroy timer
            .takeUntil(Observable.interval(
                5 /*wait 10s before to start timer*/,
                1, TimeUnit.SECONDS // Check every 1s
                )
                // Emit (=will destroy the observable) if not more listeners
                .filter(o -> !hasProgressionListeners(id))
            )
            .doOnLifecycle(
                (subscription) -> log.debug("Watching job progression (id={})", id),
                () -> log.debug("Stop watching job progression (id={})", id)
            );

    }

    @Override
    public JobVO cancel(@NonNull JobVO job, @NonNull String message) {
        Preconditions.checkNotNull(job.getId());

        // Already finished: skip
        if (JobStatusEnum.isFinished(job.getStatus())) {
            return job;
        }

        log.debug("Cancelling job #{}... ({})", job.getId(), message);

        if (runningJobsById.containsKey(job.getId())) {
            // Cancel by Future
            Future<?> future = runningJobsById.remove(job.getId());
            boolean mayInterruptIfRunning = job.getStatus().equals(JobStatusEnum.RUNNING);
            future.cancel(mayInterruptIfRunning);

            // Just update the job
            job.setStatus(JobStatusEnum.CANCELLED);

        } else {
            // Just update the job
            job.setStatus(JobStatusEnum.CANCELLED);
            job.appendToLog(message);
            // Publish job end event
            publisher.publishEvent(new JobEndEvent(job.getId(), job));
        }


        return job;
    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'start'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobStartEvent(JobVO source, Message message) {

        if (source == null) {
            log.warn("The JobStartEvent send empty payload. Message: {}", message);
            return;
        }
        log.debug("Receiving job start event for job {}", source);

        Integer jobId = source.getId();

        // Get the job entity
        JobVO target = Optional.ofNullable(jobId).flatMap(jobService::findById).orElse(null);
        if (target == null) {
            log.warn("This job doesn't exists: {}", source);
            return;
        }

        // Update start date
        target.setStartDate(new Date());
        target.setStatus(JobStatusEnum.RUNNING);
        // set Context
        target.setConfiguration(source.getConfiguration());

        // Save
        jobService.save(target);

        // Notify user
        sendUserEvent(EventLevelEnum.INFO, target);
    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'end'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobEndEvent(JobVO source, Message message) {

        if (source == null) {
            log.warn("The JobEndEvent send empty payload. Message: {}", message);
            return;
        }
        log.debug("Receiving job end event for job {}", source);

        Integer jobId = source.getId();
        JobVO target = Optional.ofNullable(jobId).flatMap(jobService::findById).orElse(null);
        if (target == null) {
            log.warn("This job doesn't exists: {}", source);
            return;
        }

        // Set end date
        target.setEndDate(new Date());
        target.setStatus(source.getStatus());
        // Update context if changed
        target.setConfiguration(source.getConfiguration());
        // Set report
        target.setReport(source.getReport());
        // Set log
        if (StringUtils.isNotBlank(source.getLog())) {
            target.setLog(source.getLog());
        }

        // Unregister listeners
        unregisterProgressionListeners(jobId);

        // Save and Notify user
        jobService.save(target);

        EventLevelEnum eventLevel = switch (source.getStatus()) {
            case ERROR, FATAL -> EventLevelEnum.ERROR;
            case WARNING, CANCELLED -> EventLevelEnum.WARNING;
            default -> EventLevelEnum.INFO;
        };

        sendUserEvent(eventLevel, target);

    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'progress'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobProgressEvent(JobProgressionVO progression, Message message) {

        if (progression == null) {
            log.warn("The JobProgressionEvent send empty payload. Message: {}", message);
            return;
        }

        int jobId = progression.getId();
        if (log.isTraceEnabled()) {
            log.trace("Receiving job progression event for job {}", this.writeValueAsString(progression));
        }

        // Notify listeners
        List<Consumer<JobProgressionVO>> listeners = getProgressionListeners(jobId);
        if (CollectionUtils.isNotEmpty(listeners)) {
            log.trace("Consume job progression event for job id={} (listener count: {}}", jobId, listeners.size());
            listeners.forEach(listener -> listener.accept(progression));
        }
    }

    protected UserEventVO sendUserEvent(@NonNull EventLevelEnum level, @NonNull JobVO job) {
        Preconditions.checkNotNull(job.getId());
        Preconditions.checkNotNull(job.getIssuer());

        // Prepare content
        String content = writeValueAsString(job);

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

    private List<Consumer<JobProgressionVO>> getProgressionListeners(Integer jobId) {
        return jobProgressionListeners.get(jobId);
    }

    private boolean hasProgressionListeners(Integer jobId) {
        return jobProgressionListeners.containsKey(jobId);
    }

    private void registerProgressionListener(Integer jobId, Consumer<JobProgressionVO> listener) {
        jobProgressionListeners.computeIfAbsent(jobId, id -> new CopyOnWriteArrayList<>()).add(listener);
    }

    private void unregisterProgressionListener(Integer jobId, Consumer<JobProgressionVO> listener) {
        jobProgressionListeners.computeIfPresent(jobId, (id, listeners) -> {
            listeners.remove(listener);
            return listeners.isEmpty() ? null : listeners;
        });
    }

    private void unregisterProgressionListeners(Integer jobId) {
        if (jobProgressionListeners.containsKey(jobId)) {
            jobProgressionListeners.remove(jobId);
        }
    }

    public String writeValueAsString(Object object) {
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

    @Override
    public <T> T readConfiguration(@NonNull JobVO job, @NonNull Class<T> configurationClass) {
        if (StringUtils.isBlank(job.getConfiguration())) return null;
        String configuration = job.getConfiguration();
        if (configurationClass.isAssignableFrom(String.class)) {
            return (T)configuration;
        } else {
            try {
                return objectMapper.readValue(configuration, configurationClass);
            } catch (JsonProcessingException e) {
                log.error(String.format("Can't parse JSON: %s", configuration), e);
                return null;
            }
        }
    }
}
