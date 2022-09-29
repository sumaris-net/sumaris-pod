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


import net.sumaris.core.util.Dates;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.security.SecurityContext;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

@ConditionalOnProperty(name = "sumaris.job.service.enabled", havingValue = "true", matchIfMissing = true)
@Service
@Slf4j
@EnableAsync
@EnableScheduling
public class JobExecutionService {

    private final JobService jobService;
    private final SecurityContext securityContext;
    private final UserMessageService userMessageService;

    private final Map<Integer, List<Consumer<JobProgressionVO>>> jobProgressionConsumerMap = new ConcurrentHashMap<>();
    private final Map<Integer, Future<?>> jobFutureMap = new ConcurrentHashMap<>();

    public JobExecutionService(JobService jobService, SecurityContext securityContext, UserMessageService userMessageService) {
        this.jobService = jobService;
        this.securityContext = securityContext;
        this.userMessageService = userMessageService;
    }

    @PostConstruct
    protected void init() {
        log.info("Job Execution Service started");
    }

    public JobVO run(JobVO job, Function<JobVO, Future<?>> asyncMethod) {
        Assert.notBlank(job.getName());
        Assert.notNull(job.getType());

        // Affect basic properties
        if (job.getUserId() == null) {
            job.setUserId(securityContext.getUserId());
        }
        job.setStatus(JobStatusEnum.PENDING);
        job.setStartDate(new Date());

        // Save job
        jobService.save(job);

        // Notify user
        userMessageService.sendJobNotification(EventLevelEnum.INFO, t("sumaris.core.job.pending", job.getName()), job);

        // Execute async method
        Future<?> future = asyncMethod.apply(job);
        jobFutureMap.put(job.getId(), future);

        return job;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void waitJobFuture() {
        synchronized (jobFutureMap) {
            if (!jobFutureMap.isEmpty()) {
                List<Map.Entry<Integer, Future<?>>> doneJobs = jobFutureMap.entrySet().stream()
                    .filter(entry -> entry.getValue().isDone() || entry.getValue().isCancelled())
                    .collect(Collectors.toList());
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
                        job.setStatus(cancelled ? JobStatusEnum.CANCELLED : JobStatusEnum.FAILED);
                        job.setLog(exception.toString());
                        String message = t("sumaris.core.job.failed", job.getName());
                        log.error(message, exception);
                        jobService.save(job);
                        // Send notification
                        userMessageService.sendJobNotification(cancelled ? EventLevelEnum.WARNING : EventLevelEnum.ERROR, message, job);
                    }
                    // Remove from map
                    jobFutureMap.remove(jobId);
                }
            }
        }
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanJobs() {
        // Get pending or running jobs started 24 hours ago
        Timestamp startedBefore = new Timestamp(Dates.addDays(new Date(), -1).getTime());
        List<JobVO> oldPendingJobs = jobService.findAll(
            JobFilterVO.builder().status(List.of(JobStatusEnum.PENDING, JobStatusEnum.RUNNING)).startedBefore(startedBefore).build()
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
        Observable<JobProgressionVO> observable = Observable.create(emitter -> {
            Consumer<JobProgressionVO> listener = emitter::onNext;
            registerListener(id, listener);
            emitter.setCancellable(() -> unregisterListener(id, listener));
        });

        return observable
            .observeOn(Schedulers.io())
            .startWith(() -> {
                JobVO job = jobService.get(id);
                return List.of(new JobProgressionVO(id, job.getName(), t("sumaris.core.job.progression.pending"), 0, 0)).iterator();
            })
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
        if (jobId == null || jobService.find(jobId).isEmpty()) {
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
        userMessageService.sendJobNotification(EventLevelEnum.INFO, t("sumaris.core.job.run", job.getName()), jobToUpdate);

    }

    @JmsListener(destination = JmsJobEventProducer.DESTINATION, selector = "operation = 'end'", containerFactory = JmsConfiguration.CONTAINER_FACTORY)
    protected void onJobEndEvent(JobVO job, Message message) {

        if (job == null) {
            log.warn("The JobEndEvent send empty payload. Message: {}", message);
            return;
        }
        log.debug("Receiving job end event for job {}", job);

        Integer jobId = job.getId();
        if (jobId == null || jobService.find(jobId).isEmpty()) {
            log.warn("This job doesn't exists: {}", job);
            return;
        }

        JobVO jobToUpdate = jobService.get(jobId);
        // Set end date
        jobToUpdate.setEndDate(new Date());
        jobToUpdate.setStatus(job.getStatus());
        // Update context if changed
        jobToUpdate.setConfiguration(job.getConfiguration());
        // Set report
        jobToUpdate.setReport(job.getReport());

        // Unregister listeners
        unregisterListeners(jobId);

        // Save and Notify user
        jobService.save(jobToUpdate);

        EventLevelEnum eventLevel;
        switch (job.getStatus()) {

            case ERROR:
            case FAILED:
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

        userMessageService.sendJobNotification(eventLevel, t("sumaris.core.job.end", jobToUpdate.getName()), jobToUpdate);

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

}
