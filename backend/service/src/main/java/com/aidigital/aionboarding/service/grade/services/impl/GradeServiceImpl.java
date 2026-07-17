package com.aidigital.aionboarding.service.grade.services.impl;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.grade.models.CreateGradeInput;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.grade.models.UpdateGradeInput;
import com.aidigital.aionboarding.service.grade.services.GradeService;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

	private final GradeEntityService gradeEntityService;
	private final PermissionService permissionService;
	private final CurrentTime currentTime;

	@Override
	@Transactional(readOnly = true)
	public List<GradeRecord> listActive() {
		return gradeEntityService.findActiveOrderByDisplayOrder().stream().map(this::toRecord).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<GradeRecord> listAll(AppUser viewer) {
		permissionService.requirePermission(viewer, PermissionKeys.GRADES_MANAGE);
		return gradeEntityService.findAllOrderByDisplayOrder().stream().map(this::toRecord).toList();
	}

	@Override
	@Transactional
	public GradeRecord create(AppUser viewer, CreateGradeInput input) {
		permissionService.requirePermission(viewer, PermissionKeys.GRADES_MANAGE);
		String name = requireValidName(input.name());
		if (gradeEntityService.existsByNameIgnoreCase(name)) {
			throw new AppException(ErrorReason.C006, "A grade with this name already exists.");
		}
		String code = deriveUniqueCode(name);

		Grade grade = new Grade();
		grade.setCode(code);
		grade.setName(name);
		grade.setDisplayOrder(nextDisplayOrder());
		grade.setIsActive(true);
		LocalDateTime now = currentTime.utcDateTime();
		grade.setCreatedAt(now);
		grade.setUpdatedAt(now);
		return toRecord(gradeEntityService.save(grade));
	}

	@Override
	@Transactional
	public GradeRecord update(AppUser viewer, Long id, UpdateGradeInput input) {
		permissionService.requirePermission(viewer, PermissionKeys.GRADES_MANAGE);
		Grade grade = requireGrade(id);
		String name = requireValidName(input.name());
		if (!name.equalsIgnoreCase(grade.getName()) && gradeEntityService.existsByNameIgnoreCase(name)) {
			throw new AppException(ErrorReason.C006, "A grade with this name already exists.");
		}
		grade.setName(name);
		grade.setUpdatedAt(currentTime.utcDateTime());
		return toRecord(gradeEntityService.save(grade));
	}

	@Override
	@Transactional
	public void deactivate(AppUser viewer, Long id) {
		permissionService.requirePermission(viewer, PermissionKeys.GRADES_MANAGE);
		Grade grade = requireGrade(id);
		grade.setIsActive(false);
		grade.setUpdatedAt(currentTime.utcDateTime());
		gradeEntityService.save(grade);
	}

	@Override
	@Transactional
	public void activate(AppUser viewer, Long id) {
		permissionService.requirePermission(viewer, PermissionKeys.GRADES_MANAGE);
		Grade grade = requireGrade(id);
		grade.setIsActive(true);
		grade.setUpdatedAt(currentTime.utcDateTime());
		gradeEntityService.save(grade);
	}

	Grade requireGrade(Long id) {
		return gradeEntityService.findById(id).orElseThrow(() -> new AppException(ErrorReason.C001, "Grade not found" +
				"."));
	}

	String requireValidName(String name) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isBlank() || trimmed.length() > 100) {
			throw new AppException(ErrorReason.C002, "Grade name must be 1-100 characters.");
		}
		return trimmed;
	}

	/**
	 * Derives a stable code from the display name, disambiguating with a numeric suffix on the
	 * rare chance two different names slugify to the same code (e.g. "Team-Lead" vs "Team Lead").
	 */
	String deriveUniqueCode(String name) {
		String base = NON_ALPHANUMERIC.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("_").replaceAll("^_+|_+$",
				"");
		if (base.isBlank()) {
			base = "grade";
		}
		String candidate = base;
		int suffix = 2;
		while (gradeEntityService.existsByCodeIgnoreCase(candidate)) {
			candidate = base + "_" + suffix;
			suffix++;
		}
		return candidate;
	}

	int nextDisplayOrder() {
		return gradeEntityService.findAllOrderByDisplayOrder().stream()
				.map(Grade::getDisplayOrder)
				.filter(java.util.Objects::nonNull)
				.max(Integer::compareTo)
				.orElse(0) + 1;
	}

	GradeRecord toRecord(Grade grade) {
		return new GradeRecord(grade.getId(), grade.getCode(), grade.getName(), grade.getDisplayOrder(),
				grade.getIsActive());
	}
}
