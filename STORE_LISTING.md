# Google Play Store Listing & Data Safety Guide

**Application ID:** `ch.simibu.sport`  
**Package:** `ch.simibu.sport`  
**Category:** Health & Fitness  
**Content Rating:** Everyone / PEGI 3  

---

## 1. Store Listing Metadata

### App Title (Max 30 characters)
```text
Sport Amigo - Workout Tracker
```

### Short Description (Max 80 characters)
```text
Log workouts, manage training routines, and track body progression with ease.
```

### Full Description (Max 4,000 characters)
```text
Sport Amigo is your personal, modern companion for strength training, workout tracking, and physical progression monitoring. Designed with high-density layouts and fluid tactile feedback, Sport Amigo makes logging sets and managing workout routines effortless.

Key Features:

🏋️ WORKOUT LOGGING & JOURNAL
- Log sets, weights, repetitions, and RPE with minimal taps.
- Auto-fill sets based on your previous performance.
- Built-in rest countdown timer with customizable haptic vibrations.
- Detailed workout history journal with duration, sets, and volume tracking.

📋 DENSE ROUTINES & TEMPLATES
- Compact, high-density routine list allows you to see all your workout plans at a glance.
- Expand routines smoothly with interactive set tables and exercise notes.
- Quick-start workouts directly with a single tap.
- Easily customize and edit routines anytime.

📖 COMPREHENSIVE EXERCISE LIBRARY
- Search through hundreds of exercises with muscle group and equipment filters.
- Clear exercise illustration icons for quick visual identification.
- Performance history and automated One-Rep Max (1RM) estimations for each exercise.

⚖️ BODY METRICS & PROGRESSION
- Log and monitor body weight changes over time.
- Track body fat percentage and body circumference measurements (chest, waist, shoulders, biceps, thighs).
- Interactive progression charts to visualize your gains.

🌍 MULTILINGUAL & CUSTOMIZABLE
- Fully translated in English, German (Deutsch), French (Français), Spanish (Español), and Italian (Italiano).
- Switch dynamically between System, Light, and Dark mode.
- Offline-first architecture: log workouts anytime, even without an active internet connection.

🔒 PRIVACY & CLOUD SYNC
- Secure cloud synchronization powered by Google Firebase.
- Your personal fitness data belongs to you—no ads, no tracking brokers, and no monetization of your personal data.
```

---

## 2. Google Play Data Safety Questionnaire Answers

When filling out the **Data Safety** section in Google Play Console, use these exact responses:

### Overview Questions
1. **Does your app collect or share any of the required user data types?**  
   👉 **Yes**
2. **Is all of the user data collected by your app encrypted in transit?**  
   👉 **Yes** (All communication with Firebase uses HTTPS / TLS)
3. **Do you provide a way for users to request that their data be deleted?**  
   👉 **Yes** (Users can delete in-app or email `simon.buechi@gmail.com`)

---

### Data Types Collected

#### 1. Personal Info
- **Email address**:
  - **Collected?** Yes
  - **Shared?** No
  - **Processed ephemerally?** No
  - **Required or optional?** Required (for account login)
  - **Purposes:** *App functionality*, *Account management*
- **Name / Display Name** *(if using Google Sign-In)*:
  - **Collected?** Yes
  - **Shared?** No
  - **Required or optional?** Optional
  - **Purposes:** *App functionality*
- **User IDs** (Firebase User ID):
  - **Collected?** Yes
  - **Shared?** No
  - **Purposes:** *App functionality*, *Account management*

#### 2. Health and Fitness
- **Fitness info** (Workouts, exercises, sets, weights, routines, body weight, measurements):
  - **Collected?** Yes
  - **Shared?** No
  - **Processed ephemerally?** No
  - **Required or optional?** Required to provide the core fitness tracking service
  - **Purposes:** *App functionality*

#### 3. App Info and Performance
- **Diagnostics / Crash logs** *(via Firebase/Google Play)*:
  - **Collected?** Yes
  - **Shared?** No
  - **Purposes:** *Analytics*, *App functionality*

---

## 3. App Access (Reviewer Credentials)

Under **Policy and Programs > App Content > App Access**:
- Select: **"All or some functionality in my app is restricted"**
- Add Instructions:
  - **User name / Email:** (Provide a test account, e.g. `tester@simibu.ch` or similar)
  - **Password:** (Provide test password)
  - **Notes:** *"The app requires an account to store and sync personal workouts. Reviewers can log in with these credentials to access all dashboard, workout logging, routines, and tracking features."*

---

## 4. Release Signing Instructions

1. **Generate your keystore** (if you don't already have one):
   ```bash
   keytool -genkey -v -keystore sport-release-key.jks -alias sport -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Create `keystore.properties`** in the project root:
   ```properties
   storeFile=sport-release-key.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=sport
   keyPassword=YOUR_KEY_PASSWORD
   ```
3. **Get SHA-1 for Firebase**:
   ```bash
   keytool -list -v -keystore sport-release-key.jks -alias sport
   ```
   Add this SHA-1 into **Firebase Console > Project Settings > Your Android App**.
4. **Build the production bundle**:
   ```bash
   ./gradlew bundleRelease
   ```
   Upload `app/build/outputs/bundle/release/app-release.aab` to Google Play Console.
