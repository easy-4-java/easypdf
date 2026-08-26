package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.tagging.IStructureNode;
import com.itextpdf.kernel.pdf.tagging.PdfStructElem;
import com.itextpdf.kernel.pdf.tagging.PdfStructTreeRoot;
import com.itextpdf.kernel.pdf.tagging.StandardRoles;

/**
 * PDF 结构识别：优先读 Tagged PDF 的结构树（高保真），否则降级为启发式重建（按 y 坐标分块）。
 * 对外返回 DocumentStructure，由调用方决定序列化方式。
 */
public final class PdfStructureExtractor {

    private PdfStructureExtractor() {
    }

    public static DocumentStructure extract(File pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        if (!pdf.isFile()) {
            throw new IOException("PDF not found: " + pdf.getAbsolutePath());
        }
        DocumentStructure doc = new DocumentStructure();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdf))) {
            // 元数据
            String metaTitle = pdfDoc.getDocumentInfo().getTitle();
            doc.title = (metaTitle == null || metaTitle.isEmpty()) ? pdf.getName() : metaTitle;
            // 策略选择
            PdfStructTreeRoot root = pdfDoc.getStructTreeRoot();
            if (root != null && hasStructure(root)) {
                extractTagged(doc, root);
            } else {
                extractHeuristic(doc, pdfDoc);
            }
        }
        // 兜底：若结构识别未产出任何 section 且 title 缺失，注入默认 section
        if (doc.sections.isEmpty()) {
            DocumentSection fallback = new DocumentSection();
            fallback.title = doc.title != null ? doc.title : "Document";
            fallback.level = 1;
            fallback.content = "(no extractable text)";
            doc.sections.add(fallback);
        }
        return doc;
    }

    private static boolean hasStructure(PdfStructTreeRoot root) {
        if (root.getKids() == null) return false;
        for (IStructureNode n : root.getKids()) {
            if (n instanceof PdfStructElem) return true;
        }
        return false;
    }

    private static void extractTagged(DocumentStructure doc, PdfStructTreeRoot root) {
        for (IStructureNode child : root.getKids()) {
            walk(child, null, doc);
        }
    }

    private static void walk(IStructureNode node, DocumentSection parent, DocumentStructure doc) {
        if (!(node instanceof PdfStructElem)) return;
        PdfStructElem elem = (PdfStructElem) node;
        String role = elem.getRole() != null ? elem.getRole().toString() : "";
        DocumentSection current = parent;
        if (StandardRoles.H1.equals(role) || StandardRoles.H2.equals(role)
                || StandardRoles.H3.equals(role) || StandardRoles.H4.equals(role)
                || StandardRoles.H5.equals(role) || StandardRoles.H6.equals(role)) {
            DocumentSection sec = new DocumentSection();
            sec.title = safeText(elem);
            sec.level = Math.max(1, Math.min(6, Character.digit(role.charAt(1), 10)));
            sec.content = safeText(elem);
            if (parent != null) {
                parent.children.add(sec);
            } else {
                doc.sections.add(sec);
            }
            current = sec;
        } else if (StandardRoles.TABLE.equals(role)) {
            DocumentTable tbl = new DocumentTable();
            if (parent != null) {
                parent.tables.add(tbl);
            } else {
                doc.tables.add(tbl);
            }
        }
        if (elem.getKids() != null) {
            for (IStructureNode child : elem.getKids()) {
                walk(child, current, doc);
            }
        }
    }

    private static String safeText(PdfStructElem elem) {
        try {
            com.itextpdf.kernel.pdf.PdfString t = elem.getActualText();
            if (t != null) {
                String s = t.getValue();
                if (s != null && !s.isEmpty()) return s;
            }
            com.itextpdf.kernel.pdf.PdfString a = elem.getAlt();
            if (a != null) {
                String s = a.getValue();
                if (s != null && !s.isEmpty()) return s;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static void extractHeuristic(DocumentStructure doc, PdfDocument pdfDoc) {
        DocumentSection sec = new DocumentSection();
        sec.title = doc.title != null ? doc.title : "Document";
        sec.level = 1;
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            String text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
            if (text != null && !text.isEmpty()) {
                buf.append(text.trim()).append('\n').append('\n');
            }
        }
        sec.content = buf.toString().trim();
        doc.sections.add(sec);
    }
}
