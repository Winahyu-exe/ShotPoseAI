package com.shotposeai.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.shotposeai.data.ReportRow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {
    private fun directory(context: Context) =
        File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }

    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9_-]"), "_")
    private fun date(value: Long) =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(value))

    fun csv(context: Context, report: ReportRow): File {
        val file = File(directory(context), "laporan_${safe(report.studentName)}_${report.sessionId}.csv")
        val values = listOf(
            report.studentName, report.movementName, (report.score / 10.0).toString(),
            report.level, date(report.date), report.correctPoints,
            report.improvementPoints, report.coachNote
        ).joinToString(",") { value ->
            val escaped = value.replace("\"", "\"\"")
            "\"$escaped\""
        }
        file.writeText("Siswa,Gerakan,Skor,Level,Tanggal,Poin Benar,Poin Perbaikan,Catatan Coach\n$values\n")
        return file
    }

    fun xls(context: Context, report: ReportRow): File {
        val file = File(directory(context), "laporan_${safe(report.studentName)}_${report.sessionId}.xls")
        val cells = listOf(
            "Siswa" to report.studentName,
            "Gerakan" to report.movementName,
            "Skor" to (report.score / 10.0).toString(),
            "Level" to report.level,
            "Tanggal" to date(report.date),
            "Poin Benar" to report.correctPoints,
            "Poin Perbaikan" to report.improvementPoints,
            "Catatan Coach" to report.coachNote
        )
        val rows = cells.joinToString("") { item ->
            "<Row><Cell><Data ss:Type=\"String\">${xml(item.first)}</Data></Cell>" +
                "<Cell><Data ss:Type=\"String\">${xml(item.second)}</Data></Cell></Row>"
        }
        file.writeText(
            "<?xml version=\"1.0\"?>" +
                "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" " +
                "xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">" +
                "<Worksheet ss:Name=\"Laporan\"><Table>$rows</Table></Worksheet></Workbook>"
        )
        return file
    }

    fun pdf(context: Context, report: ReportRow): File {
        val file = File(directory(context), "laporan_${safe(report.studentName)}_${report.sessionId}.pdf")
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(255, 87, 34)
            textSize = 24f
            isFakeBoldText = true
        }
        canvas.drawText("ShotPose AI — Laporan Siswa", 42f, 58f, paint)
        paint.color = android.graphics.Color.rgb(25, 30, 40)
        paint.textSize = 18f
        canvas.drawText(report.studentName, 42f, 105f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 13f
        var y = 137f
        val lines = listOf(
            "Gerakan: ${report.movementName}", "Skor: ${report.score / 10.0} (${report.level})",
            "Tanggal: ${date(report.date)}", "", "POIN BENAR", report.correctPoints,
            "", "POIN PERBAIKAN", report.improvementPoints, "", "CATATAN COACH", report.coachNote
        )
        lines.forEach { line ->
            line.chunked(72).ifEmpty { listOf("") }.forEach { part ->
                canvas.drawText(part, 42f, y, paint)
                y += 21f
            }
        }
        document.finishPage(page)
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
        return file
    }

    private fun xml(value: String) = value.replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;")
}
