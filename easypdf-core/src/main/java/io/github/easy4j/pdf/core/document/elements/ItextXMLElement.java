package io.github.easy4j.pdf.core.document.elements;

import java.awt.Color;

import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import io.github.easy4j.pdf.core.document.resolver.ItextAlignmentResolver;
import io.github.easy4j.pdf.core.document.resolver.ItextColorResolver;
import io.github.easy4j.pdf.core.document.resolver.ItextFontResolver;
@SuppressWarnings({"serial"})
public abstract class ItextXMLElement extends XMLCSSElement{
	
	protected String clazz ="";
	protected String dir="ltr";
	protected String style=""; 
	protected String title="";
	protected String lang=""; 
	protected String id="";
	
	/**
	 * @description: 根据元素拥有的样式，并对样式进行itext模式的转换，给元素添加样式
	 * @author : <a href="mailto:hnxyhcwdl1003@163.com">wandalong</a>
	 * @date 下午5:14:46 2013-8-15 
	 * @param element
	 * @throws  
	 * @modify by:
	 * @modify date :
	 * @modify description : TODO(描述修改内容)
	 */
	public Font getFont(){
		try {
			return ItextFontResolver.getInstance().getFont(this);
		} catch (Exception e) {
			return FontFactory.getFont("STSongStd-Light","UniGB-UCS2-H",BaseFont.NOT_EMBEDDED,12);
		}
	}
	
	public Color getColor(){
		try {
			int[] rgb = XMLColorUtils.rgb("#f00");
			System.out.println(rgb[0]+","+rgb[1]+","+rgb[2]);
			return ItextColorResolver.getInstance().getColor(this);
		} catch (Exception e) {
			return Color.BLACK;
		}
	}
	
	public int getAlignment(){
		return ItextAlignmentResolver.getInstance().getTextAlign(this);
	}
	
	public int getVerticalAlignment(){
		return ItextAlignmentResolver.getInstance().getVerticalAlign(this);
	}

	public <T> Phrase getPhrase(T argument){
		return this.getPhrase(this.getFont(),argument);
	}
	
	public <T> Phrase getPhrase(Font font,T argument){
		return new Phrase(this.text(argument),font);
	}

}
