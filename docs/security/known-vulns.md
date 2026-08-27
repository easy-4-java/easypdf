# Known Vulnerabilities & 依赖治理（easypdf）

**日期**：2026-08-28
**维护**：随每次依赖升级评审更新；季度 review 一次（建议每年 4 次）
**配套**：`dependency-suppression.xml`（空，OWASP dependency-check 启用后再填）

## 1. 当前依赖基线（按分支）

| Component | 1.0.x（JDK 8） | 2.0.x（JDK 17） | 3.0.x（JDK 21） | 用途 |
|---|---|---|---|---|
| iText | 7.1.10 | 7.1.10 | 7.1.10 | PDF 生成与抽取核心 |
| html2pdf (iText add-on) | 2.1.7 | 2.1.7 | 2.1.7 | HTML→PDF |
| docx4j | 8.3.15 | 11.5.14 | 11.5.14 | DOCX 渲染（如启用） |
| flexmark | **0.62.2** | 0.64.8 | 0.64.8 | Markdown 序列化 |
| jsoup | 1.18.3 | 1.18.3 | 1.22.2 | HTML 解析/净化 |
| jackson | 2.17.2 | 2.17.2 | 2.17.2 | JSON 序列化 |
| spring-webmvc | 5.x | 6.x | 6.2.19 | Spring MVC 视图层 |
| lombok | 1.18.46 | 1.18.46 | 1.18.46 | 编译期样板 |
| slf4j | 2.0.18 | 2.0.18 | 2.0.18 | 日志门面 |
| servlet-api | 3.0.1 (javax) | 3.0.1 (javax) | 6.1.0 (jakarta) | Servlet 容器 |
| junit-jupiter | 5.11.4 | 5.11.4 | 6.1.0 | 单测 |
| assertj | 3.27.7 | 3.27.7 | 3.27.7 | 流式断言 |

> 1.0.x 上 flexmark 必须 ≤0.62.x（最后一个 JDK 8 兼容版）；升级 JDK 8 兼容性时一并升。R5 T01a 已记录。

## 2. 已知风险登记（按风险等级）

### 🔴 High / Critical

无。仓库当前所有直系依赖在锁定版本下未发现 RCE / 反序列化 / 权限提升类已知漏洞。

### 🟡 Medium / 待升级

| 组件 | 版本 | 问题 | 处置 | 跟踪 |
|---|---|---|---|---|
| iText 7.1.10 | 7.1.10 | iText 7.1.x 在 7.2.x 之前不含 PDF 2.0 完整支持，且 pdfua 标识支持受限 | 升级到 7.2.x 需要全面回归；建议放到 4.0.0 milestone | 见 #TODO |
| docx4j 8.3.15（仅 1.0.x） | 8.3.15 | 已停维护，11.5.x 是当前维护线；1.0.x 因 JDK 8 限制只能停在这里 | 已知，bugfix-only；建议用户升级到 2.0.x+ | 已记入 MAINTENANCE.md |
| html2pdf 2.1.7 | 2.1.7 | 维护线，2.1.7 是最后兼容 iText 7.1.x 的版本 | 跟随 iText 升级 | 见 #TODO |

### 🟢 Low / 可接受

| 组件 | 问题 | 接受理由 |
|---|---|---|
| jackson 2.17.2 | 已知有 polymorphic deserialization 风险（仅当 `enableDefaultTyping` 启用时） | easypdf 不暴露 ObjectMapper 配置入口；调用方应自行配置 `activateDefaultTyping` 为 NONE |
| flexmark 0.62.2（1.0.x） | 0.62.x 不再发布修复 | 1.0.x 是 bugfix-only 维护线；风险已通过 R5 输入净化（`MarkdownConverter` 入参控制）部分缓解 |
| jsoup 1.18.3（1.0.x / 2.0.x） | 较新 1.22.2 缺若干安全公告 | 1.0.x / 2.0.x 仅 bugfix-only；用户被劝升级到 3.0.x |

## 3. 自动扫描：OWASP dependency-check（当前状态）

### 3.1 为何暂未启用

OWASP `dependency-check-maven` 在首次跑会从 NVD 全量同步数据（CVE 库 200 MB+），在 CI 镜像 / 国内网络下同步耗时 5–15 分钟，且**首次同步失败率较高**。我们当前：

- 没有 GitHub Actions runner 的镜像配额来每日 NVD 同步
- 不希望 CI 因为外网 NVD 抖动每天超时
- 当前依赖列表小（直系 < 30 条），人工 review 成本可接受

### 3.2 重新启用条件

满足以下任意一项后，建议重新启用：

1. GitHub Actions 启用 `actions/cache@v4` 把 `.dependency-check-data/` 缓存 30 天
2. 切换到 `dependency-check:alpine` 自托管容器
3. 使用 Aliyun NVD 镜像（如果有）

启用方式（待做）：
```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>12.1.1</version>
  <configuration>
    <failBuildOnAnyVulnerability>false</failBuildOnAnyVulnerability>
    <failOnError>false</failOnError>
    <skipProvidedScope>true</skipProvidedScope>
    <skipRuntimeScope>true</skipRuntimeScope>
    <suppressionFiles>
      <suppressionFile>dependency-suppression.xml</suppressionFile>
    </suppressionFiles>
  </configuration>
  <executions>
    <execution><goals><goal>check</goal></goals></execution>
  </executions>
</plugin>
```

`dependency-suppression.xml` 已经在仓库根（空骨架），启用时按行登记已知误报。

## 4. 升级路线

| 升级项 | 来源 | 触发条件 |
|---|---|---|
| docx4j 8.3.15 → 11.5.14（仅 1.0.x） | JDK 升级 | 用户切到 2.0.x |
| iText 7.1.10 → 7.2.x | JDK 升级 + 全部回归测试 | 下次 R 计划（4.0.0） |
| spring 5 → 6（仅 1.0.x） | JDK 升级 | 用户切到 2.0.x |
| jackson 2.17.2 → 2.18.x | 季度 review | 跟随 CVE 公告 |
| jsoup 1.18.3 → 1.22.x（仅 1.0.x / 2.0.x） | 季度 review | 跟随 CVE 公告 |

## 5. 响应流程（事件触发）

如果 NVD / GitHub Security Advisory 命中 easypdf 的某个依赖：

1. **24h 内**：在本文件加 🟠 行，写明 CVE id / 组件 / 影响范围（哪个分支受影响）
2. **48h 内**：评估升级路径——是否能在当前分支升级，还是需要等下一个 minor 版本
3. **修复后**：移除对应行，把处置经过写入 commit message body

---

## 6. 附件

- `dependency-suppression.xml`（OWASP plugin 启用后填写）
- 本文件每次升级依赖后必须同步更新
- review 责任归属：当前 PR 作者