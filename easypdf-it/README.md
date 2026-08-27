# easypdf-it — End-to-end Integration Tests

集成测试模块。`mvn verify` 时由 `maven-failsafe-plugin` 跑 `**/*IT.java`，与单元测试
（`maven-surefire-plugin` 跑 `**/*Test.java`）解耦。

## 当前状态

- **Activity**（无 fixture 依赖，每个 `mvn verify` 都跑）：
  - `MarkdownContractIT#missingFileSurfacesAsNotFound`
  - `WebMvcPdfViewIT#abstractViewRendersValidPdf`（3.0.x 专属，jakarta servlet）
- **Disabled**（@Disabled，等待 fixture / 模板 / 字体就位）：
  - `MarkdownContractIT#singlePagePlainText`
  - `MarkdownContractIT#multiPageWithTable`
  - `MarkdownContractIT#taggedRoundTrip`
  - `MarkdownContractIT#cjkFallback`
  - `MarkdownContractIT#extractWithReportPopulatesMetrics`
  - `MarkdownContractIT#encryptedPdfIsClassified`
  - `TierCancellationIT#cancellationHaltsExtraction`
  - `LimitsIT#maxFileBytesTripsBeforeRead`
  - `LimitsIT#maxPagesTripsOnOpen`
  - `HtmlTemplateIT#*`
  - `CjkFontIT#*`

## 启用某个 Disabled 用例的步骤

1. 在 `src/test/resources/contracts/` 提交对应 fixture PDF（确定性的，可用 iText 单测生成）
2. 在 `src/test/resources/snapshots/` 提交人工 review 过的 markdown 期望输出
3. 移除对应测试方法上的 `@Disabled` 注解
4. 在对应分支上跑 `mvn -pl easypdf-it verify` 确认通过

## 跨分支状态

| 分支 | 集成测试范围 |
|---|---|
| `feature/3.0.x` | 完整（contract + render + webmvc，jakarta servlet） |
| `feature/2.0.x` | 同步 contract + render（javax servlet，跳过 webmvc） |
| `feature/1.0.x` | 同步 contract + render（javax servlet，跳过 webmvc） |

`WebMvcPdfViewIT` 仅在 3.0.x 维护——2.0.x / 1.0.x 用的是 `javax.servlet` API，单独维护
两个分支的 fork 成本太高。后续如有 webmvc IT 需求，改为按分支 switch import。