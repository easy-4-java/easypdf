# easypdf-it 集成测试模块（生产就绪补完 T02-T03）

**日期**：2026-08-28
**分支**：`feature/1.0.x`、`feature/2.0.x`、`feature/3.0.x`
**目标**：补上 R5 安全加固后的端到端集成层，使 `mvn verify` 不再只跑单测。

## 1. 模块结构

```
easypdf-it/
├── pom.xml                                  # failsafe-plugin，verify 阶段跑
├── README.md                                # 当前活跃用例与禁用用例清单
└── src/test/
    ├── java/io/github/easy4j/pdf/it/
    │   ├── contract/
    │   │   ├── MarkdownContractIT.java      # 5 个用例：4 个 disabled（fixture 待补）+ 1 个 active
    │   │   ├── TierCancellationIT.java      # 1 个 disabled（fixture 待补）
    │   │   └── LimitsIT.java                # 2 个 disabled（fixture 待补）
    │   ├── render/
    │   │   ├── HtmlTemplateIT.java          # 3 个 disabled（模板待补）
    │   │   └── CjkFontIT.java               # 1 个 disabled
    │   └── webmvc/
    │       └── WebMvcPdfViewIT.java         # 1 个 active（3.0.x 专属）
    └── resources/
        ├── contracts/                       # fixture PDF 占位（.gitkeep）
        ├── snapshots/                       # markdown 期望快照占位（.gitkeep）
        └── templates/                       # 模板占位（.gitkeep）
```

## 2. 跨分支状态

| 分支 | 包含 webmvc IT | parent version | failsafe-plugin version | jakarta 依赖 |
|---|---|---|---|---|
| `feature/3.0.x` | ✓ | `3.0.x.20260630-SNAPSHOT` | pluginManagement 注入 | ✓ provided |
| `feature/2.0.x` | ✗ | `2.0.x.20260630-SNAPSHOT` | 显式 `${maven-failsafe-plugin.version}` | ✗ |
| `feature/1.0.x` | ✗ | `1.0.x.20260630-SNAPSHOT` | 显式 `${maven-failsafe-plugin.version}` | ✗ |

> 注：3.0.x 的 root pom `<pluginManagement>` 已有 `maven-failsafe-plugin.version`，所以子 pom 不必再写 `<version>`。2.0.x / 1.0.x 缺少 pluginManagement 注入，子 pom 必须显式声明，否则报 `plugin version is missing`。

## 3. 同步规则（已知踩坑）

### 3.1 不要 `git checkout feature/3.0.x -- pom.xml` 把整个 pom 覆盖到 2.0.x/1.0.x

我第一次同步时犯了这个错：把 3.0.x 的 pom.xml 整体覆盖到 2.0.x 上，把 `<java.version>21</java.version>` 与 `<maven.version>4.0.0-rc-5</maven.version>` 写进了 2.0.x 的 pom，导致依赖解析完全挂掉（spring-webmvc/flexmark/failsafe 全部 `version is missing`）。

正确做法：
- 用 `git checkout feature/3.0.x -- easypdf-it/` 只同步新增目录
- 在 2.0.x / 1.0.x 的 root pom 上**只追加**一行 `<module>easypdf-it</module>`，不替换其他任何内容
- 子 pom 的 `<parent><version>` 必须按目标分支改写

### 3.2 WebMVC servlet 包跨分支

3.0.x 用 `jakarta.servlet.*`，2.0.x / 1.0.x 用 `javax.servlet.*`。
IT 里的 `WebMvcPdfViewIT` 在 3.0.x 上 import jakarta，在 2.0.x / 1.0.x 上需要 fork 一份 javax 版本；本轮选择"只维护 3.0.x 版本"——维护两份分支差异成本大于收益。如果 webmvc IT 在 2.0.x / 1.0.x 上有需求，应改为按 `<profile>` 切换 import。

### 3.3 Failsafe plugin 版本来源

3.0.x 的 root pom 已有 `<pluginManagement>` 锁定 failsafe 版本；2.0.x / 1.0.x 没有，子 pom 必须显式：
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <version>${maven-failsafe-plugin.version}</version>
  ...
</plugin>
```

## 4. 当前活跃用例（每个 verify 都执行）

| 用例 | 分支 | 验证内容 |
|---|---|---|
| `MarkdownContractIT#missingFileSurfacesAsNotFound` | 全 | `PdfStructureExtractor.extract(不存在文件)` 抛 `ExtractionException(Code.NOT_FOUND)` |
| `WebMvcPdfViewIT#abstractViewRendersValidPdf` | 3.0.x | `AbstractITextPdfView.render()` 写入 `MockHttpServletResponse`，响应头 `application/pdf`、body 以 `%PDF-` 起首、`PdfReader` 能解析且至少 1 页 |

| 用例 | 分支 | 等待 |
|---|---|---|
| `MarkdownContractIT#singlePagePlainText` | 全 | fixture PDF + 人工 review 过的 markdown 快照 |
| `MarkdownContractIT#multiPageWithTable` | 全 | 同上 |
| `MarkdownContractIT#taggedRoundTrip` | 全 | 同上 |
| `MarkdownContractIT#cjkFallback` | 全 | 同上 + 系统安装中文字体 |
| `MarkdownContractIT#extractWithReportPopulatesMetrics` | 全 | 同上 |
| `MarkdownContractIT#encryptedPdfIsClassified` | 全 | 加密 PDF fixture |
| `TierCancellationIT#cancellationHaltsExtraction` | 全 | ≥ 5 页 fixture |
| `LimitsIT#maxFileBytesTripsBeforeRead` | 全 | 已知大小的 fixture |
| `LimitsIT#maxPagesTripsOnOpen` | 全 | 已知页数的 fixture |
| `HtmlTemplateIT#beetlInvoice` | 全 | `templates/beetl-invoice.btl` |
| `HtmlTemplateIT#freemarkerReport` | 全 | `templates/freemarker-report.ftl` |
| `HtmlTemplateIT#thymeleafLetter` | 全 | `templates/thymeleaf-letter.html` |
| `CjkFontIT#cjkRenderingRoundTrip` | 全 | 字体 + 中文模板 |

## 5. 验证结果

| 分支 | JDK | 模块数 | 用时 | 结果 |
|---|---|---|---|---|
| `feature/3.0.x` | Microsoft OpenJDK 21.0.12.1 | 15 | 18.1 s | BUILD SUCCESS |
| `feature/2.0.x` | Amazon Corretto 17.0.20 | 15 | 19.0 s | BUILD SUCCESS |
| `feature/1.0.x` | Amazon Corretto 1.8.0_504 | 15 | 20.2 s | BUILD SUCCESS |

每次 verify 输出：单元测试 159 全绿 + 集成测试 15 个（13 disabled + 2 active），0 失败 / 0 错误 / 0 跳过（disabled 不计入失败计数）。

## 6. 启用某个 disabled 用例的步骤

1. 在 `src/test/resources/contracts/` 提交确定性 PDF fixture（建议用 iText 单测生成，
   不引入外部随机数据）
2. 在 `src/test/resources/snapshots/` 提交人工 review 过的 markdown 期望输出
3. 移除对应测试方法上的 `@Disabled` 注解
4. 跑 `mvn -pl easypdf-it verify` 确认通过

## 7. 后续动作

- T04：root pom 加 dependency-check-maven (warn-only)，三分支各跑一遍生成 baseline
- T05：写 docs/security/known-vulns.md 跟踪 Critical/High 处置计划
- T06：CHANGELOG.md + tag
- T08-T13：cyclonedx / ci.yml / 文档 / 受保护分支 / settings.xml / final verify