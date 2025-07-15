package edu.pnu.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchScheduler {
	private final JobLauncher jobLauncher;
    private final Job analyzedTripBatchJob;

    @Scheduled(cron = "0 0 1 * * *")
    public void runAnalyzedTripBatchJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
        jobLauncher.run(analyzedTripBatchJob, params);
        System.out.println("[배치] analyzedTripBatchJob 실행됨 (1시)");
    }
}
