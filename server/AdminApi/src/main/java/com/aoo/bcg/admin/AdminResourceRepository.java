package com.aoo.bcg.admin;

import java.util.List;
import java.util.Map;

public interface AdminResourceRepository {
    List<Map<String, Object>> list(String resourceType);

    Map<String, Object> execute(String resourceType, String id, long operatorId,
            Map<String, Object> command, String forcedStatus, String ifMatch);
}
