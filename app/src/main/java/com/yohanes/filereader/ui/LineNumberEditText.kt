package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText

class LineNumberEditText(context: Context, attrs: AttributeSet? = null) :
    EditText(context, attrs) {

    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textAlign = Paint.Align.RIGHT
    }
    private val density = resources.displayMetrics.density
    private val gutterGapPx = 12 * density
    private val textStartGapPx = (8 * density).toInt()
    private var lastDigitCount = -1

    init {
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        setHorizontallyScrolling(false)
        maxLines = Int.MAX_VALUE
        setBackgroundColor(Color.TRANSPARENT)
        numberPaint.textSize = textSize * 0.85f
    }

    private fun updateGutterPadding() {
        val digitCount = lineCount.toString().length.coerceAtLeast(2)
        if (digitCount != lastDigitCount) {
            lastDigitCount = digitCount
            val gutterWidth = numberPaint.measureText("0".repeat(digitCount)) + gutterGapPx
            setPadding(gutterWidth.toInt() + textStartGapPx, paddingTop, paddingRight, paddingBottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        updateGutterPadding()
        val gutterRight = paddingLeft - textStartGapPx - (gutterGapPx / 2)
        val currentLayout = layout
        if (currentLayout != null && lineCount > 0) {
            val clip = canvas.clipBounds
            val firstLine = currentLayout.getLineForVertical(clip.top).coerceAtLeast(0)
            val lastLine = currentLayout.getLineForVertical(clip.bottom).coerceAtMost(lineCount - 1)
            for (i in firstLine..lastLine) {
                val baseline = getLineBounds(i, null)
                canvas.drawText((i + 1).toString(), gutterRight, baseline.toFloat(), numberPaint)
            }
        }
        super.onDraw(canvas)
    }
}
