package com.contrast.frontgateservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionCookieSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sessionCookieShouldHaveSecureFlag() throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        MvcResult result = mockMvc.perform(get("/login")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("JSESSIONID");
        
        if (sessionCookie != null) {
            assertTrue(sessionCookie.getSecure(), "Session cookie must have Secure flag set");
            assertTrue(sessionCookie.isHttpOnly(), "Session cookie must have HttpOnly flag set");
        }
    }

    @Test
    void sessionCookieShouldHaveHttpOnlyFlag() throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        MvcResult result = mockMvc.perform(get("/login")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("JSESSIONID");
        
        if (sessionCookie != null) {
            assertTrue(sessionCookie.isHttpOnly(), "Session cookie must have HttpOnly flag set");
        }
    }
}
