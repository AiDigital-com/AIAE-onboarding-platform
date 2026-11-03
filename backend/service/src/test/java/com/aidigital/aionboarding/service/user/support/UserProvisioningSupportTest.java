package com.aidigital.aionboarding.service.user.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.user.services.UserFirstLoginAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProvisioningSupportTest {

	@Mock
	private UserEntityService userEntityService;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private UserFirstLoginAssignmentSyncService userFirstLoginAssignmentSyncService;

	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private UserProvisioningSupport support;

	@Test
	void shouldCreateAMemberAndHandItToTheFirstLoginAssignmentRuleTest() {
		// Given:
		UserRole memberRole = Instancio.create(UserRole.class);
		when(dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER)).thenReturn(memberRole);
		when(userEntityService.save(any(User.class))).thenAnswer(invocation -> {
			User saved = invocation.getArgument(0);
			saved.setId(42L);
			return saved;
		});

		// When:
		User created = support.createUser("clerk-4", "new@test.com", "  New User ");

		// Then:
		assertThat(created.getClerkUserId()).isEqualTo("clerk-4");
		assertThat(created.getEmail()).isEqualTo("new@test.com");
		assertThat(created.getName()).isEqualTo("New User");
		assertThat(created.getRole()).isEqualTo(memberRole);
		assertThat(created.getCreatedAt()).isNotNull();
		verify(userFirstLoginAssignmentSyncService).onUserCreated(42L);
	}

	@Test
	void shouldFallBackToTheEmailAsNameWhenClerkSendsNoNameTest() {
		// Given:
		when(dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER)).thenReturn(Instancio.create(UserRole.class));
		when(userEntityService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When:
		User created = support.createUser("clerk-5", "anon@test.com", "   ");

		// Then:
		assertThat(created.getName()).isEqualTo("anon@test.com");
	}

	@Test
	void shouldResolveRoleReferencesThroughTheDictionaryTest() {
		// Given:
		UserRole teamLead = Instancio.create(UserRole.class);
		when(dictionaryLookupService.getUserRoleReference(UserRoleCode.TEAMLEAD)).thenReturn(teamLead);

		// When / Then:
		assertThat(support.resolveRole(UserRoleCode.TEAMLEAD)).isEqualTo(teamLead);
	}
}
