package guru.springframework.ghd.security;

import guru.springframework.ghd.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The `/api/v1/**` and `/admin/v1/**` chains are configured as
 * {@code SessionCreationPolicy.STATELESS} (JWT-only, see SecurityConfig). Enabling
 * Spring Session (Redis) globally must not regress that: an anonymous public API
 * call must not receive a `Set-Cookie` for the HttpSession, only ever the JWT cookie
 * set explicitly by the login endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiStatelessSessionTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicApiCallDoesNotCreateAnHttpSession() throws Exception {
        mockMvc.perform(get("/api/v1/category"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
