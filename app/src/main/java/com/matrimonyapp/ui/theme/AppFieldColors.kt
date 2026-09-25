package com.matrimonyapp.ui.theme
import com.matrimonyapp.ui.theme.AppTextPrimary

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FieldText = AppTextPrimary
private val FieldSecondary = Color.Black
private val FieldBorder = Color.Black
private val FieldFocusedBorder = Color.Black
private val FieldError = Color(0xFFB3261E)
private val FieldContainer = AppFieldBackground

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = FieldText,
        unfocusedTextColor = FieldText,
        disabledTextColor = FieldText,
        errorTextColor = FieldText,

        focusedLabelColor = FieldText,
        unfocusedLabelColor = FieldText,
        disabledLabelColor = FieldText,
        errorLabelColor = FieldError,

        focusedPlaceholderColor = FieldSecondary,
        unfocusedPlaceholderColor = FieldSecondary,
        disabledPlaceholderColor = FieldSecondary,
        errorPlaceholderColor = FieldSecondary,

        cursorColor = FieldText,
        errorCursorColor = FieldError,

        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black,
        disabledBorderColor = Color.Black,
        errorBorderColor = FieldError,

        focusedLeadingIconColor = FieldText,
        unfocusedLeadingIconColor = FieldText,
        disabledLeadingIconColor = FieldText,
        errorLeadingIconColor = FieldError,

        focusedTrailingIconColor = FieldText,
        unfocusedTrailingIconColor = FieldText,
        disabledTrailingIconColor = FieldText,
        errorTrailingIconColor = FieldError,

        focusedContainerColor = FieldContainer,
        unfocusedContainerColor = FieldContainer,
        disabledContainerColor = FieldContainer,
        errorContainerColor = FieldContainer
    )
