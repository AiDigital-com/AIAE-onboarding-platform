package com.aidigital.aionboarding.service.lesson.models;

import java.util.List;

public record ReviseLessonInput(
		String revisionRequest,
		List<String> selectedOptions
) {

}
