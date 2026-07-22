# easypdf

基于 Docx4j / iText 与多种模板引擎，快速生成 Word / PDF 文档。

- Maven: `io.github.easy4j:easypdf`
- 仓库: https://github.com/easy-4-java/easypdf
- 包名: `io.github.easy4j.easypdf.*`

## 模块

| 模块 | 说明 |
|------|------|
| `easypdf-core` | 核心抽象、Docx4j/WML 工具、iText XML→PDF |
| `easypdf-xhtml` | HTML/XHTML → WordprocessingMLPackage |
| `easypdf-freemarker` / `velocity` / `beetl` / `thymeleaf` / … | 各模板引擎适配 |
| `easypdf-bom` | 依赖 BOM |

## 参考

- https://www.docx4java.org/
- https://itextpdf.com/
