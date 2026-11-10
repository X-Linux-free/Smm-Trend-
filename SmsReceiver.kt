package com.mawja.payment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = context.getSharedPreferences("bridge", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "").orEmpty()
        val secret = prefs.getString("secret", "").orEmpty()
        if (!endpoint.startsWith("https://") || secret.isBlank()) return
        val method = prefs.getString("method", "vodafone_cash").orEmpty()
        val phoneRe = prefs.getString("phone_regex", "").orEmpty()
        val amountRe = prefs.getString("amount_regex", "").orEmpty()
        val txRe = prefs.getString("tx_regex", "").orEmpty()
        for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val raw = sms.messageBody ?: continue
            val phone = group(phoneRe, raw) ?: continue
            val amountText = group(amountRe, raw)?.replace(',', '.') ?: continue
            val amount = amountText.toDoubleOrNull() ?: continue
            val txid = group(txRe, raw) ?: continue
            val json = JSONObject().put("method_code", method).put("sender_phone", phone)
                .put("amount", amount).put("transaction_id", txid)
                .put("received_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(sms.timestampMillis)))
                .put("raw_sms", raw).put("device_name", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL)
            val input = Data.Builder().putString("endpoint", endpoint).putString("secret", secret).putString("body", json.toString()).build()
            val work = OneTimeWorkRequestBuilder<PaymentPostWorker>().setInputData(input).build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
    private fun group(regex: String, input: String): String? = try {
        val m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE).matcher(input)
        if (m.find() && m.groupCount() >= 1) m.group(1)?.trim() else null
    } catch (_: Exception) { null }
}

class PaymentPostWorker(context: Context, params: androidx.work.WorkerParameters) : androidx.work.Worker(context, params) {
    override fun doWork(): Result {
        val endpoint = inputData.getString("endpoint") ?: return Result.failure()
        val secret = inputData.getString("secret") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        return try {
            val c = URL(endpoint).openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.connectTimeout = 12000; c.readTimeout = 12000
            c.doOutput = true; c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.setRequestProperty("X-Hook-Secret", secret)
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            if (code in 200..299) Result.success() else if (code >= 500 || code == 429) Result.retry() else Result.failure()
        } catch (_: Exception) { Result.retry() }
    }
}
