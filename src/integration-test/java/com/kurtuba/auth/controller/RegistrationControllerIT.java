package com.kurtuba.auth.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.auth.AuthApplication;
import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.RegistrationResponseDto;
import com.kurtuba.auth.data.dto.UserDto;
import com.kurtuba.auth.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = AuthApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class RegistrationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper;

    private RegistrationDto registrationDto;


    @BeforeEach
    public void setup() {

        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        registrationDto = TestUtils.defaultRegistrationDtoBuilder();
    }

    @Test
    public void insertExercise_whenGivenValidExercise_thenReturns201AndSavedExercise() throws Exception {

        String jsonVal = mapper.writeValueAsString(registrationDto);

        MvcResult result = mockMvc.perform(post("/registration")
                .contentType(APPLICATION_JSON)
                .content(jsonVal)
                .accept(APPLICATION_JSON)).andReturn();

        assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
        RegistrationResponseDto returenedDto = mapper.readValue(result.getResponse().getContentAsString(), RegistrationResponseDto.class);
        assertNotNull(returenedDto.getUserMetaChangeId());
        UserDto savedUser = returenedDto.getUser();

        assertEquals(registrationDto.getName(),savedUser.getName());
        assertEquals(registrationDto.getSurname(), savedUser.getSurname());
        assertEquals(registrationDto.getUsername(), savedUser.getUsername());
        assertEquals(registrationDto.getEmail(), savedUser.getEmail());
        assertFalse(savedUser.isEmailVerified());
        assertEquals(registrationDto.getMobile(), savedUser.getMobile());
        assertFalse(savedUser.isMobileVerified());
        assertEquals(registrationDto.getAuthProvider(), savedUser.getAuthProvider());
        assertFalse(savedUser.isActivated());
        assertFalse(savedUser.isLocked());
        assertEquals(savedUser.getFailedLoginCount(),0);
        assertFalse(savedUser.isShowCaptcha());
        assertNull(savedUser.getBirthdate());
        assertNotNull(savedUser.getCreatedDate());
    }
}
