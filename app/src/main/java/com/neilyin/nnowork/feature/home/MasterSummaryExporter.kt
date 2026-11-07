package com.neilyin.nnowork.feature.home

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import com.neilyin.nnowork.feature.history.ReceiptSharer
import com.neilyin.nnowork.ui.theme.ShareCardPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 分享圖上的文字（畫面層先用 stringResource 解好，這裡不碰 Resources，方便日後測試）。 */
data class MasterSummaryImageText(
    val title: String,
    val subtitle: String,
    val company: String,
    val fromLabel: String,
    val toLabel: String,
    val fromValue: String,
    val toValue: String,
    val rows: List<Pair<String, String>>,
    val stillHolding: String,
    val modelEmployee: String,
    val footerDate: String,
    val exportFooter: String,
)

/**
 * 忍耐大師階段總結的匯出圖（iOS `exportView`：深色底＋白色收據＋底部標語）。
 * 分享圖是固定外觀的「實體卡片」，色值走 [ShareCardPalette]，不跟 App 主題。
 */
object MasterSummaryExporter {

    suspend fun share(context: Context, text: MasterSummaryImageText, chooserTitle: String) =
        ReceiptSharer.shareImage(context, clipLabel = text.title, chooserTitle = chooserTitle) { render(text) }

    /**
     * 存到系統相簿（Pictures/NNOWORK）。API 29+ 走 MediaStore，不需要權限；
     * API 26–28 需要 WRITE_EXTERNAL_STORAGE（呼叫端先確認已授權）。
     */
    suspend fun saveToGallery(context: Context, text: MasterSummaryImageText) = withContext(Dispatchers.IO) {
        val bitmap = render(text)
        try {
            val resolver = context.contentResolver
            val name = "nnowork_master_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NNOWORK")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = checkNotNull(resolver.insert(collection, values)) { "MediaStore insert failed" }
            try {
                val written = resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } ?: false
                check(written) { "PNG write failed" }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                }
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun render(text: MasterSummaryImageText): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ShareCardPalette.receiptMasterBackdrop)

        val paperRect = RectF(PAPER_X, PAPER_TOP, WIDTH - PAPER_X, PAPER_BOTTOM)
        canvas.drawRoundRect(paperRect, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ShareCardPalette.paper })

        val center = WIDTH / 2f
        val contentWidth = paperRect.width() - INNER * 2
        val left = paperRect.left + INNER
        val right = paperRect.right - INNER

        val accent = ShareCardPalette.receiptMasterAccent
        val title = textPaint(64f, ShareCardPalette.ink, bold = true, align = Paint.Align.CENTER).apply { color = accent }
        val subtitle = textPaint(36f, accent, bold = true, align = Paint.Align.CENTER)
        val company = textPaint(42f, ShareCardPalette.mutedInk, bold = false, align = Paint.Align.CENTER)
        val label = textPaint(36f, ShareCardPalette.mutedInk, bold = false)
        val value = textPaint(40f, ShareCardPalette.ink, bold = true, align = Paint.Align.RIGHT)
        val money = textPaint(40f, ShareCardPalette.money, bold = true, align = Paint.Align.RIGHT)
        val small = textPaint(30f, ShareCardPalette.mutedInk, bold = false, align = Paint.Align.CENTER)
        val employee = textPaint(42f, accent, bold = true, align = Paint.Align.CENTER)

        var y = paperRect.top + 120f
        canvas.drawText(fit(text.title, title, contentWidth), center, y, title)
        y += 64f
        canvas.drawText(fit(text.subtitle, subtitle, contentWidth), center, y, subtitle)
        y += 64f
        canvas.drawText(fit(text.company, company, contentWidth), center, y, company)
        y += 56f
        dashed(canvas, left, right, y)

        y += 84f
        row(canvas, text.fromLabel, text.fromValue, left, right, y, label, value)
        y += 72f
        row(canvas, text.toLabel, text.toValue, left, right, y, label, value)
        y += 52f
        dashed(canvas, left, right, y)

        y += 84f
        text.rows.forEachIndexed { index, (rowLabel, rowValue) ->
            row(canvas, rowLabel, rowValue, left, right, y, label, if (index == text.rows.lastIndex) money else value)
            y += 80f
        }
        y -= 28f
        dashed(canvas, left, right, y)

        y += 90f
        canvas.drawText(fit(text.stillHolding, small, contentWidth), center, y, small)
        y += 70f
        canvas.drawText(fit("🏆 ${text.modelEmployee} 🏆", employee, contentWidth), center, y, employee)
        y += 90f
        canvas.drawText(fit(text.footerDate, small, contentWidth), center, y, small)

        val footer = textPaint(40f, ShareCardPalette.paper, bold = true, align = Paint.Align.CENTER)
        canvas.drawText(fit(text.exportFooter, footer, WIDTH - PAPER_X * 2), center, PAPER_BOTTOM + 100f, footer)
        return bitmap
    }

    private fun row(canvas: Canvas, label: String, value: String, left: Float, right: Float, y: Float, labelPaint: TextPaint, valuePaint: TextPaint) {
        val half = (right - left) / 2f
        canvas.drawText(fit(label, labelPaint, half), left, y, labelPaint)
        canvas.drawText(fit(value, valuePaint, half), right, y, valuePaint)
    }

    private fun dashed(canvas: Canvas, left: Float, right: Float, y: Float) {
        val paint = Paint().apply { color = ShareCardPalette.dashedDivider; strokeWidth = 4f }
        var x = left
        while (x < right) {
            canvas.drawLine(x, y, (x + 24f).coerceAtMost(right), y, paint)
            x += 40f
        }
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean, align: Paint.Align = Paint.Align.LEFT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = align
        }

    private fun fit(value: String, paint: TextPaint, width: Float): String =
        TextUtils.ellipsize(value, paint, width, TextUtils.TruncateAt.END).toString()

    private const val WIDTH = 1080
    private const val HEIGHT = 1400
    private const val PAPER_X = 90f
    private const val PAPER_TOP = 90f
    private const val PAPER_BOTTOM = 1240f
    private const val INNER = 70f
}
