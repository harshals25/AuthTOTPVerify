# TOTP verification app

A simple Android app that generates TOTP codes using Kotlin, Jetpack Compose and [a library to generate TOTP codes](https://github.com/marcelkliemannel/kotlin-onetimepassword).
Supports manual secret, issuer entry and QR scanning, updates codes every 30 seconds, and shows a progress bar per entry.

## Features

- Add secrets manually or by scanning an otpauth:// QR.
- Multiple entries shown in a list
- 6-digit TOTP code with the issuer name at the top of the code
- Seconds remaining in the current window using a progress bar for each code
- Secrets are formatted/cleaned (spaces removed, uppercased).
- Handles without crashing if no issuer is present
- Duplicate secret protection.
- ViewModel holds state; survives rotation.

## Technologies Used

- Kotlin
- Jetpack Compose
- Google ML Kit Code Scanner (GmsBarcodeScanning) for QR
- Used https://github.com/marcelkliemannel/kotlin-onetimepassword for TOTP generation

## Installation

1. Clone the repository: git clone [https://github.com/harshals25/AuthTOTPVerify](https://github.com/harshals25/AuthTOTPVerify)
2. Open the project in Android Studio.
3. Build and run the project on an Android device or emulator.

## Usage
Once you build and run, the app by default will run the TOTP view model flow. There are furhter instructions to change to non view model flow in the MainActivity.kt

## License
This project is licensed under the MIT License.
