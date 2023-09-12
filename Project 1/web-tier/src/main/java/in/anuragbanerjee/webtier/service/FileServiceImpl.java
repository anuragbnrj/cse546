package in.anuragbanerjee.webtier.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.sqs.queueName}")
    private String queueName;

    @Value("${aws.sqs.requestQueueUrl}")
    private String requestQueueUrl;

    @Value("${aws.sqs.responseQueueUrl}")
    private String responseQueueUrl;

    private final S3Client s3Client;
    private final SqsClient sqsClient;
    private final ExecutorService executorService;

    public FileServiceImpl(S3Client s3Client, SqsClient sqsClient, ExecutorService executorService) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.executorService = executorService;
    }

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        String keyName = file.getOriginalFilename().split("[.]")[0];

        try {
//            logger.info("Uploading an Image (JPEG) to S3 - {}", keyName);
            byte[] attachment = file.getBytes();
            PutObjectResponse putObjectResult = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(keyName)
                            .contentType(MediaType.IMAGE_JPEG.toString())
                            .contentLength((long) attachment.length)
                            .build(),
                    RequestBody.fromByteBuffer(ByteBuffer.wrap(attachment)));


//            logger.info("putObjectResult = {}", putObjectResult);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(requestQueueUrl)
                    .messageBody(keyName)
                    .delaySeconds(5)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);

            return keyName;
        } catch (SdkServiceException ase) {
            logger.error("Caught an AmazonServiceException, which " + "means your request made it "
                    + "to Amazon S3, but was rejected with an error response" + " for some reason.", ase);
            logger.info("Error Message: {}", ase.getMessage());
            logger.info("Key: {}", keyName);
            throw ase;
        } catch (SdkClientException ace) {
            logger.error("Caught an AmazonClientException, which " + "means the client encountered "
                    + "an internal error while trying to " + "communicate with S3, "
                    + "such as not being able to access the network.", ace);
            logger.error("Error Message: {}, {}", keyName, ace.getMessage());
            throw ace;
        }

    }


    @PostConstruct
    public void startBackgroundThread() {
        log.info("===== Start of startBackgroundThread() =====");
        executorService.execute(() -> printFromResponseQueue());
        log.info("===== End of startBackgroundThread() =====");
    }

    private void printFromResponseQueue() {
        log.info("===== Start of printFromResponseQueue() =====");

        HashMap<QueueAttributeName, String> attributes = new HashMap<QueueAttributeName, String>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20");

        try {
            SetQueueAttributesRequest setAttrsRequest = SetQueueAttributesRequest.builder()
                    .queueUrl(responseQueueUrl)
                    .attributes(attributes)
                    .build();

            sqsClient.setQueueAttributes(setAttrsRequest);

            // Enable long polling on a message receipt.
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(responseQueueUrl)
                    .waitTimeSeconds(20)
                    .maxNumberOfMessages(1)
                    .build();

            while(true) {
                try {
                    ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveRequest);
                    if (!receiveMessageResponse.messages().isEmpty()) {
                        log.info("===== Classification Result: {} =====", receiveMessageResponse.messages().get(0).body());
                        receiveMessageResponse
                                .messages()
                                .forEach(
                                        m -> sqsClient.deleteMessage(
                                                DeleteMessageRequest
                                                        .builder()
                                                        .queueUrl(responseQueueUrl)
                                                        .receiptHandle(m.receiptHandle())
                                                        .build()
                                        )
                                );
                    }

                } catch (Exception ex) {
                    log.error("Exception occurred inside response poll loop");
                    log.error(ex.getMessage());
                }

            }
        } catch (SqsException e) {
            log.error(e.awsErrorDetails().errorMessage());
        }

        log.info("===== End of printFromResponseQueue() =====");
    }

}
