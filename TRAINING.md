# Foroom Training

The default debug build works locally without an internet connection. Accounts, sessions, chats and messages are stored only on the device. This mode supports login, registration, avatars, password changes, language changes, chat creation, chat search, persistent messages and sign-out. Chat favorites/deletion and other profile changes are unavailable.

## Run

1. Open the project in Android Studio.
2. Use JDK 17 for Gradle.
3. Select the `app` configuration and `debug` build variant.
4. Run on an emulator or Android device.

The app is installed as **Foroom Training**, separately from the server-connected app.

Demo account: `student` / `Student123!`. Use fictional credentials only.

## Server-connected mode

For a debug build connected to the original server, run:

```sh
./gradlew :app:assembleDebug -PforoomTraining=false
```

Release builds always use the original server. Local accounts, chats and messages are not shared with that server.

For Appium Inspector, the debug training package is `com.alternator.foroom.training`; the activity is `com.example.foroom.presentation.ui.activity.ForoomActivity`.

Conversations are shared by local accounts on the same device. Sign out and switch accounts to continue a conversation as another user. Data is not synchronized between devices.
