# Database Bootstrapping

## Overview

The AquaForecast app includes a database bootstrapping feature that automatically populates the database with sample data for development and testing purposes. This feature is **only available in debug builds** and is configurable through the Gradle build file.

## Configuration

The bootstrap feature is controlled by a BuildConfig field in `app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        buildConfigField("Boolean", "ENABLE_DATABASE_BOOTSTRAP", "true")  // Enable bootstrapping
    }
}
```

### Enabling/Disabling Bootstrap

To **enable** bootstrap (default for debug builds):
```kotlin
buildConfigField("Boolean", "ENABLE_DATABASE_BOOTSTRAP", "true")
```

To **disable** bootstrap:
```kotlin
buildConfigField("Boolean", "ENABLE_DATABASE_BOOTSTRAP", "false")
```

After changing this value, rebuild the project:
```bash
./gradlew clean build
```

## How It Works

1. On app startup (in `MainApplication.onCreate()`), the bootstrapper checks if:
   - `BuildConfig.ENABLE_DATABASE_BOOTSTRAP` is `true`
   - The database has not been bootstrapped yet (tracked via DataStore)

2. If both conditions are met, sample data is inserted into the database

3. Once bootstrapping is complete, a flag is set in DataStore to prevent re-bootstrapping on subsequent app launches

## Sample Data Included

The bootstrapper creates the following sample data:

### Ponds (3 ponds)
- **Pond A - Tilapia**: 1,500 fish, started 60 days ago
- **Pond B - Catfish**: 1,200 fish, started 30 days ago
- **Pond C - Tilapia**: 2,000 fish, started 45 days ago

### Farm Data
- **30 days of historical data** for each pond
- Realistic water quality parameters with natural variation:
  - Temperature: ~28°C (±1°C)
  - pH: ~7.2 (±0.3)
  - Dissolved Oxygen: ~6.5 mg/L (±0.75 mg/L)
  - Ammonia: ~0.1 mg/L (±0.05 mg/L)
  - Nitrate: ~10 mg/L (±2.5 mg/L)
  - Turbidity: ~25 NTU (±5 NTU)
- Older data (15+ days) is marked as synced

### Predictions (4 predictions)
- Current predictions for all 3 ponds with harvest dates 30-60 days in the future
- One historical prediction for Pond A

### Feeding Schedules (6 schedules)
- **Pond A**: Morning (8:00 AM) and Evening (6:00 PM) feeds
- **Pond B**: Morning (7:30 AM) and Afternoon (4:00 PM) feeds
- **Pond C**: Morning (8:30 AM) and Evening (5:30 PM) feeds
  - Note: Evening feed for Pond C is inactive (for testing inactive schedules)

## Resetting the Database

To force the database to re-bootstrap with fresh sample data:

### Option 1: Clear App Data
1. Open Android Settings → Apps → AquaForecast
2. Tap "Storage"
3. Tap "Clear Data"
4. Restart the app

### Option 2: Uninstall and Reinstall
```bash
./gradlew uninstallDebug
./gradlew installDebug
```

### Option 3: Programmatically (Developer)
You can expose the `clearBootstrapFlag()` method in development builds:

```kotlin
// In a debug-only settings screen or developer menu
val bootstrapper: DatabaseBootstrapper by inject()
lifecycleScope.launch {
    bootstrapper.clearBootstrapFlag()
    // Then manually delete database or clear app data
}
```

## Production Builds

The bootstrap feature is **automatically disabled** in release builds. The `ENABLE_DATABASE_BOOTSTRAP` BuildConfig field is only set for debug builds, ensuring no sample data appears in production.

## Implementation Details

### Files Involved

- **`app/build.gradle.kts`**: Gradle configuration with `ENABLE_DATABASE_BOOTSTRAP` flag
- **`data/local/DatabaseBootstrapper.kt`**: Core bootstrapping logic
- **`di/bootstrapModule.kt`**: Koin dependency injection module
- **`MainApplication.kt`**: Initialization on app startup

### Architecture

The bootstrapper:
- Uses **Koin** for dependency injection
- Stores bootstrap status in **DataStore** (persistent preferences)
- Runs asynchronously in a **CoroutineScope** to avoid blocking app startup
- Uses existing DAOs for data insertion (respects Room annotations and constraints)

### Adding More Sample Data

To add more sample data, edit `DatabaseBootstrapper.kt`:

1. Add new data to existing `insertSample*()` methods, or
2. Create new private suspend functions for additional entities
3. Call them from the `bootstrap()` method

Example:
```kotlin
private suspend fun insertSampleUsers() {
    val users = listOf(
        UserEntity(name = "John Farmer", email = "john@example.com"),
        UserEntity(name = "Jane Manager", email = "jane@example.com")
    )
    users.forEach { user -> userDao.insert(user) }
}

suspend fun bootstrap() {
    // ... existing code ...
    insertSamplePonds()
    insertSampleFarmData()
    insertSamplePredictions()
    insertSampleFeedingSchedules()
    insertSampleUsers()  // Add new sample data
    // ... existing code ...
}
```

## Troubleshooting

### Bootstrap Not Running
1. Verify `ENABLE_DATABASE_BOOTSTRAP` is `true` in `app/build.gradle.kts`
2. Rebuild the project: `./gradlew clean build`
3. Check Logcat for any errors during bootstrapping

### Sample Data Not Appearing
1. Check if the bootstrap flag is already set (clear app data to reset)
2. Verify DAOs have correct `@Insert` methods
3. Check for foreign key constraint violations in Logcat

### Bootstrap Running Every Time
1. Ensure DataStore is properly configured in `dataStoreModule`
2. Check that the bootstrap flag is being saved correctly
3. Verify app has storage permissions

## Best Practices

1. **Keep bootstrap data realistic**: Use values that match real-world scenarios
2. **Include edge cases**: Add inactive schedules, old predictions, etc.
3. **Performance**: Avoid inserting thousands of records; 30 days of data is sufficient
4. **Foreign keys**: Ensure related data respects database constraints
5. **Dates**: Use relative dates (e.g., "30 days ago") rather than absolute timestamps
