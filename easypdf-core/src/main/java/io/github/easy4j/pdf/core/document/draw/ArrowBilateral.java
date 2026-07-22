package io.github.easy4j.pdf.core.document.draw;

import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;

/**
 * @package io.github.easy4j.pdf.core.document.elements
 * @className: Arrow
 * @description: 左边箭头
 * @author : <a href="mailto:hnxyhcwdl1003@163.com">wandalong</a>
 * @date : 2014-1-14
 * @time : 下午7:09:03
 */
public class ArrowBilateral extends Arrow {
	public void draw(PdfContentByte canvas, float llx, float lly, float urx, float ury, float y) {
		canvas.beginText();

		BaseFont bf = null;
		try {
			bf = BaseFont.createFont(BaseFont.ZAPFDINGBATS, "", BaseFont.EMBEDDED);
		} catch (Exception e) {
			e.printStackTrace();

		}
		canvas.setFontAndSize(bf, 12);
		// LEFT
		canvas.showTextAligned(Element.ALIGN_CENTER, String.valueOf((char) 220), llx - 10, y, 0);
		// RIGHT
		canvas.showTextAligned(Element.ALIGN_CENTER, String.valueOf((char) 220), urx + 10, y + 8, 180);
		canvas.endText();
	}
}
