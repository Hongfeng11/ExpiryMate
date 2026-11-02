# ExpiryMate Android (Capacitor)

This folder contains the generated Android Studio project. The Vue web app is packaged locally by Capacitor.

## Build and open

From this `frontend` folder:

```powershell
npm run android:sync
npm run android:open
```

In Android Studio, select an emulator/device and run the `app` configuration. To create a debug APK on Windows:

```powershell
npm run android:apk
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Backend URL

A phone/emulator cannot reach the backend using the browser's `localhost`. Configure `VITE_API_BASE_URL` to an HTTPS URL reachable from the device before building the app, for example in a local `.env.production` file:

```dotenv
VITE_API_BASE_URL=https://your-api.example.com
```

The backend must allow the Capacitor web origin (`https://localhost`) in its CORS configuration and support credentialed session requests. Do not publish a production build against an HTTP API. Android emulator's `10.0.2.2` is only useful for local development and requires corresponding backend CORS/session configuration.
