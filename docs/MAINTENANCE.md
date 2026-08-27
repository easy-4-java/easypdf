# easypdf 维护策略与部署要求

> 本文档描述三分支的维护级别、技术栈基线、部署要求与已知边界。
> 适用对象：维护者、部署工程师、智能体平台集成方。
> 最后更新：2026-08-27。

## 1. 版本线支持矩阵

| 分支 | JDK | 构建工具 | 维护级别 | 定位 |
|---|---|---|---|---|
| `feature/1.0.x` | 8 | Maven 3.9.16 | **仅修复**（bugfix-only） | 遗留兼容线：仅接受缺陷修复，不接受新功能 |
| `feature/2.0.x` | 17 | Maven 3.9.16 | **同步 + 轻维护** | 中间线：代码与 3.0.x 同步，依赖基线偏旧 |
| `feature/3.0.x` | 21 | Maven 4.0.0-rc-5 | **主开发线** | 全部新功能、新依赖、新引擎版本 |

### 1.1 依赖版本基线（当前，2026-08-27）

| 依赖 | 1.0.x | 2.0.x | 3.0.x |
|---|---|---|---|
| docx4j | 8.3.15 | 11.5.2 | 11.5.14 |
| iText | 7.1.10 | 7.1.10 | 7.1.10 |
| jsoup | 1.18.3 | 1.18.3 | 1.22.2 |
| freemarker | 2.3.29 | 2.3.29 | 2.3.34 |
| thymeleaf | 3.0.11.RELEASE | 3.0.11.RELEASE | 3.1.3.RELEASE |
| velocity | 2.2 | 2.2 | 2.4.1 |
| beetl | 3.0.13.RELEASE | 3.0.13.RELEASE | 3.21.2.RELEASE |
| JUnit Jupiter | 5.11.4 | 6.1.0 | 6.1.0 |

> 注：2.0.x 的依赖版本与 1.0.x 相同、代码与 3.0.x 同步——升级依赖时**优先 3.0.x**；2.0.x 仅在确有兼容需求时单独升级，并记录于本节。

## 2. 维护策略

### 2.1 分支开发规则

1. **新功能、新依赖、安全修复**：一律提交到 `feature/3.0.x`
2. **安全漏洞修复**：3.0.x 修复后**同步**到 2.0.x；1.0.x 仅在 JDK 8 仍可构建的前提下同步（依赖版本已冻结）
3. **bugfix-only 规则（1.0.x）**：不接受重构、改名、新 API；只接受编译/运行错误修复
4. 三分支源码同步方式：**对比整合（smart-port）**，不用 git merge——每次同步后跑全量 `clean verify`（基线 159 tests）

### 2.2 发布节奏（尚未执行）

当前三分支均为 `X.x.20260630-SNAPSHOT`，**未发布到 Maven Central**。

- 首次发布前需：版本转正（如 `3.0.0`）→ tag → `-P central` deploy → Central Portal publish
- 参考：easydoc 仓库已跑通同款流程（`-P central` + portal publish，需排除 consumer.pom）
- 发布顺序：3.0.x → 2.0.x → 1.0.x

## 3. 部署要求

### 3.1 运行时

| 项 | 要求 |
|---|---|
| JVM | 与所选分支匹配：1.0.x→8+，2.0.x→17+，3.0.x→21+（iText 7.1.10 字节码 major=51，低版本可跑） |
| 内存 | 大文件提取为页级流式（`extractPerPage`），单页驻留；整篇 `extract` 需约 2× 文件大小内存（100MB PDF ≈ 200-300MB 堆） |
| 系统字体 | `HtmlPdfConverter` 依赖系统字体渲染中文（`FontProvider.addSystemFonts()`）；服务器缺 CJK 字体时中文 PDF 会缺字——需安装字体或调用 `registerFont(path)` |
| 临时目录 | 流式/InputStream 入口使用 `File.createTempFile`；需保证 `java.io.tmpdir` 可写且有足够空间 |

### 3.2 建议 JVM 参数

```bash
# 智能体服务端（安全加固 + 稳定）
-Dfile.encoding=UTF-8
-Xmx2g                          # 依文档规模调整
-XX:+UseG1GC
-Dpdfbox.javascript.disabled=true   # 若引入 PDFBox 路径，禁用 JS（当前 iText 侧已剥离 catalog JS）
```

> 说明：easypdf 自身已禁用 iText 解析时的嵌入式 JS（catalog 顶层 `/JS`/`/JavaScript`/JS OpenAction 剥离）；上述 flag 为容器额外纵深防御。

### 3.3 配置项（`PdfExtractionProperties`）

| 字段 | 默认 | 说明 |
|---|---|---|
| `maxFileBytes` | 104857600（100MB） | 文件大小上限，≤0 不限制；超限抛 `LIMIT_EXCEEDED` |
| `maxPages` | 5000 | 页数上限，≤0 不限制 |
| `restEndpoint` / `restTimeoutMillis` / `restRetries` | null / 10000 / 0 | Tier3 ML 布局服务；`restRetries>0` 时指数退避重试（上限 3 次） |
| `headFactor` / `maxHeadingTiers` / `columnGapPt` / `streamAlignTolPt` / `coverRatio` / `coverRunMinLines` | 1.22 / 3 / 55 / 6 / 1.5 / 2 | 规则引擎启发式阈值，可按文档类型调优 |
| `cjkGapFactor` | 0.22 | 中英混排空格判定系数 |
| `cacheEnabled` | false | 提取结果 LRU 缓存（进程内，容量 16） |

## 4. 已知边界（诚实声明）

| 场景 | 现状 |
|---|---|
| 自有 PDF（`markdownToPdfTagged` 生成） | **无损往返**：标题/表格/列表/正文 100% 语义还原 |
| 外来电子版 PDF（规则引擎，默认） | ≈80% 结构还原（标题聚类/格线表/流式表/代码块/列表/题注/页眉剔除） |
| 外来 PDF + REST ML 服务（docling/MinerU 类） | 90-95%（由服务决定；**尚未与真实端点联调**，仅本地 HttpServer 契约验证） |
| 扫描件（纯图片无文本层） | **不支持**（空文本 + warning "no text"）；需配合 OCR（`ddd4j-ai-extension-ocr`） |
| 加密 PDF | 抛 `ExtractionException(ENCRYPTED)`（不自动解密） |
| 损坏 PDF | 抛 `ExtractionException(CORRUPT)` |
| 超大文件 | `LIMIT_EXCEEDED` 前置拦截（解析前） |
| 嵌入式 JS | 解析前剥离 catalog 顶层 JS 向量（纵深防御） |
| 多租户 | ExtractCache 为单进程 LRU，**无租户隔离**——平台化部署需自行按租户注入独立实例 |
| 输出净化 | 剥离仅覆盖 catalog 顶层；`/Names → /JavaScript` 深层树未递归清除（提取场景无风险；若做 PDF 净化输出需补） |

## 5. 升级与故障排查

1. **升级依赖**：改 3.0.x 根 pom `properties` → 全量 `clean verify` → 按 §2.1 同步 2.0.x（如需要）
2. **提取失败排查**：先看异常 Code（`CORRUPT/ENCRYPTED/LIMIT_EXCEEDED/NOT_FOUND`），再用 `extractWithReport` 取 warnings 与耗时
3. **中文乱码**：确认服务器已装 CJK 字体或调用 `HtmlPdfConverter.registerFont(path)`
4. **性能瓶颈**：大文档优先 `extractPerPage` + `summary` 决策，避免整篇 `extract`
