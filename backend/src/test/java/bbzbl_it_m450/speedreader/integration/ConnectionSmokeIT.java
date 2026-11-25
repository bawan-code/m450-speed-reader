package bbzbl_it_m450.speedreader.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bbzbl_it_m450.speedreader.model.Text;
import bbzbl_it_m450.speedreader.model.User;

/**
 * Smoke test for the integration test setup (Phase 0): if this fails, every other IT fails too.
 * Not one of the documented test cases T-0xx.
 */
class ConnectionSmokeIT extends AbstractMySqlIT {

    @Test
    @DisplayName("Smoke: the database answers")
    void databaseAnswers() {
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Smoke: Flyway applied V1 and created the four tables")
    void flywayCreatedTheSchema() {
        assertThat(jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("1");

        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        assertThat(tables).contains("users", "texts", "reading_sessions", "bookmarks");
    }

    @Test
    @DisplayName("Smoke: the helpers save a user and a text")
    void helpersSaveData() {
        User user = createUser();
        Text text = createText(100);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, user.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT word_count FROM texts WHERE id = ?", Integer.class, text.getId())).isEqualTo(100);
    }
}
