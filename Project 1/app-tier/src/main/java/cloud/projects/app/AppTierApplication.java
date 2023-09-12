package cloud.projects.app;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import cloud.projects.app.service.PythonService;
import cloud.projects.app.service.S3Service;
import cloud.projects.app.service.SqsService;
import lombok.val;
import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootApplication
public class AppTierApplication implements CommandLineRunner {

	@Autowired
	private SqsService sqsService;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private PythonService pythonService;

	public static void main(String... args) {
		SpringApplication.run(AppTierApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		while (true) {
			log.info("--------------------------------------------------------------------");

			// Loading next key from request queue
			val key = sqsService.getNextKeyFromSQS();
			if (Objects.isNull(key)) {
				log.warn("No pending request! Polling again...");
				continue;
			}
			log.info("Recevied key: {}", key);

			// Downloading the image from S3
			val image = s3Service.downloadImage(key);
			if (Objects.isNull(image)) {
				log.error("Unable to download image! Trying again...");
				continue;
			}
			log.info("Downloaded image: {}", image);

			// Executing classifier and getting the result
			val result = pythonService.executeScriptWithImage(image);
			if (Objects.isNull(result)) {
				log.error("Python script failed!");
				continue;
			}
			log.info("Result: {}", result);

			// Post results in S3 output bucket
			s3Service.uploadResults(result);
			log.info("Uploaded results to S3");

			// publish response to queue
			sqsService.publishResult(result);
			log.info("Response posted!");
		}
	}

}
