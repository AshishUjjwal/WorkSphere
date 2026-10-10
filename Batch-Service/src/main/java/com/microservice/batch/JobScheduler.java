package com.microservice.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling // Tells Spring to start a background clock
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job employeeJob;

    public JobScheduler(JobLauncher jobLauncher, Job employeeJob) {
        this.jobLauncher = jobLauncher;
        this.employeeJob = employeeJob;
    }

    // REAL WORLD: "0 0 3 * * *" -> Runs exactly at 3:00 AM every night.
    // FOR TESTING: "*/10 * * * * *" -> Runs exactly every 10 seconds.
    
    @Scheduled(cron = "*/10 * * * * *")
    public void runJobAtThreeAM() throws Exception {
        System.out.println("⏰ SCHEDULER WOKE UP! Launching the Batch Job...");
        
        // Spring Batch requires unique parameters to run the same job multiple times
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
                
        // Launch the job defined in BatchConfig.java!
        jobLauncher.run(employeeJob, params); 
    }
}
