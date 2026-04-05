package com.company.platform.shared.cache;

import java.io.Serializable;
import java.util.UUID;

public record CachedUserInfo(UUID userId, String fullName, String email, String tenantId)
    implements Serializable {}
