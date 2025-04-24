package dadm.grupo.dadmproyecto.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object LoadImageUtils {

    fun createCircularBitmapFromBitmap(
        bitmap: Bitmap,
        targetSize: Int = 200, // Tamaño fijo para todas las imágenes
        borderColor: Int = Color.rgb(144, 74, 69), // #904A45
        borderWidth: Float = 4f
    ): Bitmap {
        val output = createBitmap(targetSize, targetSize)
        val canvas = Canvas(output)

        // Crear pintura para el borde
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth

        // Crear pintura para la imagen
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // Redimensionar la imagen para ajustarse al tamaño fijo
        val matrix = Matrix()
        val scale = targetSize / bitmap.width.toFloat()
        matrix.setScale(scale, scale)

        // Centrar la imagen si no es cuadrada
        if (bitmap.width != bitmap.height) {
            matrix.postTranslate(
                (targetSize - bitmap.width * scale) / 2f,
                (targetSize - bitmap.height * scale) / 2f
            )
        }

        shader.setLocalMatrix(matrix)
        imagePaint.shader = shader

        val radius = (targetSize / 2f) - (borderWidth / 2f)
        // Dibujar la imagen circular
        canvas.drawCircle(targetSize / 2f, targetSize / 2f, radius, imagePaint)
        // Dibujar el borde
        canvas.drawCircle(targetSize / 2f, targetSize / 2f, radius, borderPaint)

        return output
    }

    suspend fun loadBitmapFromUrl(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
