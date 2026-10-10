package com.microservice.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class BatchConfig {

    // 1. READER: Pretend this is reading 1 Million rows from a Database
    @Bean
    public ItemReader<String> reader() {
        List<String> rawData = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            rawData.add("employee_" + i + "@company.com");
        }
        System.out.println("📖 READER: Loaded data source.");
        return new ListItemReader<>(rawData);
    }

    // 2. PROCESSOR: Transform the data one-by-one (e.g., formatting email)
    @Bean
    public ItemProcessor<String, String> processor() {
        return item -> {
            String processed = item.toUpperCase();
            System.out.println("⚙️ PROCESSOR: Transformed " + item + " -> " + processed);
            return processed;
        };
    }

    // 3. WRITER: Write the processed data in CHUNKS
    @Bean
    public ItemWriter<String> writer() {
        return chunk -> {
            System.out.println("💾 WRITER: Writing a Chunk of " + chunk.size() + " items to Database/File...");
            for (String item : chunk) {
                System.out.println("   - Saved: " + item);
            }
        };
    }

    // 4. STEP: Combine Reader, Processor, and Writer into a Chunk-based Step
    @Bean
    public Step processEmployeesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processEmployeesStep", jobRepository)
                .<String, String>chunk(10, transactionManager) // Process exactly 10 items at a time!
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    // 5. JOB: The overarching Job that executes the Step(s)
    @Bean
    public Job employeeJob(JobRepository jobRepository, Step processEmployeesStep) {
        return new JobBuilder("employeeJob", jobRepository)
                .start(processEmployeesStep)
                .build();
    }
}
