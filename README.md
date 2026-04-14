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
| JVM target    | Java 17                             |
| Tests         | JUnit 5                             |

---

## Requirements

- **JDK 17 or later** — [Download Temurin](https://adoptium.net/)
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

## Runnable Builds

### OS-specific fat JAR

Build a self-contained JAR that bundles all runtime dependencies including native Skiko libs for the current OS:

```bash
./gradlew packageCurrentOsFatJar
```

The release JAR is written to `build/release-jars/` with an OS and CPU classifier, for example:

```
build/release-jars/purple-email-client-linux-x64-1.1.0.jar
```

Run it with:

```bash
java -jar build/release-jars/purple-email-client-linux-x64-1.1.0.jar
```

> **Note:** The fat JAR is OS-specific. A JAR built on Linux will not run on Windows because Compose Desktop bundles native Skiko libraries for the build OS.

For local compatibility with older instructions, this task still creates a generic copy:

```bash
./gradlew packageFatJar
java -jar build/libs/purple-email-client.jar
```

Do not use the generic file name for release assets because it hides the target OS.

### JAR bundle

Build a ZIP that contains the OS-specific fat JAR plus launcher scripts:

```bash
./gradlew packageCurrentOsJarBundle
```

The ZIP is written to:

```
build/distributions/purple-email-client-<os>-<arch>-1.1.0-jar-bundle.zip
```

On Windows, unzip it and run `run-windows.bat`. This bundle still requires Java 17 or later to be installed on the computer.

### Portable app ZIP

Build a portable application bundle with a Java runtime included:

```bash
./gradlew packagePortableDistribution
```

The ZIP is written to:

```
build/distributions/purple-email-client-<os>-<arch>-1.1.0-portable.zip
```

Use this artifact for users who do not already have Java installed.

This task requires a full JDK that includes `jlink`; a JRE-only installation is not enough.

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
2. Build OS-specific fat JARs on Linux, Windows, and macOS
3. Build JAR bundles with launcher scripts on Linux, Windows, and macOS
4. Build portable app ZIPs with a bundled Java runtime on Linux, Windows, and macOS
5. Create a GitHub Release with all runnable artifacts attached as downloadable assets

### Manual trigger

The workflow can also be triggered manually from the **Actions** tab in GitHub without pushing a tag.

---

## Project Structure

```
purple-email-client/
├── build.gradle.kts                  # Gradle build config, runnable artifact tasks
├── settings.gradle.kts               # Root project name
├── gradle.properties                 # Local JVM override (stripped in CI)
├── gradlew / gradlew.bat             # Gradle wrapper
├── .github/workflows/release.yml     # CI: test → OS builds → GitHub Release
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
