# fastpdf 合并审计

`fastpdf` 与 `easypdf` 的 PDF 代码来自同一套历史实现。合并遵循“保留有效能力、删除重复与空壳、避免恢复失效依赖”的原则。

| fastpdf 内容 | easypdf 去向 | 处理结论 |
|---|---|---|
| `fastpdf-core` | `easypdf-core` | 表格、图片、水印、页事件、字体、XML 渲染及缓存等源码已迁入并统一为 `io.github.easy4j.pdf.*` 包名 |
| `fastpdf-webmvc` | `easypdf-webmvc` | 保留 MVC PDF View 能力，改为可运行的 Spring MVC + iText 7 实现 |
| `fastpdf-struts2` | 不迁移 | 依赖旧 Struts2、废弃的辅助库和未完成的 HTML 转换逻辑 |
| `fastpdf-icepdf` | 不迁移 | 仅包含名为 `Test` 的空占位类 |
| `fastpdf-pdfbox` | 不迁移 | 仅包含名为 `d` 的空占位类；PDFBox 能力已由 `easypdf-xhtml` 直接依赖 |
| `fastpdf-xpdf` | 不迁移 | 仅包含空占位类，没有可复用 API |
| 父 POM、旧 Nexus/SVN 配置 | 不迁移 | 已由 easypdf 的版本线、发布仓库和构建配置替代 |

## Web MVC 使用方式

继承 `AbstractITextPdfView`，在 `buildPdfDocument` 中使用 iText 7 的 `Document` 添加内容，然后将该 View 作为 Spring Bean 或从 Controller 返回。基类负责缓冲、关闭文档、设置 `application/pdf` 以及写入 HTTP 响应。

```java
public class InvoicePdfView extends AbstractITextPdfView {

	@Override
	protected void buildPdfDocument(
			Map<String, Object> model,
			Document document,
			PdfDocument pdfDocument,
			HttpServletRequest request,
			HttpServletResponse response) {
		document.add(new Paragraph(String.valueOf(model.get("invoiceNo"))));
	}
}
```
