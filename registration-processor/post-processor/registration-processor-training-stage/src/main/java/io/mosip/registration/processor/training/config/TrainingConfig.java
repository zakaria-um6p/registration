package io.mosip.registration.processor.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.registration.processor.training.stage.TrainingStage;

@PropertySource("classpath:bootstrap.properties")
@Configuration
public class TrainingConfig {

    @Bean
    public TrainingStage getStage(){
        return new TrainingStage();
    }
}
