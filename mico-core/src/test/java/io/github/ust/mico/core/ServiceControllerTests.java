package io.github.ust.mico.core;

import io.github.ust.mico.core.REST.ServiceController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.CoreMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(ServiceController.class)
@OverrideAutoConfiguration(enabled = true) //Needed to override our neo4j config
public class ServiceControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ServiceRepository serviceRepository;

    @Test
    public void getCompleteServiceList() throws Exception {
        given(serviceRepository.findAll()).willReturn(
                Arrays.asList(
                        new Service("ShortName1", "1.0.1", "Test service"),
                        new Service("ShortName1", "1.0.0", "Test service"),
                        new Service("ShortName2", "1.0.0", "Test service2")));

        mvc.perform(get("/services").accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$._embedded.serviceList[*]", hasSize(3)))
                .andExpect(jsonPath("$._embedded.serviceList[?(@.shortName =='ShortName1' && @.version == '1.0.0' && @.description == 'Test service' )]", hasSize(1)))
                .andExpect(jsonPath("$._embedded.serviceList[?(@.shortName =='ShortName1' && @.version == '1.0.1' && @.description == 'Test service' )]", hasSize(1)))
                .andExpect(jsonPath("$._embedded.serviceList[?(@.shortName =='ShortName2' && @.version == '1.0.0' && @.description == 'Test service2' )]", hasSize(1)))
                .andExpect(jsonPath("$._links.self.href", is("http://localhost/services")))
                .andReturn();
    }

    @Test
    public void getServiceViaShortNameAndVersion() throws Exception {
        given(serviceRepository.findByShortNameAndVersion("ShortName1", "1.0.1")).willReturn(
                Optional.of(new Service("ShortName1", "1.0.1", "Test service")));

        mvc.perform(get("/services/ShortName1/1.0.1").accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.description", is("Test service")))
                .andExpect(jsonPath("$.shortName", is("ShortName1")))
                .andExpect(jsonPath("$.version", is("1.0.1")))
                .andExpect(jsonPath("$._links.self.href", is("http://localhost/services/ShortName1/1.0.1")))
                .andExpect(jsonPath("$._links.services.href", is("http://localhost/services")))
                .andReturn();
    }


}
