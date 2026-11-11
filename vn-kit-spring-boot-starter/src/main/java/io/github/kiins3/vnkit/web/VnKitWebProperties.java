package io.github.kiins3.vnkit.web;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for the province and ward REST API. */
@ConfigurationProperties(prefix = "vnkit.web")
public class VnKitWebProperties {

  /** Whether to expose the province and ward endpoints. Off by default. */
  private boolean enabled = false;

  /** Path the endpoints are served under. */
  private String basePath = "/api/vn";

  /** Most results a ward search returns, whatever limit the client asks for. */
  private int maxResults = 50;

  /** How long clients may cache responses. The data only changes with a library upgrade. */
  private Duration cacheMaxAge = Duration.ofDays(1);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  public Duration getCacheMaxAge() {
    return cacheMaxAge;
  }

  public void setCacheMaxAge(Duration cacheMaxAge) {
    this.cacheMaxAge = cacheMaxAge;
  }
}
