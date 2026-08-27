# Changelog

All notable changes to **easypdf** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.0.0] - 2026-08-28

### Added
- **PDF → Markdown agent-friendly API (R4)**：
  - `PdfToMarkdownConverter` 三入口（`File` / `InputStream` / `DocumentStructure`）
  - `DocumentChunker` / `ChunkOptions` / `DocumentChunk`（按字符切片，保留页码锚点）
  - `PdfExtractionProperties`（`maxFileBytes`、`maxPages`、REST endpoint 等可配置）
  - `ExtractReport`（`success` / `document` / `error` / `warnings` 等）
  - `ExtractorMetrics`（进程级 success / failures.* 计数器 + `snapshot()` 诊断）
  - `extractPerPage`（流式逐页 + PageConsumer 返回 false 即中止 = 协作式取消令牌）
- **可观测性 (R5 D1-D4 + E1-E3)**：结构化日志、ExtractorMetrics 计数器、ExtractReport、取消令牌
- **集成测试骨架 (T02-T03)**：新增 `easypdf-it` 模块，由 `maven-failsafe-plugin` 跑 `**/*IT.java`
  - `MarkdownContractIT`：`NOT_FOUND` / `extractWithReport` / 4 个快照驱动用例（disabled）
  - `TierCancellationIT`：取消令牌合约（disabled）
  - `LimitsIT`：`maxFileBytes` / `maxPages` 超限合约（disabled）
  - `HtmlTemplateIT`：beetl / freemarker / thymeleaf 渲染合约（disabled）
  - `CjkFontIT`：CJK 字体回退合约（disabled）
  - `WebMvcPdfViewIT`（3.0.x 专属）：`AbstractITextPdfView` 真实产出 `application/pdf`
- **CVE / 依赖治理骨架 (T04-T05)**：`docs/security/known-vulns.md` + `dependency-suppression.xml` 空骨架
- **跨 JDK 真实 verify (T01)**：三分支在各自目标 JDK 上 `mvn clean verify` BUILD SUCCESS

### Security (R5)
- **A1 JavaScript stripping**：解析前从 PDF catalog 移除 `/JS` / `/JavaScript` / `/OpenAction`
- **A2 XXE 防御**：iText 解析器禁止外部实体；`PdfReader` 默认禁用外部 DTD
- **A4 DoS 软门限**：`PdfExtractionProperties.maxFileBytes` 在读取前拦截
- **A6 路径与日志净化**：`requireFile()` canonical path 校验；`escapeForLog()` 防日志注入

### Fixed
- **flexmark 0.64.8 在 JDK 8 上不能编译** (T01a)：1.0.x 降级到 flexmark 0.62.2；2.0.x / 3.0.x 保留 0.64.8
- 移除孤儿测试资源：`easypdf-beetl/src/test/resources/invoice.btl` 与 `easypdf-velocity/src/test/resources/template/invoice.vm`

### Documentation
- `docs/MAINTENANCE.md`：分支支持矩阵、依赖版本表、部署要求（`docs/MAINTENANCE.md`）
- `docs/security/known-vulns.md`：依赖风险登记与升级路线
- `easypdf-it/README.md`：集成测试当前状态与启用流程

## [2.0.0] - 2026-08-28

`feature/2.0.x` 与 3.0.x 同步维护线：JDK 17 + Maven 3.9.16。

表面 API 与 3.0.0 完全一致；差异仅在 pom（`maven.compiler.release=17`、`jakarta→javax` 在 webmvc 内仍为 javax）以及编译时 toolchain。

继承 3.0.0 全部变更。

## [1.0.0] - 2026-08-28

`feature/1.0.x` 维护线：JDK 8 + Maven 3.9.16，**bugfix-only**。

> 本版本不接收新特性。新功能请升级到 2.0.0+。详见 `docs/MAINTENANCE.md`。

继承 3.0.0 全部变更（除 `jakarta` 迁移外）；本分支独立降级：
- flexmark 0.62.2（最后一个 JDK 8 兼容版本）

---

## 版本约定

- **主版本号（X）**：JDK 基线变化（1.x=JDK 8 / 2.x=JDK 17 / 3.x=JDK 21）；不兼容 API 变更
- **次版本号（Y）**：新增特性，向后兼容
- **修订号（Z）**：bugfix

## 配套链接

- [docs/MAINTENANCE.md](docs/MAINTENANCE.md) — 分支支持矩阵、部署要求
- [docs/security/known-vulns.md](docs/security/known-vulns.md) — 已知漏洞与升级路线
- [docs/superpowers/plans/](docs/superpowers/plans/) — 历史执行计划
- [easypdf-it/README.md](easypdf-it/README.md) — 集成测试骨架说明

[Unreleased]: https://github.com/easy-4-java/easypdf/compare/v3.0.0...HEAD
[3.0.0]: https://github.com/easy-4-java/easypdf/releases/tag/v3.0.0
[2.0.0]: https://github.com/easy-4-java/easypdf/releases/tag/v2.0.0
[1.0.0]: https://github.com/easy-4-java/easypdf/releases/tag/v1.0.0