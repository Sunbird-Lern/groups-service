#!/bin/bash

export sunbird_health_check_enable=true
export sunbird_us_system_setting_url=/api/data/v1/system/settings/list
export sunbird_us_org_read_url=/v1/org/read
export enable_userid_redis_cache=true
export groups_redis_ttl=86400
export user_redis_ttl=3600
export max_group_members_limit=150
export max_activity_limit=20
export max_group_limit=50
export sunbird_user_service_search_url=/private/api/user/v1/search