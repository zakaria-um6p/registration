package io.mosip.registration.processor.training;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.training.stage.TrainingStage;

public class TrainingStageApplication {

	 public static void main(String[] args){
	        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
	        ctx.scan(
	                "io.mosip.registration.processor.rest.client.config",
	                "io.mosip.registration.processor.core.config",
	                "io.mosip.registration.processor.training.config",
	                "io.mosip.registration.processor.status.config",
	                "io.mosip.registration.processor.packet.storage.config",
	                "io.mosip.registration.processor.core.kernel.beans");
	        ctx.refresh();
	        
	        TrainingStage trainingStage = ctx.getBean(TrainingStage.class);
	        trainingStage.deployVerticle();
	      
	    }
}
