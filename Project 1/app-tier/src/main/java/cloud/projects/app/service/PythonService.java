package cloud.projects.app.service;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cloud.projects.app.dto.ClassifierResult;
import lombok.val;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.utils.IoUtils;

@Log4j2
@Service
public class PythonService {

	@Value("${app.script}")
	private String script;

	@Value("${app.script-path}")
	private String scriptPath;

	@PostConstruct
	public void setup() {
		log.info("Script: {} {}", script, scriptPath);
	}

	public ClassifierResult executeScriptWithImage(File image) {

		final ProcessBuilder builder = new ProcessBuilder(script, scriptPath, image.getAbsolutePath());
		builder.redirectErrorStream(true);

		// running the script
		Process process = null;
		String result = "";
		int code = 0;
		try {

			process = builder.start();
			result = IoUtils.toUtf8String(process.getInputStream());
			code = process.waitFor();

		} catch (IOException | InterruptedException ex) {

			log.error(ex.getMessage(), ex);
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}

		}

		// Checking if script just fine
		if (code != 0) {
			log.error("Script ended with error code {}! result: {}", code, result);
			return null;
		}

		val split = result.trim().split(",");
		return new ClassifierResult(split[0].split("[.]")[0].trim(), split[1].trim());
	}

}
