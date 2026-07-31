package guru.springframework.ghd.security;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.entities.SysParam;
import guru.springframework.ghd.repositories.SysParamRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private SysParamRepository sysParamRepository;

    @BeforeEach
    void seedHotlineSysParam() {
        // guru.springframework.ghd.controllers.views.GlobalCommon is a global,
        // unscoped @ControllerAdvice applied to every controller including REST ones.
        // Its "hotline" @ModelAttribute calls Optional#get() without an isPresent()
        // check, so ANY /api/v1/** GET 500s on a database that has no "hotline"
        // sys_param row - a pre-existing bug independent of this task. Seed the row
        // so this test can actually exercise the security chain instead of that bug.
        if (sysParamRepository.findByParamKey("hotline").isEmpty()) {
            sysParamRepository.save(SysParam.builder()
                    .paramKey("hotline")
                    .paramValue("0123456789")
                    .paramName("Hotline")
                    .build());
        }
    }

    @Test
    void publicApiCallDoesNotCreateAnHttpSession() throws Exception {
        // /api/v1/category's admin listing hits a native-query + Sort combination that
        // is broken independently of this task (Spring Data JPA does not support
        // dynamic sorting on @Query(nativeQuery = true) methods) - use a simpler public
        // GET under the same STATELESS chain instead.
        mockMvc.perform(get("/api/v1/sys-param"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
