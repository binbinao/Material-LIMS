
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
