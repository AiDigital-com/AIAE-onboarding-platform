package com.aidigital.aionboarding.material;

import com.aidigital.aionboarding.domain.common.dictionary.MaterialFileKindCode;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.MaterialFileKind;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.MaterialFileKindRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialFileSummaryProjection;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialLinkSummaryProjection;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialSearchSummaryProjection;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import com.aidigital.aionboarding.service.material.support.MaterialSpecificationBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the bounded material search summary queries against real
 * PostgreSQL: {@link MaterialRepository#searchSummaries} must project {@code hasText} instead of
 * the full body and must not silently drop a material whose {@code createdByUser} association is
 * null (a real risk since the projection selects {@code createdByUser.id} via what would
 * otherwise be an implicit inner join); {@link MaterialLinkRepository#findSummariesByMaterialIdIn}
 * and {@link MaterialFileRepository#findSummariesByMaterialIdIn} must omit the extracted link
 * text and OpenAI file-upload internals while keeping every other field intact.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MaterialSearchSummaryRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private MaterialRepository materialRepository;

	@Autowired
	private MaterialLinkRepository materialLinkRepository;

	@Autowired
	private MaterialFileRepository materialFileRepository;

	@Autowired
	private MaterialFileKindRepository materialFileKindRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	private final MaterialSpecificationBuilder specificationBuilder =
			new MaterialSpecificationBuilder(new TagsFilterSupport(new ObjectMapper()));

	@Test
	void searchSummariesShouldMapHasTextFlagWithoutSelectingFullBodyTest() {
		// Given: one material with non-blank text content, one with none
		materialRepository.save(material("With Text", "This material has body text.", null));
		materialRepository.save(material("Without Text", "", null));

		// When:
		Page<MaterialSearchSummaryProjection> result = searchSummaries(0, 20);

		// Then:
		assertThat(result.getContent())
				.extracting(MaterialSearchSummaryProjection::title, MaterialSearchSummaryProjection::hasText)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple("With Text", true),
						org.assertj.core.groups.Tuple.tuple("Without Text", false)
				);
	}

	@Test
	void searchSummariesShouldNotExcludeAMaterialWithNoCreatedByUserTest() {
		// Given: a material whose createdByUser association is null (legacy/system-created data)
		Material orphaned = material("No Creator", "text", null);
		materialRepository.save(orphaned);

		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User creator = userRepository.save(user(role, "Creator", "creator@test.com"));
		materialRepository.save(material("Has Creator", "text", creator));

		// When:
		Page<MaterialSearchSummaryProjection> result = searchSummaries(0, 20);

		// Then: both rows returned; the null association resolves to a null id, not a dropped row
		assertThat(result.getContent()).hasSize(2);
		MaterialSearchSummaryProjection noCreator = result.getContent().stream()
				.filter(m -> m.title().equals("No Creator")).findFirst().orElseThrow();
		MaterialSearchSummaryProjection hasCreator = result.getContent().stream()
				.filter(m -> m.title().equals("Has Creator")).findFirst().orElseThrow();
		assertThat(noCreator.createdByUserId()).isNull();
		assertThat(hasCreator.createdByUserId()).isEqualTo(creator.getId());
	}

	@Test
	void searchSummariesShouldRespectPageSizeAndReportTotalElementsTest() {
		// Given: three materials
		for (int i = 0; i < 3; i++) {
			materialRepository.save(material("Material " + i, "text", null));
		}

		// When: requesting a page of size 2
		Page<MaterialSearchSummaryProjection> firstPage = searchSummaries(0, 2);

		// Then:
		assertThat(firstPage.getContent()).hasSize(2);
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.hasNext()).isTrue();
	}

	@Test
	void findSummariesByMaterialIdInShouldOmitExtractedTextAndMetadataErrorTest() {
		// Given: a material with two links in sort order
		Material material = materialRepository.save(material("With Links", "text", null));
		materialLinkRepository.save(link(material, "https://a.example.com", 0));
		materialLinkRepository.save(link(material, "https://b.example.com", 1));

		// When:
		List<MaterialLinkSummaryProjection> result =
				materialLinkRepository.findSummariesByMaterialIdIn(List.of(material.getId()));

		// Then: preview fields intact, in sort order; extractedText/metadataError structurally absent
		assertThat(result).extracting(MaterialLinkSummaryProjection::url)
				.containsExactly("https://a.example.com", "https://b.example.com");
		assertThat(result.get(0).title()).isEqualTo("Link title");
		assertThat(result.get(0).imageUrl()).isEqualTo("https://image.example.com/a.png");
	}

	@Test
	void findSummariesByMaterialIdInShouldOmitOpenAiInternalsTest() {
		// Given: a material with one file attachment carrying OpenAI upload state
		Material material = materialRepository.save(material("With File", "text", null));
		MaterialFileKind kind = materialFileKindRepository.findByCode(MaterialFileKindCode.FILE).orElseThrow();
		MaterialFile file = new MaterialFile();
		file.setMaterial(material);
		file.setKind(kind);
		file.setOriginalName("notes.pdf");
		file.setStorageKey("storage/notes.pdf");
		file.setMimeType("application/pdf");
		file.setSizeBytes(2048L);
		file.setOpenaiFileId("file-123");
		file.setOpenaiFilePurpose("assistants");
		file.setOpenaiFileStatus("processed");
		file.setOpenaiFileError("");
		file.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		materialFileRepository.save(file);

		// When:
		List<MaterialFileSummaryProjection> result =
				materialFileRepository.findSummariesByMaterialIdIn(List.of(material.getId()));

		// Then: display fields intact; openai* fields structurally absent from the projection
		assertThat(result).hasSize(1);
		MaterialFileSummaryProjection summary = result.get(0);
		assertThat(summary.originalName()).isEqualTo("notes.pdf");
		assertThat(summary.storageKey()).isEqualTo("storage/notes.pdf");
		assertThat(summary.mimeType()).isEqualTo("application/pdf");
		assertThat(summary.sizeBytes()).isEqualTo(2048L);
		assertThat(summary.kindCode()).isEqualTo(MaterialFileKindCode.FILE);
	}

	@Test
	void countShouldMatchTheNumberOfMaterialsMatchingTheFilterTest() {
		// Given: three materials
		for (int i = 0; i < 3; i++) {
			materialRepository.save(material("Material " + i, "text", null));
		}
		MaterialListQuery query = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.DESC
		);

		// When: counting via the same reused specification as the summary search
		Specification<Material> specification = specificationBuilder.build(query);
		long total = materialRepository.count(specification);

		// Then:
		assertThat(total).isEqualTo(3L);
	}

	private Page<MaterialSearchSummaryProjection> searchSummaries(int page, int size) {
		MaterialListQuery query = new MaterialListQuery(
				null, null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.DESC
		);
		Specification<Material> specification = specificationBuilder.build(query);
		return materialRepository.searchSummaries(specification, PageRequest.of(page, size));
	}

	private User user(UserRole role, String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Material material(String title, String textContent, User createdByUser) {
		Material material = new Material();
		material.setTitle(title);
		material.setDescription("description");
		material.setTextContent(textContent);
		material.setCoverImageStorageKey("");
		material.setCoverImageOriginalName("");
		material.setCoverImageMimeType("");
		material.setTags(List.of());
		material.setCreatedByUser(createdByUser);
		material.setCreatedBy("tester");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		material.setCreatedAt(now);
		material.setUpdatedAt(now);
		return material;
	}

	private MaterialLink link(Material material, String url, int sortOrder) {
		MaterialLink link = new MaterialLink();
		link.setMaterial(material);
		link.setUrl(url);
		link.setSortOrder(sortOrder);
		link.setTitle("Link title");
		link.setDescription("Link description");
		link.setImageUrl("https://image.example.com/a.png");
		link.setSiteName("Example");
		link.setExtractedText("extracted body text that must never reach the summary contract");
		link.setMetadataError("");
		return link;
	}
}
