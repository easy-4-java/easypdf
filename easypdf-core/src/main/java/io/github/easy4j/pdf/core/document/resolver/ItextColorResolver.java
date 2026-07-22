package io.github.easy4j.pdf.core.document.resolver;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;

import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import com.jeefw.fastxml.core.utils.XMLColorUtils;
import com.jeefw.fastxml.jdom.xhtml.ColorResolver;

/**
 * 
 * @className: FontParser
 * @description: 字体解析器：必须是定义过的字体
 * @author : <a href="mailto:hnxyhcwdl1003@163.com">wandalong</a>
 * @date : 上午10:31:53 2013-8-16
 * @modify by:
 * @modify date :
 * @modify description :
 */
public class ItextColorResolver implements ColorResolver<Color,ItextXMLElement>{

	private static ItextColorResolver instance = null;
	
	public static ItextColorResolver getInstance(){
		instance = instance==null?new ItextColorResolver():instance;
		return instance;
	}

	public Color getColor(ItextXMLElement element) throws IOException {
		 return this.getColor(element.styles(), new String[]{"color"});
	}
	
	public Color getColor(Map<String,Object> styles,String[] cssNames) throws IOException {
		for (int i = 0; i < cssNames.length; i++) {
			Object color = styles.get(cssNames[i]);
			if(color!=null&&color instanceof String){
				try {
					int[] rgb = XMLColorUtils.rgb(color.toString());
					System.out.println(rgb[0]+","+rgb[1]+","+rgb[2]);
					return new Color(rgb[0],rgb[1], rgb[2]);
				} catch (Exception e) {
					return Color.BLACK;
				}
			}
		}
		return Color.BLACK;
	}

	
	

	
}
