package io.github.easy4j.pdf.core.document.draw;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.draw.LineSeparator;

 /**
 * @package io.github.easy4j.pdf.core.document.draw
 * @className: UnderLineSeparator
 * @description: TODO
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date : 2014-1-15
 * @time : 上午8:48:28 
 */
public class UnderLineSeparator extends LineSeparator {
	
	public UnderLineSeparator() {
		this(100f);
	}
	
	public UnderLineSeparator(float percentage) {
        this(1,percentage);
    }
	
	public UnderLineSeparator(float lineWidth, float percentage) {
		this(lineWidth,percentage,BaseColor.BLACK);
    }
	
	public UnderLineSeparator(float lineWidth, float percentage, BaseColor lineColor) {
        this(lineWidth,percentage,lineColor,Element.ALIGN_CENTER,-2);
    }

	public UnderLineSeparator(float lineWidth, float percentage, BaseColor lineColor, int align, float offset) {
        this.lineWidth = lineWidth;
        this.percentage = percentage;
        this.lineColor = lineColor;
        this.alignment = align;
        this.offset = offset;
    }
	
}



