package org.phenoapps.intercross.ui.barcode

import android.Manifest
import android.graphics.Rect
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AnnotationOptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.Event
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.ui.app.TopBarState
import org.phenoapps.intercross.ui.components.CrossListItem
import org.phenoapps.intercross.ui.theme.AppTheme
import java.util.concurrent.Executors

enum class SequenceScanSlot {
    Female,
    Male,
    Cross,
}

/**
 * Represents a barcode detected in the camera frame, with its bounding box
 * in image-analysis coordinates and the decoded text value.
 */
data class DetectedBarcode(
    val boundingBox: Rect,
    val rawValue: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int,
)

@Composable
internal fun BarcodeScannerScreen(
    mode: Int,
    events: List<Event>,
    parents: List<Parent>,
    mlKitFormats: Int,
    torchEnabled: Boolean,
    onSingleScan: (String) -> Unit,
    onSequenceScan: (List<String>) -> Unit = {},
    onSequenceCode: (String) -> Unit = {},
    sequenceFemaleScanned: Boolean = false,
    sequenceMaleScanned: Boolean = false,
    sequenceCrossScanned: Boolean = false,
    sequenceFemaleCode: String = "",
    sequenceMaleCode: String = "",
    sequenceCrossIdCode: String = "",
    sequenceNeedsMale: Boolean = true,
    sequenceNeedsCrossId: Boolean = true,
    sequenceNextSlot: SequenceScanSlot? = null,
    sequenceResetNonce: Int = 0,
    sequenceSaveCount: Int = 0,
    cooldownActive: Boolean = false,
    cooldownProgress: Float = 0f,
    onOpenEvent: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    topBarState: TopBarState? = null,
    initialChildDialogEvents: List<Event>? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isInspection = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = Manifest.permission.CAMERA
    var hasPermission by remember {
        mutableStateOf(
            isInspection || ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var childDialogEvents by remember { mutableStateOf<List<Event>?>(initialChildDialogEvents) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    // Real-time barcode detection overlay state
    var detectedBarcodes by remember { mutableStateOf(emptyList<DetectedBarcode>()) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    val cooldownState = rememberUpdatedState(cooldownActive)
    val throttleState = remember { mutableStateOf(false) }
    var throttleProgress by remember { mutableStateOf(0f) }
    val modeState = rememberUpdatedState(mode)
    val eventsState = rememberUpdatedState(events)
    val parentsState = rememberUpdatedState(parents)
    val onSingleScanState = rememberUpdatedState(onSingleScan)
    val onSequenceCodeState = rememberUpdatedState(onSequenceCode)
    val onOpenEventState = rememberUpdatedState(onOpenEvent)

    val authPermissionDenyText = stringResource(R.string.brapi_auth_permission_deny)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) onShowMessage(authPermissionDenyText)
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(permission)
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            cameraProvider?.unbindAll()
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            if (topBarState != null) {
                @kotlin.OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(topBarState.title ?: topBarState.titleRes?.let { stringResource(it) }.orEmpty())
                    },
                    navigationIcon = {
                        if (topBarState.showBack) {
                            IconButton(onClick = { topBarState.onBack?.invoke() }) {
                                Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.navigate_back))
                            }
                        }
                    },
                    actions = {
                        topBarState.actions.forEach { action ->
                            if (action.iconRes != null) {
                                IconButton(onClick = { action.onClick?.invoke() }) {
                                    Icon(painterResource(action.iconRes), contentDescription = stringResource(action.labelRes))
                                }
                            } else {
                                TextButton(onClick = { action.onClick?.invoke() }) {
                                    Text(stringResource(action.labelRes))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.primary,
                        titleContentColor = AppTheme.colors.surface.topBarContentColor,
                        actionIconContentColor = AppTheme.colors.surface.topBarContentColor,
                        navigationIconContentColor = AppTheme.colors.surface.topBarContentColor,
                    ),
                )
            }
        },
    ) { innerPadding ->
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.brapi_auth_permission_deny))
                Button(onClick = { permissionLauncher.launch(permission) }) {
                    Text(stringResource(R.string.dialog_scan))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // ML Kit barcode scanner options
                val scannerOptions = remember(mlKitFormats) {
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(mlKitFormats)
                        .build()
                }
                val barcodeScanner = remember(scannerOptions) {
                    if (isInspection) null
                    else BarcodeScanning.getClient(scannerOptions)
                }

                if (isInspection) {
                    // Placeholder for preview to avoid CameraX/MLKit initialization
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .border(2.dp, Color.Gray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Barcode Scanner Preview")
                    }
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { previewSize = it },
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                cameraProvider = provider

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setResolutionSelector(
                                        androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                                            .setResolutionStrategy(
                                                androidx.camera.core.resolutionselector.ResolutionStrategy(
                                                    Size(1280, 720),
                                                    androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                                )
                                            )
                                            .build()
                                    )
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                barcodeScanner?.let { scanner ->
                                    imageAnalysis.setAnalyzer(
                                        analysisExecutor,
                                        MlKitBarcodeAnalyzer(
                                            scanner = scanner,
                                            onBarcodesDetected = { barcodes ->
                                                // Update real-time overlay with all detected barcodes
                                                detectedBarcodes = barcodes
                                            },
                                            onBarcodeDetected = { rawValue ->
                                                val cooldown = cooldownState.value
                                                val throttle = throttleState.value

                                                // Access state via .value to ensure we aren't using stale captured values
                                                if (cooldown || throttle) {
                                                    return@MlKitBarcodeAnalyzer
                                                }

                                                val text = rawValue.normalizePrintedQrPayload()
                                                if (text.isBlank()) return@MlKitBarcodeAnalyzer

                                                // Activate throttle to enforce delay between scans
                                                // Use the scope with immediate dispatcher to ensure we run on the main thread
                                                // and update state without unnecessary delay.
                                                scope.launch {
                                                    if (throttleState.value) return@launch
                                                    throttleState.value = true
                                                    
                                                    // Start a separate coroutine for the throttle timer
                                                    launch {
                                                        val duration = 3000L
                                                        val steps = 20
                                                        val stepDelay = duration / steps
                                                        for (i in 1..steps) {
                                                            delay(stepDelay)
                                                            throttleProgress = i.toFloat() / steps
                                                        }
                                                        throttleState.value = false
                                                        throttleProgress = 0f
                                                    }
                                                    
                                                    when (modeState.value) {
                                                        BARCODE_MODE_CONTINUOUS -> {
                                                            onSequenceCodeState.value(text)
                                                            // Haptic feedback
                                                            previewView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                                        }
                                                        BARCODE_MODE_SEARCH -> handleSearchScan(
                                                            code = text,
                                                            events = eventsState.value,
                                                            parents = parentsState.value,
                                                            onOpenEvent = onOpenEventState.value,
                                                            onChildren = { childDialogEvents = it },
                                                            onNoMatch = {
                                                                onShowMessage(context.getString(R.string.no_child_exists))
                                                            },
                                                        )
                                                        else -> onSingleScanState.value(text)
                                                    }
                                                }
                                            },
                                        ),
                                    )
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                runCatching {
                                    provider.unbindAll()
                                    val camera = provider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis,
                                    )
                                    cameraInstance = camera
                                    camera.cameraControl.enableTorch(torchEnabled)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        update = { _ ->
                            // Update torch state on recomposition
                            cameraInstance?.cameraControl?.enableTorch(torchEnabled)
                        },
                    )
                }

                // Real-time barcode bounding box overlay
                if (detectedBarcodes.isNotEmpty() && previewSize != IntSize.Zero) {
                    BarcodeOverlay(
                        barcodes = detectedBarcodes,
                        previewSize = previewSize,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (mode == BARCODE_MODE_CONTINUOUS) {
                    SequenceScanIndicator(
                        femaleScanned = sequenceFemaleScanned,
                        maleScanned = sequenceMaleScanned,
                        crossScanned = sequenceCrossScanned,
                        femaleCode = sequenceFemaleCode,
                        maleCode = sequenceMaleCode,
                        crossIdCode = sequenceCrossIdCode,
                        needsMale = sequenceNeedsMale,
                        needsCrossId = sequenceNeedsCrossId,
                        nextSlot = sequenceNextSlot,
                        saveCount = sequenceSaveCount,
                        cooldownActive = cooldownActive || throttleState.value,
                        cooldownProgress = if (cooldownActive) cooldownProgress else throttleProgress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            }
        }

        childDialogEvents?.let { children ->
            AlertDialog(
                onDismissRequest = {
                    childDialogEvents = null
                },
                title = { Text(stringResource(R.string.click_item_to_open_child)) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (children.isEmpty()) {
                            Text(stringResource(R.string.no_child_exists))
                        } else {
                            children.forEach { event ->
                                CrossListItem(
                                    event = event,
                                    onClick = {
                                        event.id?.let(onOpenEvent)
                                        childDialogEvents = null
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            childDialogEvents = null
                        },
                    ) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
            )
        }
    } // Scaffold
}

// ─── Barcode Overlay ────────────────────────────────────────────────────────────

/**
 * Draws real-time bounding boxes around detected barcodes on top of the camera preview.
 * ML Kit returns bounding boxes already in the upright (rotated) coordinate space,
 * so we only need to scale from that space to the preview view, accounting for
 * FILL_CENTER scaling and cropping.
 */
@Composable
private fun BarcodeOverlay(
    barcodes: List<DetectedBarcode>,
    previewSize: IntSize,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val overlayColor = Color(0xFF00E676) // Bright green for visibility on camera
    val labelStyle = TextStyle(
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        background = Color(0xCC000000),
    )

    Canvas(modifier = modifier) {
        val viewWidth = size.width
        val viewHeight = size.height

        for (barcode in barcodes) {
            val box = barcode.boundingBox

            // ML Kit bounding boxes are in the upright image coordinate space.
            // The upright dimensions are determined by swapping width/height for 90/270 rotation.
            val rotated = barcode.rotationDegrees == 90 || barcode.rotationDegrees == 270
            val sourceW = if (rotated) barcode.imageHeight.toFloat() else barcode.imageWidth.toFloat()
            val sourceH = if (rotated) barcode.imageWidth.toFloat() else barcode.imageHeight.toFloat()

            // FILL_CENTER: scale to fill the view, then center-crop the overflow
            val scale = maxOf(viewWidth / sourceW, viewHeight / sourceH)
            val scaledW = sourceW * scale
            val scaledH = sourceH * scale
            val offsetX = (viewWidth - scaledW) / 2f
            val offsetY = (viewHeight - scaledH) / 2f

            // Simple proportional mapping (no rotation needed — ML Kit already handled it)
            val left = box.left.toFloat() * scale + offsetX
            val top = box.top.toFloat() * scale + offsetY
            val right = box.right.toFloat() * scale + offsetX
            val bottom = box.bottom.toFloat() * scale + offsetY

            // Clamp to view bounds
            val clampedLeft = left.coerceIn(0f, viewWidth)
            val clampedTop = top.coerceIn(0f, viewHeight)
            val clampedRight = right.coerceIn(0f, viewWidth)
            val clampedBottom = bottom.coerceIn(0f, viewHeight)

            // Draw rounded bounding box
            drawRoundRect(
                color = overlayColor,
                topLeft = Offset(clampedLeft, clampedTop),
                size = androidx.compose.ui.geometry.Size(
                    clampedRight - clampedLeft,
                    clampedBottom - clampedTop,
                ),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.cornerPathEffect(8f),
                ),
            )

            // Draw corner accents (small L-shaped marks at corners for extra visibility)
            val cornerLen = minOf(20f, (clampedRight - clampedLeft) * 0.3f)
            // Top-left
            drawLine(overlayColor, Offset(clampedLeft, clampedTop), Offset(clampedLeft + cornerLen, clampedTop), strokeWidth = 5f)
            drawLine(overlayColor, Offset(clampedLeft, clampedTop), Offset(clampedLeft, clampedTop + cornerLen), strokeWidth = 5f)
            // Top-right
            drawLine(overlayColor, Offset(clampedRight, clampedTop), Offset(clampedRight - cornerLen, clampedTop), strokeWidth = 5f)
            drawLine(overlayColor, Offset(clampedRight, clampedTop), Offset(clampedRight, clampedTop + cornerLen), strokeWidth = 5f)
            // Bottom-left
            drawLine(overlayColor, Offset(clampedLeft, clampedBottom), Offset(clampedLeft + cornerLen, clampedBottom), strokeWidth = 5f)
            drawLine(overlayColor, Offset(clampedLeft, clampedBottom), Offset(clampedLeft, clampedBottom - cornerLen), strokeWidth = 5f)
            // Bottom-right
            drawLine(overlayColor, Offset(clampedRight, clampedBottom), Offset(clampedRight - cornerLen, clampedBottom), strokeWidth = 5f)
            drawLine(overlayColor, Offset(clampedRight, clampedBottom), Offset(clampedRight, clampedBottom - cornerLen), strokeWidth = 5f)

            // Draw barcode value label above the box
            if (barcode.rawValue.isNotBlank()) {
                val labelText = barcode.rawValue.take(20) + if (barcode.rawValue.length > 20) "…" else ""
                val textResult = textMeasurer.measure(labelText, labelStyle)
                val labelX = clampedLeft.coerceAtMost(viewWidth - textResult.size.width)
                val labelY = (clampedTop - textResult.size.height - 4f).coerceAtLeast(0f)
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(labelX, labelY),
                )
            }
        }
    }
}

// ─── ML Kit Image Analyzer ──────────────────────────────────────────────────────

private class MlKitBarcodeAnalyzer(
    private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val onBarcodesDetected: (List<DetectedBarcode>) -> Unit,
    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @AnnotationOptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )
        val imgWidth = imageProxy.width
        val imgHeight = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                // Emit all detected barcodes for overlay visualization
                val detected = barcodes.mapNotNull { barcode ->
                    val box = barcode.boundingBox ?: return@mapNotNull null
                    DetectedBarcode(
                        boundingBox = box,
                        rawValue = barcode.rawValue.orEmpty(),
                        imageWidth = imgWidth,
                        imageHeight = imgHeight,
                        rotationDegrees = rotation,
                    )
                }

                println(detected.joinToString(",") { it.rawValue })

                onBarcodesDetected(detected)

                // Emit first barcode value for scan logic
                barcodes.firstOrNull()?.rawValue?.let { value ->
                    onBarcodeDetected(value)
                }
            }
            .addOnFailureListener {
                // Clear overlay on failure
                onBarcodesDetected(emptyList())
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

// ─── Sequence Scan Indicator ────────────────────────────────────────────────────

@Composable
private fun SequenceScanIndicator(
    femaleScanned: Boolean,
    maleScanned: Boolean,
    crossScanned: Boolean,
    femaleCode: String = "",
    maleCode: String = "",
    crossIdCode: String = "",
    needsMale: Boolean,
    needsCrossId: Boolean,
    nextSlot: SequenceScanSlot?,
    saveCount: Int = 0,
    cooldownActive: Boolean = false,
    cooldownProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    var showSavedBadge by remember { mutableStateOf(false) }

    LaunchedEffect(saveCount) {
        if (saveCount > 0) {
            showSavedBadge = true
            delay(1800L)
            showSavedBadge = false
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // "Cross saved" banner
        AnimatedVisibility(
            visible = showSavedBadge,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut(),
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = Color(0xFF4CAF50),
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = stringResource(R.string.sequence_cross_saved),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Scan slot chips wrapped in a circular progress ring during cooldown
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SequenceSlotChip(
                        slot = SequenceScanSlot.Female,
                        scanned = femaleScanned,
                        code = femaleCode,
                        required = true,
                        current = nextSlot == SequenceScanSlot.Female,
                    )
                    Text("→", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    SequenceSlotChip(
                        slot = SequenceScanSlot.Male,
                        scanned = maleScanned,
                        code = maleCode,
                        required = needsMale,
                        current = nextSlot == SequenceScanSlot.Male,
                    )
                    Text("→", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    SequenceSlotChip(
                        slot = SequenceScanSlot.Cross,
                        scanned = crossScanned,
                        code = crossIdCode,
                        required = needsCrossId,
                        current = nextSlot == SequenceScanSlot.Cross,
                    )
                }
            }

            // Circular progress arc drawn around the chip surface
            if (cooldownActive) {
                Canvas(
                    modifier = Modifier.matchParentSize(),
                ) {
                    val strokeWidth = 10f
                    val cornerRadiusPx = 18.dp.toPx()
                    
                    val path = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(Offset.Zero, size),
                                cornerRadius = CornerRadius(cornerRadiusPx)
                            )
                        )
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, false)
                    val totalLength = pathMeasure.length
                    
                    // Track (subtle background ring)
                    drawPath(
                        path = path,
                        color = Color.Gray.copy(alpha = 0.3f),
                        style = Stroke(width = strokeWidth),
                    )
                    
                    // Progress arc matching the border
                    drawPath(
                        path = path,
                        color = Color(0xFF4CAF50),
                        style = Stroke(
                            width = strokeWidth, 
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(totalLength * cooldownProgress, totalLength),
                                phase = 0f
                            )
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SequenceSlotChip(
    slot: SequenceScanSlot,
    scanned: Boolean,
    code: String,
    required: Boolean,
    current: Boolean,
) {
    val iconRes = when (slot) {
        SequenceScanSlot.Female -> R.drawable.ic_female_parent
        SequenceScanSlot.Male -> R.drawable.ic_male_parent
        SequenceScanSlot.Cross -> R.drawable.ic_cross
    }
    val backgroundColor = when {
        scanned || !required -> AppTheme.colors.accent.copy(alpha = 0.15f)
        current -> AppTheme.colors.primary.copy(alpha = 0.12f)
        else -> Color.Gray.copy(alpha = 0.08f)
    }
    val borderColor = when {
        current -> AppTheme.colors.primary
        scanned || !required -> AppTheme.colors.accent
        else -> Color.Transparent
    }
    val textColor = when {
        scanned || !required -> AppTheme.colors.accent
        current -> AppTheme.colors.primary
        else -> Color.Gray
    }
    val alpha = if (required) 1f else 0.5f

    val chipShape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val borderModifier = if (current || scanned) {
        Modifier.border(1.5.dp, borderColor, chipShape)
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier.alpha(alpha).then(borderModifier),
        shape = chipShape,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor,
            )
            if (scanned && code.isNotBlank()) {
                Text(
                    text = code.take(8) + if (code.length > 8) "…" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium,
                )
            } else if (!required) {
                Text("—", style = MaterialTheme.typography.labelSmall, color = textColor)
            } else if (current) {
                Text(
                    text = stringResource(R.string.sequence_scan_waiting),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
            }
        }
    }
}

// ─── Utilities ──────────────────────────────────────────────────────────────────

private fun handleSearchScan(
    code: String,
    events: List<Event>,
    parents: List<Parent>,
    onOpenEvent: (Long) -> Unit,
    onChildren: (List<Event>) -> Unit,
    onNoMatch: () -> Unit,
) {
    val normalizedCode = code.normalizePrintedQrPayload()
    val scannedEvent = events.find { it.eventDbId == normalizedCode }
    if (scannedEvent?.id != null) {
        onOpenEvent(scannedEvent.id ?: -1L)
        return
    }

    val parent = parents.find { it.codeId == normalizedCode }
    if (parent != null) {
        val children = events.filter {
            it.femaleObsUnitDbId == parent.codeId || it.maleObsUnitDbId == parent.codeId
        }
        onChildren(children)
    } else {
        onNoMatch()
    }
}

private fun String.normalizePrintedQrPayload(): String {
    val cleaned = trim()
    if (cleaned.length > 3 && cleaned[2] == ',') {
        val errorCorrection = cleaned[0]
        val inputMode = cleaned[1]
        if (errorCorrection in "LMQH" && inputMode in "ANMBK") {
            return cleaned.drop(3).trim()
        }
    }
    return cleaned
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "SequenceScanIndicator - Female Next")
@Composable
private fun SequenceScanIndicatorFemalePreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SequenceScanIndicator(
            femaleScanned = false,
            maleScanned = false,
            crossScanned = false,
            femaleCode = "",
            maleCode = "",
            crossIdCode = "",
            needsMale = true,
            needsCrossId = true,
            nextSlot = SequenceScanSlot.Female,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "SequenceScanIndicator - Male Next")
@Composable
private fun SequenceScanIndicatorMalePreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SequenceScanIndicator(
            femaleScanned = true,
            maleScanned = false,
            crossScanned = false,
            femaleCode = "HC001",
            maleCode = "",
            crossIdCode = "",
            needsMale = true,
            needsCrossId = true,
            nextSlot = SequenceScanSlot.Male,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "SequenceScanIndicator - All Scanned")
@Composable
private fun SequenceScanIndicatorCompletePreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SequenceScanIndicator(
            femaleScanned = true,
            maleScanned = true,
            crossScanned = true,
            femaleCode = "HC001",
            maleCode = "FJ003",
            crossIdCode = "Cross-001",
            needsMale = true,
            needsCrossId = true,
            nextSlot = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "SequenceScanIndicator - Cooldown Active")
@Composable
private fun SequenceScanIndicatorCooldownPreview() {
    org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme {
        SequenceScanIndicator(
            femaleScanned = false,
            maleScanned = false,
            crossScanned = false,
            femaleCode = "",
            maleCode = "",
            crossIdCode = "",
            needsMale = true,
            needsCrossId = true,
            nextSlot = SequenceScanSlot.Female,
            cooldownActive = true,
            cooldownProgress = 0.6f,
            modifier = Modifier.padding(16.dp),
        )
    }
}
