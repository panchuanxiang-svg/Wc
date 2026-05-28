name: Android CI

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        with:
          ref: master
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: zulu
          java-version: 17
          cache: gradle

      - name: Grant Execute Permission
        run: chmod +x gradlew

      - name: Build Android
        run: |
          ./gradlew :android:clean :android:assembleDebug --refresh-dependencies --no-build-cache --stacktrace

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: android-debug-apk
          path: android/build/outputs/apk/debug/*.apk
