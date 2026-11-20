package com.mathkatex.verify.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** 圈选区域，坐标统一用原图像素。 */
sealed class RegionShape {
    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) : RegionShape()
    data class Polygon(val points: List<Pair<Float, Float>>) : RegionShape()
    data class Brush(val points: List<Pair<Float, Float>>, val radius: Float) : RegionShape()
}

/**
 * 把选中的区域裁出来：只保留圈内像素，圈外用白纸色填充。
 * 这样多模态模型不会被试卷空白、相邻题目干扰。
 */
object RegionMasker {

    fun toPath(shapes: List<RegionShape>): Path {
        val combined = Path()
        for (shape in shapes) {
            val p = Path()
            when (shape) {
                is RegionShape.Rect -> p.addRect(
                    RectF(shape.left, shape.top, shape.right, shape.bottom),
                    Path.Direction.CW
                )
                is RegionShape.Polygon -> {
                    shape.points.firstOrNull()?.let { (x, y) -> p.moveTo(x, y) }
                    shape.points.drop(1).forEach { (x, y) -> p.lineTo(x, y) }
                    p.close()
                }
                is RegionShape.Brush -> {
                    shape.points.forEach { (x, y) -> p.addCircle(x, y, shape.radius, Path.Direction.CW) }
                }
            }
            if (combined.isEmpty) {
                combined.set(p)
            } else {
                val merged = Path()
                merged.op(combined, p, Path.Op.UNION)
                combined.set(merged)
            }
        }
        return combined
    }

    fun bounds(shapes: List<RegionShape>, imageWidth: Int, imageHeight: Int, pad: Float = 16f): RectF? {
        if (shapes.isEmpty()) return null
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE
        var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (shape in shapes) {
            when (shape) {
                is RegionShape.Rect -> {
                    l = minOf(l, shape.left); t = minOf(t, shape.top)
                    r = maxOf(r, shape.right); b = maxOf(b, shape.bottom)
                }
                is RegionShape.Polygon -> shape.points.forEach { (x, y) ->
                    l = minOf(l, x); t = minOf(t, y); r = maxOf(r, x); b = maxOf(b, y)
                }
                is RegionShape.Brush -> shape.points.forEach { (x, y) ->
                    l = minOf(l, x - shape.radius); t = minOf(t, y - shape.radius)
                    r = maxOf(r, x + shape.radius); b = maxOf(b, y + shape.radius)
                }
            }
        }
        if (l > r || t > b) return null
        return RectF(
            (l - pad).coerceAtLeast(0f),
            (t - pad).coerceAtLeast(0f),
            (r + pad).coerceAtMost(imageWidth.toFloat()),
            (b + pad).coerceAtMost(imageHeight.toFloat())
        )
    }

    /** 返回只含圈内内容的裁剪图；shapes 为空时返回原图。 */
    fun mask(src: Bitmap, shapes: List<RegionShape>): Bitmap {
        if (shapes.isEmpty()) return src
        val rect = bounds(shapes, src.width, src.height) ?: return src
        val outW = rect.width().toInt().coerceAtLeast(1)
        val outH = rect.height().toInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val path = toPath(shapes)
        canvas.save()
        canvas.translate(-rect.left, -rect.top)
        canvas.clipPath(path)
        canvas.drawBitmap(src, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        return out
    }
}
