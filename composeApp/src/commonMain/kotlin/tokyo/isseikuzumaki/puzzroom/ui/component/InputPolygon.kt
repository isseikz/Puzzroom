package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import kotlin.math.roundToInt

private fun Point.toCanvasOffset(): Offset {
    return Offset(x.value.toFloat(), y.value.toFloat())
}

private fun Offset.toPoint(): Point {
    return Point(Centimeter(x.roundToInt()), Centimeter(y.roundToInt()))
}

@Composable
fun InputPolygon(
    polygons: List<Polygon> = emptyList(),
    backgroundImageUrl: String? = null,
    onNewPolygon: (Polygon) -> Unit = {},
) {
    var vertices by remember { mutableStateOf(listOf<Offset>()) }

    Box {
        backgroundImageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> {
                                    val position = event.changes.first().position
                                    println("Pointer entered at: $position")
                                }

                                PointerEventType.Move -> {
                                    val position = event.changes.first().position
                                    println("Pointer moved to: $position")
                                }

                                PointerEventType.Exit -> {
                                    val position = event.changes.first().position
                                    println("Pointer exited at: $position")
                                }

                                PointerEventType.Press -> {
                                    val position = event.changes.first().position
                                    println("Pointer pressed at: $position")
                                }

                                PointerEventType.Release -> {
                                    val position = event.changes.first().position
                                    println("Pointer released at: $position")
                                    if (vertices.size >= 2 && (position - vertices.first()).getDistance() < 40f) {
                                        Polygon.Builder().apply {
                                            vertices.forEach {
                                                add(it.toPoint())
                                            }
                                        }.build().let { onNewPolygon(it) }
                                        vertices = emptyList()
                                    } else {
                                        vertices = vertices + position
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }
        ) {
            polygons.forEach { polygon ->
                drawPoints(
                    points = polygon.points.map { it.toCanvasOffset() } + polygon.points.first()
                        .toCanvasOffset(),
                    pointMode = androidx.compose.ui.graphics.PointMode.Polygon,
                    color = androidx.compose.ui.graphics.Color.Red,
                    strokeWidth = 5f
                )
            }

            drawPoints(
                points = vertices,
                pointMode = androidx.compose.ui.graphics.PointMode.Polygon,
                color = androidx.compose.ui.graphics.Color.Blue,
                strokeWidth = 5f
            )
        }
    }
}

@Preview
@Composable
fun InputPolygonPreview() {
    PreviewTemplate {
        InputPolygon()
    }
}
