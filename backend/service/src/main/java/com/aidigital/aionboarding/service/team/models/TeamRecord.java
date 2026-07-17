package com.aidigital.aionboarding.service.team.models;

import com.aidigital.aionboarding.service.user.models.UserRecord;

import java.util.List;

public record TeamRecord(UserRecord lead, List<UserRecord> members) {

}
