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
 * Implementation of itext x m l element extending XMLCSSElement.
 *
 * [@Loong Wan](https://github.com/loong10k)
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
