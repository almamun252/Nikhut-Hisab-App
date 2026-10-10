# Nikhut Hisab

Nikhut Hisab (নিখুঁত হিসাব) is an Android personal finance app for everyday income, expenses, receivables, payables, budgets, and shopping lists. It supports English and Bangla.

Records are stored locally using Room. Google sign-in is optional for everyday use and required for Google Drive backup and restore. Sign-in and cloud backup require internet access.

## Features

- **Dashboard:** Balance summaries, date filters, charts, recent transactions, and shortcuts.
- **Income and expenses:** Add, edit, delete, search, categorize, and filter records.
- **Debt and credit:** Track lending, borrowing, repayments, remaining amounts, and deadlines.
- **Monthly budgets:** Set category budgets and compare actual spending against limits.
- **Rough Khata:** Keep temporary expenses and move them into the main ledger.
- **Shopping lists:** Track estimated/actual costs, purchases, and transfers to the main ledger.
- **PDF reports:** Generate reports for selected transaction types, periods, and categories.
- **Google Drive backup:** Manual backup and restore, plus optional scheduled backup.
- **Reminders:** Daily record-entry and transaction deadline notifications.
- **Appearance and language:** Light/dark appearance and English/Bangla interface.

Background work uses WorkManager. Battery management and network availability may delay scheduled work; exact execution times are not guaranteed.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Structure | ViewModels, repositories, DAOs, Navigation Compose |
| Local database | Room / SQLite |
| Async work | Kotlin Coroutines and Flow |
| Preferences | DataStore and SharedPreferences |
| Authentication | Firebase Authentication and Google Sign-In |
| Cloud backup | Google Drive API and Gson |
| Background jobs | WorkManager |
| Charts and images | YCharts, Vico, Coil |

## Database

`AppDatabase` uses schema version **6**, with migrations from version 1 through version 6.

| Table | Purpose |
| --- | --- |
| `transactions` | Income, expenses, lending, borrowing, and repayments |
| `budgets` | Monthly category budgets |
| `rough_transactions` | Temporary expenses |
| `shopping_list` | Shopping items, costs, and purchase state |

There is no separate category entity/table in the current database.

## Local setup

### 1. Requirements

- Android Studio compatible with the project's Android Gradle Plugin.
- Android SDK Platform **35**, plus the SDK components requested during sync.
- **JDK 21** for the Gradle daemon, configured in `gradle/gradle-daemon-jvm.properties`. Java/Kotlin compilation targets Java 11.
- Android **8.0 / API 26 or newer** on a device or emulator.
- Google Play services and a Google account for login and Drive backup testing.

Use the included Gradle wrapper. Plugin/dependency versions are defined in `gradle/libs.versions.toml` and `app/build.gradle.kts`.

### 2. Clone and open

```bash
git clone https://github.com/almamun252/Nikhut-Hisab-App.git
cd Nikhut-Hisab-App
```

Open the project root in Android Studio. Select your local Android SDK when prompted. Android Studio maintains `local.properties`; another computer's SDK path should not be copied.

### 3. Firebase configuration

Firebase configuration is excluded from this repository. A fresh clone needs its own local configuration before building, including for guest use.

1. Create/select a project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app with package name `com.almamun252.nikhuthisab`.
3. From the project root, obtain signing fingerprints:

   **Windows PowerShell:**

   ```powershell
   .\gradlew.bat :app:signingReport
   ```

   **macOS / Linux:**

   ```bash
   ./gradlew :app:signingReport
   ```

4. Add your debug SHA-1 and SHA-256 in Firebase Project settings. For distribution, also register the release or Play App Signing certificate used by the installed app.
5. Enable **Google** in Firebase Authentication's sign-in providers and set the support email.
6. Download the updated `google-services.json` and save it at `app/google-services.json`.

The Google Services plugin generates `default_web_client_id` and related resources. Do not manually add these values to `strings.xml` or edit `app/build/generated/` files.

### 4. Google Drive configuration

1. Enable the **Google Drive API** in the same project's [Google Cloud Console](https://console.cloud.google.com/).
2. Configure the OAuth consent screen / Google Auth Platform. Add test accounts when required by the project's audience and publishing status.
3. The app requests `https://www.googleapis.com/auth/drive.appdata`; configure consent for this scope.
4. Check that the Android OAuth client matches your package name and signing SHA-1. Keep the Web OAuth client used to obtain the Google ID token.
5. If restricting the Firebase API key to Android applications, allow each package-name/SHA-1 combination used by your builds and keep the required Firebase APIs available.

A new computer may use a different debug certificate. Register its fingerprint and refresh the Firebase JSON before testing login.

### 5. Build and verify

Sync Gradle, select a device, and click **Run**. To build a debug APK from PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/`.

Verify language selection, guest mode, record entry, Google login, and **Backup Now**. Keep an independent copy of important records before testing restore.

## Backup behavior

- Backup JSON includes transactions, budgets, rough entries, and shopping items.
- It is stored as `nikhut_hisab_backup.json` in Drive's application data folder, not as a normal file visible in My Drive.
- Subsequent backups update the existing backup when found. This is a snapshot, not continuous synchronization or versioned backup history.
- Restore is intended to replace current records with the backup. The implementation deletes existing records before reinserting them; deletion and reinsertion are not one atomic transaction. A failed restore may leave incomplete data.
- Backup JSON does not include all local preferences or the Firebase authentication session.

## Repository configuration

Keep personal configuration, SDK paths, signing keys, passwords, and tokens out of commits. `.gitignore` covers Firebase configuration, `local.properties`, common signing-key files, environment files, and local IDE/build folders.

Firebase client configuration is not an administrator private key. Protect backend resources with appropriate access controls and restrict API keys to required services. Never include Firebase Admin service-account private keys in the Android app or repository.

## Troubleshooting

| Problem | Check |
| --- | --- |
| Missing Firebase JSON during build | Place your own file at `app/google-services.json` |
| Missing SDK location | Select your SDK in Android Studio and check local `local.properties` |
| Login fails after changing computers/signing keys | Compare signing SHA-1 with Firebase/OAuth settings and refresh the JSON |
| Login works but Drive backup fails | Check Drive API, consent, account, network, and OAuth configuration |
| Backup file is not visible in My Drive | Backups use the hidden application data folder |
| Scheduled backup runs late | Check network, battery restrictions, and WorkManager scheduling |

## Contributing

Issues and pull requests are welcome. Include the problem, expected behavior, reproduction steps, and device/Android details. Remove account data, API key values, tokens, and private credentials from screenshots and logs.

## References

- [Firebase Google authentication](https://firebase.google.com/docs/auth/android/google-signin)
- [Firebase API keys](https://firebase.google.com/docs/projects/api-keys)
- [Drive application data](https://developers.google.com/workspace/drive/api/guides/appdata)
- [OAuth consent configuration](https://developers.google.com/workspace/guides/configure-oauth-consent)
