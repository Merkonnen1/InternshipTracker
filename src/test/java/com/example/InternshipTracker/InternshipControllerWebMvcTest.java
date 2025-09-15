package com.example.InternshipTracker;

import com.example.InternshipTracker.Controllers.InternshipController;
import com.example.InternshipTracker.services.InternshipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternshipController.class)
@AutoConfigureMockMvc(addFilters = false) // disable Security filters in this slice test
class InternshipControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    // Controller depends on InternshipService; mock it for MVC slice tests
    @MockBean
    private InternshipService internshipService;

    @Test
    void getCreate_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/dashboard/create"))
               .andExpect(status().isOk());
    }
}