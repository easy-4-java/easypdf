package io.github.easy4j.pdf.core.document.helper;

import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.draw.LineSeparator;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * @package io.github.easy4j.pdf.core.document.helper
 * @className: ChunkHelper
 * @description: TODO
 * @author : <a href="mailto:hnxyhcwdl1003@163.com">wandalong</a>
 * @date : 2014-1-14
 * @time : 下午2:31:21
 */
public final class PhraseHelper {

	private static PhraseHelper instance = null;
	private PhraseHelper(){}
	
	public static PhraseHelper getInstance(){
		instance = instance==null?new PhraseHelper():instance;
		return instance;
	}
	
	public <T> Phrase getPhrase(ItextXMLElement element,T data){
		Phrase p = new Phrase(element.text(data));
		
		//"p","h1","h2","h3","h4","h5","h6","h7"
		
		LineSeparator underLine =LineHelper.getInstance().getDeleteLine(element);
		p.add(underLine);
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(p, element);
		return p;
		
	}
	
	
	public Phrase getUnderLinePhrase(ItextXMLElement element){
		Phrase p = new Phrase(element.text()); 
		LineSeparator underLine = LineHelper.getInstance().getUnderLine(element);
		p.add(underLine);
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(p, element);
		return p;
		
	}
	
	
	public Phrase getDeleteLinePhrase(ItextXMLElement element){
		Phrase p = new Phrase(element.text()); 
		LineSeparator underLine =LineHelper.getInstance().getDeleteLine(element);
		p.add(underLine);
		ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(p, element);
		return p;
	}
	
}
