# Money Plan — Android

Native Jetpack Compose Android app for [Money Plan](../money-plan-frontend), sharing the same Firebase Auth and Railway backend API.

## Google Play status

| | |
|---|---|
| **Status** | **Closed testing in progress** |
| **Play Console app** | Money Plan |
| **Package name** | `com.moneyplann.app` |
| **Version** | 1.0.0 (`versionCode` 4 — bump before each Play upload) |
| **Category** | Finance |
| **Pricing** | Free |
| **Privacy policy** | [moneyplann.com/privacy](https://www.moneyplann.com/privacy) |
| **Support** | [support@moneyplann.com](mailto:support@moneyplann.com) · [moneyplann.com/faq](https://www.moneyplann.com/faq) |

**App Review demo account** (same as iOS / web):

- Email: `appstore.review@moneyplann.com`
- Password: `MoneyPlan-Review2026!`

Re-seed sample data if needed: `money-plan-backend/scripts/create-demo-account.ts`

---

## TODO — Play Store release path

Google Play requires **closed testing** (12 testers, 14 days) before **production** access. **Internal testing** is optional but recommended first.

### Completed

- [x] Google Play Developer account ($25)
- [x] App created in Play Console (`com.moneyplann.app`)
- [x] App access (sign-in required + demo credentials)
- [x] Privacy policy URL
- [x] Content rating (IARC)
- [x] Data safety (in progress / submitted as applicable)
- [x] Main store listing — name, descriptions, contact email, tags
- [x] Listing assets — app icon, feature graphic, phone screenshots (Expenses, Chat, Accounts)
- [x] ASO listing polish — full description, short description, Chat screenshot on **Store listings**
- [x] Upload keystore + Gradle release signing (`release/`, `keystore.properties` — local, gitignored)
- [x] Signed release bundle built (`app/build/outputs/bundle/release/app-release.aab`)

### 1. Internal testing *(optional)*

Quickly share builds for initial quality checks on your own device or a small trusted group. Builds usually appear within seconds after upload. **Optional** — you still must run closed testing before production.

- [ ] Add **Play App signing SHA-1** + upload + debug SHA-1 to Firebase → new `google-services.json` → rebuild AAB (see [Google Sign-In](#google-sign-in-debug-vs-play-store-builds))
- [ ] Play Console → **Test and release → Testing → Internal testing**
- [ ] **Create new release** → upload `app/build/outputs/bundle/release/app-release.aab`
- [ ] Release notes (e.g. `1.0.0 — initial internal test`) → **Review release** → **Start rollout**
- [ ] **Testers** tab → add your Gmail → open opt-in link → install from Play Store
- [ ] After first upload: add **Play App Signing** SHA-1 from Play Console to Firebase (required for Google Sign-In on closed testing — see [Google Sign-In](#google-sign-in-debug-vs-play-store-builds))
- [ ] Smoke test release build: email login, Google Sign-In, expenses, income, accounts, chat + AI consent

### 2. Closed testing *(required for production — in progress)*

Share with a controlled group to find issues, get feedback, and **unlock production access**.

- [ ] Play Console → **Test and release → Testing → Closed testing**
- [ ] Publish a **closed testing release** (upload AAB; bump `versionCode` in `app/build.gradle.kts` before each upload)
- [ ] Recruit **at least 12 testers** opted in (real Google accounts)
- [ ] Run closed test for **at least 14 days**
- [ ] Fix bugs from feedback; upload new builds as needed

**Current Play Console criteria:**

| Requirement | Status |
|-------------|--------|
| Publish a closed testing release | Not started |
| At least 12 testers opted in | 0 / 12 |
| Closed test running ≥ 14 days | Not started |

### 3. Production

Apply for access after closed testing meets Google’s criteria.

- [ ] Confirm dashboard checklist complete (store listing, policies, data safety, etc.)
- [ ] **Apply for access to production** (answer questions about your closed test)
- [ ] Play Console → **Production → Create new release**
- [ ] Upload tested AAB → submit for review
- [ ] After approval → publish to everyone on Google Play
- [ ] Update this README status to **Live on Google Play** + add public store link

---

## Organic growth (ASO + SEO)

App Store, Play Store, and web growth plan (no paid ads): **[`../ORGANIC_GROWTH.md`](../ORGANIC_GROWTH.md)** — checkboxes for done vs next steps (keywords, screenshots, pt-BR, in-app reviews, SEO).

---

## Store listing assets (local)

| Asset | Path |
|-------|------|
| Feature graphic (1024×500) | `play-store/feature-graphic-1024x500.png` |
| Phone screenshot — Expenses | `play-store/phone-screenshot-01-expenses.png` |
| Phone screenshot — Accounts | `play-store/phone-screenshot-02-accounts.png` |
| Phone screenshot — Chat | `play-store/phone-screenshot-03-chat.png` |

**Recommended upload order on Play:** 1 Expenses → 2 Chat → 3 Accounts *(live in Play Console)*.

App icon for Play Console: export **512×512 PNG** from `money-plan-ios/.../AppIcon.png` (same brand as iOS).

---

## Edit Play Store listing (Google Play Console)

Listing text is **not** in the Android app source — only in Play Console. Google’s UI label is **Store listings** (older docs call this “Main store listing”).

### Open the default listing

1. [https://play.google.com/console](https://play.google.com/console) → select **Money Plan** (`com.moneyplann.app`)
2. Left sidebar: **Grow users** → **Store presence** → **Store listings**
3. Open the **Default** listing (or **Default listing group** → default listing). This is the page all users see unless you add custom listings later.
4. Select locale **English (United States)** if prompted.

### Fields on that page

| Field | Limit | Use for ASO |
|-------|-------|-------------|
| **App name** | 30 | Title on Play Store |
| **Short description** | 80 | First line on listing / search snippet — paste from [`play-store/LISTING_COPY.md`](play-store/LISTING_COPY.md) |
| **Full description** | 4000 | ASO opening + feature bullets (see LISTING_COPY.md) |

**Store settings** (same sidebar) = contact email / website only — not description.  
**Store listing experiments** = A/B tests — edit the default listing first, experiment later.

### Save and publish metadata

1. **Save** (top right) on the listing page.
2. Left sidebar → **Publishing overview** (or a banner “Changes ready to publish”).
3. **Send for review** / **Publish changes** — without this step, edits may not go live.

No new AAB required for description-only changes. Listing is shared across closed testing and production tracks.

Official help: [Create and set up your app — store listing](https://support.google.com/googleplay/android-developer/answer/9859152)

---

## Requirements

- Android Studio / JDK 17+
- Android SDK 35
- Firebase Android app in project `money-plan-23efb`
- `app/google-services.json` (see `app/google-services.json.example`; file is gitignored)

## Setup

1. Copy Firebase config:

   ```bash
   # Download from Firebase Console → Android app → com.moneyplann.app
   cp /path/to/google-services.json app/google-services.json
   ```

2. Open `money-plan-android` in Android Studio, or from CLI:

   ```bash
   cd money-plan-android
   ./gradlew :app:assembleDebug
   ```

3. Run on emulator or device. Sign in with the same account you use on web/iOS.

## Production API

Default: `https://money-plan-backend-production.up.railway.app` (see `API_BASE_URL` in `app/build.gradle.kts`).

## Features (parity with web / iOS)

| Screen | Description |
|--------|-------------|
| **Login** | Email/password, Google Sign-In |
| **Expenses** | List, search, filters, CRUD |
| **Income** | Quick add, edit, delete |
| **Accounts** | Balances, create/rename/delete, default account |
| **Chat** | AI expense assistant (`POST /v1/ai/expense-chat`) with in-app consent |

---

## Release signing

Release builds use `release/money-plan-upload.keystore` and `keystore.properties` (both **gitignored** — back them up; losing them blocks future updates).

**Create a new keystore** (only if needed):

```bash
cd money-plan-android
./scripts/create-upload-keystore.sh
```

**Build a signed Play bundle:**

```bash
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

**Upload key SHA-1** (add in [Firebase Console](https://console.firebase.google.com/project/money-plan-23efb/settings/general) → Android app `com.moneyplann.app` → **Add fingerprint**):

```text
64:BE:B6:9F:D3:6F:B5:0F:F7:90:54:3B:CF:B0:9A:12:DD:31:B1:19
```

After the **first AAB upload**, Play Console → **Setup → App signing** → copy the **App signing key certificate** SHA-1 and add it to Firebase too (Google Sign-In on Play builds uses that cert).

Store password lives in local `keystore.properties` only — copy it to your password manager.

---

## Google Sign-In: debug vs Play Store builds

Google Sign-In matches the **APK signing certificate** to OAuth clients in Firebase. Different builds → different SHA-1 fingerprints.

| Build | Keystore | SHA-1 in Firebase? |
|-------|----------|-------------------|
| **Emulator / debug** (`assembleDebug`) | `~/.android/debug.keystore` | Debug SHA-1 |
| **Local release** (`bundleRelease`) | `release/money-plan-upload.keystore` | Upload key SHA-1 |
| **Closed testing / Play Store** | **Google Play App Signing** | **App signing key SHA-1** ← closed testers need this |

**Symptom:** Google works on your emulator but fails for closed testers with *“Google Sign-In is not configured…”* (Firebase error **10**).

### Fix (one-time, then rebuild)

1. **Play Console** → **Protected with Play** → open **Play Store protection**  
   On that page, click **Releases signed by Play** (or **Go to Play app signing** / **Manage Play app signing** — link is on this screen, not in the toggle list).  
   **Alternate:** left sidebar → **App integrity** → **Play app signing** / certificates.

2. Copy **App signing key certificate** → **SHA-1** (and add **Upload key** SHA-1 from the same page if listed).
   - **App signing** SHA-1 (Play Store / closed testing) — **required for testers**
   - **Upload key** SHA-1: `64:BE:B6:9F:D3:6F:B5:0F:F7:90:54:3B:CF:B0:9A:12:DD:31:B1:19`
   - **Debug** SHA-1: `25:2D:71:5F:B1:63:09:FB:01:58:94:F0:34:78:D1:1A:48:B0:36:8B`

3. **Firebase Console** → [Project settings](https://console.firebase.google.com/project/money-plan-23efb/settings/general) → Android app **`com.moneyplann.app`** → **Add fingerprint** — paste each SHA-1 above that is not already listed.

4. **Download** the updated **`google-services.json`** → replace `app/google-services.json`.

5. Bump **`versionCode`** in `app/build.gradle.kts`, rebuild, upload a new AAB to **closed testing**:

   ```bash
   ./gradlew :app:bundleRelease
   ```

6. Ask testers to update from Play Store (same track).

Adding SHA-1 in Firebase alone is **not** enough — the new `google-services.json` must be **inside** the AAB you ship.
