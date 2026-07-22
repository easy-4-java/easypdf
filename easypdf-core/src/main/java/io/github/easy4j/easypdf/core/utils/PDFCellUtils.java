package io.github.easy4j.easypdf.core.utils;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

public final class PDFCellUtils {

	/**
	 * 构造一个自定义的cell单元格内容(默认居中对齐,默认字体)
	 * @param content 单元格内容
	 * @return com.lowagie.text.pdf.PdfPCell
	 * 
	 */
	public static PdfPCell getCell(String content) {
		return getCell(content, 1, PdfPCell.ALIGN_CENTER,PdfPCell.ALIGN_MIDDLE, new Font());
	}

	public static PdfPCell getCell(Phrase phrase) {
		return new PdfPCell(phrase);
	}

	public static PdfPCell getCell(Image image) {
		return new PdfPCell(image);
	}

	public static PdfPCell getCell(PdfPCell cell) {
		return new PdfPCell(cell);
	}

	public static PdfPCell getCell(PdfPTable table, PdfPCell style) {
		return new PdfPCell(table, style);
	}

	/**
	 * 构造一个自定义的cell单元格内容(默认居中对齐)
	 * @param content  单元格内容
	 * @param font 单元格字体
	 * @return com.lowagie.text.pdf.PdfPCell
	 */
	public static PdfPCell getCell(String content, Font font) {
		return getCell(content, 1, PdfPCell.ALIGN_CENTER, PdfPCell.ALIGN_MIDDLE, font);
	}

	/**
	 * 构造一个自定义的cell单元格内容
	 * 
	 * @param content 单元格内容
	 * @param align 横向对齐
	 * @param valign  纵向对齐
	 * @param font 单元格字体
	 * @return com.lowagie.text.pdf.CellElement
	 * 
	 */
	public static PdfPCell getCell(String content, int align, int valign, Font font) {
		return getCell(content, 1, align, valign, font);
	}

	/**
	 * 构造一个自定义的cell单元格内容
	 * 
	 * @param content 单元格内容
	 * @param colspan 合并列
	 * @param font  单元格字体
	 * @return com.lowagie.text.pdf.CellElement
	 * 
	 */
	public static PdfPCell getCell(String content, int colspan, Font font) {
		return getCell(content, colspan, PdfPCell.ALIGN_CENTER, PdfPCell.ALIGN_MIDDLE, font);
	}

	/**
	 * 构造一个自定义的内嵌表cell单元格内容(默认居中对齐)
	 * 
	 * @param table 单元格内容
	 * @return com.lowagie.text.pdf.CellElement
	 * 
	 */
	public static PdfPCell getCell(PdfPTable table) {
		return getCell(table, 1, PdfPCell.ALIGN_CENTER, PdfPCell.ALIGN_MIDDLE);
	}

	/**
	 * 构造一个自定义的内嵌表cell单元格内容
	 * 
	 * @param table
	 *            单元格内容
	 * @param align
	 *            横向对齐
	 * @param valign
	 *            纵向对齐
	 * @return com.lowagie.text.pdf.CellElement
	 * 
	 */
	public static PdfPCell getCell(PdfPTable table, int align, int valign) {
		return getCell(table, 1, align, valign);
	}

	/**
	 * 构造一个自定义的内嵌表cell单元格内容
	 * 
	 * @param content
	 *            单元格内容
	 * @param colspan
	 *            合并列
	 * @return com.lowagie.text.pdf.CellElement
	 * 
	 */
	public static PdfPCell getCell(PdfPTable table, int colspan) {
		return getCell(table, colspan, PdfPCell.ALIGN_CENTER, PdfPCell.ALIGN_MIDDLE);
	}

	/**
	 * 构造一个自定义的cell单元格内容
	 * 
	 * @param content 单元格内容
	 * @param colspan 合并列
	 * @param align 横向对齐
	 * @param valign 纵向对齐
	 * @param font 单元格字体
	 * @return com.lowagie.text.pdf.CellElement
	 * 
	 */
	public static PdfPCell getCell(String content, int colspan, int align,int valign, Font font) {
		PdfPCell cell = null;
		Paragraph paragraph = null;
		// 使用自定义字体
		paragraph = new Paragraph(content == null ? "" : content, font);
		cell = new PdfPCell(paragraph);
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		// 设置对齐
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(valign);
		cell.setMinimumHeight(15);
		return cell;
	}

	public static PdfPCell getCell(Paragraph content, int colspan, int align, int valign) {
		PdfPCell cell = null;
		Paragraph paragraph = content;
		// 使用自定义字体
		cell = new PdfPCell(paragraph);
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		// 设置对齐
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(valign);
		cell.setMinimumHeight(15);
		return cell;
	}

	/**
	 * 构造一个自定义的cell<br/>
	 * 由于使用CellElement无法合并行,所以此方法中一些属性无法使用.
	 * 
	 * @param content 单元格内容
	 * @param colspan 合并列
	 * @param rowspan 合并行 
	 * @param align  横向对齐
	 * @param valign 纵向对齐
	 * @param font 单元格字体
	 * @return 生成的单元格
	 * 
	 */
	public static PdfPCell getCell(String content, int colspan, int rowspan, int align, int valign, Font font) {
		PdfPCell cell = null;
		Paragraph paragraph = null;
		// 使用自定义字体
		paragraph = new Paragraph(content == null ? "" : content, font);
		cell = new PdfPCell(paragraph);
		// 设置colspan,同样的方法可以设置rowspan
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		if (rowspan > 1) {
			cell.setRowspan(rowspan);
		}
		// 设置对齐
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(valign);
		cell.setMinimumHeight(15);
		return cell;
	}

	/**
	 * 构造一个自定义的图片cell单元格
	 * 
	 * @param img 单元格内容
	 * @param colspan 合并列
	 * @param align 横向对齐
	 * @param valign 纵向对齐
	 * @return 生成的单元格
	 * 
	 */
	public static PdfPCell getCell(Image img, int colspan, int rowspan,int align, int valign) {
		PdfPCell cell = null;
		cell = new PdfPCell(img);
		if (rowspan > 1) {
			cell.setRowspan(rowspan);
		}
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		// 设置对齐
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(valign);
		cell.setMinimumHeight(15);
		return cell;
	}

	/**
	 * 构造一个自定义的图片cell单元格
	 * 
	 * @param img 单元格内容
	 * @param colspan 合并列
	 * @param align 横向对齐
	 * @param valign 纵向对齐
	 * @return 生成的单元格
	 * 
	 */
	public static PdfPCell getCell(Image img, int colspan, int align, int valign) {
		PdfPCell cell = null;
		cell = new PdfPCell(img);
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		// 设置对齐
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(valign);
		cell.setMinimumHeight(15);
		return cell;
	}

	/**
	 * 构造一个内嵌table的单元格<br/>
	 * 由于CellElement无法实现跨行功能,所以需要内嵌一个table.以实现复杂的跨行功能.
	 * 
	 * @param table 内嵌表格
	 * @param colspan 合并列
	 * @param align 横向对齐
	 * @param valign 纵向对齐
	 * @return
	 */
	public static PdfPCell getCell(PdfPTable table, int colspan, int align, int valign) {
		PdfPCell cell = null;
		cell = new PdfPCell(table);
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		// 设置对齐
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(valign);
		cell.setMinimumHeight(15);
		return cell;
	}

	/**
	 * 获得空单元格
	 * 
	 * @return
	 */
	public static PdfPCell getEmptyCell() {
		return getEmptyCell(1);
	}

	/**
	 * 获得空单元格
	 * 
	 * @param colspan
	 * @return
	 */
	public static PdfPCell getEmptyCell(int colspan) {
		PdfPCell cell = null;
		Paragraph paragraph = null;
		paragraph = new Paragraph("");
		cell = new PdfPCell(paragraph);
		if (colspan > 1) {
			cell.setColspan(colspan);
		}
		cell.setMinimumHeight(15);
		return cell;
	}

	public static void addEmptyRow(Document document,float percentage,int height) throws DocumentException{
		//空白行
    	PdfPTable emptyRow = new PdfPTable(1);   
    	emptyRow.setWidthPercentage(percentage);   
    	emptyRow.getDefaultCell().setBorderWidthLeft(0);   
    	emptyRow.getDefaultCell().setBorderWidthRight(0);   
    	emptyRow.getDefaultCell().setBorderWidthTop(0);
    	emptyRow.getDefaultCell().setBorderWidthBottom(0);
    	emptyRow.getDefaultCell().setFixedHeight(height);
    	emptyRow.addCell("  ");  
    	document.add(emptyRow);
	}
	
	public static void addLineRow(Document document,float percentage,int lineWidth,int height) throws DocumentException{
		document.add(new Paragraph(""));
		PdfPTable tt = new PdfPTable(2);   
		tt.setWidthPercentage(percentage);   
		tt.getDefaultCell().setBorderWidthLeft(0);   
		tt.getDefaultCell().setBorderWidthRight(0);   
		tt.getDefaultCell().setBorderWidthTop(0);
		tt.getDefaultCell().setBorderWidthBottom(lineWidth);
		tt.getDefaultCell().setFixedHeight(height/2);
		tt.addCell("  ");   
		tt.addCell("  ");  
		document.add(tt);
		PdfPTable tt2 = new PdfPTable(2);   
		tt2.setWidthPercentage(percentage);   
		tt2.getDefaultCell().setBorderWidthLeft(0);   
		tt2.getDefaultCell().setBorderWidthRight(0);   
		tt2.getDefaultCell().setBorderWidthTop(0);  
		tt2.getDefaultCell().setBorderWidthBottom(0);
		tt2.getDefaultCell().setFixedHeight(height/2);
		tt2.addCell("  ");   
		tt2.addCell("  ");  
		document.add(tt2);
	}
}
