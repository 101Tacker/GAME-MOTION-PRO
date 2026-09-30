# Game Motion Pro

Game Motion Pro is an Android game-space launcher branded by Tucci Cyber Nation.

## Pro access
The current client-side Pro unlock code is `769933`, as configured for the supplied build. Users can request the code through the in-app WhatsApp button.

> Note: a client-side code is not secure licensing. Anyone with the APK/source can inspect it. A production commercial license system should validate entitlements on a server.

## First launch
1. Game Motion Pro requires a validated Internet connection.
2. It checks device/runtime state.
3. PSP setup downloads PPSSPP 1.20.4 from the official PPSSPP host and hands the APK to Android's package installer.
4. Android Game Space uses the system package installer for imported APK games and launches the installed package's launcher activity.
5. Godot source ZIPs can be catalogued, but a `project.godot` project is not itself an Android executable; use a legitimate Android export to run it.
6. PS2 and PS3 are integration-only unless a compatible, legitimate Android runtime is installed separately. Game Motion intentionally does not report them as installed or running when they are not.

## Build
The supplied repository is configured for Gradle 9.3.1, but the upload did not include `gradle-wrapper.jar` and this environment could not retrieve the Gradle distribution. Build on a machine with Android Studio/Gradle and network access.
