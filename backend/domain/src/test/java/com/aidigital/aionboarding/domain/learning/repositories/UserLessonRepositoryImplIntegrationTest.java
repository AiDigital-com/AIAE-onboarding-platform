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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link UserLessonRepositoryImpl#deleteRoadmapDerivedLessonEnrollments} — the
 * set-based {@code CriteriaDelete} that revokes a roadmap's lesson enrollments unless another
 * roadmap still grants the same lesson — against a real H2/PostgreSQL-mode schema, entirely
 * within {@code domain} (see domain/pom.xml for why not Testcontainers here).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserLessonRepositoryImplIntegrationTest {

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
	void shouldDeleteEnrollmentOnlyWhenNoOtherRoadmapStillGrantsTheLessonTest() {
		// Given: a learner enrolled in two roadmaps, one sharing lessonShared with the revoked
		// roadmap and one lesson (lessonOnlyInRevoked) unique to the revoked roadmap
		UserRole role = userRoleRepository.save(userRole());
		LessonStatus readyStatus = lessonStatusRepository.save(lessonStatus());
		LessonPublicationStatus publishedStatus = lessonPublicationStatusRepository.save(publicationStatus());
		LessonContentFormat contentFormat = lessonContentFormatRepository.save(contentFormat());

		User learner = userRepository.save(user("Learner", "learner@test.com", role));
		Lesson lessonShared = lessonRepository.save(lesson("Shared", readyStatus, publishedStatus, contentFormat));
		Lesson lessonOnlyInRevoked =
				lessonRepository.save(lesson("OnlyInRevoked", readyStatus, publishedStatus, contentFormat));

		Roadmap revokedRoadmap = roadmapRepository.save(roadmap("Revoked Roadmap"));
		Roadmap survivingRoadmap = roadmapRepository.save(roadmap("Surviving Roadmap"));

		roadmapLessonRepository.save(roadmapLesson(revokedRoadmap, lessonShared, 0));
		roadmapLessonRepository.save(roadmapLesson(revokedRoadmap, lessonOnlyInRevoked, 1));
		roadmapLessonRepository.save(roadmapLesson(survivingRoadmap, lessonShared, 0));

		userRoadmapRepository.save(userRoadmap(learner, revokedRoadmap));
		userRoadmapRepository.save(userRoadmap(learner, survivingRoadmap));

		userLessonRepository.save(incompleteUserLesson(learner, lessonShared));
		userLessonRepository.save(incompleteUserLesson(learner, lessonOnlyInRevoked));

		// When: revoking the roadmap that is being un-enrolled from
		int deletedCount = userLessonRepository.deleteRoadmapDerivedLessonEnrollments(
				List.of(learner.getId()), revokedRoadmap.getId());

		// Then: only the enrollment for the lesson with no other granting roadmap is deleted
		assertThat(deletedCount).isEqualTo(1);
		Optional<UserLesson> sharedEnrollment = userLessonRepository.findById(
				idOf(learner.getId(), lessonShared.getId()));
		Optional<UserLesson> revokedOnlyEnrollment = userLessonRepository.findById(
				idOf(learner.getId(), lessonOnlyInRevoked.getId()));
		assertThat(sharedEnrollment).isPresent();
		assertThat(revokedOnlyEnrollment).isEmpty();
	}

	private UserLesson.UserLessonId idOf(Long userId, Long lessonId) {
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(userId);
		id.setLessonId(lessonId);
		return id;
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
