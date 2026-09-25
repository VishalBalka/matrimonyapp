package com.matrimonyapp.ui.auth

import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

private val NativeBlack = android.graphics.Color.BLACK
private val NativeWhite = android.graphics.Color.WHITE
private val NativeBorder = android.graphics.Color.rgb(32, 32, 32)
private val NativeError = android.graphics.Color.rgb(179, 38, 30)


@Composable
internal fun NativeAuthText(
    text: String,
    modifier: Modifier = Modifier,
    textSize: Float = 16f,
    color: Int = NativeBlack,
    bold: Boolean = false
) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            TextView(context).apply {
                setTextColor(color)
                setTextSize(textSize)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                includeFontPadding = true
                if (bold) {
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
            }
        },
        update = {
            it.text = text
            it.setTextColor(color)
            it.setTextSize(textSize)
            it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            it.typeface = if (bold) {
                android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
                )
            } else {
                android.graphics.Typeface.DEFAULT
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun NativeAuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = {
                TextView(context).apply {
                    text = label
                    setTextColor(NativeBlack)
                    textSize = 14f
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    includeFontPadding = true
                }
            },
            update = {
                it.text = label
                it.setTextColor(NativeBlack)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(start = 4.dp)
        )

        AndroidView(
            factory = {
                EditText(context).apply {
                    setTextColor(NativeBlack)
                    setHintTextColor(NativeBlack)
                    textSize = 17f
                    gravity = Gravity.CENTER_VERTICAL
                    isSingleLine = true
                    setPadding(16, 0, 16, 0)
                    background = authFieldBackground(isError)

                    if (label.equals("Email", ignoreCase = true)) {
                        inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    } else if (visualTransformation is PasswordVisualTransformation) {
                        inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                        transformationMethod = PasswordTransformationMethod.getInstance()
                    } else {
                        inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_NORMAL
                    }

                    setText(value)

                    addTextChangedListener(SimpleTextWatcher { changed ->
                        if (changed != value) onValueChange(changed)
                    })
                }
            },
            update = {
                it.setTextColor(NativeBlack)
                it.setHintTextColor(NativeBlack)
                it.background = authFieldBackground(isError)
                if (it.text.toString() != value) {
                    it.setText(value)
                    it.setSelection(it.text.length)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )
    }
}

private fun authFieldBackground(isError: Boolean): GradientDrawable =
    GradientDrawable().apply {
        setColor(NativeWhite)
        setStroke(
            2,
            if (isError) NativeError else NativeBorder
        )
        cornerRadius = 6f
    }

private class SimpleTextWatcher(
    private val callback: (String) -> Unit
) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        callback(s?.toString().orEmpty())
    }
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
