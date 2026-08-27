# 发布到 Maven Central（手动）

Maven Central 发布链路已经在 `pom.xml` 配好（`central` profile + `central-publishing-maven-plugin` 0.11.0）。
本文件是"凭证已就位、第一次 push"的 step-by-step。

## 1. 前置条件

- 已注册 Sonatype 账号：https://issues.sonatype.org/
- 已申请命名空间 `io.github.easy4j`（提供 GitHub 仓库链接作为所有权证明）
- 已生成 GPG key 并把公钥推到 `keys.openpgp.org`（详见 `docs/security/signing.md`）
- 本机 `~/.m2/settings.xml` 已写入 `<server id="central">` 与 `<server id="gpg.passphrase">`
- 三个分支都已通过 `mvn clean verify` BUILD SUCCESS（参见 `2026-08-28-easypdf-prodready-*.md`）

## 2. 准备 release（仅第一次）

确认根 pom 的 `<distributionManagement>` 块（已存在，3.0.x 上 Aliyun mirror）：

```xml
<distributionManagement>
  <repository>
    <id>central</id>   <!-- 与 settings.xml <server id> 一致 -->
    <name>Central Repository</name>
    <url>https://repo.maven.apache.org/maven2</url>
  </repository>
  <snapshotRepository>
    <id>central-snapshots</id>
    <name>Central Snapshots Repository</name>
    <url>https://central.sonatype.com/repository/maven-snapshots</url>
  </snapshotRepository>
</distributionManagement>
```

> 实际上当前 root pom 用的是 Aliyun mirror（`2624322-release-6F6h6R`）。
> 这是为阿里云仓库预留的；切到 Central 需替换 URL。

## 3. 第一次发布 v3.0.0

```bash
# 1. 切到 3.0.x 主开发线
git checkout feature/3.0.x

# 2. 准备发布（会 bump version 到 3.0.0、改 SNAPSHOT、签 tag）
mvn -P central release:prepare
#  - 提示 tagName：默认 v3.0.0
#  - 提示 release version：默认 3.0.0
#  - 提示 next development version：默认 3.0.1-SNAPSHOT
#  - 提示 GPG passphrase：填入
#  - 触发 sign-artifacts goal

# 3. 执行发布（推送 jar / pom / sources / javadoc / GPG 签名 / SBOM 到 Central）
mvn -P central release:perform
#  - 自动 checkout tag v3.0.0
#  - 执行 deploy：mvn deploy -P central
#  - central-publishing-maven-plugin 把 staging 推到 Sonatype
#  - autoPublish=true 自动 close→release
#  - waitUntil=published 等待 publish 完成

# 4. 验证
open https://repo.maven.apache.org/maven2/io/github/easy4j/easypdf-core/3.0.0/
#  应看到：easypdf-core-3.0.0.jar / .pom / .asc / sources.jar / javadoc.jar / .module
```

## 4. 同时发布 v2.0.0 与 v1.0.0

2.0.x 与 1.0.x 走完全相同流程，分别在各自分支上跑 `release:prepare && release:perform`。
注意：
- 三个分支的 `<version>` 是 `1.0.x.20260630-SNAPSHOT` / `2.0.x.20260630-SNAPSHOT` / `3.0.x.20260630-SNAPSHOT`
- release:prepare 会把 `<version>` 改成 `1.0.0` / `2.0.0` / `3.0.0`，并 commit
- 第一次发布建议手工 `git commit --amend` 后再 push，避免 `release:prepare` 自动 push 干扰其他工作

## 5. 镜像与 Aliyun

如果 Sonatype 直连失败（大陆网络常见），临时方案：

```bash
# pom 里加 mirror（不要 commit，仅本机）
mvn -P central -Dmaven.deploy.repo.url=https://maven.aliyun.com/repository/central \
    release:perform
```

但要意识到：Aliyun mirror 不是 Sonatype Central，签名 / GPG 链不上 Central 的审核流程。
**正式发布务必走 Sonatype 原地址**。

## 6. 发布失败回滚

如果 `release:perform` 中途失败：

```bash
# 删除本地 tag（不 push）
git tag -d v3.0.0

# 回滚 release:prepare 改动的 pom version（如果已 commit）
git log --oneline -5  # 找 release:prepare 自动 commit
git reset --hard HEAD~2

# 重新执行前先 mvn clean
mvn clean
```

## 7. 验证发布成功

- https://repo.maven.apache.org/maven2/io/github/easy4j/ 应列出所有发布的 artifact
- https://search.maven.org/search?q=g:io.github.easy4j 应能搜到
- 第一次发布后，Sonatype 会发邮件到注册邮箱确认

## 8. 后续

发布到 Central 后：
- 等 24–48 小时 CDN 同步到全球镜像
- 在 README.md 加 Maven Central badge： `[Maven Central](https://img.shields.io/maven-central/v/io.github.easy4j/easypdf)`
- 同步三个分支的 pom version bump（release:prepare 已自动 bump 到 `3.0.1-SNAPSHOT` 等）

## 9. 注意事项

- **不要**把 `~/.m2/settings.xml` 提交到仓库
- **不要**把 GPG 私钥 commit 到仓库
- 命名空间 `io.github.easy4j` 的 Sonatype ticket 一旦归档会进入"已发布"状态，不可复用
- 中央发布是不可逆操作——v3.0.0 一旦发布就不能改 artifact 内容，只能发布 3.0.1 修正