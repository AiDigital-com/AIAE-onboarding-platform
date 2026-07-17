package com.aidigital.aionboarding.mappers.team;

import com.aidigital.aionboarding.api.v1.model.AddTeamMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.TeamLeadAdminViewV1;
import com.aidigital.aionboarding.api.v1.model.TeamV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamApiMapperImplTest {

	@InjectMocks
	private TeamApiMapperImpl teamApiMapperImpl;

	@Mock
	private UserApiMapper userApiMapper;

	@BeforeEach
	void setUp() {
		when(userApiMapper.toUserSummaryV1(any(UserRecord.class))).thenReturn(Instancio.create(UserSummaryV1.class));
	}

	@Test
	void shouldToTeamV1TeamRecordTest() {
		// Given:
		TeamRecord team = Instancio.create(TeamRecord.class);

		// When:
		TeamV1 actualResult = teamApiMapperImpl.toTeamV1(team);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeamV1TeamRecordWithNullTest() {
		// Given:
		TeamRecord team = null;

		// When:
		TeamV1 actualResult = teamApiMapperImpl.toTeamV1(team);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeamLeadAdminViewV1ListListTest() {
		// Given:
		List<TeamRecord> teams = Instancio.ofList(TeamRecord.class).create();
		List<UserRecord> users = Instancio.ofList(UserRecord.class).create();

		// When:
		TeamLeadAdminViewV1 actualResult = teamApiMapperImpl.toTeamLeadAdminViewV1(teams, users);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAddTeamMemberResponseV1UserRecordTest() {
		// Given:
		UserRecord member = Instancio.create(UserRecord.class);

		// When:
		AddTeamMemberResponseV1 actualResult = teamApiMapperImpl.toAddTeamMemberResponseV1(member);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAddTeamMemberResponseV1UserRecordWithNullTest() {
		// Given:
		UserRecord member = null;

		// When:
		AddTeamMemberResponseV1 actualResult = teamApiMapperImpl.toAddTeamMemberResponseV1(member);

		// Then:
		assertThat(actualResult).isNull();
	}

}