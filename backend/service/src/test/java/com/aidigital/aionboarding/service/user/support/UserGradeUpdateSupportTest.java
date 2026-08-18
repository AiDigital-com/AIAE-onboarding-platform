package com.aidigital.aionboarding.service.user.support;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.support.GroupAccessPolicy;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.UserGradeAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGradeUpdateSupportTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private GradeEntityService gradeEntityService;
	@Mock
	private GroupAccessPolicy groupAccessPolicy;
	@Mock
	private UserGradeAssignmentSyncService userGradeAssignmentSyncService;
	@Mock
	private UserRecordMapper userMapper;

	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private UserGradeUpdateSupport support;

	@Test
	void shouldRejectWhenViewerCannotEditMemberGradeTest() {
		// Given:
		AppUser lead = new AppUser(2L, "clerk-lead", "lead@test.com", "Lead", "teamlead", "Lead", null, null,
				null);
		when(groupAccessPolicy.canEditMemberGrade(lead, 5L)).thenReturn(false);

		// When-Then:
		assertThatThrownBy(() -> support.updateGrade(lead, 5L, 10L)).isInstanceOf(AppException.class);
		verify(userEntityService, never()).save(any());
	}

	@Test
	void shouldSetGradeAndTriggerSyncWhenGradeChangesTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
				null);
		User user = Instancio.of(User.class).set(field(User::getGrade), null).create();
		Grade grade = Instancio.of(Grade.class).set(field(Grade::getId), 10L).create();
		when(groupAccessPolicy.canEditMemberGrade(admin, 5L)).thenReturn(true);
		when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
		when(gradeEntityService.findById(10L)).thenReturn(Optional.of(grade));
		when(userEntityService.save(user)).thenReturn(user);
		UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
				null, 10L, "junior", "Junior");
		when(userMapper.toRecord(user)).thenReturn(updatedRecord);

		// When:
		UserRecord result = support.updateGrade(admin, 5L, 10L);

		// Then:
		assertThat(result.gradeId()).isEqualTo(10L);
		assertThat(user.getGrade()).isEqualTo(grade);
		verify(userGradeAssignmentSyncService).onGradeChanged(5L, 10L);
	}

	@Test
	void shouldClearGradeWhenGradeIdIsNullTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
				null);
		Grade existingGrade = Instancio.of(Grade.class).set(field(Grade::getId), 10L).create();
		User user = Instancio.of(User.class).set(field(User::getGrade), existingGrade).create();
		when(groupAccessPolicy.canEditMemberGrade(admin, 5L)).thenReturn(true);
		when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
		when(userEntityService.save(user)).thenReturn(user);
		UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
				null, null, null, null);
		when(userMapper.toRecord(user)).thenReturn(updatedRecord);

		// When:
		UserRecord result = support.updateGrade(admin, 5L, null);

		// Then:
		assertThat(result.gradeId()).isNull();
		assertThat(user.getGrade()).isNull();
		verify(userGradeAssignmentSyncService).onGradeChanged(5L, null);
	}

	@Test
	void shouldNotTriggerSyncWhenGradeIsUnchangedTest() {
		// Given:
		AppUser admin = new AppUser(1L, "clerk-admin", "admin@test.com", "Admin", "admin", "Admin", null, null,
				null);
		Grade existingGrade = Instancio.of(Grade.class).set(field(Grade::getId), 10L).create();
		User user = Instancio.of(User.class).set(field(User::getGrade), existingGrade).create();
		when(groupAccessPolicy.canEditMemberGrade(admin, 5L)).thenReturn(true);
		when(userEntityService.findById(5L)).thenReturn(Optional.of(user));
		when(gradeEntityService.findById(10L)).thenReturn(Optional.of(existingGrade));
		when(userEntityService.save(user)).thenReturn(user);
		UserRecord updatedRecord = new UserRecord(5L, "clerk-5", "Name", "user@test.com", "member", null, null,
				null, 10L, "junior", "Junior");
		when(userMapper.toRecord(user)).thenReturn(updatedRecord);

		// When:
		support.updateGrade(admin, 5L, 10L);

		// Then:
		verify(userGradeAssignmentSyncService, never()).onGradeChanged(anyLong(), any());
	}
}
