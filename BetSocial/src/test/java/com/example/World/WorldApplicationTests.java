package com.example.World;

import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The original smoke test, now backed by the shared PostgreSQL container.
 *
 * It used to be a bare @SpringBootTest, which meant it resolved ${DB_URL} from
 * the ambient environment. On a developer machine with DB_URL exported that
 * quietly pointed the test at the real development database; on CI, where no
 * such variable exists, it failed with "Driver org.postgresql.Driver claims to
 * not accept jdbcUrl, ${DB_URL}".
 *
 * Extending the base class makes it hermetic: same container, same migrations,
 * no dependency on how the machine happens to be configured.
 */
@DisplayName("Application context")
class WorldApplicationTests extends AbstractIntegrationTest {

	@Test
	@DisplayName("loads")
	void contextLoads() {
	}

}
