package io.github.kiins3.vnkit.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

/** Registers {@link AdminUnitsController} when {@code vnkit.web.enabled=true} in a servlet web app. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RestController.class)
@ConditionalOnProperty(prefix = "vnkit.web", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VnKitWebProperties.class)
public class VnKitWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AdminUnitsController vnKitAdminUnitsController(VnKitWebProperties properties) {
    return new AdminUnitsController(properties);
  }
}
