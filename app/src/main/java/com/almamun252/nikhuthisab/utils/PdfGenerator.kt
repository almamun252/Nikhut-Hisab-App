package com.almamun252.nikhuthisab.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.almamun252.nikhuthisab.R
import com.almamun252.nikhuthisab.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    // ব্যাকগ্রাউন্ড থ্রেডে PDF জেনারেট করার ফাংশন
    suspend fun generatePdf(
        context: Context,
        transactions: List<Transaction>,
        reportTitle: String,
        dateRange: String,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.msg_no_transactions), Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }

        // Fetch strings based on user's selected language
        val appName = context.getString(R.string.app_name)
        val lblTotalIncome = context.getString(R.string.label_total_income)
        val lblTotalExpense = context.getString(R.string.label_total_expense)
        val lblBalance = context.getString(R.string.label_balance)
        val lblDate = context.getString(R.string.label_date_header)
        val lblName = context.getString(R.string.label_name_header)
        val lblCategory = context.getString(R.string.label_category)
        val lblType = context.getString(R.string.label_type_header)
        val lblAmount = context.getString(R.string.label_amount_header)
        val strIncome = context.getString(R.string.tab_income)
        val strExpense = context.getString(R.string.tab_expense)
        val lblChart = context.getString(R.string.label_income_expense_chart)
        val lblTop5 = context.getString(R.string.label_top_5_expenses)
        val lblGeneratedBy = context.getString(R.string.label_generated_by)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Paints for styling
        val titlePaint = Paint().apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(33, 150, 243) // Primary Blue
            textAlign = Paint.Align.CENTER
        }

        val subTitlePaint = Paint().apply {
            textSize = 14f
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }

        val headerPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }

        // --- 1. Header Section ---
        canvas.drawText(appName, pageInfo.pageWidth / 2f, 50f, titlePaint)
        canvas.drawText(reportTitle, pageInfo.pageWidth / 2f, 75f, subTitlePaint)
        canvas.drawText(dateRange, pageInfo.pageWidth / 2f, 95f, subTitlePaint)

        // Calculate Totals
        val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }.toFloat()
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }.toFloat()
        val balance = totalIncome - totalExpense

        // --- 2. Summary Boxes ---
        val boxWidth = 150f
        val boxHeight = 60f
        val startY = 130f
        val gap = 20f
        val startX = (pageInfo.pageWidth - (boxWidth * 3 + gap * 2)) / 2f

        drawSummaryBox(canvas, lblTotalIncome, "৳${totalIncome.toInt()}", startX, startY, boxWidth, boxHeight, Color.rgb(255, 202, 40)) // Yellow
        drawSummaryBox(canvas, lblTotalExpense, "৳${totalExpense.toInt()}", startX + boxWidth + gap, startY, boxWidth, boxHeight, Color.rgb(244, 67, 54)) // Red
        drawSummaryBox(canvas, lblBalance, "৳${balance.toInt()}", startX + (boxWidth + gap) * 2, startY, boxWidth, boxHeight, Color.rgb(38, 198, 218)) // Teal

        // --- 3. Charts Section (Dynamic Placement based on Filters) ---
        if (totalIncome > 0 || totalExpense > 0) {
            val chartCenterY = 320f
            val hasExpenses = transactions.any { it.type == "Expense" }

            if (hasExpenses) {
                // If there are expenses, show both Pie Chart (left) and Bar Chart (right)
                drawPdfPieChart(canvas, totalIncome, totalExpense, balance, lblChart, 150f, chartCenterY, 80f)
                drawPdfBarChart(canvas, transactions, lblTop5, 320f, chartCenterY - 70f, 220f, 140f)
            } else {
                // If only income is present, center the pie chart beautifully
                drawPdfPieChart(canvas, totalIncome, totalExpense, balance, lblChart, pageInfo.pageWidth / 2f, chartCenterY, 80f)
            }
        }

        // --- 4. Table Header ---
        var currentY = 430f
        val tableMargin = 40f

        val headerBgPaint = Paint().apply { color = Color.rgb(33, 150, 243) }
        canvas.drawRect(tableMargin, currentY, pageInfo.pageWidth - tableMargin, currentY + 30f, headerBgPaint)

        canvas.drawText(lblDate, tableMargin + 10f, currentY + 20f, headerPaint)
        canvas.drawText(lblName, tableMargin + 100f, currentY + 20f, headerPaint)
        canvas.drawText(lblCategory, tableMargin + 250f, currentY + 20f, headerPaint)
        canvas.drawText(lblType, tableMargin + 380f, currentY + 20f, headerPaint)
        canvas.drawText(lblAmount, tableMargin + 460f, currentY + 20f, headerPaint)

        currentY += 50f

        // --- 5. Table Rows (With Pagination logic) ---
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

        for (tx in transactions) {
            // Check if we need a new page
            if (currentY > pageInfo.pageHeight - 60f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f // Reset Y for new page

                // Redraw Header on new page
                canvas.drawRect(tableMargin, currentY, pageInfo.pageWidth - tableMargin, currentY + 30f, headerBgPaint)
                canvas.drawText(lblDate, tableMargin + 10f, currentY + 20f, headerPaint)
                canvas.drawText(lblName, tableMargin + 100f, currentY + 20f, headerPaint)
                canvas.drawText(lblCategory, tableMargin + 250f, currentY + 20f, headerPaint)
                canvas.drawText(lblType, tableMargin + 380f, currentY + 20f, headerPaint)
                canvas.drawText(lblAmount, tableMargin + 460f, currentY + 20f, headerPaint)
                currentY += 50f
            }

            val dateStr = sdf.format(Date(tx.date))
            val typeStr = if (tx.type == "Income") strIncome else strExpense
            val amountStr = if (tx.type == "Income") "+ ৳${tx.amount.toInt()}" else "- ৳${tx.amount.toInt()}"

            // Text color based on type
            textPaint.color = if (tx.type == "Income") Color.rgb(76, 175, 80) else Color.rgb(244, 67, 54)

            // Draw Row Data
            canvas.drawText(dateStr, tableMargin + 10f, currentY, Paint().apply { textSize = 12f; color = Color.DKGRAY })

            // Title clipping if too long
            var title = tx.title
            if (title.length > 15) title = title.substring(0, 12) + "..."
            canvas.drawText(title, tableMargin + 100f, currentY, Paint().apply { textSize = 12f; color = Color.BLACK })

            canvas.drawText(tx.category, tableMargin + 250f, currentY, Paint().apply { textSize = 12f; color = Color.DKGRAY })
            canvas.drawText(typeStr, tableMargin + 380f, currentY, textPaint)
            canvas.drawText(amountStr, tableMargin + 460f, currentY, textPaint)

            // Draw Line separator
            canvas.drawLine(tableMargin, currentY + 10f, pageInfo.pageWidth - tableMargin, currentY + 10f, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })

            currentY += 30f
        }

        // --- Footer ---
        val footerPaint = Paint().apply {
            textSize = 10f
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(lblGeneratedBy, pageInfo.pageWidth / 2f, pageInfo.pageHeight - 30f, footerPaint)

        pdfDocument.finishPage(page)

        // --- Save to File with MediaStore (Android 10+) ---
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (API 29) and above
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                } else {
                    throw Exception(context.getString(R.string.msg_pdf_creation_failed))
                }
            } else {
                // For Android 9 and below
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            }

            pdfDocument.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.msg_pdf_saved, fileName), Toast.LENGTH_LONG).show()
            }
            return@withContext true

        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.msg_pdf_error, e.message), Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
    }

    private fun drawSummaryBox(canvas: Canvas, title: String, amount: String, x: Float, y: Float, width: Float, height: Float, color: Int) {
        val bgPaint = Paint().apply {
            this.color = color
            alpha = 30 // Very light background
        }
        val textPaint = Paint().apply {
            textSize = 12f
            this.color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }
        val amountPaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.color = color
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 10f, 10f, bgPaint)
        canvas.drawText(title, x + width / 2, y + 20f, textPaint)
        canvas.drawText(amount, x + width / 2, y + 45f, amountPaint)
    }

    private fun drawPdfPieChart(canvas: Canvas, income: Float, expense: Float, balance: Float, chartLabel: String, cx: Float, cy: Float, radius: Float) {
        val total = income + expense + if (balance > 0) balance else 0f
        if (total == 0f) return

        val rectF = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        var startAngle = -90f

        val slices = listOf(
            Pair(income, Color.rgb(255, 202, 40)), // Yellow
            Pair(expense, Color.rgb(244, 67, 54)), // Red
            Pair(if (balance > 0) balance else 0f, Color.rgb(38, 198, 218)) // Teal
        ).filter { it.first > 0f }

        for (slice in slices) {
            val sweepAngle = (slice.first / total) * 360f
            val paint = Paint().apply {
                color = slice.second
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        // Label
        canvas.drawText(chartLabel, cx, cy + radius + 20f, Paint().apply { textAlign = Paint.Align.CENTER; textSize = 12f; color = Color.DKGRAY })
    }

    private fun drawPdfBarChart(canvas: Canvas, transactions: List<Transaction>, chartLabel: String, startX: Float, startY: Float, width: Float, height: Float) {
        val topExpenses = transactions
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .map { Pair(it.key, it.value.sumOf { tx -> tx.amount }.toFloat()) }
            .sortedByDescending { it.second }
            .take(5)

        if (topExpenses.isEmpty()) return

        val maxExpense = topExpenses.maxOf { it.second }
        val barWidth = width / (topExpenses.size * 2)
        val gap = barWidth

        var currentX = startX + gap / 2

        val barPaint = Paint().apply { color = Color.rgb(244, 67, 54) } // Red
        val textPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY; textAlign = Paint.Align.CENTER }

        // Draw Base Line
        canvas.drawLine(startX, startY + height, startX + width, startY + height, Paint().apply { color = Color.LTGRAY; strokeWidth = 2f })

        for (expense in topExpenses) {
            val barHeight = (expense.second / maxExpense) * height
            val topY = startY + height - barHeight

            canvas.drawRect(currentX, topY, currentX + barWidth, startY + height, barPaint)

            // Draw amount on top of bar
            canvas.drawText(expense.second.toInt().toString(), currentX + barWidth / 2, topY - 5f, textPaint)

            // Draw category below base line (truncate if needed)
            var catName = expense.first
            if (catName.length > 5) catName = catName.substring(0, 4) + ".."
            canvas.drawText(catName, currentX + barWidth / 2, startY + height + 15f, textPaint)

            currentX += barWidth + gap
        }

        // Label
        canvas.drawText(chartLabel, startX + width/2, startY + height + 35f, Paint().apply { textAlign = Paint.Align.CENTER; textSize = 12f; color = Color.DKGRAY })
    }
}