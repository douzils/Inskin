package com.inskin.app

import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.inskin.app.model.WriteItem
import com.inskin.app.model.WriteItemType

/**
 * Content of the second bottom sheet used to enter a WriteItem payload.
 */
@Composable
fun WriteItemForm(type: WriteItemType, onSubmit: (WriteItem) -> Unit) {
    when (type) {
        WriteItemType.TEXT -> TextItemForm { onSubmit(it) }
        WriteItemType.URL,
        WriteItemType.SOCIAL,
        WriteItemType.VIDEO,
        WriteItemType.FILE,
        WriteItemType.APP -> UrlItemForm(type) { onSubmit(it) }
        WriteItemType.WEB_SEARCH,
        WriteItemType.ADDRESS,
        WriteItemType.DESTINATION,
        WriteItemType.NEARBY_SEARCH -> QueryItemForm(type) { onSubmit(it) }
        WriteItemType.PHONE,
        WriteItemType.EMERGENCY -> PhoneItemForm { onSubmit(it) }
        WriteItemType.SMS -> SmsItemForm { onSubmit(it) }
        WriteItemType.MAIL -> MailItemForm { onSubmit(it) }
        WriteItemType.WIFI -> WifiItemForm { onSubmit(it) }
        WriteItemType.BLUETOOTH -> BluetoothItemForm { onSubmit(it) }
        WriteItemType.CONTACT -> ContactItemForm { onSubmit(it) }
        WriteItemType.LOCATION -> LocationItemForm { onSubmit(it) }
        WriteItemType.CUSTOM_LOCATION -> CustomLocationItemForm { onSubmit(it) }
        WriteItemType.STREET_VIEW -> StreetViewItemForm { onSubmit(it) }
        WriteItemType.CRYPTO -> CryptoItemForm { onSubmit(it) }
        WriteItemType.CUSTOM_DATA,
        WriteItemType.SETTINGS,
        WriteItemType.CONDITION -> KeyValueItemForm(type) { onSubmit(it) }
        else -> Column(Modifier.padding(16.dp)) { Text(stringResource(R.string.not_implemented)) }
    }
}

@Composable
private fun TextItemForm(onSubmit: (WriteItem.Text) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.write_text_hint)) },
            singleLine = true,
            isError = text.isBlank()
        )
        Button(
            onClick = { if (text.isNotBlank()) onSubmit(WriteItem.Text(text)) },
            enabled = text.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun UrlItemForm(type: WriteItemType, onSubmit: (WriteItem) -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                error = it.isNotBlank() && !Patterns.WEB_URL.matcher(it).matches()
            },
            label = { Text(stringResource(R.string.write_url_hint)) },
            singleLine = true,
            isError = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Button(
            onClick = {
                if (!error && url.isNotBlank()) {
                    val item = if (type == WriteItemType.URL) WriteItem.Url(url) else WriteItem.UriItem(type, url)
                    onSubmit(item)
                }
            },
            enabled = url.isNotBlank() && !error,
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun QueryItemForm(type: WriteItemType, onSubmit: (WriteItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.write_query_hint)) },
            singleLine = true,
            isError = query.isBlank()
        )
        Button(
            onClick = {
                if (query.isNotBlank()) {
                    val uri = when (type) {
                        WriteItemType.WEB_SEARCH -> "https://www.google.com/search?q=${Uri.encode(query)}"
                        WriteItemType.ADDRESS -> "https://maps.google.com/?q=${Uri.encode(query)}"
                        WriteItemType.DESTINATION -> "google.navigation:q=${Uri.encode(query)}"
                        WriteItemType.NEARBY_SEARCH -> "geo:0,0?q=${Uri.encode(query)}"
                        else -> query
                    }
                    onSubmit(WriteItem.UriItem(type, uri))
                }
            },
            enabled = query.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun PhoneItemForm(onSubmit: (WriteItem.Phone) -> Unit) {
    var number by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text(stringResource(R.string.write_phone_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = number.isBlank()
        )
        Button(
            onClick = { if (number.isNotBlank()) onSubmit(WriteItem.Phone(number)) },
            enabled = number.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun SmsItemForm(onSubmit: (WriteItem.Sms) -> Unit) {
    var number by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text(stringResource(R.string.write_sms_number_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = number.isBlank()
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text(stringResource(R.string.write_sms_body_hint)) },
            singleLine = true
        )
        Button(
            onClick = { if (number.isNotBlank()) onSubmit(WriteItem.Sms(number, body.ifBlank { null })) },
            enabled = number.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun MailItemForm(onSubmit: (WriteItem.Mail) -> Unit) {
    var to by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = to,
            onValueChange = { to = it },
            label = { Text(stringResource(R.string.write_mail_to_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = to.isBlank()
        )
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text(stringResource(R.string.write_mail_subject_hint)) },
            singleLine = true
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text(stringResource(R.string.write_mail_body_hint)) },
            singleLine = false
        )
        Button(
            onClick = { if (to.isNotBlank()) onSubmit(WriteItem.Mail(to, subject.ifBlank { null }, body.ifBlank { null })) },
            enabled = to.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun WifiItemForm(onSubmit: (WriteItem.Wifi) -> Unit) {
    var ssid by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = ssid, onValueChange = { ssid = it }, label = { Text(stringResource(R.string.write_wifi_ssid_hint)) }, singleLine = true, isError = ssid.isBlank())
        OutlinedTextField(value = security, onValueChange = { security = it }, label = { Text(stringResource(R.string.write_wifi_security_hint)) }, singleLine = true, isError = security.isBlank())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(R.string.write_wifi_password_hint)) }, singleLine = true)
        Button(
            onClick = { if (ssid.isNotBlank() && security.isNotBlank()) onSubmit(WriteItem.Wifi(ssid, security, password.ifBlank { null })) },
            enabled = ssid.isNotBlank() && security.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun BluetoothItemForm(onSubmit: (WriteItem.Bluetooth) -> Unit) {
    var mac by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = mac, onValueChange = { mac = it }, label = { Text(stringResource(R.string.write_bluetooth_mac_hint)) }, singleLine = true, isError = mac.isBlank())
        Button(onClick = { if (mac.isNotBlank()) onSubmit(WriteItem.Bluetooth(mac)) }, enabled = mac.isNotBlank(), modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.action_add))
        }
    }
}

@Composable
private fun ContactItemForm(onSubmit: (WriteItem.Contact) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.write_contact_name_hint)) }, singleLine = true, isError = name.isBlank())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.write_contact_phone_hint)) }, singleLine = true)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.write_contact_email_hint)) }, singleLine = true)
        Button(
            onClick = { if (name.isNotBlank()) onSubmit(WriteItem.Contact(name, phone.ifBlank { null }, email.ifBlank { null })) },
            enabled = name.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun LocationItemForm(onSubmit: (WriteItem.Location) -> Unit) {
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text(stringResource(R.string.write_location_lat_hint)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text(stringResource(R.string.write_location_lon_hint)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        val enabled = lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null
        Button(onClick = {
            val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull()
            if (la != null && lo != null) onSubmit(WriteItem.Location(la, lo))
        }, enabled = enabled, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun CustomLocationItemForm(onSubmit: (WriteItem) -> Unit) {
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text(stringResource(R.string.write_location_lat_hint)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text(stringResource(R.string.write_location_lon_hint)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.write_label_hint)) }, singleLine = true)
        val enabled = lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null && label.isNotBlank()
        Button(onClick = {
            val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull()
            if (la != null && lo != null && label.isNotBlank()) {
                val uri = "geo:$la,$lo?q=$la,$lo(${Uri.encode(label)})"
                onSubmit(WriteItem.UriItem(WriteItemType.CUSTOM_LOCATION, uri))
            }
        }, enabled = enabled, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun StreetViewItemForm(onSubmit: (WriteItem) -> Unit) {
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text(stringResource(R.string.write_location_lat_hint)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text(stringResource(R.string.write_location_lon_hint)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        val enabled = lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null
        Button(onClick = {
            val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull()
            if (la != null && lo != null) {
                val uri = "https://maps.google.com/?cbll=$la,$lo"
                onSubmit(WriteItem.UriItem(WriteItemType.STREET_VIEW, uri))
            }
        }, enabled = enabled, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun CryptoItemForm(onSubmit: (WriteItem.Crypto) -> Unit) {
    var address by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(stringResource(R.string.write_crypto_address_hint)) }, singleLine = true, isError = address.isBlank())
        OutlinedTextField(value = network, onValueChange = { network = it }, label = { Text(stringResource(R.string.write_crypto_network_hint)) }, singleLine = true)
        Button(onClick = { if (address.isNotBlank()) onSubmit(WriteItem.Crypto(address, network.ifBlank { null })) }, enabled = address.isNotBlank(), modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.action_add))
        }
    }
}

@Composable
private fun KeyValueItemForm(type: WriteItemType, onSubmit: (WriteItem.KeyValue) -> Unit) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text(stringResource(R.string.write_key_hint)) }, singleLine = true, isError = key.isBlank())
        OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(stringResource(R.string.write_value_hint)) }, singleLine = true, isError = value.isBlank())
        Button(onClick = { if (key.isNotBlank() && value.isNotBlank()) onSubmit(WriteItem.KeyValue(type, key, value)) }, enabled = key.isNotBlank() && value.isNotBlank(), modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.action_add))
        }
    }
}

@Preview
@Composable
private fun PreviewTextForm() {
    WriteItemForm(WriteItemType.TEXT) { }
}

@Preview
@Composable
private fun PreviewUrlForm() {
    WriteItemForm(WriteItemType.URL) { }
}
