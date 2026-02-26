package ru.social.nework.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ru.social.nework.R


class TextDrawable(context: Context, text: CharSequence) : Drawable() {
        private val mIntrinsicSize: Int
        private val mTextView: TextView
        private fun createTextView(context: Context, text: CharSequence): TextView {
            val textView = TextView(context)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            textView.layoutParams = lp
            textView.gravity = Gravity.CENTER
            textView.setBackgroundResource(R.drawable.background)
            textView.setTextColor(Color.WHITE)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_TEXT_SIZE.toFloat())
            textView.text = text
            return textView
        }

        /*fun setText(text: CharSequence?) {
            mTextView.text = text
            invalidateSelf()
        }*/

        override fun draw(canvas: Canvas) {
            mTextView.draw(canvas)
        }

        override fun getOpacity(): Int {
            return PixelFormat.OPAQUE
        }

        override fun getIntrinsicWidth(): Int {
            return mIntrinsicSize
        }

        override fun getIntrinsicHeight(): Int {
            return mIntrinsicSize
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(filter: ColorFilter?) {}

        init {
            mIntrinsicSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, DRAWABLE_SIZE.toFloat(),
                context.resources.displayMetrics
            ).toInt()
            mTextView = createTextView(context, text)
            mTextView.width = mIntrinsicSize
            mTextView.height = mIntrinsicSize
            mTextView.measure(mIntrinsicSize, mIntrinsicSize)
            mTextView.layout(0, 0, mIntrinsicSize, mIntrinsicSize)
        }

        companion object {
            private const val DRAWABLE_SIZE = 40 // device-independent pixels (DP)
            private const val DEFAULT_TEXT_SIZE = 16 // device-independent pixels (DP)
        }
    }
