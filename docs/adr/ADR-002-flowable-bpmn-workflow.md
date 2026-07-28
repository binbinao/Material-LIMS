# ADR-002: Flowable BPMN for Request Workflow

## Status
Accepted (2026-06-04)

## Context
委托单（Request）有明确的生命周期状态流转：草稿 → 已提交 → 已分配 → 取样中 → 报告中 → 审批中 → 已完成/已驳回。每个阶段有不同的角色参与（Requester→Manager→Technician→Engineer→Manager）。需要一个流程引擎来管理状态流转、角色分配和审批决策。

## Decision
采用 **Flowable 7 BPMN 2.0** 嵌入式引擎管理委托工作流。

### BPMN 流程设计

```
StartEvent → AssignTask(MANAGER) → ManagerDecision(Gateway)
  ├─ assign → SampleTask(TECHNICIAN) → ReportTask(ENGINEER)
  │            → ApproveTask(MANAGER) → ApproveDecision(Gateway)
  │              ├─ approve → EndCompleted
  │              └─ reject  → ReportTask (loop back)
  └─ reject → EndRejected
```

### 关键设计决策
- **Business Key = requestId**：Flowable 流程实例与业务数据通过 `requestId` 关联
- **Candidate Groups = ROLE_xxx**：使用 Spring Security 角色作为 Flowable candidate group，JWT 角色与 Flowable 权限天然对齐
- **双轨状态**：业务表 `request.status` + Flowable 流程节点位置，两者同步更新（同事务包裹）

## Consequences

### Positive
- BPMN 图形化流程，业务人员可读
- 嵌入式部署，无需额外基础设施
- Candidate Group 与 Spring Security Role 对齐，鉴权一致
- 流程变更只需修改 BPMN XML，无需改 Java 代码

### Negative
- 业务状态（request.status）与 Flowable 状态存在双写不一致风险（已通过 @Transactional + verifyStateConsistency 缓解）
- Flowable 7 依赖较重（~30MB）
- 流程退回（approve→reject→reportTask）在 BPMN 中是 loop back，非标准子流程模式

### Alternatives Considered
- **自研状态机**：更轻量但缺少可视化、审计和复杂网关支持。随着流程复杂度增加，自研成本会指数增长
- **Camunda 7/8**：与 Flowable 同源（均派生自 Activiti），API 相似但 Flowable 的 Spring Boot Starter 集成更简洁
- **AWS Step Functions / Temporal**：适合微服务编排，但对于单体应用中的简单流程过于重量级

## Compliance
- State consistency verification added: `WorkflowService.verifyStateConsistency(requestId, expectedStatus)`
- All state transitions wrapped in `@Transactional(rollbackFor = Exception.class)`

## Date
2026-06-04