package com.neilyin.nnowork.feature.treehole

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.data.treehole.TreeHoleShareCardContent
import com.neilyin.nnowork.data.treehole.TreeHoleShareSource
import com.neilyin.nnowork.data.treehole.shareCardFooterText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 產生樹洞分享圖並開啟 Android Sharesheet。
 *
 * 注意：此層只能接收 [TreeHoleShareCardContent]，因此從型別上就無法取得
 * 貼文暱稱或公司名稱。
 */
object TreeHoleShareCardSharer {
    suspend fun share(
        context: Context,
        content: TreeHoleShareCardContent,
        source: TreeHoleShareSource,
    ) {
        Analytics.log("treehole_share_tapped", "source" to source.analyticsValue)

        val uri = withContext(Dispatchers.IO) {
            val shareDirectory = File(context.cacheDir, SHARE_DIRECTORY).apply {
                check(isDirectory || (!exists() && mkdirs())) { "無法建立分享圖暫存目錄" }
            }
            deleteExpiredCards(shareDirectory)

            val output = File(shareDirectory, "treehole-${UUID.randomUUID()}.png")
            val bitmap = render(content)
            try {
                FileOutputStream(output).use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        "無法寫入分享圖"
                    }
                }
            } finally {
                bitmap.recycle()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                output,
            )
        }

        withContext(Dispatchers.Main.immediate) {
            context.startActivity(createChooser(context, uri))
        }
    }

    private fun createChooser(context: Context, uri: Uri): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "樹洞分享卡", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "分享樹洞貼文").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun render(content: TreeHoleShareCardContent): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND_COLOR)

        val accentCard = RectF(88f, 88f, 1_024f, 1_294f)
        canvas.drawRoundRect(accentCard, 38f, 38f, fillPaint(ACCENT_COLOR))
        val card = RectF(56f, 56f, 992f, 1_262f)
        canvas.drawRoundRect(card, 38f, 38f, fillPaint(Color.WHITE))
        canvas.drawRoundRect(card, 38f, 38f, strokePaint(Color.BLACK, 8f))

        val textLeft = 120f
        val textWidth = 808
        var cursorY = 132f

        val metadata = buildList {
            content.industry?.let { add("產業｜$it") }
            content.role?.let { add("職稱｜$it") }
        }.joinToString("    ")
        if (metadata.isNotEmpty()) {
            val metadataLayout = textLayout(
                text = metadata,
                paint = textPaint(size = 34f, color = MUTED_TEXT_COLOR, bold = true),
                width = textWidth,
                maxLines = 2,
                lineSpacing = 8f,
            )
            canvas.drawLayout(metadataLayout, textLeft, cursorY)
            cursorY += metadataLayout.height + 48f
        }

        val contentSize = when {
            content.content.length > 220 -> 46f
            content.content.length > 120 -> 54f
            else -> 64f
        }
        val footerTop = 1_080f
        val availableContentHeight = (footerTop - cursorY - 48f).coerceAtLeast(contentSize)
        val maxContentLines = (availableContentHeight / (contentSize * 1.3f)).toInt().coerceAtLeast(1)
        val contentLayout = textLayout(
            text = content.content,
            paint = textPaint(size = contentSize, color = Color.BLACK, bold = true),
            width = textWidth,
            maxLines = maxContentLines,
            lineSpacing = 14f,
        )
        canvas.drawLayout(contentLayout, textLeft, cursorY)

        val footer = content.shareCardFooterText()
        if (footer != null) {
            canvas.drawLine(120f, footerTop, 928f, footerTop, strokePaint(ACCENT_COLOR, 8f))
            val footerLayout = textLayout(
                text = footer,
                paint = textPaint(size = 40f, color = Color.BLACK, bold = true),
                width = textWidth,
                maxLines = 2,
                lineSpacing = 8f,
            )
            canvas.drawLayout(footerLayout, textLeft, footerTop + 44f)
        }

        return bitmap
    }

    private fun textLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int,
        lineSpacing: Float,
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(lineSpacing, 1f)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setEllipsizedWidth(width)
            .setMaxLines(maxLines)
            .build()

    private fun Canvas.drawLayout(layout: StaticLayout, x: Float, y: Float) {
        save()
        translate(x, y)
        layout.draw(this)
        restore()
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun strokePaint(color: Int, width: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }

    private fun deleteExpiredCards(directory: File) {
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MILLIS
        directory.listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private const val SHARE_DIRECTORY = "treehole_share"
    private const val CARD_WIDTH = 1_080
    private const val CARD_HEIGHT = 1_350
    private const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1_000L
    private const val BACKGROUND_COLOR = 0xFF242329.toInt()
    private const val ACCENT_COLOR = 0xFFFFC83D.toInt()
    private const val MUTED_TEXT_COLOR = 0xFF5B5A63.toInt()
}
