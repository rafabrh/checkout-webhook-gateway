package com.shkgroups;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AgentApplication.class)
@ActiveProfiles("dev")
class AgentApplicationTests {

    @Test
    void contextLoads() {
    }
}
