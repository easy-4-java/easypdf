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
| 2.0.x / main | 17 | 11.1.3 | 8.1.4 | 8.0.0 | 3.0.0 |
| 3.0.x | 21 | 11.1.3 | 8.1.4 | 8.0.0 | 3.0.0 |

## 模块

| 模块 | 说明 |
|------|------|
| `easypdf-core` | 核心抽象、Docx4j/WML 工具 |
| `easypdf-xhtml` | HTML/XHTML → WordprocessingMLPackage |
| `easypdf-freemarker` / `velocity` / `beetl` / `thymeleaf` / … | 各模板引擎适配 |
| `easypdf-bom` | 依赖 BOM |

## 参考

- https://www.docx4java.org/
- https://itextpdf.com/
