package com.aidigital.aionboarding.service.teamdashboard.models;

import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.util.List;

public record TeamDashboardScope(String label, Long leadId, List<UserRecord> members) {
}
