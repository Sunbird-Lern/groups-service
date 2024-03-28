package org.sunbird.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sunbird.cache.util.Platform;
import org.sunbird.common.util.JsonKey;

public class ProjectUtilTest {

  @Test
  public void testGetConfigValueWithExistsInPropertyFile() {
    String exists = String.valueOf(Platform.config.getBoolean(JsonKey.SUNBIRD_HEALTH_CHECK_ENABLE));
    assertEquals("true", exists);
  }

  @Test
  public void testGetConfigValueWithNotExistsInPropertyFile() {
    Boolean exists = Platform.config.getBoolean("sunbird_health_check_not_enable");
    assertFalse(exists);
  }
}
