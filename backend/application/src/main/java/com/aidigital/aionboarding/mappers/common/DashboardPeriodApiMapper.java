package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.DashboardPeriodV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface DashboardPeriodApiMapper {

    default DashboardPeriodV1 mapDashboardPeriod(String period) {
        if (period == null || period.isBlank()) {
            return DashboardPeriodV1.MONTH;
        }
        try {
            return DashboardPeriodV1.fromValue(period);
        } catch (IllegalArgumentException ex) {
            return DashboardPeriodV1.MONTH;
        }
    }

    default String fromDashboardPeriod(DashboardPeriodV1 period) {
        return period == null ? null : period.getValue();
    }
}
