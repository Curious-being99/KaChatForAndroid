package com.kachat.app.ui.screens

import com.kachat.app.R
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.kachat.app.repository.AppSettingsRepository
import com.kachat.app.ui.theme.KaspaTeal
import com.kachat.app.ui.theme.LocalAppColors
import com.kachat.app.viewmodels.ConnectionViewModel
import com.kachat.app.viewmodels.WalletViewModel

private enum class WelcomeGuideStep { WELCOME, LANGUAGE, CURRENCY, FEES, FUNDING, NODE_CONNECTION, ADDRESS_EXPLAINER, CHATTING }

private enum class NodeChoice { DEFAULT_NODE, OWN_NODE, AUTO_DISCOVER }

/**
 * First-run guided walkthrough shown automatically right after an account is added — whether
 * created or imported (see `WalletViewModel.pendingWelcomeGuide`) — and replayable any time from
 * the Profile screen. Mirrors [KnsCreateProfileWizardScreen]'s step-enum-driven single-screen
 * wizard shape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeGuideScreen(
    walletViewModel: WalletViewModel,
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(WelcomeGuideStep.WELCOME) }
    val chattingAddress by walletViewModel.address.collectAsState()
    val spendingAddress by walletViewModel.spendingAddress.collectAsState()
    val trustedNodeAddress by connectionViewModel.trustedNodeAddress.collectAsState()

    var nodeChoice by remember(trustedNodeAddress) {
        mutableStateOf(
            when {
                trustedNodeAddress.isBlank() -> NodeChoice.AUTO_DISCOVER
                trustedNodeAddress == AppSettingsRepository.DEFAULT_TRUSTED_NODE_ADDRESS -> NodeChoice.DEFAULT_NODE
                else -> NodeChoice.OWN_NODE
            }
        )
    }
    var ownNodeInput by remember(trustedNodeAddress) {
        mutableStateOf(if (nodeChoice == NodeChoice.OWN_NODE) trustedNodeAddress else "")
    }
    var nodeValidationError by remember { mutableStateOf<String?>(null) }
    val enterAsHostPortError = stringResource(R.string.enter_as_host_port_or_grpcs_host)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.skip), tint = LocalAppColors.current.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalAppColors.current.background)
            )
        },
        containerColor = LocalAppColors.current.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (step) {
                WelcomeGuideStep.WELCOME -> WelcomeGuideStepScaffold(
                    icon = Icons.Default.WavingHand,
                    title = stringResource(R.string.welcome_to_kachat),
                    body = stringResource(R.string.lets_walk_through_the_basics_so),
                    buttonLabel = stringResource(R.string.next),
                    onNext = { step = WelcomeGuideStep.LANGUAGE }
                )
                WelcomeGuideStep.LANGUAGE -> WelcomeGuideLanguageStep(
                    walletViewModel = walletViewModel,
                    onNext = { step = WelcomeGuideStep.CURRENCY }
                )
                WelcomeGuideStep.CURRENCY -> WelcomeGuideCurrencyStep(
                    walletViewModel = walletViewModel,
                    onNext = { step = WelcomeGuideStep.FEES }
                )
                WelcomeGuideStep.FEES -> WelcomeGuideStepScaffold(
                    icon = Icons.Default.Bolt,
                    title = stringResource(R.string.how_kachat_uses_kaspa),
                    body = stringResource(R.string.kachat_lets_you_send_and_receive),
                    buttonLabel = stringResource(R.string.next),
                    onNext = { step = WelcomeGuideStep.FUNDING }
                )
                WelcomeGuideStep.FUNDING -> WelcomeGuideFundingStep(
                    chattingAddress = chattingAddress,
                    onNext = { step = WelcomeGuideStep.NODE_CONNECTION }
                )
                WelcomeGuideStep.NODE_CONNECTION -> WelcomeGuideNodeConnectionStep(
                    nodeChoice = nodeChoice,
                    onNodeChoiceChange = { nodeChoice = it; nodeValidationError = null },
                    ownNodeInput = ownNodeInput,
                    onOwnNodeInputChange = { ownNodeInput = it; nodeValidationError = null },
                    validationError = nodeValidationError,
                    onNext = {
                        val valueToApply = when (nodeChoice) {
                            NodeChoice.DEFAULT_NODE -> AppSettingsRepository.DEFAULT_TRUSTED_NODE_ADDRESS
                            NodeChoice.AUTO_DISCOVER -> ""
                            NodeChoice.OWN_NODE -> ownNodeInput
                        }
                        val trimmed = valueToApply.trim()
                        if (trimmed.isNotEmpty() && com.kachat.app.services.grpc.parseNodeAddress(trimmed) == null) {
                            nodeValidationError = enterAsHostPortError
                        } else {
                            nodeValidationError = null
                            connectionViewModel.setTrustedNodeAddress(trimmed)
                            step = WelcomeGuideStep.ADDRESS_EXPLAINER
                        }
                    }
                )
                WelcomeGuideStep.ADDRESS_EXPLAINER -> WelcomeGuideAddressExplainerStep(
                    chattingAddress = chattingAddress,
                    spendingAddress = spendingAddress,
                    onNext = { step = WelcomeGuideStep.CHATTING }
                )
                WelcomeGuideStep.CHATTING -> WelcomeGuideStepScaffold(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = stringResource(R.string.starting_a_conversation),
                    body = stringResource(R.string.to_chat_with_someone_press_create),
                    buttonLabel = stringResource(R.string.finish),
                    onNext = onFinished
                )
            }
        }
    }
}

@Composable
private fun WelcomeGuideStepScaffold(
    icon: ImageVector,
    title: String,
    body: String,
    buttonLabel: String,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            color = LocalAppColors.current.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonLabel, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Picking a language here applies it immediately via [applyAppLanguage], which recreates the
 * Activity (see [applyAppLanguage]'s doc). [WalletViewModel.markPendingWelcomeGuide] is re-armed
 * first so the guide automatically restarts from the beginning - this time actually rendered in
 * the new language - once the recreated Activity's `MainShell` observes the flag again.
 */
@Composable
private fun WelcomeGuideLanguageStep(walletViewModel: WalletViewModel, onNext: () -> Unit) {
    val current = currentAppLanguage()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Translate, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.choose_your_language),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.select_the_language_youd_like_to),
            color = LocalAppColors.current.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LocalAppColors.current.surfaceVariant)
                        .clickable {
                            // No-op re-tap of the already-active language (most commonly "System",
                            // the pre-checked top row) must not re-arm the restart flag below:
                            // setApplicationLocales() only recreates the Activity on an actual
                            // locale change, so with no change this composable never gets torn
                            // down to consume the flag itself - it would loop this same still-alive
                            // MainShell back to a fresh "welcome_guide" destination on every tap,
                            // resetting the wizard to WELCOME with no way to progress past here.
                            if (language != current) {
                                walletViewModel.markPendingWelcomeGuide()
                                applyAppLanguage(language)
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (language == AppLanguage.SYSTEM) stringResource(R.string.system_default) else language.displayName,
                        color = LocalAppColors.current.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (current == language) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Only affects Portfolio's live KAS price/value display (see `CoinGeckoApi`/`PortfolioRepository`)
 * - unlike language, this takes effect immediately with no Activity recreation, so the guide
 * simply continues to Fees on tap with no restart/reload concern.
 */
@Composable
private fun WelcomeGuideCurrencyStep(walletViewModel: WalletViewModel, onNext: () -> Unit) {
    val currencyCode by walletViewModel.currency.collectAsState()
    val current = AppCurrency.fromCode(currencyCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.choose_your_currency),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.select_the_currency_youd_like_prices),
            color = LocalAppColors.current.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppCurrency.entries.forEach { currency ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LocalAppColors.current.surfaceVariant)
                        .clickable { walletViewModel.setCurrency(currency.code) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currency.displayName,
                        color = LocalAppColors.current.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (current == currency) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WelcomeGuideFundingStep(chattingAddress: String?, onNext: () -> Unit) {
    var showQr by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.QrCode, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.fund_your_chatting_address),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.let_s_fund_your_chatting_address),
            color = LocalAppColors.current.textSecondary,
            textAlign = TextAlign.Center
        )
        if (chattingAddress != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                chattingAddress,
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable {
                        clipboardManager.setText(AnnotatedString(chattingAddress))
                        Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showQr = true }) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.show_qr_code), color = KaspaTeal, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    if (showQr && chattingAddress != null) {
        QrCodeOverlay(
            value = chattingAddress,
            onDismiss = { showQr = false },
            message = stringResource(R.string.just_send_5_10_kas_at_a_time),
            borderColor = KaspaTeal,
            borderWidth = 4.dp
        )
    }
}

@Composable
private fun WelcomeGuideNodeConnectionStep(
    nodeChoice: NodeChoice,
    onNodeChoiceChange: (NodeChoice) -> Unit,
    ownNodeInput: String,
    onOwnNodeInputChange: (String) -> Unit,
    validationError: String?,
    onNext: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Dns, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.connect_to_a_node),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.kachat_needs_to_connect_to_a),
            color = LocalAppColors.current.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        NodeChoiceRow(
            selected = nodeChoice == NodeChoice.DEFAULT_NODE,
            title = stringResource(R.string.default_option),
            badge = stringResource(R.string.recommended),
            subtitle = AppSettingsRepository.DEFAULT_TRUSTED_NODE_ADDRESS,
            onClick = { onNodeChoiceChange(NodeChoice.DEFAULT_NODE) }
        )
        Spacer(Modifier.height(10.dp))
        NodeChoiceRow(
            selected = nodeChoice == NodeChoice.OWN_NODE,
            title = stringResource(R.string.connect_your_own_node),
            badge = stringResource(R.string.best),
            subtitle = stringResource(R.string.enter_a_node_address_you_trust),
            onClick = { onNodeChoiceChange(NodeChoice.OWN_NODE) }
        )
        if (nodeChoice == NodeChoice.OWN_NODE) {
            Spacer(Modifier.height(8.dp))
            TextField(
                value = ownNodeInput,
                onValueChange = onOwnNodeInputChange,
                placeholder = { Text(stringResource(R.string.host_port_or_grpcs_host), color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LocalAppColors.current.surfaceVariant,
                    unfocusedContainerColor = LocalAppColors.current.surfaceVariant,
                    focusedTextColor = LocalAppColors.current.textPrimary,
                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                    cursorColor = KaspaTeal,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        NodeChoiceRow(
            selected = nodeChoice == NodeChoice.AUTO_DISCOVER,
            title = stringResource(R.string.auto_search_for_nodes),
            badge = null,
            subtitle = stringResource(R.string.most_taxing_on_the_device_not_as),
            onClick = { onNodeChoiceChange(NodeChoice.AUTO_DISCOVER) }
        )

        validationError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFF3B30), fontSize = 12.sp)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NodeChoiceRow(selected: Boolean, title: String, badge: String?, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (selected) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (selected) KaspaTeal else LocalAppColors.current.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Row {
                Text(title, fontWeight = FontWeight.SemiBold, color = LocalAppColors.current.textPrimary, fontSize = 14.sp)
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(badge, fontWeight = FontWeight.Bold, color = KaspaTeal, fontSize = 11.sp)
                }
            }
            Text(subtitle, color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WelcomeGuideAddressExplainerStep(chattingAddress: String?, spendingAddress: String?, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.chatting_vs_spending_address),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.address_types_explainer),
            contentDescription = null,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(16.dp))
        AddressMockRow(
            title = stringResource(R.string.chatting_address),
            address = chattingAddress.orEmpty(),
            caption = stringResource(R.string.your_public_messaging_identity)
        )
        Spacer(Modifier.height(12.dp))
        AddressMockRow(
            title = stringResource(R.string.spending_address),
            address = spendingAddress.orEmpty(),
            caption = stringResource(R.string.where_you_send_and_receive_kaspa)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.next), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddressMockRow(title: String, address: String, caption: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = LocalAppColors.current.textPrimary, fontSize = 14.sp)
        if (address.isNotEmpty()) {
            Text(
                address,
                color = LocalAppColors.current.textSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(caption, color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
    }
}
