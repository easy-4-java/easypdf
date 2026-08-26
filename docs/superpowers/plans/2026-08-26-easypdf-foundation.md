# easypdf 地基修复计划（Phase 1：P0 构建缺陷 + 测试恢复）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 修复 easypdf 三分支的两个 P0 构建缺陷（`easypdf-xhtml/pom.xml` 重复 jsoup 声明导致 Maven 4 无法解析；`disable-javadoc-doclint` profile 常激活 `maven.test.skip=true` 导致全部测试静默跳过），使三分支 `clean verify` 真实执行测试并通过。

**Architecture:** 最小化 pom 编辑，不触碰任何 Java 源码。两处修复：① 删除 `easypdf-xhtml/pom.xml` 第 182-185 行第二处 `org.jsoup:jsoup` 依赖声明（保留第 50 行第一处，版本由根 pom dependencyManagement 统一管理为 1.22.2）；② 从根 pom `disable-javadoc-doclint` profile（`<jdk>[1.8,)</jdk>` 常激活）中移除 `<maven.test.skip>true</maven.test.skip>`。修复先在 3.0.x 验证，再按"对比后整合"同步到 1.0.x/2.0.x（非直接 merge）。

**Tech Stack:** Maven 4.0.0-rc-6（本地 `~/tools/apache-maven-4.0.0-rc-6/bin/mvn`，3.0.x）、Maven 3.9.16（`/opt/homebrew/bin/mvn`，1.0.x/2.0.x 为 modelVersion 4.0.0）、JDK 21、JUnit 5、JaCoCo 0.8.15。

## Global Constraints

- 只改 pom.xml，**零 Java 源码改动**；不改任何依赖版本
- 验证命令（3.0.x）：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify` 必须 BUILD SUCCESS，且日志出现 `Tests run:` 行（测试真实执行，不再出现 `No tests to run.` / `Not compiling test sources`）
- 验证命令（1.0.x/2.0.x）：`/opt/homebrew/bin/mvn -B -ntp clean verify` 必须 BUILD SUCCESS
- 提交信息遵循仓库风格：`fix(pom): ...`
- 每个 Task 末尾独立 commit；Task 完成标准以本文件 `[ ]` 勾选为准

---

### Task 1: 删除 easypdf-xhtml/pom.xml 重复的 jsoup 声明（3.0.x）

**Files:**
- Modify: `easypdf-xhtml/pom.xml:182-185`

**Interfaces:**
- Consumes: 无（独立修复）
- Produces: `easypdf-xhtml/pom.xml` 中 `org.jsoup:jsoup` 仅剩第 50 行一处声明（版本 1.22.2 由根 pom `dependencyManagement` 提供）

- [x] **Step 1: 确认缺陷现状**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp validate -pl easypdf-xhtml 2>&1 | grep -E "ERROR|BUILD"`
Expected: FAIL —— `'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique: org.jsoup:jsoup:jar`

- [x] **Step 2: 删除第二处声明**

编辑 `easypdf-xhtml/pom.xml`，删除第 182-185 行（`<dependency>` + `org.jsoup:jsoup` 两行 + `</dependency>`），保留第 50 行第一处声明。

- [x] **Step 3: 验证 validate 通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp validate -pl easypdf-xhtml 2>&1 | grep -E "ERROR|BUILD"`
Expected: BUILD SUCCESS，无 ERROR

- [x] **Step 4: Commit**

```bash
git add easypdf-xhtml/pom.xml
git commit -m "fix(pom): remove duplicate org.jsoup dependency declaration in easypdf-xhtml"
```

---

### Task 2: 移除 disable-javadoc-doclint profile 的 maven.test.skip（3.0.x）

**Files:**
- Modify: `pom.xml:444`（`disable-javadoc-doclint` profile 的 `<properties>` 块内 `<maven.test.skip>true</maven.test.skip>` 行）

**Interfaces:**
- Consumes: 无
- Produces: 根 pom 中 `disable-javadoc-doclint` profile 不再含 `maven.test.skip`；所有 JDK≥8 构建真实编译并运行测试

- [x] **Step 1: 确认缺陷现状**

Run: `/Users/wandl/.zcode/cli/exec/sess_3f63bf5d-4770-4e7f-a1cd-21abd9742e02/call_00_gcjsyO1jZE7ZWbTOBMXX8030-stdout.log` 中 `grep -c "Tests run:"`（或重新跑一次 Maven 3 build 确认）
Expected: 0（测试从未执行）—— 对照：easydoc 同 profile **没有** `maven.test.skip`，其 947 测试正常执行

- [x] **Step 2: 编辑根 pom**

删除根 `pom.xml` 第 444 行 `<maven.test.skip>true</maven.test.skip>`（保留同 profile 内 `lombok.version`、`additionalparam`、itext 版本属性）。

- [x] **Step 3: 验证测试真实执行**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core clean verify 2>&1 | grep -E "Tests run:|BUILD"`
Expected: BUILD SUCCESS，且 `Tests run:` 出现（核心模块测试实跑，不再出现 `No tests to run.`）

- [x] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "fix(pom): remove maven.test.skip from always-active disable-javadoc-doclint profile"
```

---

### Task 3: 3.0.x 全量 clean verify（Maven 4）+ 修复暴露的测试失败

**Files:**
- 视结果而定：engine 模块测试若失败，修复对应测试/资源（不修生产代码）

**Interfaces:**
- Consumes: Task 1、Task 2 的修复
- Produces: 3.0.x 全量构建 BUILD SUCCESS + 全模块测试真实执行；测试计数基线

- [x] **Step 1: 全量构建**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify 2>&1 | tail -40`
Expected: BUILD SUCCESS；若有模块测试失败（历史遗留：测试从未跑过，失败属预期），进入 Step 2

- [x] **Step 2: 逐个处理测试失败（如存在）**

对每个失败测试：读取失败原因（`grep -A5 "FAIL" target/surefire-reports/*.txt`），区分环境性问题（字体缺失、平台路径）与真实缺陷；修复测试或资源文件，**不修改生产逻辑**；重跑对应模块 `mvn -pl <module> clean verify` 直至通过。

- [x] **Step 3: 统计测试基线并记录**

Run: `grep -h "Tests run:" */target/surefire-reports/*.txt 2>/dev/null | awk -F'[ ,]+' '{s+=$3} END {print "TOTAL:", s}'`（或从日志汇总）
Expected: 输出 > 0 的总测试数（记录到最终报告）

- [x] **Step 4: Commit（如有修复）**

```bash
git add -A
git commit -m "test: fix tests exposed after enabling test execution"
```

---

### Task 4: 同步修复到 1.0.x / 2.0.x 并验证（对比整合，非 merge）

**Files:**
- Modify: `easypdf-xhtml/pom.xml:182-185`（两个分支）、`pom.xml:444`（两个分支）

**Interfaces:**
- Consumes: Task 1-3 的修复内容（两处删除）
- Produces: 三分支均可构建；1.0.x/2.0.x 使用 Maven 3.9.16 验证（modelVersion 4.0.0）

- [x] **Step 1: 核对分支差异**

Run: `git diff feature/3.0.x feature/1.0.x -- easypdf-xhtml/pom.xml | grep -n "jsoup"` 与 `git diff feature/3.0.x feature/2.0.x -- easypdf-xhtml/pom.xml | grep -n "jsoup"`
Expected: 确认重复声明行号在 1.0.x/2.0.x 同样存在（第 50 行 + 第 183 行，之前已确认三分支均有）

- [x] **Step 2: 应用修复到 1.0.x**

```bash
git checkout feature/1.0.x
# 删除 easypdf-xhtml/pom.xml 第二处 jsoup 声明（行号同 3.0.x）
# 删除根 pom disable-javadoc-doclint profile 中 maven.test.skip=true 行
```

- [x] **Step 3: 验证 1.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify 2>&1 | tail -30`
Expected: BUILD SUCCESS + `Tests run:` 出现

- [x] **Step 4: Commit 1.0.x**

```bash
git add -A
git commit -m "fix(pom): remove duplicate jsoup declaration and maven.test.skip (sync from 3.0.x)"
```

- [x] **Step 5: 应用修复到 2.0.x**（同 Step 2）

```bash
git checkout feature/2.0.x
# 两处删除
```

- [x] **Step 6: 验证 2.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify 2>&1 | tail -30`
Expected: BUILD SUCCESS + `Tests run:` 出现

- [x] **Step 7: Commit 2.0.x**

```bash
git add -A
git commit -m "fix(pom): remove duplicate jsoup declaration and maven.test.skip (sync from 3.0.x)"
```

- [x] **Step 8: 回到 3.0.x 并推送三分支**

```bash
git checkout feature/3.0.x
git push origin feature/1.0.x feature/2.0.x feature/3.0.x
```

---

## Self-Review

- **Spec 覆盖**：P0-1（jsoup 重复）→ Task 1/4；P0-2（maven.test.skip）→ Task 2/4；三分支构建验证 → Task 3/4；"对比后整合非 merge" → Task 4 Step 1/2/5 显式 diff 核对
- **占位符扫描**：无 TBD/TODO；Task 3 Step 2 的"如存在"分支是条件性步骤（失败测试是否出现不可预知），非占位符
- **类型一致性**：无跨任务类型依赖；验证命令在 Global Constraints 中统一定义
