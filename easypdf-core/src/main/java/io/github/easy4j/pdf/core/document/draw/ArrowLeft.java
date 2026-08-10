package io.github.easy4j.pdf.core.document.draw;

import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;

 /**
 * @package io.github.easy4j.pdf.core.document.elements
 * @className: Arrow
 * @description: 左边箭头
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date : 2014-1-14
 * @time : 下午7:09:03 
 */
public class ArrowLeft extends Arrow {
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
		canvas.showTextAligned(Element.ALIGN_CENTER,String.valueOf((char) 220), llx - 10, y, 0);
		canvas.endText();
	}
}



