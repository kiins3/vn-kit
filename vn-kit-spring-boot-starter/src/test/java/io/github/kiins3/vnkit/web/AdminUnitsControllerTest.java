package io.github.kiins3.vnkit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"vnkit.web.enabled=true", "vnkit.web.max-results=5"})
@AutoConfigureMockMvc
class AdminUnitsControllerTest {

  @SpringBootApplication
  static class App {}

  @Autowired MockMvc mvc;

  @Test
  void listsProvinces() throws Exception {
    mvc.perform(get("/api/vn/provinces"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "max-age=86400, public"))
        .andExpect(jsonPath("$", hasSize(34)))
        .andExpect(jsonPath("$[0].code").value("01"))
        .andExpect(jsonPath("$[0].fullName").value("Thành phố Hà Nội"))
        .andExpect(jsonPath("$[0].type").value("CITY"))
        .andExpect(jsonPath("$[0].wards").doesNotExist());
  }

  @Test
  void oneProvinceAndItsWards() throws Exception {
    mvc.perform(get("/api/vn/provinces/79")).andExpect(jsonPath("$.name").value("Hồ Chí Minh"));
    mvc.perform(get("/api/vn/provinces/79/wards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].provinceCode").value("79"));
  }

  @Test
  void oneWard() throws Exception {
    mvc.perform(get("/api/vn/wards/26743"))
        .andExpect(jsonPath("$.fullName").value("Phường Bến Thành"))
        .andExpect(jsonPath("$.type").value("WARD"))
        .andExpect(jsonPath("$.province").doesNotExist());
  }

  @Test
  void searchesWards() throws Exception {
    mvc.perform(get("/api/vn/wards").param("q", "ben thanh"))
        .andExpect(jsonPath("$[0].code").value("26743"));
    mvc.perform(get("/api/vn/wards").param("q", "Hoà Bình").param("province", "tp.hcm"))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].provinceCode").value("79"));
  }

  @Test
  void capsTheNumberOfResults() throws Exception {
    mvc.perform(get("/api/vn/wards").param("q", "an").param("limit", "1000"))
        .andExpect(jsonPath("$", hasSize(5)));
  }

  @Test
  void errors() throws Exception {
    mvc.perform(get("/api/vn/provinces/00")).andExpect(status().isNotFound());
    mvc.perform(get("/api/vn/provinces/00/wards")).andExpect(status().isNotFound());
    mvc.perform(get("/api/vn/wards/99999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/vn/wards").param("q", " ")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/vn/wards").param("q", "ba dinh").param("province", "Atlantis")).andExpect(status().isNotFound());
  }

  @Nested
  class Configuration {

    final WebApplicationContextRunner runner =
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(VnKitWebAutoConfiguration.class));

    @Test
    void offByDefault() {
      runner.run(ctx -> assertThat(ctx).doesNotHaveBean(AdminUnitsController.class));
    }

    @Test
    void onWhenEnabled() {
      runner.withPropertyValues("vnkit.web.enabled=true")
          .run(ctx -> assertThat(ctx).hasSingleBean(AdminUnitsController.class));
    }
  }
}
