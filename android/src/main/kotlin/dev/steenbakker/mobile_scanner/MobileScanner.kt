package dev.steenbakker.mobile_scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.steenbakker.mobile_scanner.objects.DetectionSpeed
import dev.steenbakker.mobile_scanner.objects.MobileScannerStartParameters
import dev.steenbakker.mobile_scanner.utils.YuvToRgbConverter
import io.flutter.view.TextureRegistry
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt


class MobileScanner(
    private val activity: Activity,
    private val textureRegistry: TextureRegistry,
    private val mobileScannerCallback: MobileScannerCallback,
    private val mobileScannerErrorCallback: MobileScannerErrorCallback
) {

    /// Internal variables
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null
    private var scanner = BarcodeScanning.getClient()
    private var lastScanned: List<String?>? = null
    private var scannerTimeout = false
    private var displayListener: DisplayManager.DisplayListener? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    private var outputDirectory: File? = null
    private var recording: Recording? = null

    /// Configurable variables
    var scanWindow: List<Float>? = null
    private var detectionSpeed: DetectionSpeed = DetectionSpeed.NO_DUPLICATES
    private var detectionTimeout: Long = 250
    private var returnImage = false
    private var capturingVideo = false
    private var capturingImage = false


    /**
     * callback for the camera. Every frame is passed through this function.
     */
    @ExperimentalGetImage
    val captureOutput = ImageAnalysis.Analyzer { imageProxy -> // YUV_420_888 format
        val mediaImage = imageProxy.image ?: return@Analyzer
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        if (detectionSpeed == DetectionSpeed.NORMAL && scannerTimeout) {
            imageProxy.close()
            return@Analyzer
        } else if (detectionSpeed == DetectionSpeed.NORMAL) {
            scannerTimeout = true
        }

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (detectionSpeed == DetectionSpeed.NO_DUPLICATES) {
                    val newScannedBarcodes =
                        barcodes.mapNotNull { barcode -> barcode.rawValue }.sorted()
                    if (newScannedBarcodes == lastScanned) {
                        // New scanned is duplicate, returning
                        return@addOnSuccessListener
                    }
                    if (newScannedBarcodes.isNotEmpty()) lastScanned = newScannedBarcodes
                }

                val barcodeMap: MutableList<Map<String, Any?>> = mutableListOf()

                for (barcode in barcodes) {
                    if (scanWindow != null) {
                        val match = isBarcodeInScanWindow(scanWindow!!, barcode, imageProxy)
                        if (!match) {
                            continue
                        } else {
                            barcodeMap.add(barcode.data)
                        }
                    } else {
                        barcodeMap.add(barcode.data)
                    }
                }


                if (barcodeMap.isNotEmpty()) {
                    if (returnImage && barcodeMap.size == 1) {
                        val bitmap = Bitmap.createBitmap(
                            mediaImage.width,
                            mediaImage.height,
                            Bitmap.Config.ARGB_8888
                        )

                        val imageFormat = YuvToRgbConverter(activity.applicationContext)

                        imageFormat.yuvToRgb(mediaImage, bitmap)

                        val bmResult = rotateBitmap(
                            bitmap,
                            camera?.cameraInfo?.sensorRotationDegrees?.toFloat() ?: 90f
                        )
                        val framePath = saveBarcodeBitmap(bmResult)

                        mobileScannerCallback(
                            barcodeMap,
                            framePath,
                            bmResult.width,
                            bmResult.height
                        )
                        bitmap.recycle()
                        bmResult.recycle()
                        System.gc()
                    } else {

                        mobileScannerCallback(
                            barcodeMap,
                            null,
                            null,
                            null
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                mobileScannerErrorCallback(
                    e.localizedMessage ?: e.toString()
                )
            }
            .addOnCompleteListener { imageProxy.close() }

        if (detectionSpeed == DetectionSpeed.NORMAL) {
            // Set timer and continue
            Handler(Looper.getMainLooper()).postDelayed({
                scannerTimeout = false
            }, detectionTimeout)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    // scales the scanWindow to the provided inputImage and checks if that scaled
    // scanWindow contains the barcode
    private fun isBarcodeInScanWindow(
        scanWindow: List<Float>,
        barcode: Barcode,
        inputImage: ImageProxy
    ): Boolean {
        val barcodeBoundingBox = barcode.boundingBox ?: return false

        val imageWidth = inputImage.height
        val imageHeight = inputImage.width

        val left = (scanWindow[0] * imageWidth).roundToInt()
        val top = (scanWindow[1] * imageHeight).roundToInt()
        val right = (scanWindow[2] * imageWidth).roundToInt()
        val bottom = (scanWindow[3] * imageHeight).roundToInt()

        val scaledScanWindow = Rect(left, top, right, bottom)
        return scaledScanWindow.contains(barcodeBoundingBox)
    }

    // Return the best resolution for the actual device orientation.
    //
    // By default the resolution is 480x640, which is too low for ML Kit.
    // If the given resolution is not supported by the display,
    // the closest available resolution is used.
    //
    // The resolution should be adjusted for the display rotation, to preserve the aspect ratio.
    @Suppress("deprecation")
    private fun getResolution(cameraResolution: Size): Size {
        val rotation = if (Build.VERSION.SDK_INT >= 30) {
            activity.display!!.rotation
        } else {
            val windowManager =
                activity.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            windowManager.defaultDisplay.rotation
        }

        val widthMaxRes = cameraResolution.width
        val heightMaxRes = cameraResolution.height

        val targetResolution =
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                Size(widthMaxRes, heightMaxRes) // Portrait mode
            } else {
                Size(heightMaxRes, widthMaxRes) // Landscape mode
            }
        return targetResolution
    }

    /**
     * Start barcode scanning by initializing the camera and barcode scanner.
     */
    @ExperimentalGetImage
    fun start(
        barcodeScannerOptions: BarcodeScannerOptions?,
        returnImage: Boolean,
        cameraPosition: CameraSelector,
        torch: Boolean,
        detectionSpeed: DetectionSpeed,
        torchStateCallback: TorchStateCallback,
        zoomScaleStateCallback: ZoomScaleStateCallback,
        mobileScannerStartedCallback: MobileScannerStartedCallback,
        mobileScannerErrorCallback: (exception: Exception) -> Unit,
        detectionTimeout: Long,
        cameraResolution: Size?
    ) {
        this.detectionSpeed = detectionSpeed
        this.detectionTimeout = detectionTimeout
        this.returnImage = returnImage

        if (camera?.cameraInfo != null && preview != null && textureEntry != null) {
            mobileScannerErrorCallback(AlreadyStarted())

            return
        }

        lastScanned = null
        scanner = if (barcodeScannerOptions != null) {
            BarcodeScanning.getClient(barcodeScannerOptions)
        } else {
            BarcodeScanning.getClient()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        val executor = ContextCompat.getMainExecutor(activity)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            if (cameraProvider == null) {
                mobileScannerErrorCallback(CameraError())

                return@addListener
            }

            cameraProvider?.unbindAll()

            textureEntry = textureRegistry.createSurfaceTexture()

            // Preview
            val surfaceProvider = Preview.SurfaceProvider { request ->
                if (isStopped()) {
                    return@SurfaceProvider
                }

                val texture = textureEntry!!.surfaceTexture()
                texture.setDefaultBufferSize(
                    request.resolution.width,
                    request.resolution.height
                )

                val surface = Surface(texture)
                request.provideSurface(surface, executor) { }
            }

            // Build the preview to be shown on the Flutter texture
            val previewBuilder = Preview.Builder()
            preview = previewBuilder.build().apply { setSurfaceProvider(surfaceProvider) }

            // Build the analyzer to be passed on to MLKit
            val analysisBuilder = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            val displayManager =
                activity.applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

            if (cameraResolution != null) {
                // TODO: migrate to ResolutionSelector with ResolutionStrategy when upgrading to camera 1.3.0
                // Override initial resolution
                analysisBuilder.setTargetResolution(getResolution(cameraResolution))

                if (displayListener == null) {
                    displayListener = object : DisplayManager.DisplayListener {
                        override fun onDisplayAdded(displayId: Int) {}

                        override fun onDisplayRemoved(displayId: Int) {}

                        override fun onDisplayChanged(displayId: Int) {
                            analysisBuilder.setTargetResolution(getResolution(cameraResolution))
                        }
                    }

                    displayManager.registerDisplayListener(
                        displayListener, null,
                    )
                }
            }

            analysis = analysisBuilder.build().apply { setAnalyzer(executor, captureOutput) }

            try {
                camera = cameraProvider?.bindToLifecycle(
                    activity as LifecycleOwner,
                    cameraPosition,
                    preview,
                    analysis
                )
            } catch (exception: Exception) {
                mobileScannerErrorCallback(NoCamera())

                return@addListener
            }

            camera?.let {
                // Register the torch listener
                it.cameraInfo.torchState.observe(activity as LifecycleOwner) { state ->
                    // TorchState.OFF = 0; TorchState.ON = 1
                    torchStateCallback(state)
                }

                // Register the zoom scale listener
                it.cameraInfo.zoomState.observe(activity) { state ->
                    zoomScaleStateCallback(state.linearZoom.toDouble())
                }

                // Enable torch if provided
                if (it.cameraInfo.hasFlashUnit()) {
                    it.cameraControl.enableTorch(torch)
                }
            }

            val resolution = analysis!!.resolutionInfo!!.resolution
            val width = resolution.width.toDouble()
            val height = resolution.height.toDouble()
            val portrait = (camera?.cameraInfo?.sensorRotationDegrees ?: 0) % 180 == 0

            mobileScannerStartedCallback(
                MobileScannerStartParameters(
                    if (portrait) width else height,
                    if (portrait) height else width,
                    camera?.cameraInfo?.hasFlashUnit() ?: false,
                    textureEntry!!.id()
                )
            )
        }, executor)

    }

    /**
     * Stop barcode scanning.
     */
    fun stop() {
        if (isStopped()) {
            throw AlreadyStopped()
        }

        if (displayListener != null) {
            val displayManager =
                activity.applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

            displayManager.unregisterDisplayListener(displayListener)
            displayListener = null
        }

        val owner = activity as LifecycleOwner
        camera?.cameraInfo?.torchState?.removeObservers(owner)
        camera?.cameraInfo?.cameraState?.removeObservers(owner)
        camera?.cameraInfo?.zoomState?.removeObservers(owner)
        cameraProvider?.unbindAll()
        textureEntry?.release()
        scanner.close()

        camera = null
        preview = null
        textureEntry = null
        cameraProvider = null
        outputDirectory = null
        imageCapture = null
        videoCapture = null
        recording = null
        capturingVideo = false
//        capturingImage = false
//        barcodeDetected = false
    }

    private fun isStopped() = camera == null && preview == null

    /**
     * Toggles the flash light on or off.
     */
    fun toggleTorch(enableTorch: Boolean) {
        if (camera == null) {
            return
        }

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(enableTorch)
        }
    }

    /**
     * Analyze a single image.
     */
    fun analyzeImage(image: Uri, analyzerCallback: AnalyzerCallback) {
        val inputImage = InputImage.fromFilePath(activity, image)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val barcodeMap = barcodes.map { barcode -> barcode.data }

                if (barcodeMap.isNotEmpty()) {
                    analyzerCallback(barcodeMap)
                } else {
                    analyzerCallback(null)
                }
            }
            .addOnFailureListener { e ->
                mobileScannerErrorCallback(
                    e.localizedMessage ?: e.toString()
                )
            }
    }

    /**
     * Set the zoom rate of the camera.
     */
    fun setScale(scale: Double) {
        if (scale > 1.0 || scale < 0) throw ZoomNotInRange()
        if (camera == null) throw ZoomWhenStopped()
        camera?.cameraControl?.setLinearZoom(scale.toFloat())
    }

    /**
     * Reset the zoom rate of the camera.
     */
    fun resetScale() {
        if (camera == null) throw ZoomWhenStopped()
        camera?.cameraControl?.setZoomRatio(1f)
    }

    private fun saveBarcodeBitmap(bitmap: Bitmap): String? {
        val imageFile: File?
        try {
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())

            imageFile = File.createTempFile(name, ".jpg", outputDirectory)
        } catch (e: Exception) {
            mobileScannerErrorCallback(
                e.localizedMessage ?: e.toString()
            )
            return null
        }
        val out = FileOutputStream(imageFile)
        try {
            val scaledBitmap = resizeBitmap(bitmap, 320, 240)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            scaledBitmap.recycle()
            out.flush()
            out.close()
        } catch (e: IOException) {
            mobileScannerErrorCallback(
                e.localizedMessage ?: e.toString()
            )
        }
        return imageFile.absolutePath
    }


    private fun resizeBitmap(image: Bitmap, maxHeight: Int, maxWidth: Int): Bitmap {

        if (maxHeight > 0 && maxWidth > 0) {

            val sourceWidth: Int = image.width
            val sourceHeight: Int = image.height

            var targetWidth = maxWidth
            var targetHeight = maxHeight

            val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
            val targetRatio = maxWidth.toFloat() / maxHeight.toFloat()

            if (targetRatio > sourceRatio) {
                targetWidth = (maxHeight.toFloat() * sourceRatio).toInt()
            } else {
                targetHeight = (maxWidth.toFloat() / sourceRatio).toInt()
            }

            return Bitmap.createScaledBitmap(
                image, targetWidth, targetHeight, true
            )

        } else {
            throw RuntimeException()
        }
    }



    fun initVideoCamera (
        fileCallback: FileCallback) {
        cameraProvider!!.unbindAll()


        val selector = QualitySelector
            .from(
                Quality.LOWEST,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )

        val recorder = Recorder.Builder()
            .setQualitySelector(selector)
            .build()

        videoCapture = VideoCapture.withOutput(recorder)
        outputDirectory = activity.filesDir

        camera = cameraProvider!!.bindToLifecycle(
            activity as LifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,//TODO: only if panic mode
            videoCapture,
        )


        camera!!.cameraInfo.cameraState.observe(activity) { state ->
            Log.d("EZGUARD_CAMERA", state.toString())
            if (state.type == CameraState.Type.OPEN && !capturingVideo) {
                recordVideo(
                    fileCallback,
                    camera!!.cameraInfo.sensorRotationDegrees
                )
            }

        }
    }

    fun recordVideo(fileCallback: FileCallback, rotationDegrees: Int) {

        if (capturingVideo) return
        val handler = Handler(Looper.getMainLooper())

        val recordingListener = Consumer<VideoRecordEvent> { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    handler.postDelayed({ recording?.stop() }, 5000)
                }

                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        fileCallback(event.outputResults.outputUri.path, 1, rotationDegrees)
                    } else {
                        when (event.error) {
                            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED -> {
                                handler.removeCallbacksAndMessages(null)
                                fileCallback(
                                    event.outputResults.outputUri.path,
                                    1,
                                    rotationDegrees
                                )
                            }

                            else -> {
                                fileCallback(null, null, null)
                            }
                        }

                    }
                    capturingVideo = false
                }
            }
        }
        // create and start a new recording session
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val outputFile = File.createTempFile(name, ".mp4", outputDirectory)
        val fileOutputOptions = FileOutputOptions.Builder(outputFile).setFileSizeLimit(2621440)
            .build()//TODO: from server
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            fileCallback(null, null, null)
            return
        }
        recording = videoCapture?.output
            ?.prepareRecording(activity, fileOutputOptions)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(activity), recordingListener)
        capturingVideo = true
    }

    fun captureScanVerificationPhoto(
        cameraPosition: CameraSelector,
        scanImageWidth: Int,
        fileCallback: FileCallback) {

        cameraProvider!!.unbindAll()
        val targetResolution = Size(scanImageWidth, scanImageWidth * 4 / 3)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetResolution(getResolution(targetResolution))
            .build()
        outputDirectory = activity.filesDir

        camera = cameraProvider!!.bindToLifecycle(
            activity as LifecycleOwner,
            cameraPosition,
            preview,
            imageCapture,
        )

        camera!!.cameraInfo.cameraState.observe(activity) { state ->
            if (state.type == CameraState.Type.OPEN && !capturingImage) {
                capturingImage = true
                captureImage(
                    fileCallback,
                    camera!!.cameraInfo.sensorRotationDegrees
                )
            }
        }
    }


    private fun captureImage(fileCallback: FileCallback, rotationDegrees: Int) {
        val capture = imageCapture
        try {
            if (capture == null) {
                fileCallback(null, null, null)
                capturingImage = false
                return
            }

            val photoFile = File(
                outputDirectory,
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS", Locale.getDefault())
                    .format(
                        System
                            .currentTimeMillis()
                    ) + ".jpg"
            )


            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(photoFile).build()


            capture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(activity),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        fileCallback(savedUri.path, 0, rotationDegrees)
                        capturingImage = false
                    }

                    override fun onError(exception: ImageCaptureException) {
                        fileCallback(null, null, null)
                        capturingImage = false
                    }
                }
            )
        } catch (e: Exception) {
            capturingImage = false
            mobileScannerErrorCallback(
                e.localizedMessage ?: e.toString()
            )
        }
    }

}
