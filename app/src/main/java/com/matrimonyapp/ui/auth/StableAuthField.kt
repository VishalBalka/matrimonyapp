package com.matrimonyapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFF202020)
private val Error = Color(0xFFB3261E)

@Composable
fun StableAuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Black,
            style = TextStyle(color = Black, fontSize = 14.sp),
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = Black,
                fontSize = 17.sp
            ),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(White, RoundedCornerShape(10.dp))
                        .border(
                            width = 2.dp,
                            color = if (isError) Error else Border,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 17.dp)
                ) {
                    innerTextField()
                }
            }
        )
    }
}
