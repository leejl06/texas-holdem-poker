package com.leejl.poker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leejl.poker.model.Card
import com.leejl.poker.model.Suit

private val cardBackColor = Color(0xFF1A3A8A)
private val cardBackPattern = Color(0xFF2B5FD9)

@Composable
fun CardView(card: Card, modifier: Modifier = Modifier, faceUp: Boolean = true, small: Boolean = false) {
    val w = if (small) 36.dp else 48.dp
    val h = if (small) 50.dp else 68.dp
    val corner = if (small) 4.dp else 6.dp
    val fs = if (small) 12.sp else 16.sp
    val symFs = if (small) 10.sp else 14.sp

    if (faceUp) {
        val color = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS)
            Color(0xFFD32F2F) else Color(0xFF1A1A1A)

        Box(
            modifier = modifier
                .width(w).height(h)
                .background(Color.White, RoundedCornerShape(corner))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(corner)),
            contentAlignment = Alignment.TopStart
        ) {
            Column(modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {
                Text(card.rank.display, color = color, fontSize = fs, fontWeight = FontWeight.Bold,
                    lineHeight = fs, textAlign = TextAlign.Start)
                Text(card.suit.symbol, color = color, fontSize = symFs,
                    lineHeight = symFs, textAlign = TextAlign.Start)
            }
            Text(card.suit.symbol, color = color, fontSize = symFs,
                modifier = Modifier.align(Alignment.Center))
        }
    } else {
        Box(
            modifier = modifier
                .width(w).height(h)
                .background(cardBackColor, RoundedCornerShape(corner))
                .border(1.dp, cardBackPattern, RoundedCornerShape(corner)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎴", fontSize = symFs)
        }
    }
}

@Composable
fun CardFan(cards: List<Card>, faceUp: Boolean = true, small: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        modifier = Modifier.wrapContentSize()
    ) {
        cards.forEachIndexed { i, card ->
            CardView(card = card, faceUp = faceUp, small = small,
                modifier = Modifier.offset(x = (i * 0).dp).zIndex(i.toFloat()))
        }
    }
}
