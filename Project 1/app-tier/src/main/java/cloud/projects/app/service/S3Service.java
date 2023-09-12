package cloud.projects.app.service;

import java.io.File;
import java.io.FileOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cloud.projects.app.dto.ClassifierResult;
import lombok.val;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Log4j2
@Service
public class S3Service {

	@Value("${aws.access-key}")
	private String accessKey;

	@Value("${aws.secret-key}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@Value("${aws.s3.input-bucket}")
	private String inputBucket;

	@Value("${aws.s3.output-bucket}")
	private String outputBucket;

	@Value("${app.download-dir}")
	private String downloadDir;

	private S3Client s3;

	@PostConstruct
	public void setup() {
		s3 = S3Client.builder().region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.build();
		log.info("Download directory: {}", downloadDir);
	}

	@PreDestroy
	public void cleanup() {
		s3.close();
	}

	public File downloadImage(String key) {
		val request = GetObjectRequest.builder().bucket(inputBucket).key(key).build();
		val file = new File(String.format("%s%s%s.JPEG", downloadDir, File.separator, key));
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			try (val stream = new FileOutputStream(file.getAbsolutePath())) {
				stream.write(s3.getObject(request).readAllBytes());
			}
			return file;
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	public void uploadResults(ClassifierResult result) {
		val request = PutObjectRequest.builder().bucket(outputBucket).key(result.getKey()).build();
		s3.putObject(request, RequestBody.fromString(result.getResult()));
	}

}
