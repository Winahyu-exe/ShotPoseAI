package com.shotposeai.ui.common

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.shotposeai.R
import com.shotposeai.data.StudentEntity
import kotlinx.coroutines.CoroutineScope

object StudentCard {
    fun create(context: Context, student: StudentEntity, scope: CoroutineScope, subtitle: String = "${student.baseLevel} • No. ${student.attendanceNumber}", rank: Int? = null, onClick: () -> Unit): MaterialCardView {
        fun dp(n: Int) = (n * context.resources.displayMetrics.density).toInt()
        return MaterialCardView(context).apply {
            radius = dp(16).toFloat(); strokeWidth = 0; cardElevation = dp(1).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(82); setPadding(dp(16), dp(16), dp(16), dp(16)) }
            rank?.let { row.addView(TextView(context).apply { text = it.toString(); textSize = 19f; setTextColor(ContextCompat.getColor(context, R.color.text_primary)); setTypeface(typeface, Typeface.BOLD) }, LinearLayout.LayoutParams(dp(30), -2)) }
            val avatar = ImageView(context).apply { contentDescription = "Foto ${student.name}" }
            row.addView(avatar, LinearLayout.LayoutParams(dp(50), dp(50)))
            AvatarStore.bind(avatar, student.profilePhotoPath, scope)
            val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(TextView(context).apply { text = student.name; textSize = 17f; setTypeface(typeface, Typeface.BOLD); setTextColor(ContextCompat.getColor(context, R.color.text_primary)) })
            texts.addView(TextView(context).apply { text = subtitle; textSize = 12f; setTextColor(ContextCompat.getColor(context, R.color.text_secondary)); setPadding(0, dp(4), 0, 0) })
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(16) })
            addView(row)
            setOnClickListener { onClick() }
        }
    }
}
