import com.google.api.gax.rpc.ApiException
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.BatchAnnotateImagesRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageAnnotatorSettings
import com.google.protobuf.ByteString
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO


fun main() {
    val pdfPath = "/Users/example/hogehoge.pdf"
    try {
        // PDFを画像に変換
        val document = PDDocument.load(File(pdfPath))
        val renderer = PDFRenderer(document)

        val requestBuilder = BatchAnnotateImagesRequest.newBuilder()

        for (pageIndex in 0 until document.numberOfPages) {
            val image: BufferedImage = renderer.renderImageWithDPI(pageIndex, 300f) // DPIを設定
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(image, "png", byteArrayOutputStream)
            val imageBytes = ByteString.copyFrom(byteArrayOutputStream.toByteArray())

            // Vision APIリクエストの構築
            val visionImage = Image.newBuilder().setContent(imageBytes).build()
            val feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build()
            val request = AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(visionImage).build()
            requestBuilder.addRequests(request)
        }
        document.close()

        // Vision APIクライアントの生成
        val settings =
            ImageAnnotatorSettings.newBuilder().setCredentialsProvider { GoogleCredentials.getApplicationDefault() }
                .build()
        val client = ImageAnnotatorClient.create(settings)

        // リクエストの送信
        val response = client.batchAnnotateImages(requestBuilder.build())
        val list = response.responsesList.first().fullTextAnnotation.text.split("\r\n", "\n")

        client.close()

        var date = ""
        var amount = ""
        list.forEach {
            if (it.startsWith("注文日")) {
                date = LocalDate.parse(it.replace("注文日: ", ""), DateTimeFormatter.ofPattern("yyyy年M月d日"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            }
            if (it.startsWith("ご請求額")) {
                amount = it.replace(Regex(" |　"), "").replace(Regex("ご請求額:¥|,"), "")
            }
        }

        val oldFile = File(pdfPath)
        val newFile = File("/Users/example/result/${date}_Amazon_$amount.pdf")

        Files.copy(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    } catch (e: ApiException) {
        println("error")
    }
}