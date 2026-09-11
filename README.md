# Cardiolo - Android Fitness Tracker

## Overview
Cardiolo records workouts through manual entry, GPS tracking, or on-device activity classification. Routes and exercise history are stored locally with Room. This is an individual MyRuns coursework project; the Android package remains `com.example.myruns`.

## Features

### 🏃‍♂️ Activity Tracking
- **Multiple Input Types:**
  - Manual entry
  - GPS tracking
  - Automated activity detection
- **Various Activity Types:**
  - Running
  - Walking
  - Standing
  - Cycling
  - Hiking
  - And more...

### 📱 User Interface
- Material Design implementation
- Tab-based navigation
- Interactive maps for route tracking
- Detailed activity history
- Comprehensive settings management

### 🗺️ Location Services
- Real-time GPS tracking
- Route visualization
- Distance calculation
- Speed monitoring
- Location history

### 👤 User Profile Management
- Customizable user profiles
- Profile photo support
- Personal information storage

### 📊 Data Management
- Local database storage using Room
- Activity history tracking
- Detailed exercise metrics
- Unit conversion support (Metric/Imperial)

## Technical Implementation

### Architecture & Design Patterns
- MVVM (Model-View-ViewModel) architecture
- Repository pattern for data management
- LiveData for reactive UI updates
- Coroutines for asynchronous operations

### Key Technologies
- Kotlin
- Android Jetpack components
- Google Maps API
- Room Database
- ViewPager2
- Fragment-based navigation
- SharedPreferences
- Location Services

### Core Components
- Custom adapters for RecyclerView
- Fragment state management
- Service implementation for tracking
- Permission handling
- Image processing

## Development Highlights
- Activity recognition using FFT features and a Weka classifier
- Custom UI components and layouts
- Foreground tracking service with location updates and sensor cleanup
- Accelerometer fallback with gravity compensation
- Unit conversion utilities

## Future Enhancements
- Cloud synchronization
- Social sharing features
- Advanced analytics
- Workout planning
- Achievement system

## Technical Requirements
- JDK 17 and Android SDK 35 to build
- Gradle 8.10.2 through the included wrapper
- Android 9 (API 28) or later to run
- Google Play Services
- Location permissions
- Camera permissions (optional)
- Storage permissions

## Installation
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Add your Google Maps API key in the manifest
5. Build and run the application

From the repository root, `./gradlew assembleDebug` builds a debug APK after the SDK and Maps configuration are set up. Use a device with location services and an accelerometer to try automatic tracking.

## Current Limitations
- Calorie estimates use fixed weight and activity values.
- Tracking state is held in memory until the workout is saved; recovery after process termination is not implemented.
- The classifier is included, but its training data and accuracy evaluation are not. The current tests are template smoke tests, not coverage of tracking or recognition.

## Attribution
[`TrackingService.kt`](app/src/main/java/com/example/myruns/services/TrackingService.kt) connects location updates, 64-sample acceleration windows, FFT features, a Weka decision tree, and label smoothing. [`FFT.java`](app/src/main/java/com/example/myruns/services/FFT.java) comes from MEAPsoft/course demo code and retains its Columbia University, Mike Mandel, and GPL v2 notices.

## License
No project-level LICENSE file is currently included. Third-party notices remain in the relevant source files.
