package com.aidigital.aionboarding.service.permission.models;

import java.util.Map;

public record PermissionSnapshotRecord(
    String roleCode,
    Map<String, Boolean> effective,
    Map<String, Boolean> overrides
) { }
