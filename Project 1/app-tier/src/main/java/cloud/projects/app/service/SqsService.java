package cloud.projects.app.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cloud.projects.app.dto.ClassifierResult;
import lombok.val;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

@Service
public class SqsService {

	private static final Map<QueueAttributeName, String> ATTRIBUTES;

	static {
		ATTRIBUTES = Map.of(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "10");
	}

	@Value("${aws.access-key}")
	private String accessKey;

	@Value("${aws.secret-key}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@Value("${aws.sqs.req-queue-url}")
	private String reqQueueUrl;

	@Value("${aws.sqs.res-queue-url}")
	private String resQueueUrl;

	private SqsClient sqs;
	private Map<String, ReceiveMessageResponse> cache;

	@PostConstruct
	public void setup() {
		sqs = SqsClient.builder().region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.build();
		sqs.setQueueAttributes(
				SetQueueAttributesRequest.builder().queueUrl(reqQueueUrl).attributes(ATTRIBUTES).build());
		cache = new ConcurrentHashMap<>();
	}

	@PreDestroy
	public void cleanup() {
		sqs.close();
		cache.clear();
	}

	public String getNextKeyFromSQS() {
		val req = ReceiveMessageRequest.builder().maxNumberOfMessages(1).queueUrl(reqQueueUrl).waitTimeSeconds(20)
				.build();
		val response = sqs.receiveMessage(req);
		if (!response.hasMessages()) {
			return null;
		}
		val key = response.messages().get(0).body();
		cache.put(key, response);
		return key;
	}

	public void publishResult(ClassifierResult result) {
		sqs.sendMessage(SendMessageRequest.builder().queueUrl(resQueueUrl).messageBody(result.toString()).build());
		deleteMessages(cache.get(result.getKey()));
		cache.remove(result.getKey());
	}

	private void deleteMessages(ReceiveMessageResponse response) {
		response.messages().forEach(m -> sqs.deleteMessage(
				DeleteMessageRequest.builder().queueUrl(reqQueueUrl).receiptHandle(m.receiptHandle()).build()));
	}
}
