# Mawja Payment Bridge (Android companion source)

This private Android companion receives incoming SMS broadcasts on a dedicated Android phone, extracts configured fields, and forwards a structured event to `api/payment-hook.php` over HTTPS. Failed network sends are retried by WorkManager.

## Build
1. Open this `android-companion` directory in Android Studio (recent version with Android Gradle Plugin 8.7+).
2. Wait for Gradle sync and build **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Install the APK on the dedicated receiving phone. This is a private/internal app; SMS permissions are restricted by Google Play policy and this build is not intended for Play Store publication.
4. Open the app and enter:
   - Webhook URL: `https://YOUR_DOMAIN/api/payment-hook.php`
   - Webhook Secret: copy from Admin > Auto payments
   - Payment method code: e.g. `vodafone_cash` (must exactly match Admin > Payment methods > Code)
   - Regex patterns: each pattern must have **capture group 1** for sender phone, amount, and transaction ID.
5. Save and grant **Receive SMS** permission.
6. Keep the phone powered, connected to the internet, and exempt the app from battery restrictions where the phone manufacturer provides that option.

## Configure payment method
In Admin > Payment methods, enable the selected method, set its receiving number/instructions, and turn on `auto_sms`. For automatic matching, the customer must enter the sender's mobile number and exact transferred amount. The system creates its own request reference; the customer does not enter a transaction ID.

## Important format-specific setup
There is no universal SMS format across Vodafone Cash, Orange Cash, e&, WE Pay, or banks. The default regular expressions are examples only. Take a real, redacted SMS sample from the receiving phone and configure the three regex patterns to extract:
- the **customer's sender mobile number** (not the wallet's sender ID),
- the exact amount as a decimal number using `.` as decimal separator,
- the unique transaction/reference ID.

If any field cannot be extracted, the app intentionally does not submit an event. The request remains pending and no balance is credited. Do not loosen the patterns to guess values.

## Security and limitations
- Use HTTPS only. The shared webhook secret is a bearer credential: keep it private and regenerate it immediately if the phone is lost.
- The server checks method, sender number, exact amount, transaction time, and a unique transaction ID. Ambiguous matches and duplicate IDs are rejected.
- This is an SMS-assisted matcher, not a bank-certified API. An SMS may be delayed, malformed, or spoofable on a compromised phone. Keep manual review enabled for exceptions and validate with small real transfers before production.
- Android permissions, OEM battery policies, SIM/network availability, and provider message templates affect delivery. The source project is provided; an APK is not prebuilt in this environment.
