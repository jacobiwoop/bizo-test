<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/e6d8183b-532d-4325-9e3e-082e28fc95dd

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## GitHub Actions APK build

The repository includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`.

What it does:

- builds a debug APK on `push`, `pull_request`, and manual `workflow_dispatch`
- restores the checked-in debug keystore from `debug.keystore.base64`
- creates a CI `.env` file for the Secrets Gradle plugin
- uploads the generated APK as a workflow artifact

Optional repository secrets:

- `GEMINI_API_KEY`
- `SUPABASE_URL`
- `SUPABASE_KEY`

If these secrets are not defined, the workflow still injects placeholder values so the project can compile as long as runtime-only API access is not required during the build.
