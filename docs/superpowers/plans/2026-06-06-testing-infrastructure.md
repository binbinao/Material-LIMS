# Testing Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a working test foundation for Material LIMS (H2 unit + Testcontainers PG integration, Vitest frontend, reusable TestXxxConfig mocks, GitHub Actions smoke workflow) without writing any business test logic.

**Architecture:** Six backend modules get test dependencies and one example test each (Mockito for unit, Testcontainers for integration). Frontend gets Vitest + RTL + MSW with three example tests covering `access.ts`, `app.tsx`, and a service. CI runs `mvn verify` + `npm run test:run` on PR/push to main; no coverage, no secrets, no deploy.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5.10, Mockito 5.11, AssertJ 3.25, Testcontainers 1.19, WireMock 3.5, Vitest 1.6, React Testing Library 16, MSW 2.2, JaCoCo 0.8.11, GitHub Actions.

---

## File Structure

| Path | Action | Purpose |
|------|--------|---------|
| `pom.xml` | modify | root: dependencyManagement for test libs + surefire/failsafe/jacoco plugins |
| `lims-common/pom.xml` | modify | + spring-boot-starter-test, assertj-core, junit-jupiter |
| `lims-common/src/test/java/com/lims/common/util/HolidayCalendarTest.java` | create | unit test: business-day math |
| `lims-common/src/test/java/com/lims/common/security/JwtTokenProviderTest.java` | create | unit test: round-trip token |
| `lims-dao/pom.xml` | modify | + h2 (test), testcontainers-bom, testcontainers-postgresql, testcontainers-junit-jupiter |
| `lims-dao/src/test/java/com/lims/dao/AbstractDaoIT.java` | create | Testcontainers PG base class |
| `lims-dao/src/test/java/com/lims/dao/BrandMapperIT.java` | create | insert + selectById against real PG |
| `lims-dao/src/test/java/com/lims/dao/H2SchemaIT.java` | create | smoke: H2 schema loads and `brand` table is queryable |
| `lims-dao/src/test/resources/schema-h2.sql` | create | H2-flavoured schema (mechanical transformation of `schema.sql`) |
| `lims-dao/src/test/resources/lims-web/db/schema.sql` | create | copy of production schema for Testcontainers |
| `lims-service/pom.xml` | modify | + assertj-core, mockito-junit-jupiter |
| `lims-service/src/test/java/com/lims/service/BrandServiceTest.java` | create | mock BrandMapper; verify listBrands + createBrand |
| `lims-service/src/test/java/com/lims/service/ReportServiceTest.java` | create | mock ReportMapper; verify list + status filter |
| `lims-workflow/pom.xml` | modify | + spring-boot-starter-test, testcontainers-postgresql |
| `lims-workflow/src/test/java/com/lims/workflow/WorkflowServiceTest.java` | create | mock RuntimeService/TaskService; verify startProcess + completeTask |
| `lims-admin/pom.xml` | modify | + spring-boot-starter-test (no test source — module has zero Java) |
| `lims-web/pom.xml` | modify | + spring-security-test, wiremock-standalone |
| `lims-web/src/test/resources/application-test.yml` | create | test profile: H2 default, no driver-class-name, Flowable sync, mockable endpoints |
| `lims-web/src/test/java/com/lims/web/AbstractIntegrationTest.java` | create | @SpringBootTest base with Testcontainers PG + @Tag("integration") |
| `lims-web/src/test/java/com/lims/web/controller/AuthControllerIT.java` | create | smoke: GET /api/v1/auth/azure-ad-login is reachable without auth |
| `lims-web/src/test/java/com/lims/web/controller/BrandControllerIT.java` | create | smoke: GET /api/v1/brands returns 200 unauthenticated |
| `lims-web/src/test/java/com/lims/web/config/TestSecurityConfig.java` | create | @TestConfiguration providing a no-op JwtDecoder bean |
| `lims-web/src/test/java/com/lims/web/config/TestMinioConfig.java` | create | @TestConfiguration with @MockBean MinioClient |
| `lims-web/src/test/java/com/lims/web/config/TestGraphConfig.java` | create | @TestConfiguration with WireMockExtension for graph.microsoft.com |
| `lims-web/src/test/java/com/lims/web/config/TestExternalApiConfig.java` | create | @TestConfiguration with @MockBean for ExternalApiService |
| `lims-web-ui/package.json` | modify | + vitest, RTL, MSW deps; + test/test:run/test:ui/test:coverage scripts |
| `lims-web-ui/vitest.config.ts` | create | jsdom env, @/ alias, setup file |
| `lims-web-ui/src/test/setup.ts` | create | jest-dom + MSW lifecycle |
| `lims-web-ui/src/test/server.ts` | create | setupServer() |
| `lims-web-ui/src/test/handlers.ts` | create | default MSW handlers for /api/v1/auth/me, /api/v1/brands |
| `lims-web-ui/src/test/factories.ts` | create | brandFactory, requestFactory, userFactory |
| `lims-web-ui/src/test/renderWithProviders.tsx` | create | render() wrapper with antd ConfigProvider + Umi access context |
| `lims-web-ui/src/access.test.ts` | create | 3 role-checks |
| `lims-web-ui/src/app.test.tsx` | create | 1 case: getInitialState returns currentUser |
| `lims-web-ui/src/services/requestService.test.ts` | create | 2 cases: getBrands, getRequests via MSW |
| `.github/workflows/test.yml` | create | GH Actions: backend (mvn verify) + frontend (npm run test:run) |
| `docs/testing/README.md` | create | how to run tests, naming/tag conventions |
| `docs/testing/mock-strategy.md` | create | TestXxxConfig usage |
| `CODEBUDDY.md` | modify | append "Testing" section; fix `npm test` line to vitest |

**Total: 1 root + 6 module pom modifications, 16 new Java files, 2 schema files, 1 application-test.yml, 9 new frontend files, 1 GH workflow, 2 docs, 1 CODEBUDDY.md edit = ~39 files.**

---

## Parallelism Notes

- Tasks 2–6 (per-module backend) are **independent** — different directories, no shared files. Safe to parallelize via subagents.
- Tasks 10–12 (frontend) are **sequential** (each adds files the next depends on) but **independent of Tasks 2–9** (different repo subtree).
- Task 9 (TestXxxConfig classes) is **independent of Tasks 2–8** except it shares `lims-web/src/test/java/.../config/`.
- Tasks 13–16 (CI + docs) are **independent of each other and of all implementation tasks** — they can be done in parallel once Task 1 (root pom) and Tasks 7/10/11 (their upstream deps) are done.

A reasonable parallel subagent plan:
- **Agent A**: Tasks 1 → 2 → 3 → 4 → 5 → 6 (backend foundation chain)
- **Agent B**: Tasks 7 → 8 → 9 (lims-web chain)
- **Agent C**: Tasks 10 → 11 → 12 (frontend chain)
- **Agent D** (after A & B): Tasks 13, 14, 15, 16 (CI + docs)

For single-executor inline mode, follow the numbered order.

---

## Task 1: Root pom.xml — surefire + failsafe + jacoco + test deps

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add test property versions**

In `<properties>`, add:

```xml
        <junit-jupiter.version>5.10.2</junit-jupiter.version>
        <mockito.version>5.11.0</mockito.version>
        <assertj.version>3.25.3</assertj.version>
        <testcontainers.version>1.19.7</testcontainers.version>
        <wiremock.version>3.5.4</wiremock.version>
        <jacoco.version>0.8.11</jacoco.version>
```

- [ ] **Step 2: Add test dep entries in dependencyManagement**

Inside `<dependencyManagement><dependencies>`, just BEFORE the `<!-- MyBatis-Plus -->` comment, insert:

```xml
            <!-- Testing -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit-jupiter.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>${mockito.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-junit-jupiter</artifactId>
                <version>${mockito.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.assertj</groupId>
                <artifactId>assertj-core</artifactId>
                <version>${assertj.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.wiremock</groupId>
                <artifactId>wiremock-standalone</artifactId>
                <version>${wiremock.version}</version>
                <scope>test</scope>
            </dependency>
```

- [ ] **Step 3: Add the three plugins**

Inside `<build><plugins>`, AFTER the existing `maven-compiler-plugin` entry, add:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludedGroups>integration</excludedGroups>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <includes>
                        <include>**/*IT.java</include>
                    </includes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals><goal>prepare-agent</goal></goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>verify</phase>
                        <goals><goal>report</goal></goals>
                    </execution>
                </executions>
            </plugin>
```

- [ ] **Step 4: Verify build still resolves**

Run: `./mvnw -B -q -DskipTests validate`
Expected: exit 0, no errors.

- [ ] **Step 5: Commit**

```bash
git add pom.xml
git commit -m "build: add surefire/failsafe/jacoco + test deps in dependencyManagement"
```

---
## Task 2: lims-common — pom + 2 example unit tests

**Files:**
- Modify: `lims-common/pom.xml`
- Create: `lims-common/src/test/java/com/lims/common/util/HolidayCalendarTest.java`
- Create: `lims-common/src/test/java/com/lims/common/security/JwtTokenProviderTest.java`

- [ ] **Step 1: Add deps to lims-common/pom.xml**

Append inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>
```

- [ ] **Step 2: Create HolidayCalendarTest**

Create the file with the following content:

```java
package com.lims.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HolidayCalendarTest {

    private static final Set<LocalDate> HOLIDAYS = Set.of(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 5, 1)
    );

    @Test
    @DisplayName("weekday in a holiday-free week is a business day")
    void weekdayInPlainWeek() {
        assertThat(HolidayCalendar.isBusinessDay(LocalDate.of(2026, 1, 6), HOLIDAYS)).isTrue();
    }

    @Test
    @DisplayName("Saturday is not a business day")
    void saturdayIsNotBusinessDay() {
        assertThat(HolidayCalendar.isBusinessDay(LocalDate.of(2026, 1, 3), HOLIDAYS)).isFalse();
    }

    @Test
    @DisplayName("a weekday on a national holiday is not a business day")
    void holidayOnWeekdayIsNotBusinessDay() {
        assertThat(HolidayCalendar.isBusinessDay(LocalDate.of(2026, 5, 1), HOLIDAYS)).isFalse();
    }

    @Test
    @DisplayName("addBusinessDays skips weekends and holidays")
    void addBusinessDaysSkipsNonBusinessDays() {
        // 2026-04-30 (Thu) + 2 business days → 2026-05-04 (Mon, skipping 5/1 holiday)
        LocalDate result = HolidayCalendar.addBusinessDays(LocalDate.of(2026, 4, 30), 2, HOLIDAYS);
        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    @DisplayName("addBusinessDays with days=0 returns the base date unchanged")
    void addBusinessDaysZeroReturnsBase() {
        assertThat(HolidayCalendar.addBusinessDays(LocalDate.of(2026, 4, 30), 0, HOLIDAYS))
                .isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("addBusinessDays rejects negative day counts")
    void addBusinessDaysRejectsNegative() {
        assertThatThrownBy(() -> HolidayCalendar.addBusinessDays(LocalDate.of(2026, 4, 30), -1, HOLIDAYS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("countBusinessDays counts only weekdays not in the holiday set")
    void countBusinessDays() {
        // 2026-04-27 (Mon) to 2026-05-05 (Tue): 9 calendar days, 7 weekdays, 1 holiday = 6
        int count = HolidayCalendar.countBusinessDays(
                LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 5), HOLIDAYS);
        assertThat(count).isEqualTo(6);
    }
}
```

- [ ] **Step 3: Run HolidayCalendarTest — expect pass**

Run: `./mvnw -B -pl lims-common test -Dtest=HolidayCalendarTest`
Expected: 7 tests pass, `BUILD SUCCESS`.

- [ ] **Step 4: Create JwtTokenProviderTest**

Create the file with the following content:

```java
package com.lims.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-32-bytes-min-len-required-1234";

    @Test
    @DisplayName("generated token round-trips userId, email, roles, deptId")
    void roundTrip() {
        JwtTokenProvider p = newProvider();
        String token = p.generate("u-1", "alice@example.com", "Alice", "ADMIN,ENGINEER", "d-9");
        JwtTokenProvider.AuthPrincipal principal = p.parse(token);

        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo("u-1");
        assertThat(principal.email()).isEqualTo("alice@example.com");
        assertThat(principal.displayName()).isEqualTo("Alice");
        assertThat(principal.roles()).isEqualTo("ADMIN,ENGINEER");
        assertThat(principal.deptId()).isEqualTo("d-9");
    }

    @Test
    @DisplayName("garbage token returns null instead of throwing")
    void garbageTokenReturnsNull() {
        JwtTokenProvider p = newProvider();
        assertThat(p.parse("not-a-jwt")).isNull();
        assertThat(p.parse("")).isNull();
        assertThat(p.parse(null)).isNull();
    }

    @Test
    @DisplayName("hasRole matches case-insensitively across the comma list")
    void hasRoleIsCaseInsensitive() {
        JwtTokenProvider p = newProvider();
        String token = p.generate("u-2", "b@x", "Bob", "manager,ENGINEER", null);
        JwtTokenProvider.AuthPrincipal principal = p.parse(token);

        assertThat(principal).isNotNull();
        assertThat(principal.hasRole("MANAGER")).isTrue();
        assertThat(principal.hasRole("engineer")).isTrue();
        assertThat(principal.hasRole("ADMIN")).isFalse();
    }

    private static JwtTokenProvider newProvider() {
        JwtTokenProvider p = new JwtTokenProvider();
        ReflectionTestUtils.setField(p, "secret", SECRET);
        ReflectionTestUtils.setField(p, "ttlHours", 8L);
        return p;
    }
}
```

- [ ] **Step 5: Run all lims-common tests — expect pass**

Run: `./mvnw -B -pl lims-common test`
Expected: 10 tests pass, `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add lims-common/pom.xml lims-common/src/test
git commit -m "test(lims-common): add HolidayCalendar and JwtTokenProvider unit tests"
```

---
## Task 3: lims-dao — pom + AbstractDaoIT + BrandMapperIT + H2SchemaIT + schemas

**Files:**
- Modify: `lims-dao/pom.xml`
- Create: `lims-dao/src/test/java/com/lims/dao/AbstractDaoIT.java`
- Create: `lims-dao/src/test/java/com/lims/dao/BrandMapperIT.java`
- Create: `lims-dao/src/test/java/com/lims/dao/H2SchemaIT.java`
- Create: `lims-dao/src/test/resources/schema-h2.sql`
- Create: `lims-dao/src/test/resources/lims-web/db/schema.sql` (copy of production schema)

- [ ] **Step 1: Add deps to lims-dao/pom.xml**

Append inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>
```

- [ ] **Step 2: Copy production schema into lims-dao test resources**

Run:

```bash
mkdir -p lims-dao/src/test/resources/lims-web/db
cp lims-web/src/main/resources/db/schema.sql lims-dao/src/test/resources/lims-web/db/schema.sql
```

- [ ] **Step 3: Create AbstractDaoIT (Testcontainers PG base)**

Create `lims-dao/src/test/java/com/lims/dao/AbstractDaoIT.java`:

```java
package com.lims.dao;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class AbstractDaoIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("lims_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations",
                () -> "classpath:lims-web/db/schema.sql");
    }
}
```

- [ ] **Step 4: Create BrandMapperIT**

Create `lims-dao/src/test/java/com/lims/dao/BrandMapperIT.java`:

```java
package com.lims.dao;

import com.lims.dao.mapper.BrandMapper;
import com.lims.model.entity.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class BrandMapperIT extends AbstractDaoIT {

    @Autowired
    BrandMapper brandMapper;

    @Test
    @DisplayName("insert then selectById returns the same row")
    void insertAndSelect() {
        Brand brand = new Brand();
        brand.setName("TestBrand-" + System.nanoTime());
        brand.setSortOrder(1);

        brandMapper.insert(brand);
        assertThat(brand.getId()).isNotBlank();

        Brand loaded = brandMapper.selectById(brand.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo(brand.getName());
        assertThat(loaded.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("logical delete sets deleted_at; selectById then returns null")
    void logicalDelete() {
        Brand brand = new Brand();
        brand.setName("ToDelete-" + System.nanoTime());
        brandMapper.insert(brand);

        brandMapper.deleteById(brand.getId());

        assertThat(brandMapper.selectById(brand.getId())).isNull();
    }
}
```

- [ ] **Step 5: Run BrandMapperIT against Testcontainers — needs Docker**

Run: `./mvnw -B -pl lims-dao verify`
Expected (with Docker available): downloads `postgres:15-alpine` on first run (~80MB), 2 tests pass.
If Docker is **not** available, skip and note it. CI will validate.

- [ ] **Step 6: Generate schema-h2.sql**

Run:

```bash
sed 's/DEFAULT NOW()/DEFAULT CURRENT_TIMESTAMP/g' \
  lims-web/src/main/resources/db/schema.sql \
  > lims-dao/src/test/resources/schema-h2.sql
```

Then prepend a header comment. Edit `lims-dao/src/test/resources/schema-h2.sql` to start with:

```sql
-- H2-flavoured schema for service-layer unit tests (@DataJpaTest and similar).
-- Mechanical transformation of lims-web/src/main/resources/db/schema.sql (PostgreSQL 15).
-- NOT consumed by any test in sub-project A; staged here so sub-project B (business
-- unit tests) does not regenerate it. See docs/superpowers/specs/2026-06-06-testing-infrastructure-design.md §4.3.
-- Transformations:
--   * TIMESTAMP DEFAULT NOW()         -> TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--   * Other PG-specific features (CHECK, partial indexes, IF NOT EXISTS) are
--     already supported by H2 2.x; left unchanged.

```

Verify: `grep -c "DEFAULT NOW()" lims-dao/src/test/resources/schema-h2.sql` must output `0`.

- [ ] **Step 7: Create H2SchemaIT (smoke that H2 schema parses and runs)**

Create `lims-dao/src/test/java/com/lims/dao/H2SchemaIT.java`:

```java
package com.lims.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Tag("integration")
@Sql(scripts = "/schema-h2.sql")
class H2SchemaIT {

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("H2 schema loads and `brand` table is queryable")
    void brandTableExists() throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM brand")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isZero();
        }
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add lims-dao/pom.xml lims-dao/src/test
git commit -m "test(lims-dao): add Testcontainers base + BrandMapperIT + H2SchemaIT + schemas"
```

---

## Task 4: lims-service — pom + BrandServiceTest + ReportServiceTest

**Files:**
- Modify: `lims-service/pom.xml`
- Create: `lims-service/src/test/java/com/lims/service/BrandServiceTest.java`
- Create: `lims-service/src/test/java/com/lims/service/ReportServiceTest.java`

- [ ] **Step 1: Add deps to lims-service/pom.xml**

Append inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
        </dependency>
```

- [ ] **Step 2: Create BrandServiceTest**

Create `lims-service/src/test/java/com/lims/service/BrandServiceTest.java`:

```java
package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.BrandMapper;
import com.lims.model.entity.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock BrandMapper brandMapper;
    @InjectMocks BrandService brandService;

    @Test
    @DisplayName("listBrands(0, 10) coerces page<=0 to current=1 and forwards to mapper")
    void listBrandsCoercesPage() {
        Page<Brand> stub = new Page<>(1, 10);
        when(brandMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(stub);

        Page<Brand> result = brandService.listBrands(0, 10);

        assertThat(result).isSameAs(stub);
        verify(brandMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listAll returns mapper results ordered by sortOrder")
    void listAllDelegatesToMapper() {
        Brand a = new Brand(); a.setName("A"); a.setSortOrder(1);
        Brand b = new Brand(); b.setName("B"); b.setSortOrder(2);
        when(brandMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));

        List<Brand> result = brandService.listAll();

        assertThat(result).containsExactly(a, b);
    }

    @Test
    @DisplayName("createBrand delegates insert and returns the same instance")
    void createBrandReturnsEntity() {
        Brand brand = new Brand();
        brand.setName("NewBrand");

        Brand result = brandService.createBrand(brand);

        assertThat(result).isSameAs(brand);
        verify(brandMapper).insert(brand);
    }
}
```

- [ ] **Step 3: Run BrandServiceTest — expect pass**

Run: `./mvnw -B -pl lims-service test -Dtest=BrandServiceTest`
Expected: 3 tests pass.

- [ ] **Step 4: Create ReportServiceTest**

Create `lims-service/src/test/java/com/lims/service/ReportServiceTest.java`:

```java
package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.ReportMapper;
import com.lims.model.entity.Report;
import com.lims.service.report.ReportTemplateService;
import com.lims.service.report.WordToPdfConverter;
import com.lims.service.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ReportMapper reportMapper;
    @Mock AnalysisTaskMapper analysisTaskMapper;
    @Mock ReportTemplateService reportTemplateService;
    @Mock WordToPdfConverter wordToPdfConverter;
    @Mock FileStorageService fileStorageService;

    private ReportService newService() {
        return new ReportService(reportMapper, analysisTaskMapper,
                reportTemplateService, wordToPdfConverter, fileStorageService);
    }

    @Test
    @DisplayName("list applies status and requestId filters, ordered by createdAt desc")
    void listAppliesFilters() {
        Page<Report> stub = new Page<>(1, 10);
        when(reportMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(stub);

        newService().list(1, 10, "DRAFT", "req-1");

        verify(reportMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getById returns mapper result unchanged")
    void getByIdDelegates() {
        Report r = new Report();
        r.setId("r-1");
        when(reportMapper.selectById("r-1")).thenReturn(r);

        Report result = newService().getById("r-1");

        assertThat(result).isSameAs(r);
    }
}
```

- [ ] **Step 5: Run ReportServiceTest — expect pass**

Run: `./mvnw -B -pl lims-service test -Dtest=ReportServiceTest`
Expected: 2 tests pass.

- [ ] **Step 6: Commit**

```bash
git add lims-service/pom.xml lims-service/src/test
git commit -m "test(lims-service): add BrandService and ReportService unit tests"
```

---

## Task 5: lims-workflow — pom + WorkflowServiceTest

**Files:**
- Modify: `lims-workflow/pom.xml`
- Create: `lims-workflow/src/test/java/com/lims/workflow/WorkflowServiceTest.java`

- [ ] **Step 1: Add deps to lims-workflow/pom.xml**

Append inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
        </dependency>
```

- [ ] **Step 2: Create WorkflowServiceTest**

Create `lims-workflow/src/test/java/com/lims/workflow/WorkflowServiceTest.java`:

```java
package com.lims.workflow;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock RuntimeService runtimeService;
    @Mock TaskService taskService;
    @InjectMocks WorkflowService workflowService;

    @Test
    @DisplayName("startProcess forwards variables to Flowable and returns the processInstanceId")
    void startProcessReturnsInstanceId() {
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("pi-1");
        when(runtimeService.startProcessInstanceByKey(eq("requestProcess"), eq("req-1"), any(Map.class)))
                .thenReturn(instance);

        String id = workflowService.startProcess("req-1", "u-1");

        assertThat(id).isEqualTo("pi-1");
    }

    @Test
    @DisplayName("completeTask throws when no task matches the assignee or candidate user")
    void completeTaskRequiresAssignment() {
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t-1")).thenReturn(q);
        when(q.taskAssignee("u-1")).thenReturn(q);
        when(q.singleResult()).thenReturn(null);
        when(q.taskCandidateUser("u-1")).thenReturn(q);

        assertThatThrownBy(() -> workflowService.completeTask("t-1", "u-1", Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("t-1");
    }

    @Test
    @DisplayName("completeTask calls taskService.complete when assigned")
    void completeTaskHappyPath() {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t-1")).thenReturn(q);
        when(q.taskAssignee("u-1")).thenReturn(q);
        when(q.singleResult()).thenReturn(task);

        workflowService.completeTask("t-1", "u-1", Map.of("decision", "approve"));

        verify(taskService).complete("t-1", Map.of("decision", "approve"));
    }
}
```

- [ ] **Step 3: Run — expect pass**

Run: `./mvnw -B -pl lims-workflow test`
Expected: 3 tests pass.

- [ ] **Step 4: Commit**

```bash
git add lims-workflow/pom.xml lims-workflow/src/test
git commit -m "test(lims-workflow): add WorkflowService unit tests with mocked Flowable services"
```

---

## Task 6: lims-admin — pom only (no source code)

**Files:**
- Modify: `lims-admin/pom.xml`

- [ ] **Step 1: Add deps to lims-admin/pom.xml**

Append inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
```

- [ ] **Step 2: Verify build still resolves**

Run: `./mvnw -B -q -pl lims-admin -am -DskipTests validate`
Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add lims-admin/pom.xml
git commit -m "build(lims-admin): add spring-boot-starter-test (placeholder, module has no Java)"
```

---

## Task 7: lims-web — pom + application-test.yml

**Files:**
- Modify: `lims-web/pom.xml`
- Create: `lims-web/src/test/resources/application-test.yml`

- [ ] **Step 1: Add deps to lims-web/pom.xml**

Append inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock-standalone</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>
```

- [ ] **Step 2: Create application-test.yml**

Create `lims-web/src/test/resources/application-test.yml`:

```yaml
# Test profile for lims-web integration tests.
# H2 is the default datasource; AbstractIntegrationTest overrides to Testcontainers PG via
# @DynamicPropertySource. spring.datasource.driver-class-name is intentionally omitted so
# Spring Boot auto-detects from the JDBC URL prefix (H2 or PG).
spring:
  datasource:
    url: jdbc:h2:mem:lims_web_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
  sql:
    init:
      mode: never   # tests load their own fixtures
  data:
    redis:
      host: localhost
      port: 16379   # intentionally unused; cache falls back to in-memory

flowable:
  database-schema-update: true
  async-executor-activate: false   # determinism in tests

azure:
  ad:
    tenant-id: placeholder
    client-id: placeholder
    client-secret: placeholder
    redirect-uri: https://localhost:8080/api/v1/auth/callback

minio:
  endpoint: http://localhost:0
  access-key: test
  secret-key: test
  bucket: lims-test

external:
  api:
    parts:
      base-url: ""
    suppliers:
      base-url: ""
    mock:
      enabled: true   # external clients return mock data; no real HTTP

logging:
  level:
    com.lims: DEBUG
    org.flowable: WARN
    org.springframework.security: WARN
```

- [ ] **Step 3: Verify build still resolves**

Run: `./mvnw -B -q -pl lims-web -am -DskipTests validate`
Expected: exit 0.

- [ ] **Step 4: Commit**

```bash
git add lims-web/pom.xml lims-web/src/test/resources
git commit -m "test(lims-web): add test deps and application-test.yml"
```

---

## Task 8: lims-web — AbstractIntegrationTest + 2 IT smoke tests

**Files:**
- Create: `lims-web/src/test/java/com/lims/web/AbstractIntegrationTest.java`
- Create: `lims-web/src/test/java/com/lims/web/controller/AuthControllerIT.java`
- Create: `lims-web/src/test/java/com/lims/web/controller/BrandControllerIT.java`

- [ ] **Step 1: Create AbstractIntegrationTest**

Create `lims-web/src/test/java/com/lims/web/AbstractIntegrationTest.java`:

```java
package com.lims.web;

import com.lims.web.config.TestSecurityConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for all lims-web integration tests. Boots the full Spring Boot context with
 * Testcontainers PostgreSQL 15. Subclasses get MockMvc (via @AutoConfigureMockMvc
 * on the subclass) and may import additional TestXxxConfig classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(TestSecurityConfig.class)
@Tag("integration")
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("lims_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations",
                () -> "classpath:lims-web/db/schema.sql");
    }
}
```

- [ ] **Step 2: Copy production schema into lims-web test resources**

Run:

```bash
mkdir -p lims-web/src/test/resources/lims-web/db
cp lims-web/src/main/resources/db/schema.sql lims-web/src/test/resources/lims-web/db/schema.sql
```

- [ ] **Step 3: Create AuthControllerIT**

Create `lims-web/src/test/java/com/lims/web/controller/AuthControllerIT.java`:

```java
package com.lims.web.controller;

import com.lims.web.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("GET /api/v1/auth/azure-ad-login is publicly reachable")
    void azureAdLoginIsPublic() throws Exception {
        mvc.perform(get("/api/v1/auth/azure-ad-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 4: Create BrandControllerIT**

Create `lims-web/src/test/java/com/lims/web/controller/BrandControllerIT.java`:

```java
package com.lims.web.controller;

import com.lims.web.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrandControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("GET /api/v1/brands returns 200 with empty page when DB is empty")
    void listBrandsPublic() throws Exception {
        mvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }
}
```

- [ ] **Step 5: Run — requires Docker (Testcontainers PG)**

Run: `./mvnw -B -pl lims-web verify -Dtest='*IT' -DfailIfNoTests=false`
Expected (with Docker): 2 ITs pass; first run downloads `postgres:15-alpine`.
If Docker unavailable, skip and rely on CI.

- [ ] **Step 6: Commit**

```bash
git add lims-web/src/test
git commit -m "test(lims-web): add AbstractIntegrationTest + AuthControllerIT + BrandControllerIT"
```

---

## Task 9: lims-web — 4 TestXxxConfig classes

**Files:**
- Create: `lims-web/src/test/java/com/lims/web/config/TestSecurityConfig.java`
- Create: `lims-web/src/test/java/com/lims/web/config/TestMinioConfig.java`
- Create: `lims-web/src/test/java/com/lims/web/config/TestGraphConfig.java`
- Create: `lims-web/src/test/java/com/lims/web/config/TestExternalApiConfig.java`

These are skeleton stubs that prove the import pattern works. Sub-projects B-I flesh out real WireMock mappings and assertion helpers.

- [ ] **Step 1: Create TestSecurityConfig**

Create `lims-web/src/test/java/com/lims/web/config/TestSecurityConfig.java`:

```java
package com.lims.web.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security: permissive chain (all requests allowed) + a no-op JwtDecoder bean
 * that decodes any token without signature verification. Tests that need to assert
 * role-based access use @WithMockUser or SecurityMockMvcRequestPostProcessors.jwt().
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Order(0)
    @Primary
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(c -> c.disable())
                .build();
    }

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return token -> { throw new UnsupportedOperationException("Use @WithMockUser in tests"); };
    }
}
```

- [ ] **Step 2: Create TestMinioConfig**

Create `lims-web/src/test/java/com/lims/web/config/TestMinioConfig.java`:

```java
package com.lims.web.config;

import io.minio.MinioClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test MinioClient bean. Real MinioClient would attempt to connect to a non-running
 * server; we substitute a Mockito mock. Tests that need to assert upload/download
 * behaviour can verify against this mock.
 */
@TestConfiguration
public class TestMinioConfig {

    @Bean
    @Primary
    MinioClient testMinioClient() {
        return mock(MinioClient.class);
    }
}
```

- [ ] **Step 3: Create TestGraphConfig**

Create `lims-web/src/test/java/com/lims/web/config/TestGraphConfig.java`:

```java
package com.lims.web.config;

import com.microsoft.graph.requests.GraphServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test GraphServiceClient bean. Production uses Microsoft Graph SDK; tests use a
 * Mockito mock. Sub-projects that need realistic Graph behaviour should switch this
 * to a WireMock-backed client.
 */
@TestConfiguration
public class TestGraphConfig {

    @Bean
    @Primary
    GraphServiceClient<okhttp3.Request> testGraphClient() {
        return mock(GraphServiceClient.class);
    }
}
```

- [ ] **Step 4: Create TestExternalApiConfig**

Create `lims-web/src/test/java/com/lims/web/config/TestExternalApiConfig.java`:

```java
package com.lims.web.config;

import com.lims.service.ExternalApiService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test ExternalApiService bean. Production calls parts/suppliers upstream HTTP;
 * tests use a Mockito mock that returns canned data. The
 * external.api.mock.enabled=true in application-test.yml is a runtime fallback
 * for beans that aren't explicitly replaced.
 */
@TestConfiguration
public class TestExternalApiConfig {

    @Bean
    @Primary
    ExternalApiService testExternalApiService() {
        return mock(ExternalApiService.class);
    }
}
```

- [ ] **Step 5: Smoke-compile the test config classes**

Run: `./mvnw -B -pl lims-web test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add lims-web/src/test/java/com/lims/web/config
git commit -m "test(lims-web): add TestSecurityConfig, TestMinioConfig, TestGraphConfig, TestExternalApiConfig"
```

---

## Task 10: frontend — package.json (deps + scripts)

**Files:**
- Modify: `lims-web-ui/package.json`

- [ ] **Step 1: Add devDependencies block**

Open `lims-web-ui/package.json` and add a `"devDependencies"` section (it doesn't exist yet). Insert AFTER the existing `"dependencies"` block:

```json
  "devDependencies": {
    "@testing-library/dom": "^10.0.0",
    "@testing-library/jest-dom": "^6.4.0",
    "@testing-library/react": "^16.0.0",
    "@testing-library/user-event": "^14.5.0",
    "@types/node": "^20.11.0",
    "@typescript-eslint/eslint-plugin": "^7.0.0",
    "@typescript-eslint/parser": "^7.0.0",
    "@vitest/ui": "^1.6.0",
    "eslint": "^8.56.0",
    "eslint-config-prettier": "^9.1.0",
    "eslint-plugin-react": "^7.33.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "husky": "^9.0.0",
    "jsdom": "^24.0.0",
    "lint-staged": "^15.2.0",
    "msw": "^2.2.0",
    "prettier": "^3.2.0",
    "typescript": "^5.3.0",
    "vitest": "^1.6.0"
  },
```

Note: `@types/node`, `eslint*`, `prettier`, `typescript`, `husky`, `lint-staged` may already be in `devDependencies` from a previous run — if so, leave the existing entries and only add the missing keys. Check the current `package.json` first.

- [ ] **Step 2: Update scripts**

In the `"scripts"` block, replace the existing `"test": "jest"` line with:

```json
    "test": "vitest",
    "test:run": "vitest run",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest run --coverage",
```

- [ ] **Step 3: Install**

Run: `cd lims-web-ui && npm install`
Expected: install succeeds; `node_modules` is now populated.

- [ ] **Step 4: Commit**

```bash
git add lims-web-ui/package.json lims-web-ui/package-lock.json
git commit -m "build(frontend): add vitest, RTL, MSW devDependencies and test scripts"
```

---

## Task 11: frontend — vitest.config.ts + src/test/* scaffolding

**Files:**
- Create: `lims-web-ui/vitest.config.ts`
- Create: `lims-web-ui/src/test/setup.ts`
- Create: `lims-web-ui/src/test/server.ts`
- Create: `lims-web-ui/src/test/handlers.ts`
- Create: `lims-web-ui/src/test/factories.ts`
- Create: `lims-web-ui/src/test/renderWithProviders.tsx`

- [ ] **Step 1: Create vitest.config.ts**

Create `lims-web-ui/vitest.config.ts`:

```typescript
import { defineConfig } from 'vitest/config';
import path from 'node:path';

export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    exclude: ['node_modules', 'dist', '.umi', '.umi-production', '.umi-test'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
```

- [ ] **Step 2: Create src/test/server.ts**

Create `lims-web-ui/src/test/server.ts`:

```typescript
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);
```

- [ ] **Step 3: Create src/test/handlers.ts**

Create `lims-web-ui/src/test/handlers.ts`:

```typescript
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('/api/v1/auth/me', () =>
    HttpResponse.json({
      code: 200,
      message: 'success',
      data: { userId: 'u-1', email: 'test@example.com', displayName: 'Test', roles: 'ADMIN', deptId: 'd-1' },
      timestamp: '2026-06-06T00:00:00Z',
    })
  ),
  http.get('/api/v1/brands', ({ request }) => {
    const url = new URL(request.url);
    const page = url.searchParams.get('page') ?? '1';
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { records: [], total: 0, size: 10, current: Number(page) },
      timestamp: '2026-06-06T00:00:00Z',
    });
  }),
];
```

- [ ] **Step 4: Create src/test/setup.ts**

Create `lims-web-ui/src/test/setup.ts`:

```typescript
import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './server';

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

- [ ] **Step 5: Create src/test/factories.ts**

Create `lims-web-ui/src/test/factories.ts`:

```typescript
let counter = 0;
const next = () => `${Date.now()}-${++counter}`;

export const brandFactory = (overrides: Partial<API.Brand> = {}) => ({
  id: next(),
  name: `Brand-${next()}`,
  description: 'Test brand',
  sortOrder: 0,
  ...overrides,
});

export const requestFactory = (overrides: Partial<API.RequestCreateDTO> = {}) => ({
  requestNo: `REQ-${next()}`,
  brandId: next(),
  typeId: next(),
  requestReason: 'Test reason',
  priority: 'NORMAL',
  ...overrides,
});

export const userFactory = (overrides: Partial<API.CurrentUser> = {}) => ({
  userId: next(),
  email: 'test@example.com',
  displayName: 'Test User',
  roles: 'ADMIN',
  deptId: 'd-1',
  ...overrides,
});
```

- [ ] **Step 6: Create src/test/renderWithProviders.tsx**

Create `lims-web-ui/src/test/renderWithProviders.tsx`:

```tsx
import { render, RenderOptions } from '@testing-library/react';
import { ConfigProvider } from 'antd';
import React, { ReactElement } from 'react';
import { userFactory } from './factories';

interface ProvidersOptions {
  currentUser?: API.CurrentUser;
}

export function renderWithProviders(
  ui: ReactElement,
  options: ProvidersOptions & RenderOptions = {}
) {
  const { currentUser = userFactory({ roles: 'ENGINEER' }), ...rest } = options;
  return render(
    <ConfigProvider>
      <div data-current-user-roles={currentUser.roles}>{ui}</div>
    </ConfigProvider>,
    rest
  );
}
```

- [ ] **Step 7: Verify Vitest can start (no tests yet)**

Run: `cd lims-web-ui && npx vitest --run --reporter=basic 2>&1 | head -20`
Expected: "No test files found" or similar (no tests have been created yet, but config is valid).

- [ ] **Step 8: Commit**

```bash
git add lims-web-ui/vitest.config.ts lims-web-ui/src/test
git commit -m "test(frontend): add vitest config and src/test scaffolding (setup, MSW, factories, renderWithProviders)"
```

---

## Task 12: frontend — 3 example tests

**Files:**
- Create: `lims-web-ui/src/access.test.ts`
- Create: `lims-web-ui/src/app.test.tsx`
- Create: `lims-web-ui/src/services/requestService.test.ts`

- [ ] **Step 1: Create access.test.ts**

Create `lims-web-ui/src/access.test.ts`:

```typescript
import { describe, expect, it } from 'vitest';
import access from './access';

const make = (roles: string) => access({ currentUser: { userId: 'u-1', email: 't@x', displayName: 'T', roles, deptId: 'd-1' } });
const anon = () => access(undefined);

describe('access role checks', () => {
  it('ADMIN can admin', () => {
    expect(make('ADMIN').canAdmin).toBe(true);
  });

  it('ENGINEER cannot admin but can engineer', () => {
    const a = make('ENGINEER');
    expect(a.canAdmin).toBe(false);
    expect(a.canEngineer).toBe(true);
  });

  it('MANAGER can manager and engineer and technician', () => {
    const a = make('MANAGER');
    expect(a.canManager).toBe(true);
    expect(a.canEngineer).toBe(true);
    expect(a.canTechnician).toBe(true);
  });

  it('TECHNICIAN can technician but not admin/manager/engineer', () => {
    const a = make('TECHNICIAN');
    expect(a.canTechnician).toBe(true);
    expect(a.canAdmin).toBe(false);
    expect(a.canManager).toBe(false);
    expect(a.canEngineer).toBe(false);
  });

  it('undefined initialState returns all false', () => {
    const a = anon();
    expect(a.canAdmin).toBe(false);
    expect(a.canManager).toBe(false);
    expect(a.canEngineer).toBe(false);
    expect(a.canTechnician).toBe(false);
  });
});
```

- [ ] **Step 2: Create app.test.tsx**

Create `lims-web-ui/src/app.test.tsx`:

```tsx
import { describe, expect, it } from 'vitest';
import { getInitialState } from './app';

describe('getInitialState', () => {
  it('returns the current user from /auth/me', async () => {
    const state = await getInitialState();
    expect(state.currentUser).toBeDefined();
    expect(state.currentUser?.roles).toBe('ADMIN');
  });

  it('returns empty object on fetch error', async () => {
    const original = global.fetch;
    global.fetch = () => Promise.reject(new Error('network down'));
    try {
      const state = await getInitialState();
      expect(state).toEqual({});
    } finally {
      global.fetch = original;
    }
  });
});
```

- [ ] **Step 3: Create requestService.test.ts**

Create `lims-web-ui/src/services/requestService.test.ts`:

```typescript
import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../test/server';
import { getBrands, getRequests } from './requestService';

describe('requestService', () => {
  it('getBrands returns the page wrapper from MSW', async () => {
    const res = await getBrands({ page: 2, size: 10 });
    // @umijs/max request returns the parsed body; the server stub wraps in {code,data}
    expect(res).toBeDefined();
  });

  it('getRequests honours query params via MSW', async () => {
    server.use(
      http.get('/api/v1/requests', ({ request }) => {
        const url = new URL(request.url);
        return HttpResponse.json({
          code: 200,
          data: { records: [{ id: 'r-x' }], total: 1, size: 10, current: Number(url.searchParams.get('page') ?? '1') },
        });
      })
    );
    const res: any = await getRequests({ page: 3 });
    expect(res.data.records[0].id).toBe('r-x');
  });
});
```

- [ ] **Step 4: Run all frontend tests**

Run: `cd lims-web-ui && npm run test:run`
Expected: ~6-7 tests pass across 3 files.

- [ ] **Step 5: Commit**

```bash
git add lims-web-ui/src/access.test.ts lims-web-ui/src/app.test.tsx lims-web-ui/src/services/requestService.test.ts
git commit -m "test(frontend): add example tests for access, app.getInitialState, requestService"
```

---

## Task 13: GitHub Actions smoke workflow

**Files:**
- Create: `.github/workflows/test.yml`

- [ ] **Step 1: Create the workflow file**

Create `.github/workflows/test.yml`:

```yaml
name: tests

on:
  pull_request:
  push:
    branches: [main]

jobs:
  backend-tests:
    name: Backend (Java)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - name: Run unit + integration tests
        run: ./mvnw -B verify
      - name: Upload JaCoCo report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: |
            lims-common/target/site/jacoco
            lims-dao/target/site/jacoco
            lims-service/target/site/jacoco
            lims-workflow/target/site/jacoco
            lims-admin/target/site/jacoco
            lims-web/target/site/jacoco
          if-no-files-found: ignore

  frontend-tests:
    name: Frontend (Vitest)
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: lims-web-ui
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: lims-web-ui/package-lock.json
      - name: Install dependencies
        run: npm ci
      - name: Run Vitest
        run: npm run test:run
```

- [ ] **Step 2: Validate YAML syntax**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/test.yml'))" && echo "YAML OK"`
Expected: `YAML OK`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/test.yml
git commit -m "ci: add GitHub Actions smoke workflow (backend + frontend)"
```

---

## Task 14: docs/testing/README.md

**Files:**
- Create: `docs/testing/README.md`

- [ ] **Step 1: Create the file**

Create `docs/testing/README.md`:

````markdown
# Testing — Material LIMS

This directory documents the testing conventions established by sub-project A (Testing Infrastructure). Sub-projects B-I build on these conventions to add business test logic.

## How to run tests

### Backend

```bash
# Unit tests only (fast, no Docker required, runs `*Test.java` excluding @Tag("integration"))
./mvnw test

# Unit + integration tests (requires Docker for Testcontainers PG; runs `*IT.java` and `@Tag("integration")`)
./mvnw verify

# Single module
./mvnw -pl lims-service test
./mvnw -pl lims-dao verify

# Single test class / method
./mvnw -pl lims-service test -Dtest=BrandServiceTest
./mvnw -pl lims-service test -Dtest=BrandServiceTest#listBrandsCoercesPage
```

### Frontend

```bash
cd lims-web-ui

# Watch mode (re-runs on file change)
npm test

# Single run (CI mode)
npm run test:run

# Browser UI
npm run test:ui

# With coverage
npm run test:coverage
```

## Naming and tagging conventions

| File pattern | Runs in | Tag | DB |
|--------------|---------|-----|----|
| `*Test.java`  | `mvn test` (surefire) | no `@Tag` needed | none (mocked) or H2 (rare) |
| `*IT.java`    | `mvn verify` (failsafe) | `@Tag("integration")` | Testcontainers PostgreSQL 15 |
| `*.test.{ts,tsx}` | `npm run test:run` | n/a | n/a (MSW mocks backend) |

Tag annotation is required on `*IT.java` to ensure both surefire (excludedGroups) and failsafe (includes) treat it correctly.

## Testcontainers requirement

Integration tests boot a real PostgreSQL 15 container via Testcontainers. The first run downloads the image (~80MB); subsequent runs reuse it.

- **Local development**: Docker Desktop (or `colima`, `podman machine`) must be running.
- **CI**: ubuntu-latest runners have Docker pre-installed; Testcontainers works out of the box.
- **Skip locally without Docker**: run `./mvnw test` (no `verify`) — surefire excludes `*IT.java` via `@Tag("integration")`.

## When to use which test slice

| Slice | Use for | Cost |
|-------|---------|------|
| Plain JUnit + Mockito | Pure logic with no Spring, no DB (e.g., `HolidayCalendarTest`) | ~10ms per test |
| `@SpringBootTest` + `@ActiveProfiles("test")` + TestXxxConfig | Full controller / security / BPMN / external-integration tests | ~10-30s startup + 1-2s per test |
| `@MybatisPlusTest` / `@DataJpaTest` | DAO-level SQL correctness, mapper semantics | ~5s startup + 100ms per test |
| `@WebMvcTest(ControllerXxx.class)` | Single controller, mocked services | ~3s startup + 50ms per test |

Default to the lightest slice that exercises the code under test.

## Mocking external systems

See [`mock-strategy.md`](./mock-strategy.md) for the four reusable `TestXxxConfig` classes (`TestSecurityConfig`, `TestMinioConfig`, `TestGraphConfig`, `TestExternalApiConfig`) and when to use which.

## Adding a new test

1. Pick a slice (see above).
2. Create the file with the conventional name (`XxxTest` or `XxxIT`).
3. If `*IT.java`, add `@Tag("integration")` and extend `AbstractDaoIT` or `AbstractIntegrationTest` (whichever lives in your module's test sources).
4. Run `./mvnw test` (or `verify` for ITs).
5. Commit in a focused, single-purpose commit.

## Code coverage

JaCoCo runs at the `verify` phase. Reports appear at `target/site/jacoco/index.html` for each module. CI uploads them as artifacts.

**No coverage threshold is enforced** in sub-project A. Sub-project J will add module-by-module thresholds as test coverage grows.
````

- [ ] **Step 2: Commit**

```bash
git add docs/testing/README.md
git commit -m "docs(testing): add README for backend and frontend test conventions"
```

---

## Task 15: docs/testing/mock-strategy.md

**Files:**
- Create: `docs/testing/mock-strategy.md`

- [ ] **Step 1: Create the file**

Create `docs/testing/mock-strategy.md`:

````markdown
# Mock Strategy — External Systems

Material LIMS depends on four external systems at runtime. Each is replaced at test time by a reusable `@TestConfiguration` class so individual tests don't repeat setup boilerplate.

## Summary table

| Config | Replaces | Mechanism | Where it lives |
|--------|----------|-----------|----------------|
| `TestSecurityConfig` | Production `SecurityFilterChain` + `JwtDecoder` | `permitAll()` chain + no-op `JwtDecoder` | `lims-web/src/test/java/com/lims/web/config/` |
| `TestMinioConfig` | Production `MinioClient` (real S3) | `@Primary @Bean` returning a `Mockito.mock(MinioClient.class)` | same |
| `TestGraphConfig` | Production `GraphServiceClient<Request>` (Microsoft Graph SDK) | `@Primary @Bean` returning a `Mockito.mock(GraphServiceClient.class)` | same |
| `TestExternalApiConfig` | Production `ExternalApiService` (HTTP upstreams) | `@Primary @Bean` returning a `Mockito.mock(ExternalApiService.class)` | same |

Each config is auto-imported by `AbstractIntegrationTest` (currently only `TestSecurityConfig`); others are imported per-test as needed.

## TestSecurityConfig

Provides two beans:

- A permissive `SecurityFilterChain` (`@Order(0) @Primary`) that `permitAll()`s every request and disables CSRF. This means tests can call any endpoint without authentication.
- A no-op `JwtDecoder` (`@Primary`) that throws if a real JWT is decoded. Tests must use `@WithMockUser` or `SecurityMockMvcRequestPostProcessors.jwt()` to inject authentication.

### Usage

```java
@SpringBootTest
@Import(TestSecurityConfig.class)
class MyControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void adminEndpointRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/v1/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user("u-1").roles("ADMIN")))
            .andExpect(status().isOk());
    }
}
```

Sub-projects that need to test the real JWT flow (e.g., B / H) will replace this config with a custom one in their test sources.

## TestMinioConfig

Replaces the real `MinioClient` with a `Mockito.mock(MinioClient.class)`. Tests that need to assert upload/download behaviour stub the mock:

```java
@MockBean MinioClient minioClient;

@BeforeEach
void stubUpload() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
}
```

## TestGraphConfig

Skeleton stub. Sub-project that fleshes this out (planned: sub-project I — external integration failure modes) should switch to WireMock for realistic Graph behaviour:

```java
@Bean @Primary
GraphServiceClient<Request> testGraphClient() {
    WireMockServer wm = new WireMockServer(0);
    wm.start();
    return GraphServiceClient.builder()
        .authenticationProvider(... using wm.baseUrl() ...)
        .build();
}
```

## TestExternalApiConfig

Replaces `ExternalApiService` with a Mockito mock. For tests that exercise Resilience4j circuit-breaker behaviour, sub-project I will combine this with a WireMock upstream to simulate failure modes.

## When to NOT use these configs

- **Pure unit tests** (e.g., `BrandServiceTest`) don't import any of these — they use plain Mockito.
- **DAO integration tests** (`AbstractDaoIT`) don't need them — DAO tests don't touch web/security/MinIO/Graph.
- **Custom scenarios**: if a test needs different mock behaviour than the default, write a local `@TestConfiguration` in the test class and use `@Import(LocalConfig.class)` to override.
````

- [ ] **Step 2: Commit**

```bash
git add docs/testing/mock-strategy.md
git commit -m "docs(testing): add mock-strategy for the four TestXxxConfig classes"
```

---

## Task 16: Update CODEBUDDY.md

**Files:**
- Modify: `CODEBUDDY.md`

- [ ] **Step 1: Replace the frontend test command in "常用命令"**

In `CODEBUDDY.md`, find the `### 前端测试` section and replace it with:

```markdown
### 前端测试（Vitest）

```bash
cd lims-web-ui
npm test                            # vitest watch 模式
npm run test:run                    # vitest run（CI 模式，单次跑完即退出）
npm run test:ui                     # 浏览器 UI
npm run test:coverage               # 带覆盖率
```

测试文件位于 `src/**/*.test.{ts,tsx}`；`src/test/` 目录存放共享 setup（MSW、factories、renderWithProviders）。详见 `docs/testing/README.md`。
```

- [ ] **Step 2: Add a backend test command to "常用命令"**

Insert a new section between `### 后端容器化` and `### 前端安装与启动`:

```markdown
### 后端测试

```bash
./mvnw test                         # 仅单测（surefire，排除 @Tag("integration")），无需 Docker
./mvnw verify                       # 单测 + 集成测试（failsafe + Testcontainers PG），需 Docker
./mvnw -pl lims-service test -Dtest=BrandServiceTest   # 单类
./mvnw -pl lims-dao verify         # 单模块集成
```

`*Test.java` 走 surefire，H2 / 无 DB / Mockito；`*IT.java` 走 failsafe，Testcontainers PG，`@Tag("integration")`。详见 `docs/testing/README.md`。
```

- [ ] **Step 3: Append a "Testing" section at the end of the file**

Append to the end of `CODEBUDDY.md`:

```markdown
## 测试

仓库采用分层测试策略：

- **后端单测**（`lims-{common,service,workflow,web}` 的 `*Test.java`）：JUnit 5 + Mockito + AssertJ，进程内运行，无需 Docker
- **后端集成测试**（`lims-{dao,web}` 的 `*IT.java`）：`@SpringBootTest` + Testcontainers PostgreSQL 15 + Failsafe
- **前端测试**（`lims-web-ui/src/**/*.test.{ts,tsx}`）：Vitest + jsdom + React Testing Library + MSW
- **CI**：`.github/workflows/test.yml` 在 PR 与 main 上自动跑后端 + 前端

测试约定、命令、外部系统 mock 策略：见 `docs/testing/README.md` 与 `docs/testing/mock-strategy.md`。
```

- [ ] **Step 4: Verify CODEBUDDY.md still renders**

Run: `head -5 CODEBUDDY.md && echo '...' && tail -5 CODEBUDDY.md`
Expected: original header at top, new "测试" section at bottom.

- [ ] **Step 5: Commit**

```bash
git add CODEBUDDY.md
git commit -m "docs(codebuddy): add Testing section; align frontend test commands with vitest"
```

---

## Final verification (after all 16 tasks)

- [ ] **Step 1: Full backend test pass**

Run: `./mvnw -B verify`
Expected (with Docker): all 17 Java tests pass; JaCoCo reports generated.

- [ ] **Step 2: Full frontend test pass**

Run: `cd lims-web-ui && npm run test:run`
Expected: 6-7 tests pass.

- [ ] **Step 3: Push the branch and verify CI**

Run: `git push origin <branch> && open https://github.com/<org>/Material-LIMS/actions`
Expected: two jobs (`backend-tests`, `frontend-tests`) green within ~10 minutes.

- [ ] **Step 4: Clean up the temporary plan file**

Run: `rm docs/superpowers/plans/_chunk4.md`
Expected: file removed. (This was a transient file used while writing the plan; not needed for execution.)

---

