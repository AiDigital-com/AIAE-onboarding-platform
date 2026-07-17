package com.aidigital.aionboarding.service.teamdashboard.models;

public enum TeamDashboardPeriod {
	WEEK("week"),
	MONTH("month"),
	QUARTER("quarter");

	private final String code;

	TeamDashboardPeriod(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}
}
