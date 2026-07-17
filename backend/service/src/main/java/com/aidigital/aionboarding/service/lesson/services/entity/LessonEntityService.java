package com.aidigital.aionboarding.service.lesson.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lesson.models.MaterialUsageCount;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonSearchSummaryProjection;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.services.entity.LessonContentFormatEntityService;
import com.aidigital.aionboarding.service.common.dictionary.services.entity.LessonPublicationStatusEntityService;
import com.aidigital.aionboarding.service.common.dictionary.services.entity.LessonStatusEntityService;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonVisibilityFilter;
import com.aidigital.aionboarding.service.lesson.support.LessonHtmlSanitizer;
import com.aidigital.aionboarding.service.lesson.support.LessonSpecificationBuilder;
import com.aidigital.aionboarding.service.material.services.entity.MaterialEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Short-transaction CRUD helpers for the {@link Lesson} aggregate.
 * <p>
 * Each public method runs in its own transaction and returns detached/attached entities
 * for orchestrators that span multiple persistence steps. Long-running orchestrators
 * should never hold a transaction across AI calls.
 */
@Service
@RequiredArgsConstructor
public class LessonEntityService {

	private static final String DEFAULT_CONTENT_FORMAT_CODE = "markdown";

	private static final String DRAFT_STEP = "draft";

	private static final String MANUAL_STEP = "manual";

	private static final String ANONYMOUS_DISPLAY_NAME = "anonymous";

	private static final String META_STEP = "step";

	private static final String META_MODE = "mode";

	private static final String META_DESIRED_FORMAT = "desiredFormat";

	private static final String META_DEPTH = "depth";

	private static final String META_TONE = "tone";

	private final LessonRepository lessonRepository;

	private final LessonMaterialEntityService lessonMaterialEntityService;

	private final UserEntityService userEntityService;

	private final MaterialEntityService materialEntityService;

	private final LessonStatusEntityService lessonStatusEntityService;

	private final LessonPublicationStatusEntityService lessonPublicationStatusEntityService;

	private final LessonContentFormatEntityService lessonContentFormatEntityService;

	private final LessonSpecificationBuilder lessonSpecificationBuilder;

	private final LessonHtmlSanitizer lessonHtmlSanitizer;

	private final CurrentTime currentTime;

	/**
	 * Returns all {@link LessonMaterial} join records for a lesson, ordered by sort order ascending.
	 * <p>
	 * Use this in place of injecting {@link LessonMaterialEntityService} directly from an
	 * orchestration service.
	 *
	 * @param lessonId the lesson primary key
	 * @return lesson-material records in ascending sort order
	 */
	@Transactional(readOnly = true)
	public List<LessonMaterial> findLessonMaterialsByLessonId(Long lessonId) {
		return lessonMaterialEntityService.findByLessonIdOrderBySortOrderAsc(lessonId);
	}

	/**
	 * Loads lesson-material joins for several lessons.
	 *
	 * @param lessonIds lesson identifiers
	 * @return matching lesson-material joins
	 */
	@Transactional(readOnly = true)
	public List<LessonMaterial> findLessonMaterialsByLessonIds(Collection<Long> lessonIds) {
		return lessonMaterialEntityService.findByLessonIdsOrderByLessonIdAscSortOrderAsc(lessonIds);
	}

	/**
	 * Counts how many lessons reference the given material via {@link LessonMaterial}.
	 * <p>
	 * Use this in place of injecting {@link LessonMaterialEntityService} directly from an
	 * orchestration service.
	 *
	 * @param materialId the material primary key
	 * @return the number of lessons that reference the material
	 */
	@Transactional(readOnly = true)
	public long countLessonUsage(Long materialId) {
		return lessonMaterialEntityService.countByMaterialId(materialId);
	}

	/**
	 * Returns, among the given lesson IDs, those that have a teacher video with a video URL —
	 * for a card-level flag without shipping the full generation metadata JSON blob.
	 *
	 * @param lessonIds the lesson primary keys to restrict to
	 * @return the subset of {@code lessonIds} that have a teacher video URL
	 */
	@Transactional(readOnly = true)
	public Set<Long> findIdsWithTeacherVideoIn(Collection<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return Set.of();
		}
		return lessonRepository.findIdsWithTeacherVideoIn(lessonIds);
	}

	/**
	 * Returns the titles of lessons that reference the given material, ordered alphabetically,
	 * limited by the given page request.
	 * <p>
	 * Use this in place of injecting {@link LessonMaterialEntityService} directly from an
	 * orchestration service.
	 *
	 * @param materialId the material primary key
	 * @param pageable   paging parameters bounding the number of titles returned
	 * @return lesson titles referencing the material
	 */
	@Transactional(readOnly = true)
	public List<String> findLessonTitlesByMaterialId(Long materialId, Pageable pageable) {
		return lessonMaterialEntityService.findLessonTitlesByMaterialId(materialId, pageable);
	}

	/**
	 * Returns lesson usage counts grouped by material ID for exactly the given materials.
	 * <p>
	 * Use this in place of injecting {@link LessonMaterialEntityService} directly from an
	 * orchestration service.
	 *
	 * @param materialIds material identifiers to count usage for
	 * @return per-material usage counts, bounded to the given identifiers
	 */
	@Transactional(readOnly = true)
	public List<MaterialUsageCount> findLessonUsageCounts(Collection<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return List.of();
		}
		return lessonMaterialEntityService.countUsageByMaterialIds(materialIds);
	}

	/**
	 * Batch-loads materials by ID, preserving the caller's requested order and
	 * silently skipping any IDs that no longer exist.
	 *
	 * @param materialIds ordered list of material IDs to load
	 * @return list of {@link Material} entities in the same order as the input IDs
	 */
	@Transactional(readOnly = true)
	public List<Material> findMaterialsByIds(List<Long> materialIds) {
		return materialEntityService.findAllById(materialIds);
	}

	/**
	 * Persists a new draft lesson and links the given materials to it.
	 *
	 * @param viewer      authenticated user creating the lesson
	 * @param input       creation parameters
	 * @param materialIds ordered list of material IDs to attach
	 * @return the saved draft {@link Lesson}
	 */
	@Transactional
	public Lesson createDraft(AppUser viewer, CreateLessonInput input, List<Long> materialIds) {
		User owner = resolveOwner(viewer);
		Lesson lesson = buildBaseLesson(owner, input);
		lesson.setStatus(requireStatus(LessonStatusCode.DRAFT));
		lesson.setPublicationStatus(requirePublicationStatus(LessonPublicationStatusCode.PRIVATE));
		lesson.setContentHtml("");
		lesson.setContentMarkdown("");
		lesson.setErrorMessage("");
		lesson.setRevisionHistory(Collections.emptyList());
		lesson.setGenerationMetadata(generationMeta(input, DRAFT_STEP, Map.of()));
		lesson = lessonRepository.save(lesson);
		linkMaterials(lesson, materialIds);
		return lesson;
	}

	/**
	 * Persists a new manually authored lesson with READY status and links materials.
	 *
	 * @param viewer      authenticated user creating the lesson
	 * @param input       creation parameters, must include non-blank title and content
	 * @param materialIds ordered list of material IDs to attach
	 * @return the saved manual {@link Lesson}
	 */
	@Transactional
	public Lesson createManualLesson(AppUser viewer, CreateLessonInput input, List<Long> materialIds) {
		User owner = resolveOwner(viewer);
		Lesson lesson = buildBaseLesson(owner, input);
		lesson.setStatus(requireStatus(LessonStatusCode.READY));
		lesson.setPublicationStatus(requirePublicationStatus(LessonPublicationStatusCode.PRIVATE));
		// Sanitized here, the sole point where a manually authored lesson's HTML is first persisted.
		lesson.setContentHtml(lessonHtmlSanitizer.sanitize(input.contentHtml() == null ? "" : input.contentHtml()));
		lesson.setContentMarkdown("");
		lesson.setErrorMessage("");
		lesson.setRevisionHistory(Collections.emptyList());
		lesson.setGenerationMetadata(generationMeta(input, MANUAL_STEP, Map.of()));
		lesson = lessonRepository.save(lesson);
		linkMaterials(lesson, materialIds);
		return lesson;
	}

	/**
	 * Transitions the lesson to GENERATING status and snapshots the generation metadata.
	 *
	 * @param lesson             the lesson to update (must be in DRAFT status)
	 * @param generationMetadata metadata to store, or {@code null} to leave unchanged
	 * @return the saved {@link Lesson}
	 */
	@Transactional
	public Lesson markGenerating(Lesson lesson, Map<String, Object> generationMetadata) {
		lesson.setStatus(requireStatus(LessonStatusCode.GENERATING));
		lesson.setErrorMessage("");
		if (generationMetadata != null) {
			lesson.setGenerationMetadata(generationMetadata);
		}
		lesson.setUpdatedAt(currentTime.utcDateTime());
		return lessonRepository.save(lesson);
	}

	/**
	 * Transitions the lesson to READY status and stores the generated content.
	 *
	 * @param lesson          the lesson to update
	 * @param extractedTitle  title extracted from the HTML, ignored if blank
	 * @param contentHtml     generated HTML content
	 * @param contentMarkdown raw markdown, or empty string if generation produced HTML
	 * @param readyMetadata   metadata to store, or {@code null} to leave unchanged
	 * @return the saved {@link Lesson}
	 */
	@Transactional
	public Lesson markReady(
			Lesson lesson,
			String extractedTitle,
			String contentHtml,
			String contentMarkdown,
			Map<String, Object> readyMetadata
	) {
		lesson.setStatus(requireStatus(LessonStatusCode.READY));
		if (extractedTitle != null && !extractedTitle.isBlank()) {
			lesson.setTitle(extractedTitle);
		}
		// Sanitized here so AI-generated HTML is held to the same allowlist as manual authoring.
		lesson.setContentHtml(lessonHtmlSanitizer.sanitize(contentHtml == null ? "" : contentHtml));
		lesson.setContentMarkdown(contentMarkdown == null ? "" : contentMarkdown);
		lesson.setErrorMessage("");
		if (readyMetadata != null) {
			lesson.setGenerationMetadata(readyMetadata);
		}
		lesson.setUpdatedAt(currentTime.utcDateTime());
		return lessonRepository.save(lesson);
	}

	/**
	 * Transitions the lesson to FAILED status and stores the error message.
	 *
	 * @param lesson         the lesson to update
	 * @param errorMessage   human-readable error description
	 * @param failedMetadata metadata to store, or {@code null} to leave unchanged
	 * @return the saved {@link Lesson}
	 */
	@Transactional
	public Lesson markFailed(Lesson lesson, String errorMessage, Map<String, Object> failedMetadata) {
		lesson.setStatus(requireStatus(LessonStatusCode.FAILED));
		lesson.setErrorMessage(errorMessage == null ? "" : errorMessage);
		if (failedMetadata != null) {
			lesson.setGenerationMetadata(failedMetadata);
		}
		lesson.setUpdatedAt(currentTime.utcDateTime());
		return lessonRepository.save(lesson);
	}

	/**
	 * Transitions a FAILED lesson to READY and clears its error message, used when a user
	 * manually supplies content for a lesson whose AI generation failed. No-op if the lesson
	 * is not currently FAILED, so callers can invoke it unconditionally on every content edit.
	 *
	 * @param lesson the lesson to update, mutated in place
	 */
	public void clearFailureIfPresent(Lesson lesson) {
		if (LessonStatusCode.FAILED.equals(lesson.getStatus().getCode())) {
			lesson.setStatus(requireStatus(LessonStatusCode.READY));
			lesson.setErrorMessage("");
		}
	}

	/**
	 * Stores revised content metadata on the lesson after a successful revision cycle.
	 * <p>
	 * Does not change the lesson's status — the lesson must already be in READY status
	 * before calling this method.
	 *
	 * @param lesson             the lesson to update
	 * @param generationMetadata updated metadata including the revision history entry
	 * @return the saved {@link Lesson}
	 */
	@Transactional
	public Lesson saveRevised(Lesson lesson, Map<String, Object> generationMetadata) {
		lesson.setGenerationMetadata(generationMetadata);
		lesson.setErrorMessage("");
		lesson.setUpdatedAt(currentTime.utcDateTime());
		return lessonRepository.save(lesson);
	}

	/**
	 * Reloads the lesson by ID with eager JOIN FETCHes on {@code status}, {@code publicationStatus},
	 * and {@code createdByUser} inside a short read-only transaction.
	 * <p>
	 * Use this method after a generation or mutation step to obtain a fully-initialised lesson
	 * entity that can be safely assembled into a summary record outside any Hibernate session.
	 *
	 * @param lessonId the lesson primary key
	 * @return the {@link Lesson} entity with status, publicationStatus, and createdByUser initialised
	 * @throws AppException C001 if no lesson with the given ID exists
	 */
	@Transactional(readOnly = true)
	public Lesson findByIdWithFetches(Long lessonId) {
		return lessonRepository.findByIdWithFetches(lessonId)
				.orElseThrow(() -> new AppException(ErrorReason.C001, lessonId));
	}

	/**
	 * Searches lessons with the given typed filters and visibility rule, returning a bounded,
	 * sorted page of lean summary projections — content truncated to a short preview, with no
	 * generation metadata, revision history, materials, or assets.
	 *
	 * @param query      typed filter and sort parameters
	 * @param visibility security context resolved once per request
	 * @param page       zero-based page index
	 * @param size       maximum number of lessons per page
	 * @return a page of {@link LessonSearchSummaryProjection} matching the filter and visibility rule
	 */
	@Transactional(readOnly = true)
	public Page<LessonSearchSummaryProjection> searchSummaries(LessonListQuery query,
															   LessonVisibilityFilter visibility, int page, int size) {
		Specification<Lesson> specification = buildSpecification(query, visibility);
		Pageable pageable = PageRequest.of(page, size);
		return lessonRepository.searchSummaries(specification, pageable);
	}

	/**
	 * Counts lessons matching the given typed filters and visibility rule, without fetching or
	 * paginating any rows — for cheap tab-count display independent of the active Library tab.
	 *
	 * @param query      typed filter and sort parameters
	 * @param visibility security context resolved once per request
	 * @return the number of lessons matching the filter and visibility rule
	 */
	@Transactional(readOnly = true)
	public long countSummaries(LessonListQuery query, LessonVisibilityFilter visibility) {
		Specification<Lesson> specification = buildSpecification(query, visibility);
		return lessonRepository.count(specification);
	}

	/**
	 * Builds the dynamic filter, visibility, and sort {@link Specification} shared by every
	 * lesson search variant (full-entity and summary projection alike), resolving the status and
	 * publication-status codes referenced by the filter and visibility rule.
	 *
	 * @param query      typed filter and sort parameters
	 * @param visibility security context resolved once per request
	 * @return the JPA Criteria specification
	 */
	Specification<Lesson> buildSpecification(LessonListQuery query, LessonVisibilityFilter visibility) {
		Long statusId = query.statusCode() == null ? null : requireStatus(query.statusCode()).getId();
		Long publicationStatusId = query.publicationStatusCode() == null
				? null
				: requirePublicationStatus(query.publicationStatusCode()).getId();
		Long readyStatusId = requireStatus(LessonStatusCode.READY).getId();
		Long publishedStatusId = requirePublicationStatus(LessonPublicationStatusCode.PUBLISHED).getId();

		return lessonSpecificationBuilder.build(
				query, visibility, statusId, publicationStatusId, readyStatusId, publishedStatusId
		);
	}

	/**
	 * Persists any changes to the given lesson entity and returns the saved state.
	 *
	 * @param lesson the lesson entity to persist
	 * @return the saved {@link Lesson}
	 */
	@Transactional
	public Lesson save(Lesson lesson) {
		return lessonRepository.save(lesson);
	}

	/**
	 * Removes the given lesson entity from the database.
	 *
	 * @param lesson the lesson entity to delete
	 */
	@Transactional
	public void delete(Lesson lesson) {
		lessonRepository.delete(lesson);
	}

	/**
	 * Loads the {@link LessonPublicationStatus} for the given code, throwing if absent.
	 * Exposed here so orchestration services can change publication status without injecting
	 * the dictionary repository directly.
	 *
	 * @param code the publication status code (e.g., {@code "published"})
	 * @return the matching {@link LessonPublicationStatus} entity
	 * @throws AppException C001 if the code does not exist in the database
	 */
	@Transactional(readOnly = true)
	public LessonPublicationStatus findPublicationStatus(String code) {
		return lessonPublicationStatusEntityService.getReferenceByCode(code);
	}

	/**
	 * Loads the lesson by ID or throws if it does not exist.
	 *
	 * @param lessonId the lesson primary key
	 * @return the {@link Lesson} entity
	 * @throws AppException C001 if no lesson with the given ID exists
	 */
	@Transactional(readOnly = true)
	public Lesson getReference(Long lessonId) {
		return lessonRepository.findById(lessonId)
				.orElseThrow(() -> new AppException(
						ErrorReason.C001,
						lessonId
				));
	}

	/**
	 * Batch-loads lessons by ID, used in place of injecting {@link LessonRepository} directly
	 * from an orchestration service. Callers that require "all IDs must exist" semantics
	 * (e.g. roadmap lesson validation) must compare {@code result.size()} against the
	 * requested ID count themselves.
	 *
	 * @param lessonIds the lesson primary keys to load
	 * @return the {@link Lesson} entities found for the given IDs
	 */
	@Transactional(readOnly = true)
	public List<Lesson> findAllById(List<Long> lessonIds) {
		return lessonRepository.findAllById(lessonIds);
	}

	/**
	 * Builds an unpersisted {@link Lesson} entity from basic input fields and owner.
	 *
	 * @param owner the user who will own the lesson
	 * @param input creation parameters
	 * @return a transient lesson entity ready for persisting
	 */
	Lesson buildBaseLesson(User owner, CreateLessonInput input) {
		Lesson lesson = new Lesson();
		LocalDateTime now = currentTime.utcDateTime();
		lesson.setTitle(input.title() == null ? "" : input.title());
		lesson.setDescription(input.description() == null ? "" : input.description());
		lesson.setUserInstructions(input.instructions() == null ? "" : input.instructions());
		lesson.setDepth(input.depth() == null ? "" : input.depth());
		lesson.setTone(input.tone() == null ? "" : input.tone());
		lesson.setDesiredFormat(input.desiredFormat() == null ? "" : input.desiredFormat());
		lesson.setContentFormat(requireContentFormat(DEFAULT_CONTENT_FORMAT_CODE));
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setTags(input.tags() == null ? Collections.emptyList() : new ArrayList<>(input.tags()));
		lesson.setCreatedBy(displayName(owner));
		lesson.setCreatedByUser(owner);
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}

	String displayName(User owner) {
		if (owner.getName() != null && !owner.getName().isBlank()) {
			return owner.getName();
		}
		if (owner.getEmail() != null && !owner.getEmail().isBlank()) {
			return owner.getEmail();
		}
		return owner.getId() != null ? owner.getId().toString() : ANONYMOUS_DISPLAY_NAME;
	}

	/**
	 * Persists {@link LessonMaterial} join records for each provided material ID,
	 * skipping any IDs that no longer exist in the database.
	 *
	 * @param lesson      the lesson to link materials to
	 * @param materialIds ordered list of material IDs to attach
	 */
	void linkMaterials(Lesson lesson, List<Long> materialIds) {
		if (materialIds == null || materialIds.isEmpty()) {
			return;
		}
		LocalDateTime now = currentTime.utcDateTime();
		int order = 0;
		List<LessonMaterial> links = new ArrayList<>();
		for (Material material : materialEntityService.findAllById(materialIds)) {
			LessonMaterial link = new LessonMaterial();
			LessonMaterial.LessonMaterialId id = new LessonMaterial.LessonMaterialId();
			id.setLessonId(lesson.getId());
			id.setMaterialId(material.getId());
			link.setId(id);
			link.setLesson(lesson);
			link.setMaterial(material);
			link.setSortOrder(order++);
			link.setCreatedAt(now);
			links.add(link);
		}
		if (!links.isEmpty()) {
			lessonMaterialEntityService.saveAll(links);
		}
	}

	/**
	 * Resolves the {@link User} entity for the authenticated viewer.
	 *
	 * @param viewer the current authenticated user
	 * @return the corresponding {@link User} entity
	 * @throws AppException C005 if viewer is unauthenticated; C001 if user not found
	 */
	User resolveOwner(AppUser viewer) {
		if (viewer == null || viewer.internalId() == null) {
			throw new AppException(
					ErrorReason.C005,
					"Authenticated user is required"
			);
		}
		return userEntityService.findById(viewer.internalId())
				.orElseThrow(() -> new AppException(
						ErrorReason.C001,
						viewer.internalId()
				));
	}

	/**
	 * Loads the {@link LessonStatus} for the given status code or fails fast.
	 *
	 * @param code the status code constant
	 * @return the matching {@link LessonStatus} entity
	 * @throws IllegalStateException if the status code is missing from the database
	 */
	LessonStatus requireStatus(String code) {
		return lessonStatusEntityService.getReferenceByCode(code);
	}

	/**
	 * Loads the {@link LessonPublicationStatus} for the given code or fails fast.
	 *
	 * @param code the publication status code constant
	 * @return the matching {@link LessonPublicationStatus} entity
	 * @throws IllegalStateException if the code is missing from the database
	 */
	LessonPublicationStatus requirePublicationStatus(String code) {
		return lessonPublicationStatusEntityService.getReferenceByCode(code);
	}

	/**
	 * Loads the {@link LessonContentFormat} for the given code or fails fast.
	 *
	 * @param code the content format code constant
	 * @return the matching {@link LessonContentFormat} entity
	 * @throws IllegalStateException if the code is missing from the database
	 */
	LessonContentFormat requireContentFormat(String code) {
		return lessonContentFormatEntityService.getReferenceByCode(code);
	}

	/**
	 * Builds the initial generation metadata map for a new lesson.
	 *
	 * @param input the lesson creation input
	 * @param step  the creation step label (e.g., {@code "draft"} or {@code "manual"})
	 * @param extra additional entries to merge into the metadata
	 * @return a mutable metadata map
	 */
	Map<String, Object> generationMeta(
			CreateLessonInput input,
			String step,
			Map<String, Object> extra
	) {
		Map<String, Object> meta = new HashMap<>();
		meta.put(META_STEP, step);
		meta.put(META_MODE, input.mode() == null ? LessonCreationModeV1.GENERATE.name() : input.mode().name());
		meta.put(META_DESIRED_FORMAT, input.desiredFormat());
		meta.put(META_DEPTH, input.depth());
		meta.put(META_TONE, input.tone());
		if (extra != null) {
			meta.putAll(extra);
		}
		return meta;
	}
}
