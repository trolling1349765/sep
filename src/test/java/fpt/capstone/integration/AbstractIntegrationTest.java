package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.entity.User;
import fpt.capstone.repository.PermissionRepository;
import fpt.capstone.repository.RefreshTokenRepository;
import fpt.capstone.repository.RightRepository;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.EmailService;
import fpt.capstone.service.PermissionCacheService;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.mysql.MySQLContainer;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

        // Singleton container: started once per JVM and never stopped between test
        // classes. Deliberately NO @Testcontainers/@Container - the JUnit extension
        // stops static containers after each class, which would leave the cached
        // Spring context pointing at a dead DB for the second IT class. Ryuk reaps
        // the container when the failsafe JVM exits.
        @ServiceConnection
        static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.33");

        static {
                MYSQL.start();
        }

        @Autowired
        protected MockMvc mockMvc;
        @Autowired
        protected UserRepository userRepository;
        @Autowired
        protected RefreshTokenRepository refreshTokenRepository;
        @Autowired
        protected PasswordEncoder passwordEncoder;
        // RBAC fixtures (plain @Autowired fields do not affect the context-cache key)
        @Autowired
        protected RoleRepository roleRepository;
        @Autowired
        protected RightRepository rightRepository;
        @Autowired
        protected PermissionRepository permissionRepository;
        @Autowired
        protected SystemLogRepository systemLogRepository;
        @Autowired
        protected PermissionCacheService permissionCacheService;

        // Shared by BOTH IT classes so the merged context configuration is identical
        // (bean overrides are part of the context cache key). Replaces EmailServiceImpl
        // (real Gmail SMTP). Recorded invocations reset after every test method.
        @MockitoBean
        protected EmailService emailService;

        protected static final String DEFAULT_PASSWORD = "Password1";

        // DB rows, rate-limit buckets and lock counters outlive individual tests
        // (shared context + container), so every test works with unique data.
        private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000);

        protected static String uniq() {
                return Long.toString(SEQ.incrementAndGet());
        }

        protected static String uniqueEmail() {
                return "it" + uniq() + "@example.com";
        }

        protected static String uniquePhone() {
                return "09" + String.format("%08d", SEQ.incrementAndGet() % 100_000_000);
        }

        protected static String uniqueIp() {
                long n = SEQ.incrementAndGet();
                return "10." + (n % 250) + "." + ((n / 250) % 250) + "." + (1 + n % 200);
        }

        protected static String uniqueNationalId() {
                return String.format("%012d", SEQ.incrementAndGet());
        }

        /** POST /auth/register with a valid body; expects 200; returns cookies + body. */
        protected MvcResult registerUser(String email, String phone) throws Exception {
                String body = """
                                {
                                  "fullName": "IT User",
                                  "dateOfBirth": "01/01/2000",
                                  "email": "%s",
                                  "phone": "%s",
                                  "password": "%s",
                                  "passwordConfirmation": "%s"
                                }
                                """.formatted(email, phone, DEFAULT_PASSWORD, DEFAULT_PASSWORD);
                return mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk())
                                .andReturn();
        }

        /** POST /auth/login with X-Forwarded-For = ip; callers assert the status. */
        protected MvcResult loginAs(String credential, String password, String ip) throws Exception {
                String body = """
                                {
                                  "credential": "%s",
                                  "password": "%s"
                                }
                                """.formatted(credential, password);
                return mockMvc.perform(post("/auth/login")
                                .header("X-Forwarded-For", ip)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andReturn();
        }

        protected Cookie cookieOf(MvcResult result, String name) {
                return result.getResponse().getCookie(name);
        }

        /** refresh_token cookie value has the form "<tokenId>:<rawToken>". */
        protected long refreshTokenId(Cookie refreshCookie) {
                return Long.parseLong(refreshCookie.getValue().split(":", 2)[0]);
        }

        protected String refreshTokenRaw(Cookie refreshCookie) {
                return refreshCookie.getValue().split(":", 2)[1];
        }

        protected String userIdFromBody(MvcResult result) throws Exception {
                return JsonPath.read(result.getResponse().getContentAsString(), "$.data.userId");
        }

        /**
         * Registers a fresh account, reassigns it to the given seeded role directly
         * in the DB, and returns the register-response cookies (the filter reloads
         * the role from DB on every request, so the original token stays valid).
         */
        protected MvcResult registerUserWithRole(String roleName) throws Exception {
                MvcResult result = registerUser(uniqueEmail(), uniquePhone());
                String userId = userIdFromBody(result);
                User user = userRepository.getUserById(userId);
                user.setRole(roleRepository.findByName(roleName).orElseThrow());
                userRepository.save(user);
                return result;
        }
}
