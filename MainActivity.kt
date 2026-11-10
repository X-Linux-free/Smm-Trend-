package com.mawja.payment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bridge", Context.MODE_PRIVATE) }
    private lateinit var endpoint: EditText
    private lateinit var secret: EditText
    private lateinit var method: EditText
    private lateinit var phoneRegex: EditText
    private lateinit var amountRegex: EditText
    private lateinit var txRegex: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 28, 28, 28); setBackgroundColor(0xFFF7F8FC.toInt()) }
        root.addView(TextView(this).apply { text = "Mawja Payment Bridge"; textSize = 23f; setTypeface(null, Typeface.BOLD); setTextColor(0xFF20243A.toInt()) })
        root.addView(TextView(this).apply { text = "اربط هاتف استقبال التحويلات بموقع Mawja Trend. احفظ الإعدادات ثم امنح إذن استقبال الرسائل."; textSize = 14f; setPadding(0, 12, 0, 18) })
        endpoint = field(root, "Webhook URL", "https://YOUR_DOMAIN/api/payment-hook.php")
        secret = field(root, "Webhook Secret", "", true)
        method = field(root, "Payment method code (مثال: vodafone_cash)", "vodafone_cash")
        phoneRegex = field(root, "Sender phone regex (capture group 1)", "(?:من|رقم|هاتف)\\s*(01[0-9]{9})")
        amountRegex = field(root, "Amount regex (capture group 1)", "(?:مبلغ|قيمة|EGP|جنيه)\\s*[:：]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)")
        txRegex = field(root, "Transaction ID regex (capture group 1)", "(?:رقم العملية|رقم المعاملة|Transaction ID|Ref)\\s*[:：#]?\\s*([A-Za-z0-9._/-]{4,100})")
        val save = Button(this).apply { text = "حفظ الإعدادات"; setOnClickListener { saveSettings(); askPermission() } }
        root.addView(save)
        status = TextView(this).apply { text = "الحالة: ${if (prefs.getString("endpoint", "").orEmpty().isNotBlank()) "تم حفظ الإعدادات" else "لم يتم الإعداد"}"; setPadding(0, 18, 0, 0); textSize = 14f }
        root.addView(status)
        root.addView(TextView(this).apply { text = "تنبيه: يجب ضبط أنماط Regex حسب نص رسائل مزود المحفظة الحقيقي. لا يمكن ضمان استخراج بيانات كل المحافظ تلقائياً بنمط واحد."; setPadding(0, 18, 0, 0); textSize = 13f })
        setContentView(ScrollView(this).apply { addView(root) })
        endpoint.setText(prefs.getString("endpoint", "https://YOUR_DOMAIN/api/payment-hook.php"))
        secret.setText(prefs.getString("secret", "")); method.setText(prefs.getString("method", "vodafone_cash"))
        phoneRegex.setText(prefs.getString("phone_regex", "(?:من|رقم|هاتف)\\s*(01[0-9]{9})"))
        amountRegex.setText(prefs.getString("amount_regex", "(?:مبلغ|قيمة|EGP|جنيه)\\s*[:：]?\\s*([0-9]+(?:[.,][0-9]{1,2})?)"))
        txRegex.setText(prefs.getString("tx_regex", "(?:رقم العملية|رقم المعاملة|Transaction ID|Ref)\\s*[:：#]?\\s*([A-Za-z0-9._/-]{4,100})"))
    }

    private fun field(root: LinearLayout, label: String, default: String, password: Boolean = false): EditText {
        root.addView(TextView(this).apply { text = label; textSize = 13f; setTypeface(null, Typeface.BOLD); setPadding(0, 10, 0, 4) })
        return EditText(this).also { e -> e.setText(default); e.textSize = 14f; e.setSingleLine(false); e.minLines = 1; e.inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT; root.addView(e) }
    }
    private fun saveSettings() {
        prefs.edit().putString("endpoint", endpoint.text.toString().trim()).putString("secret", secret.text.toString().trim())
            .putString("method", method.text.toString().trim()).putString("phone_regex", phoneRegex.text.toString())
            .putString("amount_regex", amountRegex.text.toString()).putString("tx_regex", txRegex.text.toString()).apply()
        status.text = "تم حفظ الإعدادات. تأكد من صحة الأنماط قبل تفعيل الشحن التلقائي."
    }
    private fun askPermission() {
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 21)
        else status.text = "جاهز لاستقبال الرسائل المدعومة."
    }
}
