package io.github.easy4j.pdf.core.cache;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeefw.fastkit.lang3.StringUtils;

/**
 * 
 * *******************************************************************
 * @className	： PDFTemplateCacheManager
 * @description	： TODO(描述这个类的作用)
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date		： Oct 22, 2016 8:11:58 PM
 * @version 	V1.0 
 * *******************************************************************
 */
public class PDFTemplateCacheManager {
	
	protected static Logger LOG = LoggerFactory.getLogger(PDFTemplateCacheManager.class);
	/*protected static POIFontFactory fontFactory = POIFontFactory.getInstance();
	private volatile static PDFTemplateCacheManager singleton;
 	protected static ConcurrentMap<Workbook, Map<String,POICellStyleKey>> COMPLIED_FONTKEY = new ConcurrentHashMap<Workbook, Map<String,POICellStyleKey>>();
 	protected static ConcurrentMap<Workbook, Map<POICellStyleKey,CellStyle>> COMPLIED_CELLSTYLE = new ConcurrentHashMap<Workbook, Map<POICellStyleKey,CellStyle>>();
	
	public static PDFTemplateCacheManager getInstance() {
		if (singleton == null) {
			synchronized (PDFTemplateCacheManager.class) {
				if (singleton == null) {
					singleton = new PDFTemplateCacheManager();
				}
			}
		}
		return singleton;
	}
	
	private PDFTemplateCacheManager(){
		
	}
	
	public POICellStyleKey getCellStyleKey(Workbook wb,Font font) {
		return getCellStyleKey(wb, font.getFontName(), font.getBoldweight(), font.getColor(), 
				font.getFontHeightInPoints() , font.getItalic(), font.getStrikeout(), 
				font.getTypeOffset(), font.getUnderline(), font.getCharSet());
	}
	
	public POICellStyleKey getCellStyleKey(Workbook wb,String fontName,short fontHeightInPoints) {
		return getCellStyleKey(wb, fontName, Font.COLOR_NORMAL, fontHeightInPoints);
	}
	
	public POICellStyleKey getCellStyleKey(Workbook wb,String fontName, short color, short fontHeightInPoints) {
		return getCellStyleKey(wb, fontName, Font.BOLDWEIGHT_NORMAL, color, fontHeightInPoints);
	}
	
	public POICellStyleKey getCellStyleKey(Workbook wb,String fontName, short boldweight, short color, short fontHeightInPoints) {
		return getCellStyleKey(wb, fontName, boldweight, color, fontHeightInPoints , false, false, Font.SS_NONE, Font.U_NONE, Font.DEFAULT_CHARSET);
	}
	
	public POICellStyleKey getCellStyleKey(Workbook wb,String fontName,short boldweight, short color, short fontHeightInPoints,
			boolean italic, boolean strikeout, short typeOffset, byte underline, int charSet) {
		if (wb != null && StringUtils.isNotEmpty(fontName)) {
			String format =  StringUtils.join(new Object[]{ fontName , boldweight , color , fontHeightInPoints , italic , strikeout , typeOffset , underline , charSet }, "-");
			Map<String, POICellStyleKey> fontMap = COMPLIED_FONTKEY.get(wb);
			if (fontMap == null) {
				fontMap = new HashMap<String, POICellStyleKey>();
				Map<String, POICellStyleKey> existing = COMPLIED_FONTKEY.putIfAbsent(wb, fontMap);
	 			if (existing != null) {
	 				fontMap = existing;
	 			}
			}
			POICellStyleKey fontKey = fontMap.get(format);
			if (fontKey == null) {
				fontKey = new POICellStyleKey(fontName, boldweight, color, fontHeightInPoints, italic, strikeout, typeOffset, underline, charSet);
				fontMap.put(format, fontKey);
				COMPLIED_FONTKEY.replace(wb, fontMap);
			}
			return fontKey;
		}
		return null;
	}

	public Map<POICellStyleKey,CellStyle> getCacheCellStyles(Workbook wb){
		if(wb != null){
			Map<POICellStyleKey,CellStyle> ret = COMPLIED_CELLSTYLE.get(wb);
			if (ret != null) {
				return ret;
			}
			ret = new HashMap<POICellStyleKey, CellStyle>();
			Map<POICellStyleKey,CellStyle> existing = COMPLIED_CELLSTYLE.putIfAbsent(wb, ret);
 			if (existing != null) {
 				ret = existing;
 			}
			return ret;
		}
		return null;
	}
	
	public CellStyle getCellStyle(Workbook wb,Font font) {
		return getCellStyle(wb, font.getFontName(), font.getBoldweight(), font.getColor(), 
				font.getFontHeightInPoints() , font.getItalic(), font.getStrikeout(), 
				font.getTypeOffset(), font.getUnderline(), font.getCharSet());
	}
	
	public CellStyle getCellStyle(Workbook wb,String fontName,short fontHeightInPoints) {
		return getCellStyle(wb,fontName, Font.COLOR_NORMAL, fontHeightInPoints);
	}
	
	public CellStyle getCellStyle(Workbook wb,String fontName, short color, short fontHeightInPoints) {
		return getCellStyle(wb,fontName, Font.BOLDWEIGHT_NORMAL, color, fontHeightInPoints);
	}
	
	public CellStyle getCellStyle(Workbook wb,String fontName, short boldweight, short color, short fontHeightInPoints) {
		return getCellStyle(wb,fontName, boldweight, color, fontHeightInPoints , false, false, Font.SS_NONE, Font.U_NONE, Font.DEFAULT_CHARSET);
	}
	
	public CellStyle getCellStyle(Workbook wb,String fontName,short boldweight, short color, short fontHeightInPoints,
			boolean italic, boolean strikeout, short typeOffset, byte underline, int charSet){
		//获取当前Workbook对象的样式缓存
		Map<POICellStyleKey,CellStyle> styleMap = this.getCacheCellStyles(wb);
		//获取对应字体key
		POICellStyleKey cellStyleKey = this.getCellStyleKey( wb, fontName, boldweight, color, fontHeightInPoints, italic, strikeout, typeOffset, underline, charSet);
		//单元格样式
		CellStyle ret = styleMap.get(cellStyleKey);
		if (ret != null) {
			return ret;
		}
		//创建单元格样式
		ret = wb.createCellStyle();
		//设置字体
		ret.setFont(fontFactory.getFont(wb, fontName, boldweight, color, fontHeightInPoints, italic, strikeout, typeOffset, underline, charSet));
		//缓存字体
		styleMap.put(cellStyleKey, ret);
		//替换原有key的数据
		COMPLIED_CELLSTYLE.put(wb, styleMap);
		return ret;
	}
	
	public void destroy(Workbook wb) {
		if(wb != null){
			//清除Workbook对应的字体缓存
			COMPLIED_FONTKEY.remove(wb);
			COMPLIED_CELLSTYLE.remove(wb);
		}
	}*/
	
}

