# CLAUDE.md

## Project Overview

This repository contains a legacy Java desktop email client that is being migrated to Java 17.

The application is a Maven-based Swing project under the package `com.project.emailclient`. It provides email-client functionality such as connecting to an account, reading messages, composing mail, replying, forwarding, deleting, and showing RSS/latest-news content.

## Primary Migration Goal

Modernize the current application so it builds, tests, and remains maintainable on Java 17 while preserving the existing Swing desktop behavior.

This is a migration project, not a rewrite. Keep changes focused, incremental, and reviewable.

## Repository Priorities

When working in this repository, prioritize these paths:

- `pom.xml`
- `src/main/java/com/project/emailclient`
- `src/test/java/com/project/emailclient`
- `README.md`

Avoid spending time on generated files or IDE metadata unless the user explicitly asks.

## Current Technical Context

- Java target: 17
- Build tool: Maven
- UI framework: Swing
- Existing mail stack: JavaMail-era `javax.mail` dependencies
- Existing RSS stack: ROME/JDOM-era dependencies
- Current tests: legacy placeholder JUnit 3 test

The current Maven configuration may fail on modern JDKs because source/target settings are not explicitly configured for Java 17.

## Important Existing Classes

- `EmailClient.java`: main Swing email client frame and much of the current email workflow.
- `ConnectDialog.java`: modal connection settings dialog.
- `MessageDialog.java`: compose, reply, and forward dialog.
- `MessagesTableModel.java`: table model for email message rows.
- `RSSFrame.java` and `RssReader.java`: RSS/latest-news UI and feed loading.
- `DownloadingDialog.java`: download progress dialog.
- `MainClass.java`, `JavaApplication2.java`, and `FileList.java`: likely demo or legacy utility code. Inspect before deleting.

## Working Rules

- Use local project files as the source of truth.
- Keep package names stable unless there is a clear migration reason.
- Focus refactors on `src/` and `pom.xml`.
- Preserve user-facing behavior unless the user approves a behavior change.
- Do not require real email credentials or live internet access in automated tests.
- Do not introduce Spring Boot, JavaFX, Gradle, databases, or unrelated frameworks.
- Prefer small service/helper classes over large rewrites.
- Do not log or print passwords.
- Avoid silent exception swallowing.
- Close mail, network, and stream resources safely.

## Recommended Refactor Direction

Separate responsibilities gradually:

- Swing UI classes should handle layout, events, and user feedback.
- Email connection, send, receive, delete, and content extraction should move into dedicated service/helper classes.
- RSS fetching and formatting should move out of Swing frames.
- Provider settings for Gmail, Yahoo, and Outlook/Hotmail should live in a small configuration model.
- Table models should use generics and clear row/column behavior.

## Verification
/
For build-level changes, run:

```bash
mvn test
```

If tests cannot run because of an external condition, report the exact blocker. Otherwise, fix local failures before finishing.

## Claude Configuration

Project-specific Claude Code assets are under `.claude/`:

- `.claude/commands/`: reusable slash-command prompts.
- `.claude/agents/`: specialized agent definitions.
- `.claude/skills/`: project-local skill notes and future reusable workflows.
- `.claude/rules/`: coding and review rules.
- `.claude/hooks/`: hook documentation and future hook scripts/configuration.
