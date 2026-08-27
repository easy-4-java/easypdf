package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.itextpdf.kernel.geom.BezierCurve;
import com.itextpdf.kernel.geom.Line;
import com.itextpdf.kernel.geom.LineSegment;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Path;
import com.itextpdf.kernel.geom.Point;
import com.itextpdf.kernel.geom.Subpath;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;

/**
 * 单遍收集页面元素：文本块（坐标/字号/粗体/mcid）、图片（字节+位置）、格线笔画（水平/垂直）。
 * 供 LayoutAnalyzer 各层分析与 Tagged 结构 mcid 关联使用。
 */
public final class PageModelListener implements IEventListener {

    private final PageModel model;

    public PageModelListener(int pageNo) {
        this.model = new PageModel(pageNo);
    }

    /** 逐页解析整个文档，返回每页的 PageModel。 */
    public static List<PageModel> collect(PdfDocument doc) {
        List<PageModel> all = new ArrayList<PageModel>();
        for (int i = 1; i <= doc.getNumberOfPages(); i++) {
            PdfPage page = doc.getPage(i);
            PageModelListener l = new PageModelListener(i);
            new PdfCanvasProcessor(l).processPageContent(page);
            all.add(l.model);
        }
        return all;
    }

    public PageModel getModel() {
        return model;
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        Set<EventType> s = new HashSet<EventType>();
        s.add(EventType.RENDER_TEXT);
        s.add(EventType.RENDER_IMAGE);
        s.add(EventType.RENDER_PATH);
        return s;
    }

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (data instanceof TextRenderInfo) {
            onText((TextRenderInfo) data);
        } else if (data instanceof ImageRenderInfo) {
            onImage((ImageRenderInfo) data);
        } else if (data instanceof PathRenderInfo) {
            onPath((PathRenderInfo) data);
        }
    }

    private void onText(TextRenderInfo ti) {
        String text = ti.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        LineSegment base = ti.getBaseline();
        float x = base.getStartPoint().get(Vector.I1);
        float y = base.getStartPoint().get(Vector.I2);
        float size = ti.getAscentLine().getStartPoint().get(Vector.I2)
                - ti.getDescentLine().getStartPoint().get(Vector.I2);
        if (size <= 0f) {
            size = ti.getFontSize();
        }
        String fn = fontName(ti).toLowerCase();
        boolean bold = fn.contains("bold");
        boolean mono = fn.contains("mono") || fn.contains("courier") || fn.contains("consolas");
        model.chunks.add(new PageChunk(text, x, y, Math.abs(size), bold, mono, model.pageNo, ti.getMcid()));
    }

    private void onImage(ImageRenderInfo ii) {
        try {
            PdfImageXObject xo = ii.getImage();
            if (xo == null) {
                return;
            }
            byte[] bytes = xo.getImageBytes();
            if (bytes == null || bytes.length == 0) {
                return;
            }
            String ext = xo.identifyImageFileExtension();
            Matrix ctm = ii.getImageCtm();
            float px = ctm.get(Matrix.I31);
            float py = ctm.get(Matrix.I32);
            model.images.add(new RawImage(bytes, ext == null ? "png" : ext,
                    px, py, xo.getWidth(), xo.getHeight(), model.pageNo, ii.getMcid()));
        } catch (Exception ignored) {
            // 部分内联/损坏图片跳过
        }
    }

    private void onPath(PathRenderInfo pri) {
        boolean stroke = (pri.getOperation() & PathRenderInfo.STROKE) != 0;
        boolean fill = (pri.getOperation() & PathRenderInfo.FILL) != 0;
        if (!stroke && !fill) {
            return;
        }
        Path path = pri.getPath();
        if (path == null) {
            return;
        }
        Matrix ctm = pri.getCtm();
        for (Subpath sp : path.getSubpaths()) {
            for (Object seg : sp.getSegments()) {
                List<Point> pts = null;
                if (seg instanceof Line) {
                    pts = ((Line) seg).getBasePoints();
                } else if (seg instanceof BezierCurve) {
                    pts = ((BezierCurve) seg).getBasePoints();
                }
                if (pts == null || pts.size() < 2) {
                    continue;
                }
                Point a = transform(ctm, pts.get(0));
                Point b = transform(ctm, pts.get(pts.size() - 1));
                if (stroke) {
                    RawStroke rs = new RawStroke((float) a.getX(), (float) a.getY(),
                            (float) b.getX(), (float) b.getY(), pri.getLineWidth(), model.pageNo);
                    if (rs.horizontal() || rs.vertical()) {
                        model.strokes.add(rs);
                    }
                }
            }
        }
        // html2pdf 等渲染器把边框画成细填充四边形环（FILL 而非 STROKE，4 段闭合环）：
        // 单子路径的外包盒某一维 ≤3.5pt 时，折算为等价的水平/垂直笔画。
        if (fill && path.getSubpaths().size() == 1) {
            List<Point> outline = subpathOutline(ctm, path.getSubpaths().get(0));
            if (outline.size() >= 3) {
                float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                for (Point p : outline) {
                    minX = Math.min(minX, (float) p.getX()); maxX = Math.max(maxX, (float) p.getX());
                    minY = Math.min(minY, (float) p.getY()); maxY = Math.max(maxY, (float) p.getY());
                }
                float dx = maxX - minX, dy = maxY - minY;
                if (dx >= 1f && dy <= 3.5f) {
                    model.strokes.add(new RawStroke(minX, (minY + maxY) / 2f, maxX, (minY + maxY) / 2f,
                            Math.max(dy, 0.5f), model.pageNo));
                } else if (dy >= 1f && dx <= 3.5f) {
                    model.strokes.add(new RawStroke((minX + maxX) / 2f, minY, (minX + maxX) / 2f, maxY,
                            Math.max(dx, 0.5f), model.pageNo));
                }
            }
        }
    }

    /** 汇总子路径全部线段端点（按顺序），用于识别闭合四边形。 */
    private static List<Point> subpathOutline(Matrix ctm, Subpath sp) {
        List<Point> pts = new ArrayList<Point>();
        for (Object seg : sp.getSegments()) {
            List<Point> base = null;
            if (seg instanceof Line) {
                base = ((Line) seg).getBasePoints();
            } else if (seg instanceof BezierCurve) {
                base = ((BezierCurve) seg).getBasePoints();
            }
            if (base == null || base.isEmpty()) {
                continue;
            }
            if (pts.isEmpty()) {
                pts.add(transform(ctm, base.get(0)));
            }
            pts.add(transform(ctm, base.get(base.size() - 1)));
        }
        return pts;
    }

    private static Point transform(Matrix ctm, Point p) {
        if (ctm == null) {
            return p;
        }
        Vector v = new Vector((float) p.getX(), (float) p.getY(), 1f).cross(ctm);
        return new Point(v.get(Vector.I1), v.get(Vector.I2));
    }

    private static String fontName(TextRenderInfo ti) {
        try {
            return ti.getFont().getFontProgram().getFontNames().getFontName();
        } catch (Exception e) {
            return "";
        }
    }
}
