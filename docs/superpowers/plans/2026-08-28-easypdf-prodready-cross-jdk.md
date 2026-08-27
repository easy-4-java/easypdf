# Cross-JDK Real Verify（生产就绪补完 T01）

**日期**：2026-08-28
**分支**：`feature/1.0.x`、`feature/2.0.x`、`feature/3.0.x`
**目标**：在每个分支的目标 JDK 上跑完整 `mvn clean verify`，暴露并修复任何"源一致但实际不可构建"的隐患。

## 1. 执行环境

| 项 | 值 |
|---|---|
| 系统 | macOS 26.5.2 arm64 |
| 系统 Maven | Apache Maven 3.9.16（Homebrew） |
| 系统 Java | OpenJDK 26.0.1（Homebrew，**不用于构建**） |
| Maven Wrapper | 1.0.x / 2.0.x → 3.9.16；3.0.x → 4.0.0-rc-5 |
| 实际构建 JDK | 用 `JAVA_HOME=$(/usr/libexec/java_home -v N)` 切换 |

| JDK 安装 | 来源 | 用于分支 |
|---|---|---|
| `1.8.0_504` arm64 | Amazon Corretto 8 | `feature/1.0.x` |
| `17.0.20` arm64 | Amazon Corretto 17 | `feature/2.0.x` |
| `21.0.12.1` arm64 | Microsoft OpenJDK 21 | `feature/3.0.x` |

> 备注：Maven Wrapper jar (`maven-wrapper.jar`) 在仓库根 `.mvn/wrapper/` 下不存在，导致 `mvnw` 无法启动。改用系统 `mvn` + `JAVA_HOME` 切换可绕过；后续可在 CI 上修复（CI 走 `actions/setup-java` 拉 JDK + 系统 `mvn`，与本机方式一致）。

## 2. 暴露的真实漏洞：flexmark 字节码不兼容

**症状**：`easypdf-xhtml` 模块编译失败，错误为

```
错误的类文件: .../com/vladsch/flexmark/flexmark-ext-gfm-strikethrough/0.64.8/...jar
  类文件具有错误的版本 55.0, 应为 52.0
```

**根因**：`pom.xml` 第 78 行硬编码 `<flexmark.version>0.64.8</flexmark.version>`。该版本是 major=55（Java 11+）字节码，不能在 JDK 8 上加载。

**影响**：1.0.x 分支在 JDK 8 上完全无法编译，之前 R5 测试一直在 3.0.x / JDK 21 上跑过，没暴露这个回归。

**修复**：仅在 `feature/1.0.x` 分支把 `flexmark.version` 降到 `0.62.2`（最后一个 JDK 8 兼容的 flexmark）。2.0.x / 3.0.x 保留 0.64.8（已在 JDK 17 / 21 上验证可用）。

```diff
- <flexmark.version>0.64.8</flexmark.version>
+ <flexmark.version>0.62.2</flexmark.version>
```

**Commit**：`feature/1.0.x` 上 `1244581 fix: downgrade flexmark to 0.62.2 on 1.0.x branch (0.64.8 is JDK 11+, breaks JDK 8 build)`。

**为何不同步到 2.0.x / 3.0.x**：这是符合预期的"分支差异"（与 webmvc servlet 的 javax/jakarta 差异同性质），不属于"源不一致"。

## 3. 各分支验证结果

| 分支 | JDK | Maven | 模块数 | 测试 | 用时 | 结果 |
|---|---|---|---|---|---|---|
| `feature/1.0.x` | Corretto 1.8.0_504 | 3.9.16 | 14 | 107 (xhtml) + 52 (其他) ≈ 159 | 20.9 s | **BUILD SUCCESS** |
| `feature/2.0.x` | Corretto 17.0.20 | 3.9.16 | 14 | 同上 | 16.1 s | **BUILD SUCCESS** |
| `feature/3.0.x` | Microsoft 21.0.12.1 | 4.0.0-rc-5 | 14 | 同上 | 17.1 s | **BUILD SUCCESS** |

每条命令的最终 Reactor 末行均为 `BUILD SUCCESS`；失败 0 / 错误 0 / 跳过 0。

> 注：JaCoCo 在跨模块时输出 "Skipping JaCoCo execution due to missing execution data file"，这是因为准备 agent 在子 module 跑而 root module 没跑——不影响覆盖率门（90% 行覆盖仍由各 module 单独检查）。

## 4. 配套发现（不属于本轮 fix 范围，记入下一轮）

1. **`maven-wrapper.jar` 缺失**：`mvnw` 不可用。CI 当前也用系统 `mvn`（非 wrapper），所以 CI 上不会暴露；但本地开发者克隆后 `./mvnw` 会失败。可考虑：
   - `mvn -N wrapper:wrapper` 重新生成 wrapper（会引入 wrapper 插件作为 root dep）
   - 或在 README 加一句"请用 `JAVA_HOME=... mvn clean verify`"
2. **JaCoCo 报告缺失 exec**：root module 没有源文件，所以 `prepare-agent` 不生成 exec，root module 的 `report` / `check` 跳过。这不是 bug，但 JaCoCo `check` 因此只在子 module 上真正生效。
3. **mvn 默认跑在 JDK 26**：Homebrew 把 `mvn` 链接到 JDK 26 上，所以任何忘了 `export JAVA_HOME=...` 的命令都会以 JDK 26 跑——超出 3.0.x 的 `requireJavaVersion [21.0,)` enforcer 范围时会挂掉（这次没挂是恰好没有触发）。建议在仓库根 `.envrc` 或 README 写一行警告。

## 5. 验证产物

- 本次 verify 不产生 commit 除 fix 之外；目标只是"能跑过"
- 各 module `target/` 目录保留，便于后续 inspect
- 没有产生测试快照、SBOM、CVE 报告（这些由后续 T04 / T06 / T08 处理）

## 6. 收尾

- 1.0.x commit `1244581` 等待推送（T01z 阶段一并推）
- 2.0.x / 3.0.x 本轮无 commit
- 推进到 T02（`easypdf-it` 集成测试模块）