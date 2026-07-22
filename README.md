# easypdf

基于 Docx4j / iText 与多种模板引擎，快速生成 Word / PDF 文档。

- Maven: `io.github.easy4j:easypdf`
- 仓库: https://github.com/easy-4-java/easypdf
- 包名: `io.github.easy4j.pdf.*`

## 版本线

| 分支 | JDK | 版本号 |
|------|-----|--------|
| `feature/1.0.x` | 1.8 | `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` / `main` | 17 | `2.0.x.20260630-SNAPSHOT` |
| `feature/3.0.x` | 21 | `3.0.x.20260630-SNAPSHOT` |

三条线 Java 源码/注释/文档保持一致，仅 JDK 与配套 Maven 依赖版本不同。

### Docx4j 版本矩阵（扩展模块与 core 解耦）

| 线 | JDK | `docx4j` (core/JAXB) | `docx4j-export-fo` | `docx4j-ImportXHTML` | `xhtmlrenderer` |
|----|-----|----------------------|--------------------|----------------------|-----------------|
| 1.0.x | 1.8 | 8.3.15 | 8.3.15 | 8.3.15 | 3.0.0 |
| 2.0.x / main | 17 | 11.5.2 | 11.5.2 | 11.4.8 | 3.0.0 |
| 3.0.x | 21 | 11.5.2 | 11.5.2 | 11.4.8 | 3.0.0 |

> JDK 17/21 采用你此前手动验证过的组合：core/export-fo 升到 **11.5.2**，ImportXHTML 单独为 **11.4.8**（扩展模块版本与 core 解耦）。主代码不直接依赖 `javax/jakarta.xml.bind` 类型，保证三线源码一致。

## 模块

| 模块 | 说明 |
|------|------|
| `easypdf-core` | 核心抽象、Docx4j/WML 工具 |
| `easypdf-xhtml` | HTML/XHTML → WordprocessingMLPackage |
| `easypdf-webmvc` | Spring MVC + iText 7 PDF View 集成 |
| `easypdf-freemarker` / `velocity` / `beetl` / `thymeleaf` / … | 各模板引擎适配 |
| `easypdf-bom` | 依赖 BOM |

## fastpdf 合并说明

`fastpdf-core` 中可复用的 iText/PDF 辅助代码已经归入 `easypdf-core`；本次继续迁入其 Spring MVC PDF View 能力，并升级为 iText 7 实现。Struts2 适配和 ICEpdf/PDFBox/Xpdf 占位模块不再迁移，详细取舍见 [fastpdf 合并审计](docs/fastpdf-migration.md)。

## 参考

- https://www.docx4java.org/
- https://itextpdf.com/
