package com.shotposeai.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.shotposeai.databinding.DialogConfirmActionBinding

object ConfirmActionDialog {
    fun show(context: Context, message: String, action: String = "Hapus", onConfirm: () -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogConfirmActionBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        binding.tvConfirmMessage.text = message
        binding.btnConfirm.text = action
        binding.btnConfirm.setOnClickListener { dialog.dismiss(); onConfirm() }
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
    }
}
