package io.github.easy4j.easypdf.core.document.helper;

import com.itextpdf.text.pdf.draw.DottedLineSeparator;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.github.easy4j.easypdf.core.document.draw.UnderLineSeparator;
import io.github.easy4j.easypdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.easypdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

 /**
 * @package io.github.easy4j.easypdf.core.document.helper
 * @className: LineHelper
 * @description: TODO
 * @author : <a href="mailto:hnxyhcwdl1003@163.com">wandalong</a>
 * @date : 2014-1-14
 * @time : 下午6:46:12 
 */
public final class LineHelper {
	
	private static LineHelper instance = null;
	private LineHelper(){}
	
	public static LineHelper getInstance(){
		instance = instance==null?new LineHelper():instance;
		return instance;
	}
	
	public LineSeparator getLine(ItextXMLElement element){
		
		LineSeparator line = new LineSeparator();
		/*
		line.setAlignment(align)
		line.setLineColor(color)
		line.setLineWidth(lineWidth)
		line.setOffset(offset)
		line.setPercentage(percentage)
		*/
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(line, element);
		return line;
	}
	
	public LineSeparator getUnderLine(ItextXMLElement element){
		LineSeparator underLine = new UnderLineSeparator(); 
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(underLine, element);
		return underLine;
	}
	
	
	public LineSeparator getDottedLine(ItextXMLElement element){
		DottedLineSeparator dottedLine = new DottedLineSeparator(); 
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(dottedLine, element);
		return dottedLine;
	}
	
	
	public LineSeparator getDeleteLine(ItextXMLElement element){
		DottedLineSeparator dottedLine = new DottedLineSeparator(); 
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(dottedLine, element);
		return dottedLine;
	}
	
	
}



