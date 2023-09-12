package cloud.projects.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassifierResult {

	private String key;
	private String result;

	public String toString() {
		return String.format("(%s, %s)", key, result);
	}

}
