# Project Instructions: "GiveAway" Android Application
**Role:** Senior Android Developer / Architect
**Context:** We are a team of 2 developers building a final project for a college Android course.
**Goal:** Create a fully functional native Android app using Kotlin that allows users to give away unwanted items to people in their local area.

## 1. Project Overview & Scope
* **App Name:** GiveAway
* **Core Function:** Users post items they want to give away; other users can view, filter, and contact the owner via WhatsApp.
* **Team Size Constraints:** 2 Developers (Therefore: **NO** Map View implementation required).
* **Design Standard:** Material Design (use standard components).

## 2. Technical Architecture (Strict Requirements)
You must adhere to **Clean Architecture** and **MVVM** principles.

### Architecture Components
* **Language:** Kotlin (100%).
* **Pattern:** MVVM (Model-View-ViewModel).
* **Navigation:** Android Navigation Component (Single Activity, multiple Fragments).
* **Data Flow:** Unidirectional. Repository pattern must be used to mediate between Local and Remote data sources.
* **Concurrency:** Coroutines & LiveData (or Flow). **No synchronous network calls on the main thread.**

### Data Management (CRITICAL)
* **Remote Source:** Firebase Firestore (or Realtime DB) for storing posts and user data.
* **Local Source (Cache):** **ROOM Database** (SQLite).
    * *Constraint:* You **MUST** implement a local cache. Data is fetched from the remote DB, saved into ROOM, and the UI observes the ROOM database.
    * *Constraint:* **DO NOT** rely on Firebase's native offline persistence for the "local store" requirement. You must manually implement the Room caching logic.
* **Images:**
    * Remote: Store images in Firebase Storage.
    * Loading: Use **Picasso** library for loading images into ImageViews.
    * Local: Cache image URLs in Room.

### Libraries & Dependencies
* **UI:** Material Design Components, ConstraintLayout.
* **Navigation:** `androidx.navigation:navigation-fragment-ktx`, `androidx.navigation:navigation-ui-ktx`.
* **Database:** `androidx.room` (Room, KTX, Compiler).
* **Network/Async:** Coroutines, Firebase SDK.
* **Image Loading:** Picasso.
* **ViewModel/LiveData:** `androidx.lifecycle`.

## 3. Implementation Guidelines

### A. Code Style & Quality
* **Clean Code:** Meaningful variable names, short functions, modular code.
* **No Duplication:** abstract common logic into BaseFragment or utility classes.
* **Safety:** Use **SafeArgs** for passing parameters between fragments.

### B. User Interface (UI/UX)
* **Loading States:** You must show a **Spinner (ProgressBar)** while data is loading asynchronously.
* **Feedback:** Use Toasts or Snackbars for success/error messages.
* **Design:** Follow the mockups strictly (Login, Register, Feed, My Posts, Profile).

### C. Feature Breakdown

#### 1. Authentication
* **Service:** Firebase Authentication.
* **Features:** Sign Up, Login, Logout, Auto-login (check currentUser on app launch).
* **Profile:** Edit Name/Photo functionality.

#### 2. The Feed (Home Screen)
* **Display:** RecyclerView displaying items.
* **Data:** Observes `AllPostsLiveData` from the local Room database.
* **Refresh:** Swipe-to-refresh or on-load logic to fetch fresh data from Firebase -> Update Room.
* **Filtering:**
    * Filter by **City** (populate spinner via external 3rd party API or static list).
    * Filter by **Category** (Furniture, Electronics, etc.).

#### 3. Post Creation (Give Away)
* **Inputs:** Title, Description, Image (Camera/Gallery), City, WhatsApp Number.
* **Action:** Save to Firebase Storage (Image) -> Save URL & Metadata to Firestore -> Sync to Local Room.

#### 4. Item Details
* **Action:** "Contact" button must open an external **WhatsApp** intent using the phone number provided in the post.

#### 5. My Posts
* **Query:** `SELECT * FROM posts WHERE userId = currentUserId`.
* **Actions:** Edit Post, Delete Post (Must update both Firebase and Room).

## 4. Development Workflow for AI Agent
When I ask you to generate code, follow this sequence:
1.  **Define the Entity:** Create the Room Entity / Data Class.
2.  **Define the DAO:** Interface with SQL queries.
3.  **Create the Repository:** Logic to check cache, fetch from Remote, save to Cache.
4.  **Create the ViewModel:** Expose LiveData to the View.
5.  **Create the UI (XML + Fragment):** Bind the ViewModel.

**Reference Repo Standards:** Follow naming conventions and structure similar to `Colman2026Android`.
