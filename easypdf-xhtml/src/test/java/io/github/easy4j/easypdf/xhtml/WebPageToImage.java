package io.github.easy4j.easypdf.xhtml;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

public class WebPageToImage {

    public static void main(String[] args) throws Exception {

        // 1、源文件夹
        File sourceDir1 = new File("D://sourceDir1");
        File sourceDir2 = new File("D://sourceDir2");
        File targetDir = new File("D://targetDir");
        File logFile = new File("D://targetDir/log.txt");
        FileUtils.forceMkdir(targetDir);

        // 2、源文件夹下的所有文件
        List<String> reportUrls = Arrays.asList("https://www.baidu.com", "https://www.baidu.com");

        int i = 0;
        for (String url : reportUrls) {

            i++;

            String pdfPath = String.format("D://targetDir/%d.pdf", i);

            // Fetch the HTML content using Jsoup
            Document document = Jsoup.connect(url).get();
            document.outputSettings().syntax(Document.OutputSettings.Syntax.html);

            // Convert Jsoup Document to W3C Document
            org.w3c.dom.Document w3cDocument = new W3CDom().fromJsoup(document);

            // Render the HTML content to a PDF using OpenHtmlToPdf
            try (FileOutputStream os = new FileOutputStream(pdfPath)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withW3cDocument(w3cDocument, url);
                builder.toStream(os);
                builder.run();
            }

        }

/*

        // Convert the PDF to an image using PDFBox
        try (RandomAccessReadBuffer readBuffer = new RandomAccessReadBuffer(Files.newInputStream(Paths.get(pdfPath)));
             PDDocument pdfDocument = Loader.loadPDF(readBuffer)) {
            PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
            BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300); // Render the first page at 300 DPI
            // Save the image to a file
            ImageIO.write(image, "png", new File(imagePath));
        }*/
    }
}
