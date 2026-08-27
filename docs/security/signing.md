# GPG Signing Setup

easypdf 在 Maven Central 发布时（`mvn -P central release:perform`）需要 GPG 签名。
本文件是"凭证填入"阶段的 step-by-step。

## 1. 本机生成 key

```bash
gpg --full-gen-key
# - Kind: RSA and RSA (default)
# - Length: 4096
# - Name: Loong Wan <hiwepy@github.com>
# - Email: 20489781+loong10k@users.noreply.github.com （与 git author 一致）
# - Passphrase: [强密码]
```

## 2. 列出 key id

```bash
gpg --list-secret-keys --keyid-format LONG
# 找到 sec 行末尾的 rsa4096/XXXXXXXX 那个 XXXXXXXX —— 那是 key id
```

## 3. 写入 `~/.m2/settings.xml`

```xml
<settings>
  <servers>
    <!-- 占位符；正式发布前替换为真实值 -->
    <server>
      <id>gpg.passphrase</id>
      <passphrase>REPLACE_ME_GPG_PASSPHRASE</passphrase>
    </server>
    <server>
      <id>central</id>
      <username>REPLACE_ME_SONATYPE_USER</username>
      <password>REPLACE_ME_SONATYPE_TOKEN</password>
    </server>
    <server>
      <id>ossrh</id>
      <username>REPLACE_ME</username>
      <password>REPLACE_ME</password>
    </server>
  </servers>
  <properties>
    <gpg.keyname>REPLACE_ME_KEY_ID_LONG_FORMAT</gpg.keyname>
    <gpg.passphrase>server-id-gpg.passphrase</gpg.passphrase>
  </properties>
</settings>
```

注意：
- `gpg.keyname` 是 `gpg --list-secret-keys` 输出的长格式 key id（40 位 hex）
- `gpg.passphrase` 这里用 `server-id-` 前缀指代从 `<servers>` 读取（避免 plaintext passphrase 出现在 properties 中）

## 4. 推公钥到 keys.openpgp.org

```bash
gpg --send-keys REPLACE_ME_KEY_ID_LONG_FORMAT
```

Central 在签名验证时从公钥服务器拉 key。后续如果 Sonatype 找不到，自动流程会失败。

## 5. 在 CI 上注入（GitHub Actions）

```yaml
- name: Import GPG key
  uses: crazy-max/gpg-action@v3
  with:
    gpg_private_key: ${{ secrets.GPG_PRIVATE_KEY }}
    passphrase: ${{ secrets.GPG_PASSPHRASE }}
```

把私钥与 passphrase 加到仓库 Settings → Secrets and variables → Actions：

```bash
# 导出私钥（armored format）
gpg --armor --export-secret-keys REPLACE_ME_KEY_ID_LONG_FORMAT > gpg-private.asc
# 复制文件内容到 GitHub Secret：GPG_PRIVATE_KEY
```

## 6. 验证

```bash
mvn -pl easypdf-core -am -DskipTests package
# 应在 package 阶段看到：
#   --- gpg:3.2.8:sign (sign-artifacts) @ easypdf-core ---
#   gpg: 正在创建 detached signature ...
#   gpg: 已创建签名：'...asc'
```

## 7. 不签 GPG 的后果

如果 key 缺失或 passphrase 错误，Maven Central 会在 staging upload 阶段拒绝，
`central-publishing-maven-plugin` 报 `401` 或 `403`。**不会**静默跳过——这是好的，
因为静默跳过会让用户信任未签名的 artifact。

## 8. 密钥轮换

GPG 主密钥建议：
- 1 年有效期，到期前 30 天续期：`gpg --edit-key KEY_ID expire`
- 续期后重新 `gpg --send-keys`
- 不推荐 3 年以上有效期——一旦私钥泄露损失过大
- 子密钥（subkey）保留 1 年即可，主密钥锁在离线存储