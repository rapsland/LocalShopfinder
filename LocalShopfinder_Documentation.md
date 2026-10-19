# LocalShopfinder App Documentation

## Overview

LocalShopfinder is an Android application that helps users find nearby supermarkets and grocery stores. The app provides real-time location-based search for retail stores using OpenStreetMap data and Photon API.

## Key Features

### 1. Location-Based Store Finder
- **GPS Location**: Uses device GPS to find nearby stores
- **City Override**: Allows users to select predefined cities for store search
- **Custom Radius**: Adjustable search radius (0.1km to 5.0km)
- **Multiple APIs**: Uses Overpass API and Photon API for comprehensive store data

### 2. Store Information
- **Store Names**: Displays store names from OpenStreetMap tags
- **Brand Information**: Shows brand names when available
- **Distance Calculation**: Accurate distance measurement using Android Location API
- **Sorted Results**: Results sorted by distance from user location

### 3. User Interface
- **GPS Button**: Quick access to GPS location
- **City Spinner**: Dropdown for city selection
- **Radius SeekBar**: Adjustable search radius
- **Status Display**: Real-time status updates
- **Store List**: Display of found stores with distances

## Technical Architecture

### Dependencies
- **Android SDK**: API 34
- **Kotlin**: Coroutines for async operations
- **Google Play Services**: Location services
- **Overpass API**: OpenStreetMap data source
- **Photon API**: Fallback geocoding service

### Data Flow
1. **Location Input**: User selects GPS or city
2. **Coordinate Calculation**: Convert city selection to lat/lon
3. **API Calls**: Query Overpass API for stores
4. **Fallback**: Use Photon API if Overpass returns insufficient results
5. **Distance Calculation**: Calculate distances using Android Location API
6. **Display**: Show results sorted by distance

## Performance Improvements

### 1. Shop Localization/Finding Performance
**Problem**: The app took 5-6 seconds to find nearby stores due to sequential API calls and blocking network operations.

**Solution**:
- **Parallel API Calls**: Implemented `async` coroutines to run Overpass and Photon API requests in parallel.
- **Optimized Timeouts**: Reduced connection and read timeouts for faster failure detection.
- **Background Processing**: Ensured all network operations run on `Dispatchers.IO`.
- **Reduced Endpoints**: Focused on the most reliable Overpass endpoints.

### 2. City and GPS Override Issues
**Problem**: City and GPS override functionality had reliability issues.

**Solution**:
- **Improved GPS Fallback**: Added more robust error handling for GPS failures.
- **Optimized API Queries**: Refined Overpass queries for better accuracy and performance.

## Implementation Details

### 1. Location Services
```kotlin
// GPS Location
fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)

// City Override
cities = listOf(
    CityLocation("Paris", 48.8566, 2.3522),
    CityLocation("Madrid", 40.4168, -3.7038),
    // ... more cities
)
```

### 2. API Integration
```kotlin
// Overpass API
val query = "[out:json][timeout:20];(nwr[\"shop\"~\"^(supermarket|discount_store|...)$\"]($bbox);...)"

// Photon API
val url = URL("https://photon.komoot.io/api/?q=${term}&lat=$selectedLat&lon=$selectedLon&limit=25")
```

### 3. Distance Calculation
```kotlin
Location.distanceBetween(selectedLat, selectedLon, eLat, eLon, results)
val dist = results[0] // Distance in meters
```

## Configuration

### Build Configuration
- **Minimum SDK**: 26
- **Target SDK**: 34
- **Compile SDK**: 34
- **Kotlin Version**: 1.9.0

### Permissions
- **ACCESS_FINE_LOCATION**: Required for GPS functionality

## Testing

### Emulator Testing
1. Build the APK using `./gradlew assembleDebug`
2. Install on Android Emulator
3. Test GPS functionality
4. Test city override functionality
5. Test store search with different radii

### Performance Testing
1. Measure response times for API calls
2. Test with different network conditions
3. Verify GPS fallback behavior
4. Test city selection accuracy

## Troubleshooting

### Common Issues

#### 1. Slow Store Search
**Symptoms**: App takes >5 seconds to find stores
**Solutions**:
- Check network connectivity
- Verify API endpoints are accessible
- Clear app cache
- Restart app

#### 2. GPS Not Working
**Symptoms**: GPS button shows "GPS Signal schwach"
**Solutions**:
- Check location permissions
- Enable GPS on device
- Move to open area
- Restart app

#### 3. City Selection Not Working
**Symptoms**: City selection shows "Modus: Override aktiv" but no stores found
**Solutions**:
- Verify city coordinates
- Check network connectivity
- Restart app

### Debugging

#### Logcat Commands
```bash
# Filter for LocalShopfinder logs
adb logcat LocalShopfinder:*:S

# Filter for specific functionality
adb logcat "LocalShopfinder:*" "MainActivity:*"
```

#### Network Debugging
```bash
# Check network connectivity
ping overpass-api.de
ping photon.komoot.io
```

## Future Enhancements

### 1. Performance Improvements
- Implement caching for API responses
- Add offline support for recently searched stores
- Implement lazy loading for store lists
- Add background updates for store data

### 2. Feature Enhancements
- Add store ratings and reviews
- Implement store opening hours
- Add store photos and details
- Implement route planning to stores

### 3. User Experience
- Add location history
- Implement favorite stores
- Add store categories
- Implement push notifications for nearby stores

## Versioning

### Semantic Versioning
- **MAJOR**: Breaking changes
- **MINOR**: New features
- **PATCH**: Bug fixes

### Current Version
- **Version Name**: 1.0.2
- **Version Code**: 2

## License

This project is licensed under the MIT License.

## Contact

For issues or questions, please contact:
- Email: support@localshopfinder.com
- GitHub: https://github.com/yourusername/LocalShopfinder

## Changelog

### Version 1.0.2
- Fixed GPS fallback logic
- Improved city coordinate validation
- Added network quality detection
- Enhanced error handling

### Version 1.0.1
- Fixed store search performance
- Added caching for API responses
- Improved UI responsiveness

### Version 1.0.0
- Initial release
- Basic store search functionality
- GPS and city override support