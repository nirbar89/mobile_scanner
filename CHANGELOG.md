## 3.5.2
Improvements:
* Updated to `play-services-mlkit-barcode-scanning` version 18.3.0

Bugs fixed:
* Fixed the `updateScanWindow()` function not completing on Android and MacOS. (thanks @navaronbracke !)
* Fixed some camera access issues, when the camera could have been null on Android. (thanks @navaronbracke !)
* Fixed a crash on Android when there is no camera. (thanks @navaronbracke !)
* Fixed a bug with the `noDuplicates` detection speed. (thanks @pgeof !)
* Fixed a synchronization issue for the torch state. (thanks @navaronbracke !)

## 3.5.1
Improvements:
* The `type` of an `Address` is now non-null.
* The `type` of an `Email` is now non-null.
* The `phoneNumber` of an `SMS` is now non-null.
* The `latitude` and `longitude` of a `GeoPoint` are now non-null.
* The `phones` and `urls` of `ContactInfo` are now non-null.
* The `url` of a `UrlBookmark` is now non-null.
* The `type` of `Phone` is now non-null.
* The `width` and `height` of `BarcodeCapture` are now non-null.
* The `BarcodeCapture` class now exposes a `size`.
* The list of `corners` of a `Barcode` is now non-null.

Bugs fixed:
* Fixed the default values for the `format` and `type` arguments of the Barcode constructor.
  These now use `BarcodeFormat.unknown` and `BarcodeType.unknown`, rather than `BarcodeFormat.ean13` and `BarcodeType.text`.
  (thanks @navaronbracke !)
* Fixed messages not being sent on the main thread for Android, iOS and MacOS. (thanks @navaronbracke !)

## 3.5.0
New Features:
* Added the option to switch between bundled and unbundled MLKit for Android. (thanks @woolfred !)
* Added the option to specify the camera resolution for Android. (thanks @EArminjon !)
* Added a sample with a scanner overlay. (thanks @Spyy004 !)

Bugs fixed:
* Fixed the scan window calculation taking into account the widget coordinates, instead of the screen coordinates. (thanks @jlin5 !)
* Fixed the scan window calculation returning wrong results. (thanks @MBulli !)
* Fixed the BarcodeCapture format on MacOS. (thanks @ryanduffyne !)
* Fixed the timeout for scanning on MacOS. (thanks @ryanduffyne !)
* Fixed Android builds failing by downgrading from Kotlin 1.9.10 to 1.7.22. (thanks @vbuberen !)
* Fixed images on iOS being rotated, resulting in bad detection rates. (thanks @EArminjon !)
* Fixed scan timeout not working on iOS. (thanks @navaronbracke !)
* Fixed a crash on iOS when the device is nil. (thanks @navaronbracke !)
* Fixed a case of an unhandled exception when starting the scanner. (thanks @navaronbracke !)

Improvements:
* Improved MacOS memory footprint by using a background queue. (thanks @ryanduffyne !)

## 3.4.1
* Changed MediaQuery.sizeOf(context) to of(context).size for compatibility with older Flutter versions.

## 3.4.0
New Features:
* This PR adds an option to add an overlay to the scanner which is only visible when the scanner has started. (thanks @svenopdehipt !)

Improvements:
* fix a bug in the static interop binding of PhotoCapabilities (thanks @navaronbracke !)
* [Web] add the corners from the ZXing result to the barcode on web (thanks @navaronbracke !)
* update the example app web entrypoint to the latest template by running flutter create . --platforms=web (thanks @navaronbracke !)
* add better handling for the case where scanning barcodes is unsupported (for example a desktop running the browser sample) (thanks @navaronbracke !)
* [Web] fix the permission denied handling on the web, by using the NotAllowedError error message as defined by MDN (thanks @navaronbracke !)
* add app bars with back buttons to the example app (so that you can go back easily) (thanks @navaronbracke !)

* [iOS] Implements a fix from issue iOS After first QR Code Scan, When Scanning again, First Image stays on Camera buffer (thanks @FlockiiX !)

* By dynamically adjusting the positioning and scaling of the scan window relative to the texture, the package ensures optimal coverage and alignment for scanning targets. (thanks @sdkysfzai !)
* In the original package, If there are multiple barcode/qrcodes in the screen, the scan would randomly pick up any barcode/qrcode that shows in the screen, This upgrade fixes it and picks on the qrcode/barcode that is in the center of the camera. (thanks @sdkysfzai !)
* In the original package if you changed the camera size, it would still pick scans even if the barcode are not shown in the screen, This issue is also fixed in the upgraded packaged. (thanks @sdkysfzai !)

* [iOS] This removes a threading warning (and potentially jank). (thanks @ened !)
* [Android] fix(ScanImage): fix android image result is not correct format and orientation (thanks @phanbaohuy96 !)

* [iOS] Respect detectionTimeout on iOS devices, instead of arbitrarily waiting 10 frames (thanks @jorgenpt !)
* [iOS] Don't start a second scan until the first one is done, to keep memory usage more fixed if the device is slow (thanks @jorgenpt !)
* [Android] This PR ensure that the camera is not stopped in the callback. (thanks @g123k !)
* [macOS] Fix some macOS build errors (thanks @svenopdehipt !)

* [Android] Fixed an issue which caused the App Lifecycle States to not work correctly on Android. (thanks @androi7 !)

## 3.3.0
Bugs fixed:
* Fixed bug where onDetect method was being called multiple times
* [Android] Fix Gradle 8 compatibility by adding the `namespace` attribute to the build.gradle.

Improvements:
* [Android] Upgraded camera2 dependency
* Added zoomScale value notifier in MobileScannerController for the application to know the zoom scale value set actually.
  The value is notified from the native SDK(CameraX/AVFoundation).
* Added resetZoomScale() in MobileScannerController to reset zoom ratio with 1x.
  Both Android and iOS, if the device have ultra-wide camera, calling setZoomScale with small value causes to use ultra-wide camera and may be diffcult to detect barcodes.
  resetZoomScale() is useful to use standard camera with zoom 1x.
  setZoomScale() with the specific value can realize same effect, but added resetZoomScale for avoiding floating point errors.
  The application can know what zoom scale value is selected actually by subscribing zoomScale above after calling resetZoomScale.
* [iOS] Call resetZoomScale while starting scan.
  Android camera is initialized with a zoom of 1x, whereas iOS is initialized with the minimum zoom value, which causes to select the ultra-wide camera unintentionally ([iOS] Impossible to focus and scan the QR code due to picking the wide back camera #554).
  Fixed this issue by calling resetZoomScale
* [iOS] Remove zoom animation with ramp function to match Android behavior.

## 3.2.0
Improvements:
* [iOS] Updated GoogleMLKit/BarcodeScanning to 4.0.0 
* [Android] Updated com.google.mlkit:barcode-scanning from 17.0.3 to 17.1.0

Bugs fixed:
* Fixed onDetect not working with analyzeImage when autoStart is false in MobileScannerController
* [iOS] Explicit returned type for compactMap

## 3.1.1
Bugs fixed:
* [iOS] Fixed a bug that caused a crash when switching from camera.

## 3.1.0
Improvements:
* [iOS] No longer automatically focus on faces.
* [iOS] Fixed build error.
* [Web] Waiting for js libs to load.
* Do not returnImage if not specified.
* Added raw data in barcode object.
* Fixed several bugs.

## 3.0.0
This big release contains all improvements from the beta releases.
In addition to that, this release contains:

Improvements:
* Fixed an issue in which the scanner would freeze if two scanner widgets where placed in a page view,
and the paged was swiped. An example has been added in the example app.
You need to set startDelay: true if used in a page view.
* [Web] Automatically inject js libraries.
* [macOS] The minimum build version is now macOS 10.14 in according to the latest Flutter version.
* [Android] Fixed an issue in which the scanWindow would remain even after disposing the scanner.
* Updated dependencies.

## 3.0.0-beta.4
Fixes:
* Fixes a permission bug on Android where denying the permission would cause an infinite loop of permission requests.
* Updates the example app to handle permission errors with the new builder parameter.
  Now it no longer throws uncaught exceptions when the permission is denied.
* Updated several dependencies

Features:
* Added a new `errorBuilder` to the `MobileScanner` widget that can be used to customize the error state of the preview. (Thanks @navaronbracke !) 

## 3.0.0-beta.3
Deprecated:
* The `onStart` method has been renamed to `onScannerStarted`.
* The `onPermissionSet` argument of the `MobileScannerController` is now deprecated.

Breaking changes:
* `MobileScannerException` now uses an `errorCode` instead of a `message`.
* `MobileScannerException` now contains additional details from the original error.
* Refactored `MobileScannerController.start()` to throw `MobileScannerException`s
  with consistent error codes, rather than string messages.
  To handle permission errors, consider catching the result of `MobileScannerController.start()`.
* The `autoResume` attribute has been removed from the `MobileScanner` widget.
  The controller already automatically resumes, so it had no effect.
* Removed `MobileScannerCallback` and `MobileScannerArgumentsCallback` typedef.
* [Web] Replaced `jsqr` library with `zxing-js` for full barcode support.

Improvements:
* Toggling the device torch now does nothing if the device has no torch, rather than throwing an error.
* Removed `called stop while already stopped` messages.

Features:
* You can now provide a `scanWindow` to the `MobileScanner()` widget.
* You can now draw an overlay over the scanned barcode. See the barcode scanner window in the example app for more information.
* Added a new `placeholderBuilder` function to the `MobileScanner` widget to customize the preview placeholder.
* Added `autoStart` parameter to MobileScannerController(). If set to false, controller won't start automatically.
* Added `hasTorch` function on MobileScannerController(). After starting the controller, you can check if the device has a torch.
* [iOS] Support `torchEnabled` parameter from MobileScannerController() on iOS
* [Web] Added ability to use custom barcode scanning js libraries 
  by extending `WebBarcodeReaderBase` class and changing `barCodeReader` property in `MobileScannerWebPlugin`

Fixes:
* Fixes the missing gradle setup for the Android project, which prevented gradle sync from working.
* Fixes `MobileScannerController.stop()` throwing when already stopped.
* Fixes `MobileScannerController.toggleTorch()` throwing if the device has no torch.
  Now it does nothing if the torch is not available.
* Fixes a memory leak where the `MobileScanner` would keep listening to the barcode events.
* Fixes the `MobileScanner` preview depending on all attributes of `MediaQueryData`.
  Now it only depends on its layout constraints.
* Fixed a potential crash when the scanner is restarted due to the app being resumed.
* [iOS] Fix crash when changing torch state
  
## 3.0.0-beta.2
Breaking changes:
* The arguments parameter of onDetect is removed. The data is now returned by the onStart callback
in the MobileScanner widget.
* onDetect now returns the object BarcodeCapture, which contains a List of barcodes and, if enabled, an image.
* allowDuplicates is removed and replaced by MobileScannerSpeed enum.
* onPermissionSet in MobileScanner widget is deprecated and will be removed. Use the onPermissionSet
onPermissionSet callback in MobileScannerController instead.
* [iOS] The minimum deployment target is now 11.0 or higher.

Features:
* The returnImage is working for both iOS and Android. You can enable it in the MobileScannerController.
The image will be returned in the BarcodeCapture object provided by onDetect.
* You can now control the DetectionSpeed, as well as the timeout of the DetectionSpeed. For more
info see the DetectionSpeed documentation. This replaces the allowDuplicates function.

Other improvements:
* Both the [iOS] and [Android] codebases have been refactored completely.
* [iOS] Updated POD dependencies

## 3.0.0-beta.1
Breaking changes:
* [Android] SDK updated to SDK 33.

Features:
* [Web] Add binaryData for raw value.
* [iOS] Captures the last scanned barcode with Barcode.image.
* [iOS] Add support for multiple formats on iOS with BarcodeScannerOptions.
* Add displayValue which returns barcode value in a user-friendly format.
* Add autoResume option to MobileScannerController which automatically resumes the camera when the application is resumed

Other changes:
* [Android] Revert camera2 dependency to stable release
* [iOS] Update barcode scanning library to latest version
* Several minor code improvements

## 2.0.0
Breaking changes:
This version is only compatible with flutter 3.0.0 and later.

## 1.1.2-play-services
This version uses the MLKit play-services model on Android in order to save space.
With the example app, this version reduces the release version from 14.9MB to 7MB.
More information: https://developers.google.com/ml-kit/vision/barcode-scanning/android

## 1.1.2
This version is the last version that will run on Flutter 2.x

Bugfixes:
* Changed onDetect to be mandatory.

## 1.1.1-play-services
This version uses the MLKit play-services model on Android in order to save space.
With the example app, this version reduces the release version from 14.9MB to 7MB.
More information: https://developers.google.com/ml-kit/vision/barcode-scanning/android

## 1.1.1
Bugfixes:
* Add null checks for Android.
* Update camera dependency for Android.
* Fix return type for analyzeImage.
* Add fixes for Flutter 3.

## 1.1.0
Bugfixes:
* Fix for 'stream already listened to' exception.
* Fix building on Android with latest Flutter version.
* Add several WEB improvements.
* Upgraded several dependencies.

## 1.0.0
BREAKING CHANGES:
This version adds a new allowDuplicates option which now defaults to FALSE. this means that it will only call onDetect once after a scan.
If you still want duplicates, you can set allowDuplicates to true.
This also means that you don't have to check for duplicates yourself anymore.

New features:
* We now have web support! Keep in mind that only QR codes are supported right now.

Bugfixes:
* Fixed hot reload not working.
* Fixed Navigator.of(context).pop() not working in the example app due to duplicate MaterialApp declaration.
* Fixed iOS MLKit version not resolving the latest version.
* Updated all dependencies

## 0.2.0
You can provide a path to controller.analyzeImage(path) in order to scan a local photo from the gallery!
Check out the example app to see how you can use the image_picker plugin to retrieve a photo from
the gallery. Please keep in mind that this feature is only supported on Android and iOS.

Another feature that has been added is a format selector!
Just keep in mind that iOS for now only supports 1 selected barcode.

## 0.1.3
* Fixed crash after asking permission. [#29](https://github.com/juliansteenbakker/mobile_scanner/issues/29)
* Upgraded cameraX from 1.1.0-beta01 to 1.1.0-beta02

## 0.1.2
* MobileScannerArguments is now exported. [#7](https://github.com/juliansteenbakker/mobile_scanner/issues/7)

Bugfixes:
* Fixed application crashing when stop() or start() is called multiple times. [#5](https://github.com/juliansteenbakker/mobile_scanner/issues/5)
* Fixes controller not being disposed correctly. [#23](https://github.com/juliansteenbakker/mobile_scanner/issues/23)
* Catch error when no camera is found. [#19](https://github.com/juliansteenbakker/mobile_scanner/issues/19)

## 0.1.1
mobile_scanner is now compatible with sdk >= 2.12 and flutter >= 2.2.0

## 0.1.0
We now have MacOS support using Apple's Vision framework!
Keep in mind that for now, only the raw value of the barcode object is supported.

Bugfixes:
* Fixed a crash when dispose is called in a overridden method. [#5](https://github.com/juliansteenbakker/mobile_scanner/issues/5) 

## 0.0.3
* Added some API docs and README
* Updated the example app

## 0.0.2
Fixed on iOS:
* You can now set the torch
* You can select the camera you want to use

## 0.0.1
Initial release!
Things working on Android:
* Scanning barcodes using the latest version of MLKit and CameraX!
* Switching camera's
* Toggling of the torch (flash)

Things working on iOS:
* Scanning barcodes using the latest version of MLKit and AVFoundation!
