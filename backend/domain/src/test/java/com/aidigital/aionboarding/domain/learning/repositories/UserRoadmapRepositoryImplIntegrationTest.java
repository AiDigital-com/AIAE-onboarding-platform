package com.aidigital.aionboarding.domain.learning.repositories;

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
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapLessonRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link UserRoadmapRepositoryImpl#findCompletedRoadmapsForUserLesson} — the
 * Criteria-API double-negative EXISTS/NOT EXISTS query — against a real H2/PostgreSQL-mode
 * schema, entirely within {@code domain} (see domain/pom.xml for why not Testcontainers here).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRoadmapRepositoryImplIntegrationTest {

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
		// Given: learner enrolled in two roadmaps
		//   roadmapFullyDone: L1, L2 — both completed
		//   roadmapStillOpen: L1, L3 — L3 not completed, so not fully done
		UserRole role = userRoleRepository.save(userRole());
		LessonStatus readyStatus = lessonStatusRepository.save(lessonStatus());
		LessonPublicationStatus publishedStatus = lessonPublicationStatusRepository.save(publicationStatus());
		LessonContentFormat contentFormat = lessonContentFormatRepository.save(contentFormat());

		User learner = userRepository.save(user("Learner", "learner@test.com", role));
		Lesson lessonOne = lessonRepository.save(lesson("L1", readyStatus, publishedStatus, contentFormat));
		Lesson lessonTwo = lessonRepository.save(lesson("L2", readyStatus, publishedStatus, contentFormat));
		Lesson lessonThree = lessonRepository.save(lesson("L3", readyStatus, publishedStatus, contentFormat));

		Roadmap roadmapFullyDone = roadmapRepository.save(roadmap("Zeta Roadmap"));
		Roadmap roadmapStillOpen = roadmapRepository.save(roadmap("Alpha Roadmap"));

		roadmapLessonRepository.save(roadmapLesson(roadmapFullyDone, lessonOne, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmapFullyDone, lessonTwo, 1));
		roadmapLessonRepository.save(roadmapLesson(roadmapStillOpen, lessonOne, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmapStillOpen, lessonThree, 1));

		userRoadmapRepository.save(userRoadmap(learner, roadmapFullyDone));
		userRoadmapRepository.save(userRoadmap(learner, roadmapStillOpen));

		userLessonRepository.save(completedUserLesson(learner, lessonOne));
		userLessonRepository.save(completedUserLesson(learner, lessonTwo));
		userLessonRepository.save(incompleteUserLesson(learner, lessonThree));

		// When: checking after completing L1, which belongs to both roadmaps
		List<CompletedRoadmapProjection> result =
				userRoadmapRepository.findCompletedRoadmapsForUserLesson(learner.getId(), lessonOne.getId());

		// Then: only the fully-completed roadmap containing L1 is returned
		assertThat(result).extracting(CompletedRoadmapProjection::getId).containsExactly(roadmapFullyDone.getId());
		assertThat(result).extracting(CompletedRoadmapProjection::getTitle).containsExactly("Zeta Roadmap");
	}

	@Test
	void shouldReturnEmptyWhenLessonNotInAnyEnrolledRoadmapTest() {
		// Given: learner enrolled in a roadmap that does not contain the changed lesson
		UserRole role = userRoleRepository.save(userRole());
		LessonStatus readyStatus = lessonStatusRepository.save(lessonStatus());
		LessonPublicationStatus publishedStatus = lessonPublicationStatusRepository.save(publicationStatus());
		LessonContentFormat contentFormat = lessonContentFormatRepository.save(contentFormat());

		User learner = userRepository.save(user("Learner2", "learner2@test.com", role));
		Lesson unrelatedLesson = lessonRepository.save(lesson("Unrelated", readyStatus, publishedStatus, contentFormat));
		Lesson standaloneLesson = lessonRepository.save(lesson("Standalone", readyStatus, publishedStatus, contentFormat));
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

	private UserRole userRole() {
		UserRole role = new UserRole();
		role.setCode(UserRoleCode.MEMBER);
		role.setName("Member");
		role.setDisplayOrder(1);
		role.setIsActive(true);
		return role;
	}

	private LessonStatus lessonStatus() {
		LessonStatus status = new LessonStatus();
		status.setCode(LessonStatusCode.READY);
		status.setName("Ready");
		status.setDisplayOrder(1);
		status.setIsActive(true);
		return status;
	}

	private LessonPublicationStatus publicationStatus() {
		LessonPublicationStatus status = new LessonPublicationStatus();
		status.setCode(LessonPublicationStatusCode.PUBLISHED);
		status.setName("Published");
		status.setDisplayOrder(1);
		status.setIsActive(true);
		return status;
	}

	private LessonContentFormat contentFormat() {
		LessonContentFormat format = new LessonContentFormat();
		format.setCode(LessonContentFormatCode.MARKDOWN);
		format.setName("Markdown");
		format.setDisplayOrder(1);
		format.setIsActive(true);
		return format;
	}

	private User user(String name, String email, UserRole role) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Lesson lesson(
			String title, LessonStatus status, LessonPublicationStatus publicationStatus, LessonContentFormat contentFormat
	) {
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
