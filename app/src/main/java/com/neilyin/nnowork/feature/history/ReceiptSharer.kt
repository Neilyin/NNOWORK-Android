package com.neilyin.nnowork.feature.history

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.neilyin.nnowork.ui.theme.ShareCardPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ReceiptShareContent(
    val companyName: String,
    val master: Boolean,
    val firstStampDate: Long,
    val completedDate: Long,
    val totalDays: Int,
    val totalStamps: Int,
    val savedAmount: Double,
)

object ReceiptSharer {
    suspend fun share(context: Context, content: ReceiptShareContent) {
        val uri = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, DIRECTORY).apply {
                check(isDirectory || (!exists() && mkdirs())) { "無法建立收據暫存目錄" }
            }
            directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > MAX_AGE_MS }
                ?.forEach(File::delete)
            val output = File(directory, "receipt-${UUID.randomUUID()}.png")
            val bitmap = render(content)
            try {
                FileOutputStream(output).use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "無法寫入收據圖片" }
                }
            } finally {
                bitmap.recycle()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
        }

        withContext(Dispatchers.Main.immediate) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "離職收據", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "分享離職收據"))
        }
    }

    private fun render(content: ReceiptShareContent): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val accent = if (content.master) ShareCardPalette.receiptMasterAccent else ShareCardPalette.receiptAccent
        canvas.drawColor(if (content.master) ShareCardPalette.receiptMasterBackdrop else ShareCardPalette.receiptBackdrop)

        val paper = Paint().apply { color = ShareCardPalette.paper }
        canvas.drawRect(80f, 80f, WIDTH - 80f, HEIGHT - 80f, paper)
        val border = Paint().apply { color = ShareCardPalette.paperBorder; style = Paint.Style.STROKE; strokeWidth = 10f }
        canvas.drawRect(80f, 80f, WIDTH - 80f, HEIGHT - 80f, border)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 62f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ShareCardPalette.ink
            textSize = 38f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val muted = Paint(body).apply { color = ShareCardPalette.mutedInk; textSize = 30f }
        val center = WIDTH / 2f
        canvas.drawText(if (content.master) "模範員工忍耐證明" else "離職收據", center, 190f, title)
        canvas.drawText(content.companyName.take(22), center, 250f, Paint(muted).apply { textAlign = Paint.Align.CENTER })
        divider(canvas, 305f)

        var y = 390f
        val rows = listOf(
            "開始日期" to dateFormat().format(Date(content.firstStampDate)),
            "完成日期" to dateFormat().format(Date(content.completedDate)),
            "忍耐天數" to "${content.totalDays} 天",
            "蓋章次數" to "${content.totalStamps} 章",
            "自由預備金" to NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(content.savedAmount),
        )
        rows.forEachIndexed { index, (label, value) ->
            canvas.drawText(label, 145f, y, muted)
            canvas.drawText(value, WIDTH - 145f, y, Paint(body).apply { textAlign = Paint.Align.RIGHT; color = if (index == rows.lastIndex) ShareCardPalette.money else ShareCardPalette.ink })
            y += 115f
        }
        divider(canvas, y - 45f)

        val message = if (content.master) "撐到這裡，真的不是普通人。" else "祝福你的新旅程，奔向更適合你的地方。"
        canvas.drawText(message, center, y + 55f, Paint(body).apply { textAlign = Paint.Align.CENTER; color = accent; textSize = 32f })
        canvas.drawText("離職集點卡", center, HEIGHT - 155f, Paint(muted).apply { textAlign = Paint.Align.CENTER })
        return bitmap
    }

    private fun divider(canvas: Canvas, y: Float) {
        val paint = Paint().apply { color = ShareCardPalette.dashedDivider; strokeWidth = 4f }
        var x = 140f
        while (x < WIDTH - 140f) {
            canvas.drawLine(x, y, (x + 28f).coerceAtMost(WIDTH - 140f), y, paint)
            x += 46f
        }
    }

    private const val WIDTH = 1080
    private const val HEIGHT = 1420
    private const val DIRECTORY = "receipt_share"
    private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private fun dateFormat() = SimpleDateFormat("yyyy/M/d", Locale.TAIWAN)
}
