# Purple Email Client

A lightweight desktop email client built with Kotlin, Jetpack Compose for Desktop, and JavaMail. Manage multiple email accounts from a single window with a clean, modern UI.

---

## Features

- Multi-profile email account management
- IMAP and POP3 inbox support
- SMTP send support
- Inbox view with message list and body preview
- Automatic new-mail polling
- Persistent profiles stored locally (no cloud sync)
- Single runnable fat JAR — no installer needed

---

## Tech Stack

| Layer         | Technology                          |
|---------------|-------------------------------------|
| Language      | Kotlin 2.1                          |
| UI framework  | Jetpack Compose for Desktop 1.7     |
| Mail protocol | JavaMail (javax.mail 1.6.2)         |
| Build         | Gradle 8+ (Kotlin DSL)              |
| JVM target    | Java 17 (runs on JDK 21+)           |
| Tests         | JUnit 5                             |

---

## Requirements

- **JDK 21 or later** — [Download Temurin](https://adoptium.net/)
- **Git**

No separate Maven or Gradle installation is needed — the project includes the Gradle wrapper (`gradlew`).

---

## Local Setup

```bash
git clone https://github.com/fattahpour/purple-email-client.git
cd purple-email-client
```

---

## Build

```bash
./gradlew build
```

---

## Run Tests

```bash
./gradlew test
```

Test reports are written to `build/reports/tests/test/index.html`.

---

## Run the Application

```bash
./gradlew run
```

---

## Fat JAR

Build a single self-contained JAR that bundles all runtime dependencies including native Skiko libs for the current OS:

```bash
./gradlew packageFatJar
```

The JAR is written to:

```
build/libs/purple-email-client.jar
```

Run it with:

```bash
java -jar build/libs/purple-email-client.jar
```

> **Note:** The fat JAR is OS-specific (it bundles native Skiko libs for the OS it was built on). Build on the target OS.

---

## Email Profile Configuration

Profiles are stored in a plain-text properties file in your home directory:

```
~/.java-email-client/profiles.properties
```

The file is created automatically on first use. Each profile stores the account name, provider, host, port, protocol, and username. **Passwords are never written to disk** — you are prompted each time you connect.

### Supported Providers

| Provider      | IMAP Host            | SMTP Host            |
|---------------|----------------------|----------------------|
| Gmail         | imap.gmail.com       | smtp.gmail.com       |
| Outlook/Hotmail | outlook.office365.com | smtp.office365.com |
| Yahoo Mail    | imap.mail.yahoo.com  | smtp.mail.yahoo.com  |
| Custom        | (user-defined)       | (user-defined)       |

---

## Security Notes

- **Never commit passwords or credentials** to the repository.
- **Never store real email credentials** in `profiles.properties` as plain text if you share your machine.
- For Gmail and Yahoo, enable **2-factor authentication** and use an **App Password** instead of your main account password.
- For Outlook/Microsoft 365 personal accounts, use your regular password or an app password if MFA is enabled.
- The `.gitignore` excludes `.env`, `*.pem`, `*.key`, `*.p12`, and `*.jks` files to prevent accidental credential commits.

---

## Release

Releases are built and published automatically by GitHub Actions when a version tag is pushed.

### Create a release

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

The workflow will:
1. Run all tests
2. Build the fat JAR
3. Create a GitHub Release with the JAR attached as a downloadable asset

### Manual trigger

The workflow can also be triggered manually from the **Actions** tab in GitHub without pushing a tag.

---

## Project Structure

```
purple-email-client/
├── build.gradle.kts                  # Gradle build config, fat JAR task
├── settings.gradle.kts               # Root project name
├── gradle.properties                 # Local JVM override (stripped in CI)
├── gradlew / gradlew.bat             # Gradle wrapper
├── .github/workflows/release.yml     # CI: test → fat JAR → GitHub Release
└── src/
    ├── main/
    │   ├── java/com/project/emailclient/
    │   │   ├── EmailClient.java       # Legacy Swing entry point (kept for reference)
    │   │   ├── MailProfile.java       # Account profile model
    │   │   ├── MailProfileStore.java  # Profile persistence (profiles.properties)
    │   │   ├── MailService.java       # IMAP/POP3/SMTP operations
    │   │   ├── MessageSummary.java    # Inbox message value object
    │   │   ├── ProviderConfig.java    # Provider host/port/SSL presets
    │   │   └── MessagesTableModel.java
    │   └── kotlin/com/project/emailclient/
    │       ├── Main.kt                # Application entry point
    │       └── ui/
    │           ├── App.kt             # Root Compose layout
    │           ├── AppState.kt        # Application state holder
    │           ├── InboxScreen.kt     # Inbox message list UI
    │           ├── ProfileDialogUi.kt # Add/edit profile dialog
    │           ├── ConnectDialogUi.kt # Connect to account dialog
    │           └── ComposeDialogUi.kt # Compose new message dialog
    └── test/java/com/project/emailclient/
        ├── MailProfileTest.java
        ├── MailProfileStoreTest.java
        ├── MailServiceTest.java
        ├── MessagesTableModelTest.java
        └── ProviderConfigTest.java
```

---

## Troubleshooting

### Build fails with "Unsupported class file major version"

Your JDK is too old. This project requires JDK 21 or later. Check with:

```bash
java -version
```

### Gradle uses the wrong JDK

`gradle.properties` may contain a `org.gradle.java.home` pointing to a local SDK path. Either update it to point to your JDK 21 installation or remove the line to let Gradle use the system JDK.

### Gmail: "Username and Password not accepted"

Gmail requires an **App Password** when 2FA is enabled. Generate one at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) and use it in place of your regular password.

### Outlook: connection timeout

Ensure your firewall allows outbound connections on port 993 (IMAP) and 587 (SMTP). Some corporate networks block these ports.

### Fat JAR does not start on a different OS

The fat JAR bundles OS-specific native libraries. Rebuild on the target operating system.
