package com.sagestock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sagestock.domain.StockSnapshot
import com.sagestock.ui.theme.PriceTextStyle
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography
import com.sagestock.ui.theme.priceColorOf

/** 종목 행(종목명·시장칩·티커 / 스파크라인 / 가격·등락%). 홈·검색·포트폴리오 공용. */
@Composable
fun SnapshotRow(snapshot: StockSnapshot, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    val changeColor = priceColorOf(snapshot.changePercent)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    snapshot.stock.name,
                    style = SageTypography.titleSmall,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                MiniChip(snapshot.stock.market.name)
            }
            Spacer(Modifier.height(2.dp))
            Text(snapshot.stock.ticker, style = SageTypography.labelSmall, color = c.textTertiary)
        }

        Sparkline(
            data = snapshot.sparkline,
            up = snapshot.changePercent >= 0,
            modifier = Modifier.padding(horizontal = 10.dp),
        )

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatPrice(snapshot.stock.market, snapshot.price), style = PriceTextStyle, color = c.textPrimary)
            Text(formatSignedPercent(snapshot.changePercent), style = SageTypography.labelSmall, color = changeColor)
        }
    }
}
