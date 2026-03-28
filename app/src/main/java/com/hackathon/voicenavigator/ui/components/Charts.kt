package com.hackathon.voicenavigator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.voicenavigator.data.model.ChartData
import com.hackathon.voicenavigator.data.model.ChartDataPoint
import com.hackathon.voicenavigator.data.model.StockData
import com.hackathon.voicenavigator.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Line Chart Component for World Bank ESG data
 */
@Composable
fun LineChart(
    chartData: ChartData,
    modifier: Modifier = Modifier,
    lineColor: Color = ChartBlue
) {
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier.padding(8.dp)) {
        // Title
        Text(
            text = chartData.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Unit: ${chartData.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Chart Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color.White)
            ) {
                val dataPoints = chartData.dataPoints
                if (dataPoints.isEmpty()) return@Canvas

                val paddingLeft = 60f
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingRight = 20f

                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                val minValue = dataPoints.minOf { it.value }
                val maxValue = dataPoints.maxOf { it.value }
                val valueRange = if (maxValue - minValue == 0.0) 1.0 else maxValue - minValue

                // Draw grid lines
                val gridLines = 5
                for (i in 0..gridLines) {
                    val y = paddingTop + chartHeight * (1 - i.toFloat() / gridLines)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1f
                    )

                    // Y-axis labels
                    val value = minValue + valueRange * i / gridLines
                    val label = "%.1f".format(value)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        8f,
                        y + 5f,
                        android.graphics.Paint().apply {
                            textSize = 22f
                            color = android.graphics.Color.GRAY
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // Draw X-axis labels (show every nth year)
                val step = maxOf(1, dataPoints.size / 6)
                for (i in dataPoints.indices step step) {
                    val x = paddingLeft + chartWidth * i / (dataPoints.size - 1).coerceAtLeast(1)
                    drawContext.canvas.nativeCanvas.drawText(
                        dataPoints[i].year,
                        x,
                        size.height - 5f,
                        android.graphics.Paint().apply {
                            textSize = 20f
                            color = android.graphics.Color.GRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // Draw line chart
                if (dataPoints.size > 1) {
                    val path = Path()
                    val fillPath = Path()

                    dataPoints.forEachIndexed { index, point ->
                        val x = paddingLeft + chartWidth * index / (dataPoints.size - 1)
                        val y = paddingTop + chartHeight * (1 - (point.value - minValue).toFloat() / valueRange.toFloat())

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, paddingTop + chartHeight)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    // Fill area
                    fillPath.lineTo(
                        paddingLeft + chartWidth,
                        paddingTop + chartHeight
                    )
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        color = lineColor.copy(alpha = 0.1f)
                    )

                    // Draw line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw data points
                    dataPoints.forEachIndexed { index, point ->
                        val x = paddingLeft + chartWidth * index / (dataPoints.size - 1)
                        val y = paddingTop + chartHeight * (1 - (point.value - minValue).toFloat() / valueRange.toFloat())
                        drawCircle(
                            color = lineColor,
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }
                }

                // Label "WORLD"
                val lastPoint = dataPoints.last()
                val lastX = paddingLeft + chartWidth
                val lastY = paddingTop + chartHeight * (1 - (lastPoint.value - minValue).toFloat() / valueRange.toFloat())
                drawContext.canvas.nativeCanvas.drawText(
                    "WORLD",
                    lastX - 10f,
                    lastY - 10f,
                    android.graphics.Paint().apply {
                        textSize = 22f
                        color = lineColor.toArgb()
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }
        }
    }
}

/**
 * Pie/Donut Chart for DOW stocks
 */
@Composable
fun PieChart(
    stocks: List<StockData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = "DIA (Dow 30) 10 Largest",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pie chart
            Card(
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val total = stocks.sumOf { it.percentage }
                    var startAngle = -90f
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = minOf(centerX, centerY) - 8f
                    val innerRadius = radius * 0.5f

                    stocks.forEachIndexed { index, stock ->
                        val sweepAngle = (stock.percentage / total * 360).toFloat()
                        val color = ChartColors[index % ChartColors.size]

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(centerX - radius, centerY - radius),
                            size = Size(radius * 2, radius * 2)
                        )

                        startAngle += sweepAngle
                    }

                    // Inner circle for donut effect
                    drawCircle(
                        color = Color.White,
                        radius = innerRadius,
                        center = Offset(centerX, centerY)
                    )
                }
            }

            // Legend
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                stocks.forEachIndexed { index, stock ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    ChartColors[index % ChartColors.size],
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stock.name}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${stock.percentage}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading chart placeholder
 */
@Composable
fun ChartLoading(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", color = PrimaryBlue)
        }
    }
}
