package io.mosip.registration.processor.training.stage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Service
@RefreshScope
public class TrainingStage extends MosipVerticleAPIManager {

	private static Logger log = RegProcessorLogger.getLogger(TrainingStage.class);

	private MosipEventBus mosipEventBus;

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.training.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	@Autowired
	MosipRouter router;

	@Value("${server.port}")
	private String port;

	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;
	
	@Autowired
	Utilities utility;


	@Override
	public MessageDTO process(MessageDTO object) {
		String regId = object.getRid();
		log.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), regId, "TrainingStage::process()::entry");

		object.setMessageBusAddress(MessageBusAddress.TRAINING_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.FALSE);
		try {
//			InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(regId);
			
			JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
			String nameKey = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.NAME), MappingJsonConstants.VALUE);
			String dob = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.DOB), MappingJsonConstants.VALUE);
			String gender = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.GENDER), MappingJsonConstants.VALUE);
			String phone = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE), MappingJsonConstants.VALUE);
			String email = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL), MappingJsonConstants.VALUE);

			List<String> fields = new ArrayList<>();
			fields.addAll(Arrays.asList(nameKey.split(",")));
			fields.add(dob);
			fields.add(gender);
			fields.add(email);
			fields.add(phone);
			Map<String, String> fieldsMap = packetManagerService.getFields(regId, fields, object.getReg_type().name(), ProviderStageName.TRAINING);
		
			List<String> modalities = new ArrayList<String>();
			modalities.add(BiometricType.FACE.value());
			String individualBiometricsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);
			BiometricRecord biometricRecord = packetManagerService.getBiometrics(regId, individualBiometricsLabel, modalities, object.getReg_type().name(), ProviderStageName.TRAINING);
			ByteArrayInputStream inStreambj = new ByteArrayInputStream(biometricRecord.getSegments().get(0).getBdb());
			BufferedImage newImage = ImageIO.read(inStreambj);
			ImageIO.write(newImage, "jpg", new File("outputImage.jpg"));
			log.debug("Training stage success !!");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}

		return object;
	}

	@Override
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.TRAINING_BUS_IN, MessageBusAddress.TRAINING_BUS_OUT,
				messageExpiryTimeLimit);
	}

	@Override
	public void start() throws Exception {
		router.setRoute(
				this.postUrl(getVertx(), MessageBusAddress.TRAINING_BUS_IN, MessageBusAddress.TRAINING_BUS_OUT));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

}
