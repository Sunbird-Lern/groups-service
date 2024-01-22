package org.sunbird.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sunbird.cache.util.Platform;
import org.sunbird.common.util.JsonKey;

public class ProjectUtilTest {

  @Test
  public void testGetConfigValueWithExistsInPropertyFile() {
    String exists = Platform.getBoolean(JsonKey.SUNBIRD_HEALTH_CHECK_ENABLE, false).toString();
    assertEquals("true", exists);
  }

  @Test
  public void testGetConfigValueWithNotExistsInPropertyFile() {
    Boolean exists = Platform.getBoolean("sunbird_health_check_not_enable", false);
    assertFalse(exists);
  }
}
