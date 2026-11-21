package com.example.ui.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.NmsRepository
import com.example.data.model.Nurse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object RosterPrinter {

    // Hold strong reference while page renders for printing
    private var activePrintWebView: WebView? = null

    fun printThreeDayRoster(
        context: Context,
        ward: String,
        nurses: List<Nurse>,
        state: NmsRepository.AppState
    ) {
        val html = buildThreeDayRosterHtml(ward, nurses, state)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        printHtml(context, "NMS_Roster_${ward}_$todayStr", html)
    }

    fun printHtml(context: Context, jobName: String, html: String) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            val webView = WebView(context)
            activePrintWebView = webView
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null && view != null) {
                        val adapter = view.createPrintDocumentAdapter(jobName)
                        val attrs = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                        printManager.print(jobName, adapter, attrs)
                    }
                    activePrintWebView = null
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    private fun buildThreeDayRosterHtml(
        ward: String,
        nurses: List<Nurse>,
        state: NmsRepository.AppState
    ): String {
        val ymdFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("dd MMM yyyy (EEE)", Locale.US)
        val printTimeFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val calToday = Calendar.getInstance()
        val calTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }

        val yesterdayStr = ymdFmt.format(calYesterday.time)
        val todayStr = ymdFmt.format(calToday.time)
        val tomorrowStr = ymdFmt.format(calTomorrow.time)

        val yesterdayLabel = labelFmt.format(calYesterday.time)
        val todayLabel = labelFmt.format(calToday.time)
        val tomorrowLabel = labelFmt.format(calTomorrow.time)
        val printedAt = printTimeFmt.format(Date())

        val hospitalName = state.footer.hospitalName.ifBlank { "Nurses Management System (NMS)" }
        val hospitalAddr = state.footer.address

        val sb = StringBuilder()
        sb.append(
            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <style>
              @page { size: A4 portrait; margin: 12mm; }
              * { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
              body { font-family: sans-serif; color: #0f172a; margin: 0; padding: 0; font-size: 12px; }
              .hdr { text-align: center; border-bottom: 2px solid #0f766e; padding-bottom: 8px; margin-bottom: 10px; }
              .hdr h1 { margin: 0; font-size: 17px; color: #0f766e; }
              .hdr p { margin: 2px 0; font-size: 11px; color: #475569; }
              .meta { display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 8px; color: #334155; }
              .legend { font-size: 10.5px; color: #334155; margin-bottom: 10px; padding: 6px 8px; background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 4px; }
              table { width: 100%; border-collapse: collapse; table-layout: fixed; }
              th, td { border: 1px solid #334155; padding: 6px 5px; text-align: center; vertical-align: middle; word-wrap: break-word; }
              th { background: #e2e8f0; color: #0f172a; font-size: 11px; font-weight: 700; }
              td.sl { width: 32px; font-weight: 600; }
              td.nm { text-align: left; width: 30%; font-weight: 600; }
              td.hr { width: 16%; font-weight: 600; color: #1e293b; }
              th.col-today {
                background: #fde047 !important;
                color: #7c2d12 !important;
                border: 2px solid #b45309 !important;
              }
              td.col-today {
                background: #fef9c3 !important;
                border-left: 2px solid #b45309 !important;
                border-right: 2px solid #b45309 !important;
                font-weight: 800;
              }
              tr:last-child td.col-today { border-bottom: 2px solid #b45309 !important; }
              .badge-today {
                display: inline-block;
                background: #b45309;
                color: #ffffff;
                font-size: 9px;
                padding: 1px 5px;
                border-radius: 4px;
                margin-bottom: 2px;
                letter-spacing: 0.5px;
              }
              .inc { font-size: 10px; color: #b45309; font-weight: 700; }
              .sh { font-size: 13px; font-weight: 800; }
              .sh-M { color: #1d4ed8; }
              .sh-E { color: #c2410c; }
              .sh-N { color: #6d28d9; }
              .sh-D { color: #15803d; }
              .sh-L { color: #b91c1c; }
              .ft { margin-top: 14px; display: flex; justify-content: space-between; font-size: 10.5px; color: #475569; }
            </style>
            </head>
            <body>
              <div class="hdr">
                <h1>${escHtml(hospitalName)}</h1>
                ${if (hospitalAddr.isNotBlank()) "<p>${escHtml(hospitalAddr)}</p>" else ""}
                <p><strong>Duty Roster — Ward: ${escHtml(ward)}</strong></p>
              </div>
              <div class="meta">
                <span><strong>Schedule:</strong> $yesterdayLabel &nbsp;|&nbsp; $todayLabel &nbsp;|&nbsp; $tomorrowLabel</span>
                <span><strong>Total Nurses:</strong> ${nurses.size}</span>
              </div>
              <div class="legend">
                <strong>Shift Key:</strong> M = Morning (8am–2pm) &nbsp;·&nbsp; E = Evening (2pm–8pm) &nbsp;·&nbsp; N = Night (8pm–8am) &nbsp;·&nbsp; D/O = Day Off &nbsp;·&nbsp; CL/EL/ML = Leave
              </div>
              <table>
                <thead>
                  <tr>
                    <th style="width:34px;">SL</th>
                    <th style="width:30%; text-align:left;">Name</th>
                    <th style="width:16%;">HRIS ID</th>
                    <th>Previous Day<br>$yesterdayLabel</th>
                    <th class="col-today"><span class="badge-today">TODAY</span><br>$todayLabel</th>
                    <th>Next Day<br>$tomorrowLabel</th>
                  </tr>
                </thead>
                <tbody>
            """.trimIndent()
        )

        if (nurses.isEmpty()) {
            sb.append("<tr><td colspan=\"6\">No nurses assigned to this ward.</td></tr>")
        } else {
            nurses.forEachIndexed { index, nurse ->
                val yCell = formatCellForDate(nurse.id, ward, yesterdayStr, state)
                val tCell = formatCellForDate(nurse.id, ward, todayStr, state)
                val nCell = formatCellForDate(nurse.id, ward, tomorrowStr, state)
                val inchargeTag = if (nurse.cat == "Nurse Incharge") " <span class=\"inc\">★ Incharge</span>" else ""

                sb.append("<tr>")
                sb.append("<td class=\"sl\">${String.format(Locale.US, "%02d", index + 1)}</td>")
                sb.append("<td class=\"nm\">${escHtml(nurse.name)}$inchargeTag</td>")
                sb.append("<td class=\"hr\">${escHtml(nurse.hris.ifBlank { "—" })}</td>")
                sb.append("<td>$yCell</td>")
                sb.append("<td class=\"col-today\">$tCell</td>")
                sb.append("<td>$nCell</td>")
                sb.append("</tr>")
            }
        }

        sb.append(
            """
                </tbody>
              </table>
              <div class="ft">
                <span>Printed: $printedAt</span>
                <span>Signature of Controlling Officer: ____________________</span>
              </div>
            </body>
            </html>
            """.trimIndent()
        )

        return sb.toString()
    }

    private fun formatCellForDate(
        nurseId: String,
        ward: String,
        dateStr: String,
        state: NmsRepository.AppState
    ): String {
        val leave = state.leaves.find { l ->
            l.nurseId == nurseId && l.stage in listOf("OK", "FIN") && l.from <= dateStr && dateStr <= l.to
        }
        if (leave != null) {
            return "<span class=\"sh sh-L\">${escHtml(leave.type)}</span>"
        }

        var confirmedShift = ""
        listOf("M", "E", "N", "D").forEach { s ->
            val key = "$dateStr|$ward|$s"
            if ((state.roster[key] ?: emptyList()).contains(nurseId)) {
                confirmedShift = s
            }
        }

        val pendingReq = state.dutyRequests.find { q ->
            q.type == "duty" && q.nurseId == nurseId && q.date == dateStr && q.ward == ward && q.stage in listOf("NI", "SUP")
        }

        val shift = pendingReq?.shift ?: confirmedShift
        if (shift.isBlank()) return "—"

        val label = if (shift == "D") "D/O" else shift
        val pendingPrefix = if (pendingReq != null) "⏳ " else ""
        return "<span class=\"sh sh-$shift\">$pendingPrefix${escHtml(label)}</span>"
    }

    private fun escHtml(input: String): String = TextUtils.htmlEncode(input)
}
