package com.example.springbatch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChunkProcessingConfiguration {

    /*TODO: Job과 Step , tasklet의 연관관계 및 설정에 대해서 다시 정리 진행하기*/
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job chunkProcessingJob(){
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(this.taskBaseStep())
                .next(this.chunkBaseStep())
                .build();
    }

    private Step taskBaseStep() {
        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(this.tasklet())
                .build();
    }

    private Step chunkBaseStep(){
        return stepBuilderFactory.get("chunkBaseStep")
                .<String,String>chunk(10)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<String> itemWriter() {
        return items -> log.info("chunk item size :{}", items.size());
//        return items -> items.forEach(log::info);
    }

    /*TODO: Reader에서 읽은 데이터를 가공하여 writer로 넘겨주는 역할*/
    private ItemProcessor<String, String> itemProcessor() {
        return item -> item+ ", Spring-Batch";
    }

    private ItemReader<String> itemReader() {
        return new ListItemReader<>(getItems());
    }

    /*TODO: tasklet을 통해서 chunk구현.
    *       가능은하지만, 코드도 길어지고, 유용하지는 않다.
    * */
    private Tasklet tasklet() {
        List<String> items = getItems();

        return ((contribution, chunkContext) -> {
            StepExecution stepExecution = contribution.getStepExecution();
            int chunkSize = 10;
            int fromIndex = stepExecution.getReadCount();
            int toIndex = fromIndex+ chunkSize;

            if(fromIndex >= items.size()){
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(fromIndex,toIndex);

            log.info("task item size : {}",subList.size());
            stepExecution.setReadCount(toIndex);

            //fromIndex가 items size를 넘을떄까지 계속 진행.
            return RepeatStatus.CONTINUABLE;
        });
    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for(int i=0; i<100 ; i++){
            items.add("hello");
        }

        return items;
    }

}
