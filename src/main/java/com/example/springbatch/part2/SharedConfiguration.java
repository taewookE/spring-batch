package com.example.springbatch.part2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SharedConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    /*TODO: get뒤에 오는것이 jobName이 되는것 같다. start()와 next()를 이용해서 2개의 Step을 엮어줄수 있다.
    *       Step은 하나의 Step에서만 데이터를 공유하고 Job은 포함된 Step의 데이터를 모두 공유한다.
    *
    *
    * */
    @Bean
    public Job shareJob(){
        return jobBuilderFactory.get("shareJob")
                .incrementer(new RunIdIncrementer())
                .start(this.shareStep())
                .next(this.shareStep2())
                .build();
    }

    private Step shareStep2() {
        return stepBuilderFactory.get("shareStep2")
                .tasklet((contribution,chunkContext)->{
                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

                    JobExecution jobExecution = stepExecution.getJobExecution();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

                    log.info("jobkey : {} , stepKey: {}",
                            jobExecutionContext.getString("jobKey","emptyJobKey"),
                            stepExecutionContext.getString("stepKey", "emptyStepKey")
                            );

                    return RepeatStatus.FINISHED;
                }).build();
    }

    /*TODO : contribution > stepExecution > stepExecutionContext
    *        stepExecution > JobExecution > JobInstance > JobExecutionContext
    *        각 테이블과의 관계에 대해서 정리를 잘 해서 이해하는 단계가 필요.
    *  */
    @Bean
    public Step shareStep(){
        return stepBuilderFactory.get("shareStep")
                .tasklet((contribution,chunkContext)-> {
                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();
                    stepExecutionContext.putString("stepKey","step execution context");

                    JobExecution jobExecution = stepExecution.getJobExecution();
                    JobInstance jobInstance = jobExecution.getJobInstance();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();
                    jobExecutionContext.putString("jobKey","job execution context");
                    JobParameters jobParameters = jobExecution.getJobParameters();

                    log.info("jobName: {} , stepName : {} , parameter : {}",
                            jobInstance.getJobName(),
                            stepExecution.getStepName(),
                            jobParameters.getLong("run.id"));

                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
