package pl.edu.pja.s27599.digitaldiary.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import androidx.core.graphics.drawable.toBitmap

import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


data class ImageDrawData(
    val scale: Float,
    val offset: Offset,
    val canvasSize: Size
)

@Composable
fun ImageEditor(
    modifier: Modifier = Modifier,
    imageUri: Uri,
    onImageEdited: (Uri) -> Unit,
    saveTrigger: State<Boolean>
) {
    val context = LocalContext.current

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val paths = remember { mutableStateListOf<PathWrapper>() }
    var currentPath by remember { mutableStateOf<PathWrapper?>(null) }

    var imageDrawData by remember { mutableStateOf<ImageDrawData?>(null) }

    LaunchedEffect(imageUri) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .allowHardware(false)
            .build()
        val result = (imageLoader.execute(request) as SuccessResult).drawable
        imageBitmap = result.toBitmap().asImageBitmap()
    }

    LaunchedEffect(saveTrigger.value) {
        if (saveTrigger.value) {
            if (imageBitmap != null && imageDrawData != null) {
                val editedBitmap = renderImageAndPathsToBitmap(
                    context,
                    imageUri,
                    paths,
                    imageDrawData!!.canvasSize,
                    imageDrawData!!.scale,
                    imageDrawData!!.offset
                )
                val newUri = saveBitmapToFile(context, editedBitmap)
                onImageEdited(newUri)
            }
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newPath = Path()
                        newPath.moveTo(offset.x, offset.y)
                        currentPath = PathWrapper(newPath, Color.Red)
                        paths.add(currentPath!!)
                    },
                    onDragEnd = {
                        currentPath = null
                    },
                    onDragCancel = {
                        currentPath = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPath?.path?.lineTo(change.position.x, change.position.y)
                        paths.remove(currentPath)
                        paths.add(currentPath!!)
                    }
                )
            }
    ) {
        imageBitmap?.let { img: ImageBitmap ->
            val srcSize = Size(img.width.toFloat(), img.height.toFloat())
            val dstSizeCanvas = size
            val scaledRect = ContentScale.Fit.computeScaleFactor(srcSize, dstSizeCanvas)

            val finalScaledWidth = srcSize.width * scaledRect.scaleX
            val finalScaledHeight = srcSize.height * scaledRect.scaleY
            val topLeftOffset = Offset(
                x = (dstSizeCanvas.width - finalScaledWidth) / 2,
                y = (dstSizeCanvas.height - finalScaledHeight) / 2
            )

            imageDrawData = ImageDrawData(
                scale = scaledRect.scaleX,
                offset = topLeftOffset,
                canvasSize = dstSizeCanvas
            )

            drawImage(
                image = img,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset(topLeftOffset.x.toInt(), topLeftOffset.y.toInt()),
                dstSize = IntSize(finalScaledWidth.toInt(), finalScaledHeight.toInt())
            )
        }

        paths.forEach { pathWrapper ->
            drawPath(
                path = pathWrapper.path,
                color = pathWrapper.color,
                style = Stroke(width = 5.dp.toPx())
            )
        }
    }
}

data class PathWrapper(
    val path: Path,
    val color: Color
)

private suspend fun renderImageAndPathsToBitmap(
    context: android.content.Context,
    originalUri: Uri,
    paths: List<PathWrapper>,
    canvasSize: Size,
    imageScale: Float,
    imageOffset: Offset,
): Bitmap = withContext(Dispatchers.IO) {
    val imageLoader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(originalUri)
        .allowHardware(false)
        .build()

    val result = (imageLoader.execute(request) as SuccessResult).drawable
    val originalBitmap = result.toBitmap()

    val editableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(editableBitmap)

    canvas.drawBitmap(originalBitmap, 0f, 0f, null)

    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    val inverseScale = 1f / imageScale
    val androidTranslateX = -imageOffset.x * inverseScale
    val androidTranslateY = -imageOffset.y * inverseScale

    canvas.save()
    canvas.translate(androidTranslateX, androidTranslateY)
    canvas.scale(inverseScale, inverseScale)

    paths.forEach { pathWrapper ->
        paint.color = pathWrapper.color.toArgb()
        canvas.drawPath(pathWrapper.path.asAndroidPath(), paint)
    }

    canvas.restore()
    editableBitmap
}

private suspend fun saveBitmapToFile(context: android.content.Context, bitmap: Bitmap): Uri {
    return suspendCoroutine { continuation ->
        val filename = "edited_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val file = File(context.externalCacheDir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        continuation.resume(Uri.fromFile(file))
    }
}