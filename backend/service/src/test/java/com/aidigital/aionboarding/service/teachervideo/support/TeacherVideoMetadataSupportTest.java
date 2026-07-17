package com.aidigital.aionboarding.service.teachervideo.support;

import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.service.common.mapping.TextValueNormalizer;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherVideoMetadataSupportTest {

	private final TeacherVideoMetadataSupport support =
			new TeacherVideoMetadataSupport(new TextValueNormalizer());

	@Test
	void mutableMetadataShouldCopyExistingMetadataTest() {
		Lesson lesson = new Lesson();
		lesson.setGenerationMetadata(Map.of("existing", "value"));

		Map<String, Object> metadata = support.mutableMetadata(lesson);
		metadata.put("next", "value");

		assertThat(metadata).containsEntry("existing", "value").containsEntry("next", "value");
		assertThat(lesson.getGenerationMetadata()).doesNotContainKey("next");
	}

	@Test
	void mutableMetadataShouldReturnEmptyMutableMapWhenMissingTest() {
		Lesson lesson = new Lesson();

		Map<String, Object> metadata = support.mutableMetadata(lesson);
		metadata.put("teacherVideo", Map.of());

		assertThat(metadata).containsKey("teacherVideo");
	}

	@Test
	void isActiveStatusShouldRecognizePendingProcessingAndGeneratingTest() {
		assertThat(support.isActiveStatus("pending")).isTrue();
		assertThat(support.isActiveStatus("processing")).isTrue();
		assertThat(support.isActiveStatus("generating")).isTrue();
		assertThat(support.isActiveStatus("completed")).isFalse();
		assertThat(support.isActiveStatus(null)).isFalse();
	}

	@Test
	void hasActiveTeacherVideoShouldUseRecordStatusTest() {
		TeacherVideoRecord teacherVideo = new TeacherVideoRecord(
				"heygen", "", "", "", "", "video-1", "processing",
				"", 60, "", "", "", null, null, null
		);

		assertThat(support.hasActiveTeacherVideo(teacherVideo)).isTrue();
		assertThat(support.hasActiveTeacherVideo(null)).isFalse();
	}
}
