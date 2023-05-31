package com.example.carrentalsystem.Controllers;

import com.example.carrentalsystem.Payload.Request.*;
import com.example.carrentalsystem.Repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
public class ProfileControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    private String userToken;

    @Test()
    @Order(1)
    void addUser() throws Exception {
        SignupRequest signupRequest = new SignupRequest("ProfileTestUser","profiletest@test.pl", "TestPassword", new HashSet<>(List.of("user")));

        if(userRepository.getUserByUsername(signupRequest.getUsername()) == null){
            mvc.perform(post("/api/auth/signup").contentType(APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(signupRequest)))
                    .andExpect(status().isOk())
                    .andReturn();
        } else {
            mvc.perform(post("/api/auth/signup").contentType(APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(signupRequest)))
                    .andExpect(status().isConflict())
                    .andReturn();
        }
    }

    @Test()
    @Order(2)
    void loginUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("ProfileTestUser", "TestPassword");

        MvcResult result = mvc.perform(post("/api/auth/signin").contentType(APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        userToken = new JSONObject(result.getResponse().getContentAsString()).getString("token");
    }

    @Test()
    @Order(3)
    void changeUserPassword() throws Exception {
        ChangePasswordRequest passwordRequest = new ChangePasswordRequest("NewPassword", userToken);

        mvc.perform(post("/api/profile/change-password").contentType(APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test()
    @Order(4)
    void loginUserAfterPasswordChange() throws Exception {
        LoginRequest loginRequest = new LoginRequest("ProfileTestUser", "TestPassword");

        mvc.perform(post("/api/auth/signin").contentType(APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test()
    @Order(5)
    void deleteAll() {
        userRepository.deleteById(userRepository.getUserByToken(userToken).getId());
    }
}
