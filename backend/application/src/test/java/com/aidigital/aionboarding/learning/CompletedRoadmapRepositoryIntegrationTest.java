package com.aidigital.aionboarding.learning;

import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.CompletedRoadmapProjection;
import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.learning.repositories.UserRoadmapRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapLessonRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Criteria-API rewrite of
 * {@link UserRoadmapRepository#findCompletedRoadmapsForUserLesson} against real PostgreSQL: a
 * roadmap is returned only when it contains the changed lesson AND every lesson in the roadmap
 * now has a completed enrollment for the user — the double-negative ("no incomplete lesson
 * exists") that replaced the original native {@code LEFT JOIN ... WHERE x IS NULL} anti-join.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CompletedRoadmapRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Autowired
	private RoadmapRepository roadmapRepository;

	@Autowired
	private RoadmapLessonRepository roadmapLessonRepository;

	@Autowired
	private UserRoadmapRepository userRoadmapRepository;

	@Autowired
	private UserLessonRepository userLessonRepository;

	@Test
	void shouldReturnOnlyRoadmapsFullyCompletedAndContainingTheChangedLessonTest() {
		// Given: learner enrolled in three roadmaps
		//   roadmapFullyDone: L1, L2 — both completed
		//   roadmapStillOpen: L1, L3 — L3 not completed, so not fully done
		//   roadmapUnrelated: L4 only — doesn't contain L1 at all
		User learner = userRepository.save(user("Learner", "learner@test.com"));
		Lesson lessonOne = lessonRepository.save(lesson("L1"));
		Lesson lessonTwo = lessonRepository.save(lesson("L2"));
		Lesson lessonThree = lessonRepository.save(lesson("L3"));
		Lesson lessonFour = lessonRepository.save(lesson("L4"));

		Roadmap roadmapFullyDone = roadmapRepository.save(roadmap("Zeta Roadmap"));
		Roadmap roadmapStillOpen = roadmapRepository.save(roadmap("Alpha Roadmap"));
		Roadmap roadmapUnrelated = roadmapRepository.save(roadmap("Unrelated Roadmap"));

		roadmapLessonRepository.save(roadmapLesson(roadmapFullyDone, lessonOne, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmapFullyDone, lessonTwo, 1));
		roadmapLessonRepository.save(roadmapLesson(roadmapStillOpen, lessonOne, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmapStillOpen, lessonThree, 1));
		roadmapLessonRepository.save(roadmapLesson(roadmapUnrelated, lessonFour, 0));

		userRoadmapRepository.save(userRoadmap(learner, roadmapFullyDone));
		userRoadmapRepository.save(userRoadmap(learner, roadmapStillOpen));
		userRoadmapRepository.save(userRoadmap(learner, roadmapUnrelated));

		userLessonRepository.save(completedUserLesson(learner, lessonOne));
		userLessonRepository.save(completedUserLesson(learner, lessonTwo));
		userLessonRepository.save(incompleteUserLesson(learner, lessonThree));
		userLessonRepository.save(incompleteUserLesson(learner, lessonFour));

		// When: checking after completing L1, which belongs to both roadmapFullyDone (now fully
		// complete) and roadmapStillOpen (still missing L3)
		List<CompletedRoadmapProjection> result =
				userRoadmapRepository.findCompletedRoadmapsForUserLesson(learner.getId(), lessonOne.getId());

		// Then: only the fully-completed roadmap containing L1 is returned
		assertThat(result).extracting(CompletedRoadmapProjection::getId).containsExactly(roadmapFullyDone.getId());
		assertThat(result).extracting(CompletedRoadmapProjection::getTitle).containsExactly("Zeta Roadmap");
	}

	@Test
	void shouldReturnMultipleCompletedRoadmapsOrderedByTitleCaseInsensitiveTest() {
		// Given: two fully-completed roadmaps both containing the changed lesson
		User learner = userRepository.save(user("Learner2", "learner2@test.com"));
		Lesson sharedLesson = lessonRepository.save(lesson("Shared"));

		Roadmap roadmapZebra = roadmapRepository.save(roadmap("zebra roadmap"));
		Roadmap roadmapApple = roadmapRepository.save(roadmap("Apple Roadmap"));
		roadmapLessonRepository.save(roadmapLesson(roadmapZebra, sharedLesson, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmapApple, sharedLesson, 0));
		userRoadmapRepository.save(userRoadmap(learner, roadmapZebra));
		userRoadmapRepository.save(userRoadmap(learner, roadmapApple));
		userLessonRepository.save(completedUserLesson(learner, sharedLesson));

		// When:
		List<CompletedRoadmapProjection> result =
				userRoadmapRepository.findCompletedRoadmapsForUserLesson(learner.getId(), sharedLesson.getId());

		// Then: case-insensitive title order, "Apple" before "zebra"
		assertThat(result).extracting(CompletedRoadmapProjection::getTitle)
				.containsExactly("Apple Roadmap", "zebra roadmap");
	}

	@Test
	void shouldReturnEmptyWhenLessonNotInAnyEnrolledRoadmapTest() {
		// Given: learner enrolled in a roadmap that does not contain the changed lesson
		User learner = userRepository.save(user("Learner3", "learner3@test.com"));
		Lesson unrelatedLesson = lessonRepository.save(lesson("Unrelated"));
		Lesson standaloneLesson = lessonRepository.save(lesson("Standalone"));
		Roadmap roadmap = roadmapRepository.save(roadmap("Some Roadmap"));
		roadmapLessonRepository.save(roadmapLesson(roadmap, unrelatedLesson, 0));
		userRoadmapRepository.save(userRoadmap(learner, roadmap));
		userLessonRepository.save(completedUserLesson(learner, unrelatedLesson));
		userLessonRepository.save(completedUserLesson(learner, standaloneLesson));

		// When: checking a lesson that isn't part of any roadmap the learner is enrolled in
		List<CompletedRoadmapProjection> result =
				userRoadmapRepository.findCompletedRoadmapsForUserLesson(learner.getId(), standaloneLesson.getId());

		// Then:
		assertThat(result).isEmpty();
	}

	private User user(String name, String email) {
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Lesson lesson(String title) {
		LessonStatus status = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow();
		LessonPublicationStatus publicationStatus =
				lessonPublicationStatusRepository.findByCode(LessonPublicationStatusCode.PUBLISHED).orElseThrow();
		LessonContentFormat contentFormat =
				lessonContentFormatRepository.findByCode(LessonContentFormatCode.MARKDOWN).orElseThrow();
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

		Lesson lesson = new Lesson();
		lesson.setTitle(title);
		lesson.setDescription("description");
		lesson.setStatus(status);
		lesson.setUserInstructions("");
		lesson.setDepth("standard");
		lesson.setTone("clear");
		lesson.setDesiredFormat("structured theoretical lesson");
		lesson.setContentFormat(contentFormat);
		lesson.setContentMarkdown("content");
		lesson.setContentHtml("<p>content</p>");
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setGenerationMetadata(java.util.Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of());
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}

	private Roadmap roadmap(String title) {
		Roadmap roadmap = new Roadmap();
		roadmap.setTitle(title);
		roadmap.setDescription("description");
		roadmap.setTags(List.of());
		roadmap.setCreatedBy("tester");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		roadmap.setCreatedAt(now);
		roadmap.setUpdatedAt(now);
		return roadmap;
	}

	private RoadmapLesson roadmapLesson(Roadmap roadmap, Lesson lesson, int sortOrder) {
		RoadmapLesson roadmapLesson = new RoadmapLesson();
		RoadmapLesson.RoadmapLessonId id = new RoadmapLesson.RoadmapLessonId();
		id.setRoadmapId(roadmap.getId());
		id.setLessonId(lesson.getId());
		roadmapLesson.setId(id);
		roadmapLesson.setRoadmap(roadmap);
		roadmapLesson.setLesson(lesson);
		roadmapLesson.setSortOrder(sortOrder);
		roadmapLesson.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return roadmapLesson;
	}

	private UserRoadmap userRoadmap(User user, Roadmap roadmap) {
		UserRoadmap userRoadmap = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(user.getId());
		id.setRoadmapId(roadmap.getId());
		userRoadmap.setId(id);
		userRoadmap.setUser(user);
		userRoadmap.setRoadmap(roadmap);
		userRoadmap.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return userRoadmap;
	}

	private UserLesson completedUserLesson(User user, Lesson lesson) {
		UserLesson userLesson = incompleteUserLesson(user, lesson);
		userLesson.setCompletedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
		return userLesson;
	}

	private UserLesson incompleteUserLesson(User user, Lesson lesson) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(user.getId());
		id.setLessonId(lesson.getId());
		userLesson.setId(id);
		userLesson.setUser(user);
		userLesson.setLesson(lesson);
		userLesson.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return userLesson;
	}
}
