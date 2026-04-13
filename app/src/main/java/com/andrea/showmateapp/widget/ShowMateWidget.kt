package com.andrea.showmateapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.andrea.showmateapp.ui.MainActivity

class ShowMateWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val showName = prefs.getString("next_show_name", null)
        val watchlistCount = prefs.getInt("watchlist_count", 0)

        provideContent {
            WidgetContent(showName = showName, watchlistCount = watchlistCount)
        }
    }

    @Composable
    private fun WidgetContent(showName: String?, watchlistCount: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF12122A)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▶  ShowMate",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF9C6FFF)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(8.dp))

            if (showName != null) {
                Text(
                    text = "Siguiente en tu lista:",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 10.sp
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = showName,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
            } else {
                Text(
                    text = if (watchlistCount > 0) {
                        "$watchlistCount series en tu lista"
                    } else {
                        "¡Empieza a añadir series!"
                    },
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Toca para descubrir",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

class ShowMateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShowMateWidget()
}
