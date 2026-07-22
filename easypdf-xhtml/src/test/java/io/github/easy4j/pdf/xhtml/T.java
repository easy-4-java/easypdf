package io.github.easy4j.pdf.xhtml;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.file.Files;
import java.nio.file.Paths;

public class T {

    public static void main(String[] args) throws Exception {

        String url = "https://www.baidu.com";
        // Connect to the URL and parse the HTML document
        Document document = Jsoup.connect(url).get();
        // 设置HTML语法
        document.outputSettings().syntax(Document.OutputSettings.Syntax.html) ;
        // 构建PDF文档，最后将上面的Document进行输出
        PdfRendererBuilder builder = new PdfRendererBuilder();
        // 使用字体，字体名要与模板中CSS样式中指定的字体名相同
       // builder.useFont(new ClassPathResource("/fonts/BabelStoneHan.ttf").getFile(), "BabelStoneHan", 1, BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.toStream(Files.newOutputStream(Paths.get("D:/test.pdf")));
        builder.useFastMode() ;
        //builder.withW3cDocument(new W3CDom().fromJsoup(document), new ClassPathResource("/templates/").getPath());
        builder.run() ;

    }

}
