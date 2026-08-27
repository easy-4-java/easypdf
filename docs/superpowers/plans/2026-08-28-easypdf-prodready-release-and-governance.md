# Release & Branch Governance（生产就绪补完 T06-T12）

**日期**：2026-08-28

## T06：CHANGELOG.md

- Keep a Changelog 1.1 风格 + SemVer
- 三分支一致 SHA：`2c914ed644a7`
- 三个版本：v1.0.0（JDK 8 bugfix-only）/ v2.0.0（JDK 17 同步）/ v3.0.0（JDK 21 主线）

## T07：Annotated tag

三个 annotated tag（不签 GPG，本机无 key；按 plan 凭证后填）：

| tag | commit | 所在分支 HEAD |
|---|---|---|
| `v1.0.0` | d838fe70826e | feature/1.0.x |
| `v2.0.0` | 5627a103b5e7 | feature/2.0.x |
| `v3.0.0` | b937617b8477 | feature/3.0.x |

按计划不 push 到远端。push 命令：`git push origin v1.0.0 v2.0.0 v3.0.0`。

## T08：CycloneDX SBOM

- 插件：`org.cyclonedx:cyclonedx-maven-plugin:2.9.1`
- 绑定到 `package` 阶段
- 输出：`target/bom.json`（CycloneDX 1.6）+ `target/bom.xml`
- JSON 自动 attach 为 `*-cyclonedx.jar` classifier
- 三分支各跑通，1.0.x 33 components / 2.0.x 34 components / 3.0.x 38 components

## T09：CI artifact 上传

`.github/workflows/ci.yml`（三分支各一份）已加：
- failsafe-reports（`**/target/failsafe-reports`）
- cyclonedx-sbom（`**/target/bom.json`）
- 保留期 14 天

## T10：发布文档

- `docs/security/signing.md` — GPG key 生成 + Sonatype user/token 配置 + CI secret 注入
- `docs/security/central-publish.md` — `mvn -P central release:perform` step-by-step

## T11：分支保护 JSON

仓库根三份草稿（不进 git）：

- `protection-1.0.x.json`
- `protection-2.0.x.json`
- `protection-3.0.x.json`

要点：
- `enforce_admins=true`（含 admin 自身也走 PR review）
- `required_linear_history=true`（禁止 merge commit，只走 rebase / fast-forward）
- `restrictions.users=["hiwepy"]`（push 权限收窄到单一 owner）
- `required_status_checks.strict=true`（必须等 CI 通过才能合并）

**未 PUT** — 等你审过后再说。一旦 PUT 后，admin 也不能 force-push，
`gh api -X DELETE /repos/easy-4-java/easypdf/branches/<branch>/protection` 才能撤销。

## T12：~/.m2/settings.xml

未做修改。检查显示 `<server id="central">` 与 `<server id="ossrh">` 已存在
且含真实凭证（`vktCay` + token）。按 plan "凭证后填"路线，本机已具备发布能力。

## 关联计划文件

- `2026-08-28-easypdf-prodready-cross-jdk.md` — T01 跨 JDK verify
- `2026-08-28-easypdf-prodready-it.md` — T02-T03 easypdf-it
- `2026-08-28-easypdf-prodready-cve-baseline.md` — T04-T05 CVE & 治理
- 本文 — T06-T12 发布与分支治理