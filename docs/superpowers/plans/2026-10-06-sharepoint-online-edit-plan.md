# SharePoint 在线编辑接入 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Material LIMS 中接通 Microsoft 365 SharePoint 在线编辑闭环(Graph drive 上传 → Office Online 编辑 → 快照拉回),代码层完整、demo fallback 与现状一致;前后端一起动。

**Architecture:** 在 `lims-service/service/sharepoint/` 新建 `SharePointClient` 接口 + 三个 `@Service` 实现,通过 Spring `@ConditionalOnProperty`/`@ConditionalOnMissingBean` 在三种 profile 形态下选其一;`ReportService.createReport` / `reviseReport` / `syncFromSharePoint` / `getEditUrl` 在不破坏本地报告的前提下注入调用;前端 `ReportDetail` 增加 SharePoint 可用性 gate,`ReportEdit` 增加 mock 与未配置的空态卡片;i18n 5 个新 key。

**Tech Stack:** Spring Boot 3.2、Java 17、MyBatis-Plus 3、Microsoft Graph SDK 5.77、msal4j 1.14、WireMock 3.10(测试)、Umi Max 4 / React 18 / antd 5 / Jest + Testing Library。

**Spec:** `docs/superpowers/specs/2026-10-06-sharepoint-online-edit-design.md`

## Global Constraints

- 路径基础前缀:`/Users/jiduobin/Documents/GitHub/Material-LIMS`
- Java 17、Spring Boot 3.2.x、MyBatis-Plus 3.x(已在 root `pom.xml` 中固定)
- Microsoft Graph SDK `5.77.0`、msal4j `1.14.2`(已在 root `dependencyManagement`,模块 `lims-service` 中引用即可,**不要**重复声明版本)
- 新增测试依赖 WireMock `3.10.0`(`<scope>test</scope>`,仅 `lims-service/pom.xml`)
- 共享错误码:复用现有 `M365_INTEGRATION_ERROR(5003)`(在 `lims-common/.../exception/ErrorCode.java`),不新增 `SHAREPOINT_*`
- 配置项命名:`sharepoint.*`(全小写,中划线变 camelCase 走 Spring 绑定);开关 `sharepoint.enabled: ${SHAREPOINT_ENABLED:false}`
- 后端条件注解三元组合必须保证恰好一个 `SharePointClient` bean:
  - `GraphSharePointClient`:`@ConditionalOnProperty(sharepoint.enabled=true) + @ConditionalOnProperty(azure.ad.enabled=true)`
  - `MockSharePointClient`:`@ConditionalOnProperty(lims.demo.enabled=true)`
  - `NoOpSharePointClient`:`@ConditionalOnMissingBean(SharePointClient.class)`
- `MicrosoftGraphClient` 现有方法不动,仅追加 `getDriveItem`、`getDriveItemContent`、`putDriveItemContent`、`resolveDriveId`
- 前端依赖零新增;antd 已供应 `Empty`/`Alert`/`Tooltip`
- i18n key 五个新增:`report.edit.title`、`report.edit.unavailable`、`report.edit.mockHint`、`report.detail.onlineEdit`、`report.detail.sharepointUnavailable`(中英双份,见 spec §4.5.3)
- mock URL 前缀 `https://example.invalid/mock-sharepoint/`,前端据此判定"演示模式"
- 提交者按 root commit 单;每个 Task 末尾独立 commit

---

## File Structure

**新增 (lims-common)**
- 无(`ErrorCode.M365_INTEGRATION_ERROR` 已存在;`BusinessException` 已支持 message + cause 构造)

**新增 (lims-service/src/main/java/com/lims/service/sharepoint)**
- `SharePointClient.java` 接口
- `SharePointPathResolver.java` `record SharePointPath` + 路径计算逻辑
- `SharePointProperties.java` `@ConfigurationProperties("sharepoint")`
- `GraphSharePointClient.java` `@Service`,Graph 实现
- `MockSharePointClient.java` `@Service`,dev 实现
- `NoOpSharePointClient.java` `@Service`,默认实现
- `SharePointException.java`(继承 `BusinessException`,使用 `M365_INTEGRATION_ERROR`)

**修改 (lims-service)**
- `ReportService.java`:在 `createReport` / `reviseReport` / `syncFromSharePoint` / `getEditUrl` 注入 `SharePointClient` 调用
- `MicrosoftGraphClient.java`:追加 4 个 drive 方法

**修改 (lims-service/pom.xml)**
- 测试依赖:`wiremock-standalone:3.10.0` (test scope)

**新增 (lims-service/src/test/java/com/lims/service/sharepoint)**
- `MockSharePointClientTest.java`
- `GraphSharePointClientTest.java`(用 `@RegisterExtension WireMockExtension`)
- `SharePointPathResolverTest.java`
- `NoOpSharePointClientTest.java`

**修改 (lims-service/src/test/java/com/lims/service)**
- 新增 `ReportServiceSharePointTest.java`(覆盖 createReport / sync 在 SharePoint 失败时的降级)

**修改 (lims-web/src/main/resources)**
- `application.yml`:新增 `sharepoint.*` 配置块
- `application-dev.yml`:新增 `sharepoint.enabled: false` + 注释

**修改 (lims-web-ui/src/pages/report)**
- `ReportDetail/index.tsx`:Online Edit 按钮按 `sharepointEditUrl` 可用性切换 disabled
- `ReportEdit/index.tsx`:三态(empty / mock / real)
- `ReportDetail/__tests__/buttons.test.tsx`:断言 Online Edit 按钮在 `sharepointEditUrl == null` 时 disabled

**新增 (lims-web-ui/src/pages/report/ReportEdit)**
- `__tests__/index.test.tsx`

**修改 (lims-web-ui/src/locales)**
- `en-US.ts`:5 个新 key
- `zh-CN.ts`:5 个新 key

**修改 (docs)**
- `docs/runbook/uat-environment.md`:补 SHAREPOINT_* env 章节

---

### Task 1: 后端 — 添加 `sharepoint.*` 配置 + `SharePointProperties`

**Files:**
- Modify: `lims-web/src/main/resources/application.yml`
- Modify: `lims-web/src/main/resources/application-dev.yml`
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/SharePointProperties.java`
- Test: `lims-service/src/test/java/com/lims/service/sharepoint/SharePointPropertiesTest.java`

**Interfaces**

- Produces:
  - `com.lims.service.sharepoint.SharePointProperties` — Spring `@ConfigurationProperties(prefix = "sharepoint")` record,字段:`enabled`、`strategy`、`hostname`、`sitePath`、`library`、`folderRoot`、`driveId`、`editUrlSuffix`(全为 String 或 boolean;`enabled` 为 boolean)。

- [ ] **Step 1: 写失败测试**

`lims-service/src/test/java/com/lims/service/sharepoint/SharePointPropertiesTest.java`:

```java
package com.lims.service.sharepoint;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SharePointPropertiesTest.Config.class)
@TestPropertySource(properties = {
    "sharepoint.enabled=true",
    "sharepoint.strategy=flat",
    "sharepoint.hostname=contoso.sharepoint.com",
    "sharepoint.site-path=/sites/lims",
    "sharepoint.library=Documents",
    "sharepoint.folder-root=Reports",
    "sharepoint.drive-id=drv-abc",
    "sharepoint.edit-url-suffix=?action=edit"
})
class SharePointPropertiesTest {

    @org.springframework.beans.factory.annotation.Autowired
    SharePointProperties props;

    @Test
    void bindsAllFields() {
        assertThat(props.enabled()).isTrue();
        assertThat(props.strategy()).isEqualTo("flat");
        assertThat(props.hostname()).isEqualTo("contoso.sharepoint.com");
        assertThat(props.sitePath()).isEqualTo("/sites/lims");
        assertThat(props.library()).isEqualTo("Documents");
        assertThat(props.folderRoot()).isEqualTo("Reports");
        assertThat(props.driveId()).isEqualTo("drv-abc");
        assertThat(props.editUrlSuffix()).isEqualTo("?action=edit");
    }

    @EnableConfigurationProperties(SharePointProperties.class)
    static class Config {}
}
```

- [ ] **Step 2: 运行测试,确认失败**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
./mvnw -pl lims-service -am test -Dtest='SharePointPropertiesTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`SharePointProperties` 类不存在 → `ClassNotFoundException` 或编译失败。

- [ ] **Step 3: 创建 `SharePointProperties.java`**

`lims-service/src/main/java/com/lims/service/sharepoint/SharePointProperties.java`:

```java
package com.lims.service.sharepoint;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sharepoint")
public record SharePointProperties(
    boolean enabled,
    String strategy,
    String hostname,
    String sitePath,
    String library,
    String folderRoot,
    String driveId,
    String editUrlSuffix
) {}
```

- [ ] **Step 4: 添加 `sharepoint.*` 配置块**

`lims-web/src/main/resources/application.yml`,在 `minio:` 块之后新增:

```yaml
# SharePoint document library for report .docx uploads
sharepoint:
  enabled: ${SHAREPOINT_ENABLED:false}
  strategy: ${SHAREPOINT_STRATEGY:folder_layout}   # folder_layout | flat
  hostname: ${SHAREPOINT_HOSTNAME:}
  site-path: ${SHAREPOINT_SITE_PATH:/sites/lims}
  library: ${SHAREPOINT_LIBRARY:Documents}
  folder-root: ${SHAREPOINT_FOLDER_ROOT:Reports}
  drive-id: ${SHAREPOINT_DRIVE_ID:}
  edit-url-suffix: ${SHAREPOINT_EDIT_URL_SUFFIX:?action=edit}
```

`lims-web/src/main/resources/application-dev.yml`,在 `minio:` 块之后新增:

```yaml
# SharePoint — dev profile keeps it disabled; the MockSharePointClient bean
# (gated by lims.demo.enabled=true) gives the same UX without Azure creds.
sharepoint:
  enabled: false
```

- [ ] **Step 5: 在 `lims-web` 模块启用 `@ConfigurationProperties` 扫描**

若 `lims-web/src/main/java/com/lims/web/config/ApplicationConfig.java`(或等价的 `@SpringBootApplication` 入口)未启用扫描,在该类上加 `@ConfigurationPropertiesScan("com.lims.service.sharepoint")`。具体路径以 `grep -rn '@SpringBootApplication' lims-web/src/main/java` 实际为准。

- [ ] **Step 6: 重新运行测试,确认通过**

```bash
./mvnw -pl lims-service -am test -Dtest='SharePointPropertiesTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`Tests run: 1, Failures: 0`。

- [ ] **Step 7: Commit**

```bash
git add lims-service/src/main/java/com/lims/service/sharepoint/SharePointProperties.java \
        lims-service/src/test/java/com/lims/service/sharepoint/SharePointPropertiesTest.java \
        lims-web/src/main/resources/application.yml \
        lims-web/src/main/resources/application-dev.yml
git -c user.email=claude@local -c user.name=Claude commit -m "feat(sharepoint): add sharepoint.* config block and SharePointProperties"
```

---

### Task 2: 后端 — `SharePointPathResolver` + `SharePointPath` record

**Files:**
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/SharePointPath.java`
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/SharePointPathResolver.java`
- Test: `lims-service/src/test/java/com/lims/service/sharepoint/SharePointPathResolverTest.java`

**Interfaces**

- Produces:
  - `com.lims.service.sharepoint.SharePointPath` — record `(String driveRelativePath, String filename)`,前者以 `/` 结尾或不带斜杠(空字符串表示库根)。
  - `com.lims.service.sharepoint.SharePointPathResolver.resolve(Request, Report)` — 返回 `SharePointPath`,基于 spec §4.2.3。

- [ ] **Step 1: 写失败测试**

`lims-service/src/test/java/com/lims/service/sharepoint/SharePointPathResolverTest.java`:

```java
package com.lims.service.sharepoint;

import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SharePointPathResolverTest {

    private final SharePointProperties props = new SharePointProperties(
        true, "folder_layout", "", "/sites/lims", "Documents", "Reports", "", "?action=edit"
    );
    private final SharePointPathResolver resolver = new SharePointPathResolver(props);

    private Request req(int year, int month) {
        Request r = new Request();
        r.setRequestNo("REQ-2026-0001");
        r.setCreatedAt(LocalDateTime.of(year, month, 15, 0, 0));
        return r;
    }

    private Report report(String version) {
        Report rep = new Report();
        rep.setVersionNumber(version);
        return rep;
    }

    @Test
    void folderLayout_january() {
        var p = resolver.resolve(req(2026, 1), report("V1.0"));
        assertThat(p.driveRelativePath()).isEqualTo("Reports/2026/01/");
        assertThat(p.filename()).isEqualTo("REQ-2026-0001_V1.0.docx");
    }

    @Test
    void folderLayout_december() {
        var p = resolver.resolve(req(2026, 12), report("V2.0"));
        assertThat(p.driveRelativePath()).isEqualTo("Reports/2026/12/");
        assertThat(p.filename()).isEqualTo("REQ-2026-0001_V2.0.docx");
    }

    @Test
    void folderLayout_leapFebruary() {
        var p = resolver.resolve(req(2024, 2), report("V1.0"));
        assertThat(p.driveRelativePath()).isEqualTo("Reports/2024/02/");
    }

    @Test
    void flatStrategy() {
        SharePointProperties flat = new SharePointProperties(
            true, "flat", "", "/sites/lims", "Documents", "Reports", "", "?action=edit"
        );
        SharePointPathResolver r = new SharePointPathResolver(flat);
        var p = r.resolve(req(2026, 5), report("V3.0"));
        assertThat(p.driveRelativePath()).isEmpty();
        assertThat(p.filename()).isEqualTo("REQ-2026-0001_V3.0.docx");
    }
}
```

- [ ] **Step 2: 运行测试,确认失败**

```bash
./mvnw -pl lims-service -am test -Dtest='SharePointPathResolverTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`SharePointPathResolver` 不存在 → 编译失败。

- [ ] **Step 3: 实现 `SharePointPath` record**

`lims-service/src/main/java/com/lims/service/sharepoint/SharePointPath.java`:

```java
package com.lims.service.sharepoint;

public record SharePointPath(String driveRelativePath, String filename) {}
```

- [ ] **Step 4: 实现 `SharePointPathResolver`**

`lims-service/src/main/java/com/lims/service/sharepoint/SharePointPathResolver.java`:

```java
package com.lims.service.sharepoint;

import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import org.springframework.stereotype.Component;

@Component
public class SharePointPathResolver {

    private final SharePointProperties props;

    public SharePointPathResolver(SharePointProperties props) {
        this.props = props;
    }

    public SharePointPath resolve(Request parent, Report report) {
        String filename = parent.getRequestNo() + "_" + report.getVersionNumber() + ".docx";
        String path = switch (props.strategy()) {
            case "flat" -> "";
            default -> "%s/%04d/%02d/".formatted(
                props.folderRoot(),
                parent.getCreatedAt().getYear(),
                parent.getCreatedAt().getMonthValue());
        };
        return new SharePointPath(path, filename);
    }
}
```

- [ ] **Step 5: 重新运行测试,确认通过**

```bash
./mvnw -pl lims-service -am test -Dtest='SharePointPathResolverTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`Tests run: 4, Failures: 0`。

- [ ] **Step 6: Commit**

```bash
git add lims-service/src/main/java/com/lims/service/sharepoint/SharePointPath.java \
        lims-service/src/main/java/com/lims/service/sharepoint/SharePointPathResolver.java \
        lims-service/src/test/java/com/lims/service/sharepoint/SharePointPathResolverTest.java
git -c user.email=claude@local -c user.name=Claude commit -m "feat(sharepoint): add SharePointPath and SharePointPathResolver"
```

---

### Task 3: 后端 — `SharePointClient` 接口 + `SharePointException`

**Files:**
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/SharePointException.java`
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/SharePointClient.java`

**Interfaces**

- Produces:
  - `com.lims.service.sharepoint.SharePointException extends BusinessException`,构造时使用 `M365_INTEGRATION_ERROR`。
  - `com.lims.service.sharepoint.SharePointClient` — 接口:`boolean isEnabled()`、`SharePointUploadResult uploadDocx(String localPath, SharePointPath path)`、`byte[] downloadDocx(String sharepointFileId)`、`String composeEditUrl(String sharepointFileId)`、嵌套 record `SharePointUploadResult(String fileId, String editUrl)`。

- [ ] **Step 1: 创建 `SharePointException`**

`lims-service/src/main/java/com/lims/service/sharepoint/SharePointException.java`:

```java
package com.lims.service.sharepoint;

import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;

public class SharePointException extends BusinessException {
    public SharePointException(String message) {
        super(ErrorCode.M365_INTEGRATION_ERROR, message);
    }

    public SharePointException(String message, Throwable cause) {
        super(ErrorCode.M365_INTEGRATION_ERROR, message);
        initCause(cause);
    }
}
```

(核对 `BusinessException` 构造签名;若不匹配,调整为对应的两参数或带 cause 版本。)

- [ ] **Step 2: 创建 `SharePointClient` 接口**

`lims-service/src/main/java/com/lims/service/sharepoint/SharePointClient.java`:

```java
package com.lims.service.sharepoint;

public interface SharePointClient {

    boolean isEnabled();

    SharePointUploadResult uploadDocx(String localPath, SharePointPath path);

    byte[] downloadDocx(String sharepointFileId);

    String composeEditUrl(String sharepointFileId);

    record SharePointUploadResult(String fileId, String editUrl) {}
}
```

- [ ] **Step 3: 编译验证(本任务不写测试 — 实现由后续 Task 4–6 覆盖)**

```bash
./mvnw -pl lims-common,lims-model,lims-dao,lims-service,lims-web -am compile
```

期望:成功。

- [ ] **Step 4: Commit**

```bash
git add lims-service/src/main/java/com/lims/service/sharepoint/SharePointException.java \
        lims-service/src/main/java/com/lims/service/sharepoint/SharePointClient.java
git -c user.email=claude@local -c user.name=Claude commit -m "feat(sharepoint): add SharePointClient interface and SharePointException"
```

---

### Task 4: 后端 — `MicrosoftGraphClient` 增加 drive 方法

**Files:**
- Modify: `lims-service/src/main/java/com/lims/service/sync/MicrosoftGraphClient.java`

**Interfaces**

- Produces(新增方法,签名):
  - `String resolveDriveId()` — 若 `cache != null` 返回,否则 `GET /sites/{hostname}:/sites/{sitePath}?$select=id` → `GET /sites/{siteId}/drives?$filter=name eq '{library}'` → 返回首个 `id`。缓存到 `volatile String driveIdCache`。
  - `Map<String,Object> putDriveItemContent(String driveId, String parentPath, String filename, byte[] body, String contentType)` — `PUT .../root:/{parentPath}{filename}:/content`,返回 driveItem JSON。
  - `Map<String,Object> getDriveItem(String driveId, String itemId, String select)` — `GET .../items/{itemId}?$select={select}`。
  - `byte[] getDriveItemContent(String driveId, String itemId)` — `GET .../items/{itemId}/content`,返回 byte[]。失败抛 `SharePointException`。

(构造时额外注入 `SharePointProperties`;已在 `GraphSharePointClient` 也需要,见 Task 5。)

- [ ] **Step 1: 写失败测试**

`lims-service/src/test/java/com/lims/service/sync/MicrosoftGraphClientDriveTest.java`:

```java
package com.lims.service.sync;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lims.service.sharepoint.SharePointProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class MicrosoftGraphClientDriveTest {

    private WireMockServer wm;
    private MicrosoftGraphClient client;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        SharePointProperties props = new SharePointProperties(
            true, "folder_layout", "contoso.sharepoint.com", "/sites/lims",
            "Documents", "Reports", "", "?action=edit"
        );
        RestTemplate rt = new RestTemplate();
        client = new MicrosoftGraphClient(rt);
        // Inject base URL by reflection on doGet; use a fixed token endpoint skip.
        ReflectionTestUtils.setField(client, "graphBaseUrl", "http://localhost:" + wm.port() + "/v1.0");
        ReflectionTestUtils.setField(client, "tenantId", "tenant");
        ReflectionTestUtils.setField(client, "clientId", "client");
        ReflectionTestUtils.setField(client, "clientSecret", "secret");
    }

    @AfterEach
    void tearDown() { wm.stop(); }

    @Test
    void putDriveItemContent_returns201Json() {
        wm.stubFor(put(urlEqualTo("/v1.0/drives/drv-1/root:/Reports/2026%2F01%2FREQ_V1.0.docx:/content"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"item-123\",\"webUrl\":\"https://contoso.sharepoint.com/x\"}")));

        Map<String, Object> r = client.putDriveItemContent(
            "drv-1", "Reports/2026/01/", "REQ_V1.0.docx",
            "hello".getBytes(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertThat(r.get("id")).isEqualTo("item-123");
        assertThat(r.get("webUrl")).isEqualTo("https://contoso.sharepoint.com/x");
    }
}
```

注意:由于 `MicrosoftGraphClient` 当前 `GRPAH_BASE` 写死为 `https://graph.microsoft.com/v1.0`,需要小重构:把 `GRAPH_BASE` 改为可注入的 `@Value` 或加 setter;测试用反射覆盖。

- [ ] **Step 2: 运行测试,确认失败**

```bash
./mvnw -pl lims-service -am test -Dtest='MicrosoftGraphClientDriveTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`putDriveItemContent` 不存在 → 编译失败。

- [ ] **Step 3: 修改 `MicrosoftGraphClient`**

- 把 `private static final String GRAPH_BASE` 改为 `private String graphBaseUrl`,从 `@Value("${azure.ad.graph-base-url:https://graph.microsoft.com/v1.0}")` 注入。
- 暴露 `volatile String driveIdCache`,`@Value("${sharepoint.drive-id:}")` 注入 `configuredDriveId`。
- 新增方法实现(伪代码,具体参考 spec §4.2.4 + Graph docs):

```java
public synchronized String resolveDriveId() {
    if (configuredDriveId != null && !configuredDriveId.isBlank()) return configuredDriveId;
    if (driveIdCache != null) return driveIdCache;
    // GET sites/{host}:/sites/{sitePath}?$select=id
    // GET sites/{id}/drives?$filter=name eq '{library}'
    // pick first; cache; return
}
public Map<String,Object> putDriveItemContent(String driveId, String parentPath, String filename, byte[] body, String contentType) {
    String url = graphBaseUrl + "/drives/" + driveId + "/root:/" +
                 urlEncode(parentPath + filename) + ":/content";
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(getAccessToken());
    h.setContentType(MediaType.parseMediaType(contentType));
    return doExchange(url, HttpMethod.PUT, new HttpEntity<>(body, h));
}
public Map<String,Object> getDriveItem(String driveId, String itemId, String select) {
    String url = graphBaseUrl + "/drives/" + driveId + "/items/" + itemId + "?$select=" + select;
    return doGet(url);
}
public byte[] getDriveItemContent(String driveId, String itemId) {
    String url = graphBaseUrl + "/drives/" + driveId + "/items/" + itemId + "/content";
    try {
        ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET,
            new HttpEntity<>(bearerHeaders()), byte[].class);
        return resp.getBody();
    } catch (Exception e) {
        throw new SharePointException("Graph download failed: " + e.getMessage(), e);
    }
}
```

- [ ] **Step 4: 重新运行测试,确认通过**

```bash
./mvnw -pl lims-service -am test -Dtest='MicrosoftGraphClientDriveTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`Tests run: 1, Failures: 0`。

- [ ] **Step 5: Commit**

```bash
git add lims-service/src/main/java/com/lims/service/sync/MicrosoftGraphClient.java \
        lims-service/src/test/java/com/lims/service/sync/MicrosoftGraphClientDriveTest.java
git -c user.email=claude@local -c user.name=Claude commit -m "feat(graph): add drive item methods on MicrosoftGraphClient"
```

---

### Task 5: 后端 — `GraphSharePointClient` 实现

**Files:**
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/GraphSharePointClient.java`
- Create: `lims-service/src/test/java/com/lims/service/sharepoint/GraphSharePointClientTest.java`

**Interfaces**

- Produces:
  - `GraphSharePointClient implements SharePointClient`,`@Component` + 两个 `@ConditionalOnProperty`:
    ```java
    @ConditionalOnProperty(name = "sharepoint.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "azure.ad.enabled", havingValue = "true")
    ```

- [ ] **Step 1: 写失败测试**

`lims-service/src/test/java/com/lims/service/sharepoint/GraphSharePointClientTest.java`:

```java
package com.lims.service.sharepoint;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lims.service.sync.MicrosoftGraphClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphSharePointClientTest {

    private WireMockServer wm;
    private GraphSharePointClient client;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        SharePointProperties props = new SharePointProperties(
            true, "folder_layout", "contoso.sharepoint.com", "/sites/lims",
            "Documents", "Reports", "drv-1", "?action=edit"
        );
        MicrosoftGraphClient mgc = new MicrosoftGraphClient(new org.springframework.web.client.RestTemplate());
        ReflectionTestUtils.setField(mgc, "graphBaseUrl", "http://localhost:" + wm.port() + "/v1.0");
        ReflectionTestUtils.setField(mgc, "tenantId", "t");
        ReflectionTestUtils.setField(mgc, "clientId", "c");
        ReflectionTestUtils.setField(mgc, "clientSecret", "s");
        client = new GraphSharePointClient(mgc, props);
    }

    @AfterEach
    void tearDown() { wm.stop(); }

    @Test
    void uploadDocx_happyPath_returnsFileIdAndEditUrl() throws Exception {
        wm.stubFor(put(urlMatching("/v1.0/drives/drv-1/root:.*REQ.*\\.docx:/content"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"item-1\",\"webUrl\":\"https://contoso.sharepoint.com/x\"}")));

        Path docx = Files.createTempFile("test", ".docx");
        Files.writeString(docx, "fake-docx");

        var r = client.uploadDocx(docx.toString(),
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));

        assertThat(r.fileId()).isEqualTo("item-1");
        assertThat(r.editUrl()).isEqualTo("https://contoso.sharepoint.com/x?action=edit");
    }

    @Test
    void uploadDocx_409Conflicts_fallsBackToGetExisting() throws Exception {
        wm.stubFor(put(urlMatching("/v1.0/drives/drv-1/root:.*"))
            .willReturn(aResponse().withStatus(409)));
        wm.stubFor(get(urlMatching("/v1.0/drives/drv-1/root:.*"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"item-existing\",\"webUrl\":\"https://contoso.sharepoint.com/existing\"}")));

        Path docx = Files.createTempFile("test", ".docx");
        Files.writeString(docx, "fake");

        var r = client.uploadDocx(docx.toString(),
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));

        assertThat(r.fileId()).isEqualTo("item-existing");
    }

    @Test
    void uploadDocx_5xx_throwsSharePointException() throws Exception {
        wm.stubFor(put(urlMatching("/v1.0/drives/drv-1/root:.*"))
            .willReturn(aResponse().withStatus(500).withBody("server error")));

        Path docx = Files.createTempFile("test", ".docx");
        Files.writeString(docx, "fake");

        assertThatThrownBy(() -> client.uploadDocx(docx.toString(),
                new SharePointPath("Reports/", "REQ_V1.0.docx")))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void composeEditUrl_appendsSuffix() {
        wm.stubFor(get(urlEqualTo("/v1.0/drives/drv-1/items/item-9?$select=webUrl"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"webUrl\":\"https://contoso.sharepoint.com/y\"}")));

        assertThat(client.composeEditUrl("item-9")).isEqualTo("https://contoso.sharepoint.com/y?action=edit");
    }
}
```

- [ ] **Step 2: 添加 wiremock 依赖**

`lims-service/pom.xml`,在 `<dependencies>` 末尾添加:

```xml
<dependency>
  <groupId>org.wiremock</groupId>
  <artifactId>wiremock-standalone</artifactId>
  <version>3.10.0</version>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 3: 运行测试,确认失败**

```bash
./mvnw -pl lims-service -am test -Dtest='GraphSharePointClientTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`GraphSharePointClient` 不存在 → 编译失败。

- [ ] **Step 4: 实现 `GraphSharePointClient`**

`lims-service/src/main/java/com/lims/service/sharepoint/GraphSharePointClient.java`:

```java
package com.lims.service.sharepoint;

import com.lims.service.sync.MicrosoftGraphClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "sharepoint.enabled", havingValue = "true")
@ConditionalOnProperty(name = "azure.ad.enabled", havingValue = "true")
public class GraphSharePointClient implements SharePointClient {

    private static final String DOCX_MIME =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final MicrosoftGraphClient graph;
    private final SharePointProperties props;

    public GraphSharePointClient(MicrosoftGraphClient graph, SharePointProperties props) {
        this.graph = graph;
        this.props = props;
    }

    @Override public boolean isEnabled() { return true; }

    @Override
    public SharePointUploadResult uploadDocx(String localPath, SharePointPath path) {
        String driveId;
        try {
            driveId = graph.resolveDriveId();
        } catch (Exception e) {
            throw new SharePointException("drive resolution failed: " + e.getMessage(), e);
        }

        String parentPath = path.driveRelativePath();
        String filename = path.filename();
        byte[] body;
        try { body = Files.readAllBytes(Path.of(localPath)); }
        catch (IOException e) {
            throw new SharePointException("read local docx failed: " + e.getMessage(), e);
        }

        try {
            Map<String, Object> item = graph.putDriveItemContent(driveId, parentPath, filename, body, DOCX_MIME);
            String id = (String) item.get("id");
            String webUrl = (String) item.get("webUrl");
            return new SharePointUploadResult(id, webUrl + props.editUrlSuffix());
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("409")) {
                // Existing file path: try to look it up by the same driveRelativePath + filename
                // Implementation: re-call Graph GET /drives/{driveId}/root:/{parentPath}{filename}
                // (left as a one-shot retry inside this catch — done by graph.getDriveItem path lookup if added)
                log.info("SharePoint upload 409; treating as existing item");
                return new SharePointUploadResult(null, null); // to be replaced by lookup below
            }
            throw new SharePointException("Graph upload failed: " + msg, e);
        }
    }

    @Override
    public byte[] downloadDocx(String sharepointFileId) {
        try {
            String driveId = graph.resolveDriveId();
            return graph.getDriveItemContent(driveId, sharepointFileId);
        } catch (SharePointException e) {
            throw e;
        } catch (Exception e) {
            throw new SharePointException("download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String composeEditUrl(String sharepointFileId) {
        try {
            String driveId = graph.resolveDriveId();
            Map<String, Object> item = graph.getDriveItem(driveId, sharepointFileId, "webUrl");
            String webUrl = (String) item.get("webUrl");
            return webUrl + props.editUrlSuffix();
        } catch (Exception e) {
            throw new SharePointException("compose edit url failed: " + e.getMessage(), e);
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
```

> 注:`uploadDocx` 中 409 回退路径需要 lookup;推荐在 `MicrosoftGraphClient` 增加 `getDriveItemByPath(driveId, parentPath, filename)`(GET `/drives/{driveId}/root:/{parentPath}{filename}`)。**此处只承诺 4 个 Task 4 方法,在 Task 4 中改 spec:**增加 `getDriveItemByPath` 同款 skeleton。Task 5 用 `getDriveItemByPath` 替换占位。

- [ ] **Step 5: 重新运行测试,确认通过**

```bash
./mvnw -pl lims-service -am test -Dtest='GraphSharePointClientTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`Tests run: 4, Failures: 0`。

- [ ] **Step 6: Commit**

```bash
git add lims-service/pom.xml \
        lims-service/src/main/java/com/lims/service/sharepoint/GraphSharePointClient.java \
        lims-service/src/test/java/com/lims/service/sharepoint/GraphSharePointClientTest.java
git -c user.email=claude@local -c user.name=Claude commit -m "feat(sharepoint): add GraphSharePointClient implementation"
```

---

### Task 6: 后端 — `MockSharePointClient` + `NoOpSharePointClient`

**Files:**
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/MockSharePointClient.java`
- Create: `lims-service/src/main/java/com/lims/service/sharepoint/NoOpSharePointClient.java`
- Create: `lims-service/src/test/java/com/lims/service/sharepoint/MockSharePointClientTest.java`
- Create: `lims-service/src/test/java/com/lims/service/sharepoint/NoOpSharePointClientTest.java`

**Interfaces**

- Produces:
  - `MockSharePointClient implements SharePointClient`,`@Component` + `@ConditionalOnProperty(lims.demo.enabled=true)`。`uploadDocx` 返回 deterministic id(`mock-{reportId-or-hash}`),`composeEditUrl` 返回 `https://example.invalid/mock-sharepoint/{fileId}?action=edit`,`downloadDocx` 从传入路径或配置路径读文件(简化为重新读 `localPath`,因此需要接口签名扩展)。
  - **重要**:为了 mock 的 `downloadDocx` 可工作,接口需要让 `MockSharePointClient` 知道本地路径。**Task 5 接口需修:**增加 `byte[] downloadDocx(String sharepointFileId)` 的替代调用 `downloadDocx(String sharepointFileId, Supplier<byte[]> localFallback)` 或者 — 更干净 — 在 `MockSharePointClient` 注入 `FileStorageService`/`ReportMapper`,按 `sharepointFileId` 反查 `Report.file_url` 后读 MinIO。
  - `NoOpSharePointClient implements SharePointClient`,`@Component` + `@ConditionalOnMissingBean(SharePointClient.class)`。`isEnabled()` → false;其它 throw `SharePointException`。

- [ ] **Step 1: 写失败测试**

`MockSharePointClientTest.java`:

```java
package com.lims.service.sharepoint;

import com.lims.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MockSharePointClientTest {

    private FileStorageService storage;
    private MockSharePointClient client;

    @BeforeEach
    void setUp() {
        storage = Mockito.mock(FileStorageService.class);
        client = new MockSharePointClient(storage);
    }

    @Test
    void isEnabled_true() {
        assertThat(client.isEnabled()).isTrue();
    }

    @Test
    void uploadDocx_returnsDeterministicIdAndMockUrl() {
        var r = client.uploadDocx("/tmp/anywhere.docx",
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));
        assertThat(r.fileId()).startsWith("mock-");
        assertThat(r.editUrl()).startsWith("https://example.invalid/mock-sharepoint/");
        assertThat(r.editUrl()).endsWith("?action=edit");
    }

    @Test
    void downloadDocx_readsFromFileStorage() {
        Mockito.when(storage.openStream("minio:key/x.docx")).thenReturn(new ByteArrayInputStream("hello".getBytes()));
        // We bypass the storage by file_url lookup; this test asserts downloadDocx calls storage.openStream
        // for the key it recorded at upload time.
        var r = client.uploadDocx("/tmp/y.docx", new SharePointPath("", "REQ_V1.0.docx"));
        // After upload, file_id is mock-...; but mock download reads by file_id; so it must first record the local file_url
        // In MockSharePointClient we store a map from fileId -> localPath at upload time.
        byte[] body = client.downloadDocx(r.fileId());
        assertThat(new String(body)).isEqualTo("hello");
    }
}
```

`NoOpSharePointClientTest.java`:

```java
package com.lims.service.sharepoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoOpSharePointClientTest {
    private final NoOpSharePointClient c = new NoOpSharePointClient();

    @Test
    void isEnabled_false() { assertThat(c.isEnabled()).isFalse(); }

    @Test
    void uploadDocx_throws() {
        assertThatThrownBy(() -> c.uploadDocx("x", new SharePointPath("", "x.docx")))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void downloadDocx_throws() {
        assertThatThrownBy(() -> c.downloadDocx("id"))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void composeEditUrl_throws() {
        assertThatThrownBy(() -> c.composeEditUrl("id"))
            .isInstanceOf(SharePointException.class);
    }
}
```

- [ ] **Step 2: 运行测试,确认失败**

```bash
./mvnw -pl lims-service -am test -Dtest='MockSharePointClientTest,NoOpSharePointClientTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:编译失败。

- [ ] **Step 3: 实现 `MockSharePointClient`**

`lims-service/src/main/java/com/lims/service/sharepoint/MockSharePointClient.java`:

```java
package com.lims.service.sharepoint;

import com.lims.service.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "lims.demo.enabled", havingValue = "true")
public class MockSharePointClient implements SharePointClient {

    private final FileStorageService storage;
    private final ConcurrentMap<String, String> localFileById = new ConcurrentHashMap<>();

    public MockSharePointClient(FileStorageService storage) {
        this.storage = storage;
    }

    @Override public boolean isEnabled() { return true; }

    @Override
    public SharePointUploadResult uploadDocx(String localPath, SharePointPath path) {
        String fileId = "mock-" + UUID.randomUUID();
        // For mock, we don't actually upload; we record the local path so downloadDocx can replay.
        localFileById.put(fileId, localPath);
        String editUrl = "https://example.invalid/mock-sharepoint/" + fileId + "?action=edit";
        log.info("MockSharePoint upload: fileId={} path={}", fileId, path);
        return new SharePointUploadResult(fileId, editUrl);
    }

    @Override
    public byte[] downloadDocx(String sharepointFileId) {
        String localPath = localFileById.get(sharepointFileId);
        if (localPath == null) {
            throw new SharePointException("Mock: unknown fileId " + sharepointFileId);
        }
        try (InputStream in = storage.openStream(localPath)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new SharePointException("Mock: read " + localPath + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String composeEditUrl(String sharepointFileId) {
        return "https://example.invalid/mock-sharepoint/" + sharepointFileId + "?action=edit";
    }
}
```

> 注意:`FileStorageService` 当前 API(参考现有 `LocalFileStorageService` / `MinioFileStorageService` 父类/接口)是否提供 `openStream(String key)`,若没有,改为 `storage.download(String key) -> byte[]`,参考 `FileStorageService` 实际方法。

- [ ] **Step 4: 实现 `NoOpSharePointClient`**

`lims-service/src/main/java/com/lims/service/sharepoint/NoOpSharePointClient.java`:

```java
package com.lims.service.sharepoint;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(SharePointClient.class)
public class NoOpSharePointClient implements SharePointClient {

    @Override public boolean isEnabled() { return false; }

    @Override public SharePointUploadResult uploadDocx(String localPath, SharePointPath path) {
        throw new SharePointException("SharePoint integration is not enabled in this environment");
    }

    @Override public byte[] downloadDocx(String sharepointFileId) {
        throw new SharePointException("SharePoint integration is not enabled in this environment");
    }

    @Override public String composeEditUrl(String sharepointFileId) {
        throw new SharePointException("SharePoint integration is not enabled in this environment");
    }
}
```

- [ ] **Step 5: 重新运行测试,确认通过**

```bash
./mvnw -pl lims-service -am test -Dtest='MockSharePointClientTest,NoOpSharePointClientTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`Tests run: ≥ 4, Failures: 0`。

- [ ] **Step 6: Commit**

```bash
git add lims-service/src/main/java/com/lims/service/sharepoint/MockSharePointClient.java \
        lims-service/src/main/java/com/lims/service/sharepoint/NoOpSharePointClient.java \
        lims-service/src/test/java/com/lims/service/sharepoint/MockSharePointClientTest.java \
        lims-service/src/test/java/com/lims/service/sharepoint/NoOpSharePointClientTest.java
git -c user.email=claude@local -c user.name=Claude commit -m "feat(sharepoint): add MockSharePointClient and NoOpSharePointClient"
```

---

### Task 7: 后端 — `ReportService` 接入 `SharePointClient`

**Files:**
- Modify: `lims-service/src/main/java/com/lims/service/ReportService.java`
- Create: `lims-service/src/test/java/com/lims/service/ReportServiceSharePointTest.java`

**Interfaces**

- Consumes: `SharePointClient`,`SharePointPathResolver`(构造注入)
- Behavior changes:
  - `createReport`:在 `fileStorageService.upload(docx, …)` 之后、`reportMapper.updateById(report)` 之前,若 `sharePointClient.isEnabled()`,调 `uploadDocx` 写入 `sharepointFileId` / `sharepointEditUrl`;try/catch `SharePointException` + log.warn。
  - `reviseReport`:同上。
  - `syncFromSharePoint`:去掉 `!demoEnabled` 抛 `OPERATION_NOT_ALLOWED` 的分支;改为:`sharepointFileId` 为 null 时 log.warn 返回;否则调 `downloadDocx` 写本地 → `fileStorageService.upload` → `wordToPdfConverter.convert` → 更新 `file_url` / `pdf_url`;try/catch `SharePointException` + log.warn,不动旧 URL。
  - `getEditUrl(reportId)`:若 `sharePointClient.isEnabled()` 且 `sharepointFileId` 非空,调 `composeEditUrl` 返回;否则返回 null。

- [ ] **Step 1: 写失败测试**

`lims-service/src/test/java/com/lims/service/ReportServiceSharePointTest.java`:

```java
package com.lims.service;

import com.lims.common.exception.BusinessException;
import com.lims.dao.mapper.ReportMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import com.lims.service.report.ReportTemplateService;
import com.lims.service.report.WordToPdfConverter;
import com.lims.service.sharepoint.MockSharePointClient;
import com.lims.service.sharepoint.NoOpSharePointClient;
import com.lims.service.sharepoint.SharePointClient;
import com.lims.service.sharepoint.SharePointException;
import com.lims.service.sharepoint.SharePointPathResolver;
import com.lims.service.sharepoint.SharePointProperties;
import com.lims.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReportServiceSharePointTest {

    private ReportMapper reportMapper;
    private RequestMapper requestMapper;
    private ReportTemplateService tpl;
    private WordToPdfConverter conv;
    private FileStorageService storage;
    private SharePointClient client;
    private ReportService svc;

    @BeforeEach
    void setUp() {
        reportMapper = mock(ReportMapper.class);
        requestMapper = mock(RequestMapper.class);
        tpl = mock(ReportTemplateService.class);
        conv = mock(WordToPdfConverter.class);
        storage = mock(FileStorageService.class);
        client = mock(SharePointClient.class);
        when(client.isEnabled()).thenReturn(true);

        SharePointProperties props = new SharePointProperties(
            true, "folder_layout", "", "/sites/lims", "Documents", "Reports", "", "?action=edit");
        SharePointPathResolver resolver = new SharePointPathResolver(props);

        svc = new ReportService(reportMapper, null, requestMapper, tpl, conv, storage,
            client, resolver);
    }

    private Request parent(String status) {
        Request r = new Request();
        r.setId("req-1");
        r.setRequestNo("REQ-2026-0001");
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.of(2026, 1, 15, 0, 0));
        return r;
    }

    @Test
    void createReport_sharePointSuccess_setsFileIdAndEditUrl() throws Exception {
        when(requestMapper.selectById("req-1")).thenReturn(parent("REPORTING"));
        when(reportMapper.nextReportNumber()).thenReturn(1);
        when(tpl.generate(any(), any(), any(), any())).thenReturn(java.nio.file.Files.createTempFile("r", ".docx"));
        when(storage.upload(any(), anyString())).thenReturn("minio:reports/req-1/r.docx");
        when(conv.convert(any())).thenReturn(null);
        when(client.uploadDocx(anyString(), any()))
            .thenReturn(new SharePointClient.SharePointUploadResult("item-9", "https://example.com/x?action=edit"));

        Report created = svc.createReport("req-1", "user-1");

        assertThat(created.getSharepointFileId()).isEqualTo("item-9");
        assertThat(created.getSharepointEditUrl()).isEqualTo("https://example.com/x?action=edit");
    }

    @Test
    void createReport_sharePointThrows_keepsLocalRecordButEmptyMeta() throws Exception {
        when(requestMapper.selectById("req-1")).thenReturn(parent("REPORTING"));
        when(reportMapper.nextReportNumber()).thenReturn(2);
        when(tpl.generate(any(), any(), any(), any())).thenReturn(java.nio.file.Files.createTempFile("r", ".docx"));
        when(storage.upload(any(), anyString())).thenReturn("minio:reports/req-1/r.docx");
        when(client.uploadDocx(anyString(), any())).thenThrow(new SharePointException("Graph 401"));

        Report created = svc.createReport("req-1", "user-1");

        assertThat(created.getSharepointFileId()).isNull();
        assertThat(created.getSharepointEditUrl()).isNull();
    }

    @Test
    void syncFromSharePoint_noFileId_isNoOp() {
        Report r = new Report();
        r.setId("rpt-1");
        r.setRequestId("req-1");
        r.setSharepointFileId(null);
        when(reportMapper.selectById("rpt-1")).thenReturn(r);
        svc.syncFromSharePoint("rpt-1");
        verify(client, never()).downloadDocx(anyString());
    }
}
```

> 该测试要 `ReportService` 构造签名与现有一致。当前签名:`ReportService(ReportMapper, AnalysisTaskMapper, RequestMapper, ReportTemplateService, WordToPdfConverter, FileStorageService)` + `@Value demoEnabled`。本任务扩展为追加 `SharePointClient` + `SharePointPathResolver`。

- [ ] **Step 2: 运行测试,确认失败**

```bash
./mvnw -pl lims-service -am test -Dtest='ReportServiceSharePointTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:构造器不匹配 → 编译失败。

- [ ] **Step 3: 修改 `ReportService`**

- 添加构造器参数:`SharePointClient sharePointClient, SharePointPathResolver pathResolver`;保留 `@RequiredArgsConstructor` 但改为手写构造器以便扩展。
- `createReport`:在 `fileStorageService.upload(docx, "reports/" + requestId)` 后加:
  ```java
  try {
      if (sharePointClient.isEnabled()) {
          var spPath = pathResolver.resolve(parent, report);
          var r = sharePointClient.uploadDocx(docx.toString(), spPath);
          report.setSharepointFileId(r.fileId());
          report.setSharepointEditUrl(r.editUrl());
      }
  } catch (SharePointException e) {
      log.warn("SharePoint upload failed (local report kept): {}", e.getMessage());
  }
  ```
- `reviseReport`:同 `createReport`,regenerate docx → upload → setSharepointMeta。
- `syncFromSharePoint`:移除 `if (!demoEnabled)` 抛错分支;改为:
  ```java
  if (report.getSharepointFileId() == null) { log.warn(...); return; }
  try {
      byte[] body = sharePointClient.downloadDocx(report.getSharepointFileId());
      Path tmp = Files.createTempFile("lims-sharepoint-sync-" + reportId, ".docx");
      Files.write(tmp, body);
      String newUrl = fileStorageService.upload(tmp, "reports/" + report.getRequestId());
      report.setFileUrl(newUrl);
      Path pdf = wordToPdfConverter.convert(tmp);
      if (pdf != null) report.setPdfUrl(fileStorageService.upload(pdf, "reports/" + report.getRequestId()));
      reportMapper.updateById(report);
  } catch (SharePointException e) {
      log.warn("SharePoint sync failed (file_url unchanged): {}", e.getMessage());
  }
  ```
- `getEditUrl(reportId)`:
  ```java
  Report r = reportMapper.selectById(reportId);
  if (r == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
  if (!sharePointClient.isEnabled() || r.getSharepointFileId() == null) return null;
  return sharePointClient.composeEditUrl(r.getSharepointFileId());
  ```

- [ ] **Step 4: 重新运行测试,确认通过**

```bash
./mvnw -pl lims-service -am test -Dtest='ReportServiceSharePointTest' -Dsurefire.failIfNoSpecifiedTests=false
```

期望:`Tests run: 3, Failures: 0`。

- [ ] **Step 5: 运行全部相关测试,确认未破坏现有行为**

```bash
./mvnw -pl lims-service -am test -Dtest='ReportServiceSharePointTest,MockSharePointClientTest,NoOpSharePointClientTest,GraphSharePointClientTest,SharePointPathResolverTest,SharePointPropertiesTest' -Dsurefire.failIfNoSpecifiedTests=false
```

- [ ] **Step 6: Commit**

```bash
git add lims-service/src/main/java/com/lims/service/ReportService.java \
        lims-service/src/test/java/com/lims/service/ReportServiceSharePointTest.java
git -c user.email=claude@local -c user.name=Claude commit -m "feat(report): wire ReportService through SharePointClient"
```

---

### Task 8: 前端 — i18n 新增 5 个 key

**Files:**
- Modify: `lims-web-ui/src/locales/en-US.ts`
- Modify: `lims-web-ui/src/locales/zh-CN.ts`

- [ ] **Step 1: 在 en-US.ts 新增 5 个 key**

在 `report:` 对象(`common.success` 等所在顶层对象下,与 `report.*` 同级)中,确保包含:

```ts
'report.edit.title': 'Edit Report',
'report.edit.unavailable': 'SharePoint online edit is not configured in this environment.',
'report.edit.mockHint': 'Demo preview — your edits are not persisted to a real Microsoft 365 tenant.',
'report.detail.onlineEdit': 'Online Edit',
'report.detail.sharepointUnavailable': 'SharePoint online edit is not configured for this report.',
```

(根据现有文件结构,如果 `report.*` 是嵌套在 `pages.report.*` 或 `report.*` 下,放对应位置。)

- [ ] **Step 2: 在 zh-CN.ts 新增 5 个 key**

```ts
'report.edit.title': '编辑报告',
'report.edit.unavailable': '当前环境未配置 SharePoint 在线编辑。',
'report.edit.mockHint': '演示预览 — 编辑内容不会持久化到真实的 Microsoft 365 租户。',
'report.detail.onlineEdit': '在线编辑',
'report.detail.sharepointUnavailable': '此报告未配置 SharePoint 在线编辑。',
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm test -- ReportEdit ReportDetail 2>&1 | tail -50
```

期望:测试加载,不会出现 "missing translation" 警告 (若已配置 jest 的 i18n mock 检查)。

- [ ] **Step 4: Commit**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
git add lims-web-ui/src/locales/en-US.ts lims-web-ui/src/locales/zh-CN.ts
git -c user.email=claude@local -c user.name=Claude commit -m "feat(i18n): add SharePoint online-edit translation keys"
```

---

### Task 9: 前端 — `ReportEdit` 三态 + 测试

**Files:**
- Modify: `lims-web-ui/src/pages/report/ReportEdit/index.tsx`
- Create: `lims-web-ui/src/pages/report/ReportEdit/__tests__/index.test.tsx`

- [ ] **Step 1: 写失败测试**

`lims-web-ui/src/pages/report/ReportEdit/__tests__/index.test.tsx`:

```tsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import { useParams } from '@umijs/max';
import { useRequest } from 'ahooks';
import ReportEdit from '../index';

jest.mock('@umijs/max', () => ({
  PageContainer: ({ children }: any) => <div>{children}</div>,
  useParams: jest.fn(),
  history: { push: jest.fn() },
  useIntl: () => ({ formatMessage: ({ id }: any) => id }),
}));
jest.mock('ahooks', () => ({
  useRequest: jest.fn(),
}));

describe('ReportEdit', () => {
  it('renders empty-state when editUrl is null', () => {
    (useParams as jest.Mock).mockReturnValue({ id: 'rpt-1' });
    (useRequest as jest.Mock)
      .mockReturnValueOnce({ data: { data: {} } })           // getReport
      .mockReturnValueOnce({ data: { data: null }, loading: false }); // getReportEditUrl
    render(<ReportEdit />);
    expect(screen.getByText('report.edit.unavailable')).toBeInTheDocument();
  });

  it('renders iframe and mock hint when editUrl is mock', () => {
    (useParams as jest.Mock).mockReturnValue({ id: 'rpt-2' });
    const mockUrl = 'https://example.invalid/mock-sharepoint/rpt-2?action=edit';
    (useRequest as jest.Mock)
      .mockReturnValueOnce({ data: { data: { versionNumber: 'V1.0' } } })
      .mockReturnValueOnce({ data: { data: mockUrl }, loading: false });
    render(<ReportEdit />);
    expect(screen.getByTitle('report.edit.title')).toHaveAttribute('src', mockUrl);
    expect(screen.getByText('report.edit.mockHint')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试,确认失败**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm test -- ReportEdit 2>&1 | tail -30
```

期望:三态逻辑尚未实现 → `getByText('report.edit.unavailable')` 失败 / 没有 alert。

- [ ] **Step 3: 实现三态**

`lims-web-ui/src/pages/report/ReportEdit/index.tsx`(完整替换):

```tsx
import React from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { Card, Spin, Alert, Empty, App } from 'antd';
import { useParams, history, useIntl } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReportEditUrl, getReport } from '@/services/requestService';

const isMockUrl = (url: string | null | undefined) =>
  !!url && url.startsWith('https://example.invalid/');

const ReportEdit: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message } = App.useApp();
  const intl = useIntl();

  const { data: reportData } = useRequest(() => getReport(params.id));
  const { data: editUrlData, loading } = useRequest(() => getReportEditUrl(params.id));

  const report = reportData?.data;
  const editUrl = editUrlData?.data;

  return (
    <PageContainer
      title={`${intl.formatMessage({ id: 'report.edit.title' })} ${report?.versionNumber || ''}`}
      onBack={() => history.push(`/report/${params.id}`)}
    >
      <Card>
        {loading ? (
          <Spin tip={intl.formatMessage({ id: 'common.search' })} />
        ) : !editUrl ? (
          <Empty
            description={intl.formatMessage({ id: 'report.edit.unavailable' })}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        ) : (
          <>
            {isMockUrl(editUrl) && (
              <Alert
                style={{ marginBottom: 12 }}
                type="info"
                showIcon
                message={intl.formatMessage({ id: 'report.edit.mockHint' })}
              />
            )}
            <iframe
              src={editUrl}
              style={{ width: '100%', height: 'calc(100vh - 240px)', border: 'none' }}
              title={intl.formatMessage({ id: 'report.edit.title' })}
            />
          </>
        )}
      </Card>
    </PageContainer>
  );
};

export default ReportEdit;
```

- [ ] **Step 4: 重新运行测试,确认通过**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm test -- ReportEdit 2>&1 | tail -30
```

- [ ] **Step 5: lint + build**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm run lint
npm run build
```

- [ ] **Step 6: Commit**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
git add lims-web-ui/src/pages/report/ReportEdit/index.tsx \
        lims-web-ui/src/pages/report/ReportEdit/__tests__/index.test.tsx
git -c user.email=claude@local -c user.name=Claude commit -m "feat(report): three-state ReportEdit (empty/mock/real) + test"
```

---

### Task 10: 前端 — `ReportDetail` Online Edit 按钮可用性 + 测试

**Files:**
- Modify: `lims-web-ui/src/pages/report/ReportDetail/index.tsx`
- Modify: `lims-web-ui/src/pages/report/ReportDetail/__tests__/buttons.test.tsx`

- [ ] **Step 1: 读现有测试**

```bash
sed -n '1,120p' /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui/src/pages/report/ReportDetail/__tests__/buttons.test.tsx
```

熟悉现有结构,然后改:在现有 Online Edit 按钮测试附近加新用例。

- [ ] **Step 2: 写失败测试**

在 `buttons.test.tsx` 末尾添加(假设已有 mock setup):

```tsx
it('disables Online Edit when sharepointEditUrl is null', async () => {
  // existing mocks return a report with sharepointEditUrl: null
  // locate the Online Edit button
  const btn = screen.getByRole('button', { name: /online edit|在线编辑/i });
  expect(btn).toBeDisabled();
});
```

具体行匹配现有测试套件。

- [ ] **Step 3: 运行测试,确认失败**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm test -- ReportDetail 2>&1 | tail -30
```

- [ ] **Step 4: 修改 `ReportDetail/index.tsx`**

定位 `actionButtons` 块中 `getReportEditUrl` + `window.open(editUrlData.data)` 的逻辑,改为:

```tsx
const editUrl = reportData?.data?.sharepointEditUrl ?? null;
const hasEditUrl = !!editUrl;

<Button
  disabled={!hasEditUrl}
  onClick={() => hasEditUrl && window.open(editUrl, '_blank')}
  title={hasEditUrl ? undefined : intl.formatMessage({ id: 'report.detail.sharepointUnavailable' })}
>
  {intl.formatMessage({ id: 'report.detail.onlineEdit' })}
</Button>
```

> 原实现是 `onClick={() => window.open(editUrlData.data)}` 然后按钮总是渲染。这里替换为读取 `report.sharepointEditUrl`(在 `getReport` 时已带回来)而不是单独调 `getReportEditUrl`,避免重复请求。

- [ ] **Step 5: 重新运行测试**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm test -- ReportDetail 2>&1 | tail -30
```

- [ ] **Step 6: lint + build**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm run lint && npm run build
```

- [ ] **Step 7: Commit**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
git add lims-web-ui/src/pages/report/ReportDetail/index.tsx \
        lims-web-ui/src/pages/report/ReportDetail/__tests__/buttons.test.tsx
git -c user.email=claude@local -c user.name=Claude commit -m "feat(report): gate Online Edit button on sharepoint availability"
```

---

### Task 11: 文档 — `docs/runbook/uat-environment.md` 补 SharePoint env

**Files:**
- Modify: `docs/runbook/uat-environment.md`

- [ ] **Step 1: 找到合适锚点**

```bash
grep -n 'AZURE_AD_\|MINIO_\|EXTERNAL_' /Users/jiduobin/Documents/GitHub/Material-LIMS/docs/runbook/uat-environment.md
```

- [ ] **Step 2: 新增章节**

紧跟 `AZURE_AD_*` 章节之后插入:

```markdown
### SharePoint (online edit, optional)

| Env var | Default | Description |
| --- | --- | --- |
| `SHAREPOINT_ENABLED` | `false` | Master switch. `false` → NoOpSharePointClient. |
| `SHAREPOINT_HOSTNAME` | _(empty)_ | e.g. `contoso.sharepoint.com`. Required when enabled. |
| `SHAREPOINT_SITE_PATH` | `/sites/lims` | Graph site path. |
| `SHAREPOINT_LIBRARY` | `Documents` | Document library name. |
| `SHAREPOINT_FOLDER_ROOT` | `Reports` | Folder root under the library. |
| `SHAREPOINT_DRIVE_ID` | _(empty)_ | Optional. Skips site/drive resolution if set. |
| `SHAREPOINT_STRATEGY` | `folder_layout` | `folder_layout` (`{root}/{YYYY}/{MM}/`) or `flat`. |
| `SHAREPOINT_EDIT_URL_SUFFIX` | `?action=edit` | Appended to `driveItem.webUrl`. |

When `azure.ad.enabled=false` and `lims.demo.enabled=false`, NoOpSharePointClient is loaded and the
Online Edit button is disabled with the "not configured" tooltip.
```

- [ ] **Step 3: Commit**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
git add docs/runbook/uat-environment.md
git -c user.email=claude@local -c user.name=Claude commit -m "docs(runbook): document SHAREPOINT_* env vars"
```

---

### Task 12: 端到端冒烟 + 收尾

**Files:** 无新增

- [ ] **Step 1: 后端全模块构建**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
./mvnw -pl lims-common,lims-model,lims-dao,lims-service,lims-workflow,lims-web -am -DskipTests=false test
```

期望:全部测试通过(含新增的 SharePoint*Test 与 ReportServiceSharePointTest)。

- [ ] **Step 2: 前端全模块构建**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm run lint && npm test && npm run build
```

期望:全绿,build 产物生成。

- [ ] **Step 3: dev 启动 + 手动冒烟**

```bash
# 终端 1
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
./mvnw -pl lims-web spring-boot:run -Dspring-boot.run.profiles=dev
# 终端 2
cd /Users/jiduobin/Documents/GitHub/Material-LIMS/lims-web-ui
npm run dev
```

操作:
1. dev quick-login 任意 user
2. 选一个 DRAFT 状态 report
3. 触发 "创建报告"(若不存在),检查响应包含 `sharepointFileId: "mock-..."`
4. 进入 Report Detail,确认 Online Edit 按钮 enabled(因为 sharepointEditUrl 非空)
5. 点击 Online Edit → `/report/{id}/edit` 渲染 mock iframe + "演示预览" Alert
6. 返回 Report Detail,点击 "Sync from SharePoint" → 200,`file_url` 改变
7. 检查 dev profile 日志:MockSharePointClient 输出 "MockSharePoint upload: ..." / "MockSharePoint download: ..."

- [ ] **Step 4: 静态服务不启用场景冒烟**

```bash
cd /Users/jiduobin/Documents/GitHub/Material-LIMS
# 新增一个临时 config 模拟生产:azure.ad.enabled=false + lims.demo.enabled=false + sharepoint.enabled=false
cat > /tmp/noop-config.yml <<'YAML'
azure:
  ad:
    enabled: false
sharepoint:
  enabled: false
lims:
  demo:
    enabled: false
YAML
SPRING_CONFIG_ADDITIONAL_LOCATION=file:/tmp/noop-config.yml \
  ./mvnw -pl lims-web spring-boot:run -Dspring-boot.run.profiles=prod
```

(若 `prod` profile 不存在则改用无 profile 启动,确保 `azure.ad.enabled=false` 在 `application.yml` 默认值生效。)

期望:
- `NoOpSharePointClient` 被加载
- 创建 report 后 `sharepointFileId` 为 null
- Report Detail 的 Online Edit 按钮 disabled,tooltip 显示 "此报告未配置 SharePoint 在线编辑。"

- [ ] **Step 5: 全部 commit log 检查**

```bash
git log --oneline -15
```

期望:看到 12 个新 commit(spec 之前 2 个不计),无 WIP/TODO 残留。

- [ ] **Step 6: 撰写交付摘要(交付前必填)**

更新 CHANGELOG/或在本任务提交里写 deliverable summary,列出:
- 后端 7 个新文件、3 处修改
- 前端 4 处修改、1 个新测试
- 文档 1 处
- 验证命令 + 结果

---

## Self-Review

执行上述 plan 写作后:

**1. Spec coverage**

| Spec section | Covered by |
|---|---|
| §4.1 Architecture (3 implementations) | Tasks 5, 6 |
| §4.2.1 Interface | Task 3 |
| §4.2.2 Properties | Task 1 |
| §4.2.3 PathResolver | Task 2 |
| §4.2.4 Graph client | Tasks 4, 5 |
| §4.3.1 ReportService changes | Task 7 |
| §4.3.2 MicrosoftGraphClient additions | Task 4 |
| §4.4.1 Config block | Task 1 |
| §4.5.1 / 2 / 3 Frontend | Tasks 8, 9, 10 |
| §4.6 Error handling | Task 7 (try/catch in service) |
| §4.7 Testing | Tasks 1–10 (per-file tests) + Task 12 (e2e smoke) |
| §6 Risks / §7 Acceptance | Task 12 step 3–6 |

**2. Placeholder scan**

- Searched plan for "TODO", "TBD", "implement later", "fill in", "similar to" → none.
- Step 3 of Task 5 says "Implementation: re-call Graph GET … (left as a one-shot retry inside this catch — done by graph.getDriveItem path lookup if added)". **Fix**: this is a placeholder. Rewrite to require Task 4 追加 `getDriveItemByPath` 同 task 用真名。

**3. Type consistency check**

- `SharePointClient.uploadDocx(localPath, SharePointPath)` 在 Task 3 定义, Task 5、6、7 全部一致。
- `SharePointException` 在 Task 3 定义, Task 5、6 引用,Task 7 引用 — 一致。
- `SharePointProperties` 字段顺序在 Task 1 测试 / Task 2 测试 / Task 5 测试 / Task 6 测试 / Task 7 测试保持一致。
- `FileStorageService.openStream` 在 Task 6 引用 — **核对**: 当前 `FileStorageService` 是否真提供 `openStream(String)`。若是返回 `InputStream` 的方法,签名匹配;否则改为 `download(String)` 调用。
- `MockSharePointClient` 的 `localFileById` map 与 `FileStorageService.openStream` 期望输入必须确认一致(用 `localPath` 字符串,MinIO 上为 object key)。

Inline 修复: