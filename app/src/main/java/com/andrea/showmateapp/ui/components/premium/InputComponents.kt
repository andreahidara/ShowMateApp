package com.andrea.showmateapp.ui.components.premium

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.andrea.showmateapp.ui.theme.InputBackground
import com.andrea.showmateapp.ui.theme.PrimaryPurpleLight
import com.andrea.showmateapp.ui.theme.TextGray
import com.andrea.showmateapp.ui.theme.TextWhite

@Composable
fun PrimaryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray) },
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = TextGray) }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        isError = isError,
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = InputBackground,
            unfocusedContainerColor = InputBackground,
            disabledContainerColor = InputBackground,
            focusedBorderColor = PrimaryPurpleLight,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            errorBorderColor = Color.Red,
            cursorColor = PrimaryPurpleLight
        )
    )
}
