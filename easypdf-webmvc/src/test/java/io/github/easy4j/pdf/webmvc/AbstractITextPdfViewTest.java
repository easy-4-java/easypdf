package io.github.easy4j.pdf.webmvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

/**
 * Verifies that the MVC view produces a complete PDF response.
 */
public class AbstractITextPdfViewTest {

	@Test
	public void shouldRenderModelAsPdfResponse() throws Exception {
		AbstractITextPdfView view = new AbstractITextPdfView() {
			@Override
			protected void buildPdfDocument(
					Map<String, Object> model,
					Document document,
					PdfDocument pdfDocument,
					HttpServletRequest request,
					HttpServletResponse response) {
				document.add(new Paragraph(String.valueOf(model.get("message"))));
			}
		};
		MockHttpServletResponse response = new MockHttpServletResponse();

		view.render(
				Collections.<String, Object>singletonMap("message", "Hello PDF"),
				new MockHttpServletRequest(),
				response);

		byte[] content = response.getContentAsByteArray();
		Assert.assertEquals(AbstractITextPdfView.PDF_CONTENT_TYPE, response.getContentType());
		Assert.assertTrue(content.length > 100);
		Assert.assertEquals("%PDF-", new String(content, 0, 5, StandardCharsets.US_ASCII));
	}
}
