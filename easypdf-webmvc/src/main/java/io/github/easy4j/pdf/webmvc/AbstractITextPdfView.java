package io.github.easy4j.pdf.webmvc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

/**
 * Base Spring MVC view for creating an iText PDF response.
 *
 * <p>The complete document is buffered before it is copied to the servlet
 * response. This prevents a partially generated PDF from being committed when
 * document construction fails.</p>
 */
public abstract class AbstractITextPdfView extends AbstractView {

	public static final String PDF_CONTENT_TYPE = "application/pdf";

	protected AbstractITextPdfView() {
		setContentType(PDF_CONTENT_TYPE);
	}

	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ByteArrayOutputStream outputStream = createTemporaryOutputStream();
		PdfWriter writer = newPdfWriter(outputStream);
		PdfDocument pdfDocument = newPdfDocument(writer);
		Document document = newDocument(pdfDocument);
		try {
			prepareDocument(model, document, pdfDocument, request, response);
			buildPdfDocument(model, document, pdfDocument, request, response);
		} finally {
			document.close();
		}
		writeToResponse(response, outputStream);
	}

	/**
	 * Creates the low-level writer. Subclasses may override this to configure
	 * writer properties such as compression or encryption.
	 *
	 * @param outputStream buffered response destination
	 * @return PDF writer
	 */
	protected PdfWriter newPdfWriter(OutputStream outputStream) {
		return new PdfWriter(outputStream);
	}

	/**
	 * Creates the low-level PDF document.
	 *
	 * @param writer configured PDF writer
	 * @return PDF document
	 */
	protected PdfDocument newPdfDocument(PdfWriter writer) {
		return new PdfDocument(writer);
	}

	/**
	 * Creates the layout document exposed to subclasses.
	 *
	 * @param pdfDocument low-level PDF document
	 * @return layout document
	 */
	protected Document newDocument(PdfDocument pdfDocument) {
		return new Document(pdfDocument);
	}

	/**
	 * Hook for metadata, page events and document properties. It runs before
	 * content is added.
	 *
	 * @param model Spring MVC model
	 * @param document layout document
	 * @param pdfDocument low-level PDF document
	 * @param request current request
	 * @param response current response; only headers should be changed here
	 */
	protected void prepareDocument(
			Map<String, Object> model,
			Document document,
			PdfDocument pdfDocument,
			HttpServletRequest request,
			HttpServletResponse response) {
		// Default implementation intentionally empty.
	}

	/**
	 * Adds the application-specific PDF content.
	 *
	 * @param model Spring MVC model
	 * @param document layout document
	 * @param pdfDocument low-level PDF document
	 * @param request current request
	 * @param response current response; only headers should be changed here
	 * @throws Exception when document generation fails
	 */
	protected abstract void buildPdfDocument(
			Map<String, Object> model,
			Document document,
			PdfDocument pdfDocument,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception;
}
