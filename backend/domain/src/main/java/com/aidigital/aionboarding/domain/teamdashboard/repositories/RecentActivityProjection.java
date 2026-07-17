package com.aidigital.aionboarding.domain.teamdashboard.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RecentActivityProjection {

	String getKind();

	LocalDateTime getHappenedAt();

	Long getUserId();

	String getWho();

	String getAvatarStorageKey();

	String getAvatarColor();

	String getWhat();

	BigDecimal getScore();

	Boolean getPassed();
}
