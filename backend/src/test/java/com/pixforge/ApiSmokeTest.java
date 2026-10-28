package com.pixforge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeTest {
    @Autowired MockMvc mvc;

    @Test void startsAndServesTemplates() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/templates")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(8));
        mvc.perform(get("/api/templates/travel-postcard")).andExpect(status().isOk()).andExpect(jsonPath("$.requiredImageCount").value(2));
        mvc.perform(get("/api/templates/urban-portrait")).andExpect(status().isOk()).andExpect(jsonPath("$.coverUrl").value("/templates/pexels-bollieball-5189976.jpg"));
    }
}
