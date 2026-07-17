package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.DashboardPeriodV1;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardPeriodApiMapperImplTest {

	private final DashboardPeriodApiMapperImpl dashboardPeriodApiMapperImpl = new DashboardPeriodApiMapperImpl();

	@Test
	void shouldMapDashboardPeriodStringTest() {
		// Given:
		String period = "value";

		// When:
		DashboardPeriodV1 actualResult = dashboardPeriodApiMapperImpl.mapDashboardPeriod(period);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldFromDashboardPeriodDashboardPeriodV1Test() {
		// Given:
		DashboardPeriodV1 period = Instancio.create(DashboardPeriodV1.class);

		// When:
		String actualResult = dashboardPeriodApiMapperImpl.fromDashboardPeriod(period);

		// Then:
		assertThat(actualResult).isNotNull();
	}

}