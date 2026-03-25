package org.ndviet.tests.base;

import com.platform.testframework.testng.PlatformTestNGBase;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import org.ndviet.library.RestAssured;
import org.ndviet.library.configuration.ConfigurationManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import static org.ndviet.library.configuration.Constants.API_AUTH_BEARER_TOKEN;
import static org.ndviet.library.configuration.Constants.API_BASE_URL;

public abstract class BaseApiTest extends PlatformTestNGBase {

  protected static final String BASE_URL =
      ConfigurationManager.getInstance().getValue(API_BASE_URL);

  protected static final String BEARER_TOKEN =
      ConfigurationManager.getInstance().getValue(API_AUTH_BEARER_TOKEN);

  @BeforeMethod(alwaysRun = true)
  public void openApiSession(Method method) {
    log.step("Open API session | " + method.getName());
    RestAssured.openSessionWithBearerToken(BASE_URL, BEARER_TOKEN);
    log.endStep();
  }

  @AfterMethod(alwaysRun = true)
  public void closeApiSession() {
    log.step("Close API session");
    RestAssured.closeSession();
    log.endStep();
  }

  protected String uniqueSku() {
    return "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US);
  }

  protected String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase(Locale.US) + "@example.com";
  }
}
