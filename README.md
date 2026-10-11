# Campus Study Group Finder — Prototype 1 (UI)

COMP3074 Group P100 · Kotlin + Jetpack Compose + MVVM

## What works in this prototype
| Screen | State |
|---|---|
| 1 Splash | **Logic:** checks for a remembered sign-in, then routes to Home or Login |
| 2 Login / Sign Up | **Logic:** validation, create account, sign in, wrong-password / duplicate errors, stays signed in |
| 3 Feed (Home) | UI + sample data, search by course/topic |
| 4 Session Details | UI + sample data, Join/Leave toggles locally, creator-only Edit/Delete |
| 5 Create / Edit Session | UI only (one screen, two modes) |
| 6 Full-Screen Map | UI with a drawn map placeholder |
| 7 My Sessions | Joined / Created tabs + empty state |
| 8 Profile | Shows the signed-in user, **Sign Out works** |
| 9 About | Team members |
| 10 Edit Profile | Edits name, program and course chips (saved locally) |

Demo account: `aisha.patel@georgebrown.ca` / `password123` (or tap Sign Up).

## Run it
Open this folder in Android Studio and press Run, or `gradlew assembleDebug`. Unit tests: `gradlew testDebugUnitTest`.

Toolchain: AGP 9.4.0, Gradle 9.7.1, Kotlin 2.2.10 (AGP built-in Kotlin), compileSdk/targetSdk 35, minSdk 26.
Android Studio's bundled Java 25 can't run Gradle 8.x, which is why the project is on Gradle 9.

## Where things live (who owns what next)
```
data/model          User, StudySession (match proposal Section 7)
data/repository     AuthRepository + LocalAuthRepository   -> Zuheib swaps in Firebase Auth
                    SessionRepository + FakeSessionRepository -> Munir swaps in Firestore
di/AppContainer     the one place implementations are chosen
ui/theme            colours, type, spacing (every screen uses these, never raw values)
ui/components       shared buttons, cards, text fields, toggle, map placeholder
ui/navigation       Routes + AppNavHost (nav diagram) + bottom bar
ui/<screen>         one package per screen
```
Swapping fake data for Firebase only changes `AppContainer`; screens talk to the interfaces.

## Not in Prototype 1 (planned, see Development Plan)
Google Maps + location permission (Ben, wk 8) · Firestore data and real join/leave (Munir, wk 4–7) ·
create/edit validation and saving (Zuheib, wk 5) · date/time pickers, date filter, share by email.
