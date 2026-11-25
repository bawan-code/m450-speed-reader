package bbzbl_it_m450.speedreader.integration;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import bbzbl_it_m450.speedreader.model.Text;
import bbzbl_it_m450.speedreader.model.User;
import bbzbl_it_m450.speedreader.repository.BookmarkRepository;
import bbzbl_it_m450.speedreader.repository.ReadingSessionRepository;
import bbzbl_it_m450.speedreader.repository.TextRepository;
import bbzbl_it_m450.speedreader.repository.UserRepository;

/**
 * Base class for all LB2 integration tests (*IT): a real MySQL in Docker, the schema from Flyway.
 * Contract: docs/PLAN-LB2.md §4.
 *
 * Do not make test classes or methods @Transactional: Spring would roll the test back at the end
 * and hide exactly the commit/rollback behaviour the tests are meant to show.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractMySqlIT {

    // Singleton container: started once for ALL IT classes, stopped by Testcontainers at JVM end.
    // Not @Container: that would stop it after the first class while Spring's cached context
    // still points to its port -> the next IT class fails with connection errors.
    // Own my.cnf (src/test/resources/mysql-conf): Testcontainers' default config doesn't work with MySQL 9.
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:9.7")
            .withConfigurationOverride("mysql-conf");

    static {
        MYSQL.start();
    }

    private static final AtomicLong SEQUENCE = new AtomicLong();

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected TextRepository texts;

    @Autowired
    protected ReadingSessionRepository sessions;

    @Autowired
    protected BookmarkRepository bookmarks;

    /** Every test starts from empty tables. Children first, because of the foreign keys. */
    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM bookmarks");
        jdbc.update("DELETE FROM reading_sessions");
        jdbc.update("DELETE FROM texts");
        jdbc.update("DELETE FROM users");
    }

    /** A persisted user with a unique username and email. */
    protected User createUser() {
        long n = SEQUENCE.incrementAndGet();
        return users.save(User.builder()
                .username("user" + n)
                .email("user" + n + "@example.com")
                .build());
    }

    /** A persisted text with the given word count. */
    protected Text createText(int wordCount) {
        return texts.save(Text.builder()
                .title("Text " + SEQUENCE.incrementAndGet())
                .content("word ".repeat(wordCount).trim())
                .wordCount(wordCount)
                .build());
    }
}
