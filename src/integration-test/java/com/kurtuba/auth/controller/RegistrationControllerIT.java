package com.kurtuba.auth.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.AuthApplication;
import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.RegistrationResponseDto;
import com.kurtuba.auth.data.dto.UserDto;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.data.repository.RoleRepository;
import com.kurtuba.auth.support.PostgresIntegrationTestSupport;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = AuthApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class RegistrationControllerIT extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @Autowired
    private LocalizationSupportedLangRepository localizationSupportedLangRepository;

    @Autowired
    private RoleRepository roleRepository;

    private ObjectMapper mapper;

    private RegistrationDto registrationDto;


    @BeforeEach
    public void setup() {

        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        registrationDto = TestUtils.defaultRegistrationDtoBuilder();
        registrationDto.setPreferredVerificationContact(ContactType.MOBILE);
        localizationSupportedLangRepository
                .findByLanguageCode(registrationDto.getLanguageCode())
                .orElseGet(() -> localizationSupportedLangRepository.save(LocalizationSupportedLang.builder()
                        .languageCode(registrationDto.getLanguageCode())
                        .createdDate(Instant.now())
                        .build()));
        localizationSupportedCountryRepository
                .findByCountryCode(registrationDto.getCountryCode())
                .orElseGet(() -> localizationSupportedCountryRepository.save(LocalizationSupportedCountry.builder()
                        .countryCode(registrationDto.getCountryCode())
                        .createdDate(Instant.now())
                        .build()));
        roleRepository.findByName(AuthoritiesType.USER.name())
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(AuthoritiesType.USER.name())
                        .build()));
    }

    @Test
    public void insertExercise_whenGivenValidExercise_thenReturns201AndSavedExercise() throws Exception {

        String jsonVal = mapper.writeValueAsString(registrationDto);

        MvcResult result = mockMvc.perform(post("/auth/registration")
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

    @Test
    public void register_whenUsingMobileOnly_thenReturns201AndSavesUserWithoutEmail() throws Exception {
        registrationDto.setEmail(null);
        registrationDto.setPreferredVerificationContact(ContactType.MOBILE);

        String jsonVal = mapper.writeValueAsString(registrationDto);

        MvcResult result = mockMvc.perform(post("/auth/registration")
                .contentType(APPLICATION_JSON)
                .content(jsonVal)
                .accept(APPLICATION_JSON)).andReturn();

        assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());
        RegistrationResponseDto returnedDto = mapper.readValue(result.getResponse().getContentAsString(), RegistrationResponseDto.class);
        assertNotNull(returnedDto.getUserMetaChangeId());

        UserDto savedUser = returnedDto.getUser();
        assertNull(savedUser.getEmail());
        assertEquals(registrationDto.getMobile(), savedUser.getMobile());
        assertFalse(savedUser.isMobileVerified());
        assertFalse(savedUser.isActivated());
    }
}
