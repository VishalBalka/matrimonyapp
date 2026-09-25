package com.matrimonyapp.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.matrimonyapp.R

@Composable
fun BrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 108.dp
) {
    Image(
        painter = painterResource(id = R.drawable.brand_emblem),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}
