package com.kachat.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.Gravity
import android.view.WindowManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.kachat.app.R
import com.kachat.app.models.BackupRetention
import com.kachat.app.models.Conversation
import com.kachat.app.models.MessageEntity
import com.kachat.app.repository.ChatRepository
import com.kachat.app.ui.theme.KaspaBlue
import com.kachat.app.ui.theme.KaspaSubtext
import com.kachat.app.ui.theme.KaspaTeal
import com.kachat.app.ui.theme.LocalAppColors
import com.kachat.app.util.ChatTimeFormat
import com.kachat.app.util.KaspaAddress
import com.kachat.app.util.rememberCameraCaptureLauncher
import com.kachat.app.util.authenticateWithDeviceCredential
import com.kachat.app.util.ImageMessage
import com.kachat.app.util.ImagePrep
import com.kachat.app.util.MessageReply
import com.kachat.app.util.TextLinkify
import com.kachat.app.util.MessageProtocol
import com.kachat.app.util.VoiceMessage
import com.kachat.app.util.VoiceMessageContent
import com.kachat.app.viewmodels.ChatViewModel
import com.kachat.app.viewmodels.ConnectionStatus as ConnStatus
import com.kachat.app.viewmodels.ConnectionViewModel
import com.kachat.app.viewmodels.NodeInfo
import com.kachat.app.viewmodels.SettingsViewModel
import com.kachat.app.viewmodels.WalletViewModel
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatThreadScreen(
    navController: NavController,
    contactId: String,
    chatViewModel: ChatViewModel = hiltViewModel(),
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    portfolioViewModel: com.kachat.app.viewmodels.PortfolioViewModel = hiltViewModel(),
    startInPaymentMode: Boolean = false
) {
    val showFeeEstimate by settingsViewModel.showFeeEstimate.collectAsState()
    val conversations by chatViewModel.conversations.collectAsState()
    val conversation = conversations.find { it.contact.id == contactId }
    val messages by chatViewModel.getMessages(contactId).collectAsState(initial = emptyList())
    val revealedPhotoTxIds by chatViewModel.revealedPhotoTxIds.collectAsState()

    val dotColorHex by connectionViewModel.dotColorHex.collectAsState()
    val spendingBalance by walletViewModel.spendingBalance.collectAsState()
    val spendingBalanceSompi by walletViewModel.spendingBalanceSompi.collectAsState()
    val myKnsProfile by walletViewModel.knsProfile.collectAsState()
    val myAddress by walletViewModel.address.collectAsState()
    val paymentAmount by chatViewModel.paymentAmount.collectAsState()
    val fiatPriceInCurrency by portfolioViewModel.currentPriceUsd.collectAsState()
    val fiatCurrencyCode by portfolioViewModel.currency.collectAsState()
    val estimatedFee by chatViewModel.estimatedFeeSompi.collectAsState()
    val messageText by chatViewModel.messageText.collectAsState()
    val voiceRecordingState by chatViewModel.voiceRecordingState.collectAsState()
    val pendingPhotoUri by chatViewModel.pendingPhotoUri.collectAsState()
    val replyingTo by chatViewModel.replyingTo.collectAsState()
    val kaspaExplorer by chatViewModel.kaspaExplorer.collectAsState()
    val networkFeeRate by chatViewModel.networkFeeRate.collectAsState()
    val feeRateOverride by chatViewModel.feeRateOverride.collectAsState()

    var paymentMode by remember { mutableStateOf(startInPaymentMode) }
    var showComposerMenu by remember { mutableStateOf(false) }
    var composerMenuAnchor by remember { mutableStateOf(Offset.Zero) }
    var showFeeEditor by remember { mutableStateOf(false) }
    var feeEditorInput by remember { mutableStateOf("") }
    // The live fee preview already reflects whatever's currently being composed (text/photo/voice/
    // payment, each a different mass) — dividing it back out by the rate that produced it recovers
    // that mass without duplicating any of estimatedFeeSompi's own calculation here.
    val effectiveRate = feeRateOverride?.toDouble() ?: networkFeeRate
    val openFeeEditor: (Long) -> Unit = { currentFeeSompi ->
        feeEditorInput = "%.8f".format(java.util.Locale.US, currentFeeSompi / 100_000_000.0)
        showFeeEditor = true
    }
    val clipboardManager = LocalClipboardManager.current
    val micContext = LocalContext.current
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) chatViewModel.startVoiceRecording(contactId)
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) chatViewModel.setPendingPhoto(uri)
    }
    val startCameraCapture = rememberCameraCaptureLauncher { uri -> chatViewModel.setPendingPhoto(uri) }
    val startVoiceRecordingIfPermitted = {
        if (chatViewModel.voiceRecordingSupported) {
            if (ContextCompat.checkSelfPermission(micContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                chatViewModel.startVoiceRecording(contactId)
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Saving a photo to the gallery needs WRITE_EXTERNAL_STORAGE only on API 28 and below (scoped
    // storage makes MediaStore inserts permission-free from API 29 on) — the pending bytes/name
    // survive the async permission prompt in this state, then get saved once it resolves.
    var pendingPhotoSave by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    val writeStoragePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingPhotoSave
        pendingPhotoSave = null
        if (granted && pending != null) {
            val saved = ImagePrep.saveToGallery(micContext, pending.first, pending.second)
            Toast.makeText(micContext, if (saved) micContext.getString(R.string.photo_saved) else micContext.getString(R.string.could_not_save_photo), Toast.LENGTH_SHORT).show()
        }
    }
    val savePhotoIfPermitted = { bytes: ByteArray, fileName: String ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(micContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val saved = ImagePrep.saveToGallery(micContext, bytes, fileName)
            Toast.makeText(micContext, if (saved) micContext.getString(R.string.photo_saved) else micContext.getString(R.string.could_not_save_photo), Toast.LENGTH_SHORT).show()
        } else {
            pendingPhotoSave = bytes to fileName
            writeStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(contactId) {
        chatViewModel.markAsRead(contactId)
    }

    DisposableEffect(contactId) {
        chatViewModel.setActiveContact(contactId)
        onDispose { chatViewModel.setActiveContact(null) }
    }

    LaunchedEffect(paymentMode) {
        if (paymentMode) {
            chatViewModel.refreshSpendingUtxos()
            walletViewModel.refreshSpendingAddress()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(contactId))
                            Toast.makeText(micContext, micContext.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        ContactAvatar(
                            imageUrl = conversation?.contact?.knsAvatarUrl,
                            fallbackText = conversation?.contact?.alias ?: contactId.takeLast(8),
                            size = 36.dp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = conversation?.contact?.alias ?: com.kachat.app.util.KaspaAddress.shortDisplay(contactId),
                            color = LocalAppColors.current.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaspaTeal)
                    }
                },
                actions = {
                    val statusColor = Color(dotColorHex)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(LocalAppColors.current.surface, CircleShape)
                            .clickable { navController.navigate("connection_status") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(statusColor, CircleShape))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { navController.navigate("chat_info/$contactId") }) {
                        Icon(Icons.Default.Info, "Chat Info", tint = KaspaTeal, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        },
        bottomBar = {
            // navigationBarsPadding() keeps the mic/send row clear of the system nav bar
            // (gesture pill or 3-button bar) when the keyboard is closed — its height varies
            // a lot across devices/manufacturers, so a fixed dp padding isn't enough on every
            // phone. imePadding() on the Scaffold above already handles the keyboard-open case.
            Column(modifier = Modifier.background(LocalAppColors.current.background).navigationBarsPadding().padding(8.dp)) {
                if (paymentMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showFeeEstimate && estimatedFee != null) {
                                Surface(
                                    color = LocalAppColors.current.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { openFeeEditor(estimatedFee ?: 0L) }
                                ) {
                                    Text(
                                        text = "fee: ${ChatRepository.formatKas(estimatedFee ?: 0L)} KAS",
                                        color = KaspaTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Surface(
                                color = LocalAppColors.current.surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "available: $spendingBalance",
                                    color = LocalAppColors.current.textSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val fiatAmountState = com.kachat.app.util.rememberKaspaFiatAmountState(
                                onKasTextChange = { chatViewModel.setPaymentAmount(it) }
                            )
                            TextField(
                                value = fiatAmountState.displayText,
                                onValueChange = { fiatAmountState.onDisplayTextChange(it, fiatPriceInCurrency) },
                                placeholder = {
                                    Text(
                                        if (fiatAmountState.isFiatMode) fiatCurrencyCode.uppercase() else stringResource(R.string.amount_kas),
                                        color = Color.DarkGray
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(25.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = LocalAppColors.current.surface,
                                    unfocusedContainerColor = LocalAppColors.current.surface,
                                    focusedTextColor = LocalAppColors.current.textPrimary,
                                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                                    cursorColor = KaspaTeal,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    val currentUtxos by chatViewModel.currentUtxos.collectAsState()
                                    val networkFeeRate by chatViewModel.networkFeeRate.collectAsState()
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        fiatAmountState.conversionLabelText(fiatPriceInCurrency, fiatCurrencyCode)?.let { label ->
                                            Text(
                                                label,
                                                color = LocalAppColors.current.textSecondary,
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .clickable { fiatAmountState.toggleMode(fiatPriceInCurrency) }
                                                    .padding(end = 8.dp)
                                            )
                                        }
                                        TextButton(onClick = {
                                            // Mirror KaspaWalletEngine's own fee calculation exactly
                                            // (real Kaspa mass model, assuming a recipient + change
                                            // output) so the amount filled in here is always actually
                                            // sendable — the previous naive formula (300 + count*100)
                                            // didn't match the real fee, so "Max" sends kept failing
                                            // with "insufficient funds".
                                            val mass = com.kachat.app.util.KaspaMass.calculateMass(
                                                numInputs = currentUtxos.size.coerceAtLeast(1),
                                                outputScriptLens = listOf(34, 34),
                                                payloadSize = 0
                                            )
                                            val fee = com.kachat.app.util.KaspaMass.calculateFee(mass, networkFeeRate.toLong())

                                            val maxSendableSompi = (spendingBalanceSompi - fee).coerceAtLeast(0L)
                                            val maxSendableKas = maxSendableSompi.toDouble() / 100_000_000.0
                                            fiatAmountState.setMaxKas(maxSendableKas, fiatPriceInCurrency)
                                        }) {
                                            Text(stringResource(R.string.max), color = KaspaTeal, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            
                            ChatActionButton(Icons.AutoMirrored.Filled.Chat, onClick = { 
                                paymentMode = false
                                chatViewModel.setPaymentAmount("")
                            })
                            ChatActionButton(Icons.Default.Mic)
                        }

                        Button(
                            onClick = {
                                if (paymentAmount.isNotEmpty()) {
                                    chatViewModel.sendPayment(contactId, paymentAmount)
                                    chatViewModel.setPaymentAmount("")
                                    paymentMode = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.send_payment), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (pendingPhotoUri != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showFeeEstimate && estimatedFee != null) {
                            Surface(
                                color = LocalAppColors.current.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .clickable { openFeeEditor(estimatedFee ?: 0L) }
                            ) {
                                Text(
                                    text = "fee: ${ChatRepository.formatKas(estimatedFee ?: 0L)} KAS",
                                    color = KaspaTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(LocalAppColors.current.surface)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { chatViewModel.cancelPendingPhoto() }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cancel_photo), tint = Color(0xFFFF3B30))
                            }
                            val thumbnailContext = LocalContext.current
                            // Fixed downsample for a quick composition-time thumbnail decode — the real
                            // compression (ImagePrep.prepareForChatMessage) happens off the main thread
                            // in the ViewModel when Send is tapped, this is display-only.
                            val thumbnail = remember(pendingPhotoUri) {
                                pendingPhotoUri?.let { uri ->
                                    try {
                                        thumbnailContext.contentResolver.openInputStream(uri)?.use {
                                            android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 })
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            if (thumbnail != null) {
                                Image(
                                    bitmap = thumbnail.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(stringResource(R.string.photo), color = LocalAppColors.current.textPrimary, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { chatViewModel.sendPendingPhoto(contactId) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(KaspaTeal, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.send_photo),
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else if (voiceRecordingState.status == ChatViewModel.VoiceRecordingStatus.RECORDING) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showFeeEstimate && estimatedFee != null) {
                            Surface(
                                color = LocalAppColors.current.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .clickable { openFeeEditor(estimatedFee ?: 0L) }
                            ) {
                                Text(
                                    text = "fee: ${ChatRepository.formatKas(estimatedFee ?: 0L)} KAS",
                                    color = KaspaTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(LocalAppColors.current.surface)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { chatViewModel.cancelVoiceRecording() }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cancel_recording), tint = Color(0xFFFF3B30))
                            }
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Recording... ${formatRecordingElapsed(voiceRecordingState.elapsedMs)}",
                                color = LocalAppColors.current.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { chatViewModel.stopAndSendVoiceRecording(contactId) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(KaspaTeal, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.send),
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        replyingTo?.let { reply ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .background(LocalAppColors.current.surface, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Replying to ${if (reply.direction == "sent") "yourself" else (conversation?.contact?.alias ?: com.kachat.app.util.KaspaAddress.shortDisplay(contactId))}",
                                        color = KaspaTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        VoiceMessage.parseOrNull(reply.plaintextBody)?.let { "🎤 Audio message" }
                                            ?: ImageMessage.parseOrNull(reply.plaintextBody)?.let { "📷 Photo" }
                                            ?: MessageReply.parseOrNull(reply.plaintextBody)?.text
                                            ?: (reply.plaintextBody ?: ""),
                                        color = LocalAppColors.current.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { chatViewModel.cancelReply() }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_reply), tint = LocalAppColors.current.textSecondary)
                                }
                            }
                        }
                        if (showFeeEstimate && estimatedFee != null && messageText.isNotEmpty()) {
                            Surface(
                                color = LocalAppColors.current.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .clickable { openFeeEditor(estimatedFee ?: 0L) }
                            ) {
                                Text(
                                    text = "fee: ${ChatRepository.formatKas(estimatedFee ?: 0L)} KAS",
                                    color = KaspaTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = messageText,
                                onValueChange = { chatViewModel.setMessageText(it) },
                                placeholder = { Text(stringResource(R.string.message), color = LocalAppColors.current.textSecondary) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = LocalAppColors.current.surface,
                                    unfocusedContainerColor = LocalAppColors.current.surface,
                                    focusedTextColor = LocalAppColors.current.textPrimary,
                                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                                    cursorColor = KaspaTeal,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 4
                            )

                            if (messageText.isEmpty()) {
                                Box(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        // Top edge, not bottom — this button sits near the bottom of the
                                        // screen, so the menu opens upward from just above it instead of
                                        // downward over the message field/tab bar.
                                        composerMenuAnchor = coords.positionInWindow()
                                    }
                                ) {
                                    ChatActionButton(Icons.Default.Add, onClick = { showComposerMenu = true })
                                }
                                if (showComposerMenu) {
                                    CenteredOptionsMenu(onDismissRequest = { showComposerMenu = false }, anchor = composerMenuAnchor) {
                                        PopupMenuRow(Icons.Default.CameraAlt, stringResource(R.string.camera)) {
                                            showComposerMenu = false
                                            startCameraCapture()
                                        }
                                        HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                                        PopupMenuRow(Icons.Default.Image, stringResource(R.string.send_photo_2)) {
                                            showComposerMenu = false
                                            photoPickerLauncher.launch("image/*")
                                        }
                                        HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                                        PopupMenuRow(Icons.Default.Mic, stringResource(R.string.send_audio_message)) {
                                            showComposerMenu = false
                                            startVoiceRecordingIfPermitted()
                                        }
                                        HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                                        PopupMenuRow(painterResource(R.drawable.ic_kaspa_logo), stringResource(R.string.send_kaspa), iconTint = Color.Unspecified) {
                                            showComposerMenu = false
                                            paymentMode = true
                                        }
                                        HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                                        PopupMenuRow(Icons.Default.Apps, stringResource(R.string.play_chess)) {
                                            showComposerMenu = false
                                            chatViewModel.startChessGame(contactId)
                                        }
                                        if (conversation?.contact?.handshakeComplete != true) {
                                            HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                                            PopupMenuRow(Icons.Default.BackHand, stringResource(R.string.send_handshake)) {
                                                showComposerMenu = false
                                                chatViewModel.sendHandshake(contactId)
                                            }
                                        }
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        chatViewModel.sendMessage(contactId, messageText)
                                        chatViewModel.setMessageText("")
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(KaspaTeal, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.send),
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var highlightedMessageId by remember { mutableStateOf<String?>(null) }
        val jumpToReply: (String) -> Unit = { targetId ->
            val index = messages.indexOfFirst { it.id == targetId }
            if (index >= 0) {
                coroutineScope.launch {
                    scrollState.animateScrollToItem(index)
                    highlightedMessageId = targetId
                    delay(1200)
                    if (highlightedMessageId == targetId) highlightedMessageId = null
                }
            } else {
                Toast.makeText(micContext, micContext.getString(R.string.original_message_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        // Auto-scroll to bottom when new messages arrive
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                scrollState.animateScrollToItem(messages.size - 1)
            }
        }

        // Also re-pin to the bottom when the keyboard's own inset animation finishes (not
        // just when messageText changes — the IME resize is system-driven and can complete
        // after Compose's own recomposition, so keying on messageText alone raced with it
        // and left the latest message hidden behind the keyboard without a real scroll-up).
        val imeVisible = WindowInsets.isImeVisible
        LaunchedEffect(imeVisible, messageText.isEmpty()) {
            if (messages.isNotEmpty()) {
                scrollState.animateScrollToItem(messages.size - 1)
            }
        }

        // Not scrollState.canScrollForward — that flips true for a frame or two whenever
        // the viewport merely shrinks (keyboard opening, the fee-estimate row appearing),
        // even though the user never scrolled and is still logically at the latest
        // message. Only show the button once the last message isn't even partially
        // among the visible items — a real, deliberate scroll away from the bottom.
        // Tolerate a 1-item gap — the keyboard/fee-row resize can leave the second-to-last
        // message as the last fully visible one for a moment even when the re-pin effect
        // above hasn't finished animating yet. A genuine scroll-up to read history moves
        // by much more than one item, so this threshold still catches real cases.
        val showScrollToBottom by remember {
            derivedStateOf {
                val lastVisibleIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                lastVisibleIndex != null && lastVisibleIndex < messages.lastIndex - 1
            }
        }

        // Swipe-left-to-reveal-timestamps (iMessage-style) — see the matching implementation in
        // BroadcastScreens.kt for the full rationale; kept in sync with it.
        val revealOffsetPx = remember { Animatable(0f) }
        val maxRevealOffsetPx = with(LocalDensity.current) { 64.dp.toPx() }

        // Computed here (not inside the LazyColumn content below) - LazyListScope's item-builder
        // lambda isn't a real @Composable context, so a bare remember() call in it fails to
        // compile ("@Composable invocations can only happen from the context of a @Composable
        // function").
        val chessSourceMessages = remember(messages) {
            messages.map {
                com.kachat.app.util.ChessGameEngine.SimpleChessSourceMessage(
                    id = it.id,
                    plaintextBody = it.plaintextBody,
                    isOutgoing = it.direction == "sent",
                    blockTimestamp = it.blockTimestamp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            revealOffsetPx.snapTo((revealOffsetPx.value + delta).coerceIn(-maxRevealOffsetPx, 0f))
                        }
                    },
                    onDragStopped = { coroutineScope.launch { revealOffsetPx.animateTo(0f) } }
                )
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.no_messages_yet),
                                color = LocalAppColors.current.textSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                        if (index == 0 || !ChatTimeFormat.isSameDay(messages[index - 1].blockTimestamp, msg.blockTimestamp)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Surface(color = LocalAppColors.current.surface, shape = RoundedCornerShape(12.dp)) {
                                    Text(
                                        ChatTimeFormat.formatDateDivider(msg.blockTimestamp),
                                        color = LocalAppColors.current.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        val chessEnvelopeForRow = remember(msg.plaintextBody) {
                            com.kachat.app.util.ChessMessage.parseOrNull(
                                MessageReply.parseOrNull(msg.plaintextBody)?.text ?: msg.plaintextBody
                            )
                        }
                        val chessSummaryForRow = remember(chessEnvelopeForRow, chessSourceMessages, myAddress) {
                            val address = myAddress
                            if (chessEnvelopeForRow != null && address != null) {
                                com.kachat.app.util.ChessGameEngine.summarize(chessEnvelopeForRow.gameId, chessSourceMessages, address, contactId)
                            } else {
                                null
                            }
                        }
                        val isLatestChessForRow = remember(chessEnvelopeForRow, chessSourceMessages) {
                            chessEnvelopeForRow != null && com.kachat.app.util.ChessGameEngine.isLatestChessMessage(
                                com.kachat.app.util.ChessGameEngine.SimpleChessSourceMessage(
                                    id = msg.id,
                                    plaintextBody = msg.plaintextBody,
                                    isOutgoing = msg.direction == "sent",
                                    blockTimestamp = msg.blockTimestamp
                                ),
                                chessSourceMessages
                            )
                        }
                        MessageBubble(
                            message = msg,
                            chessSummary = chessSummaryForRow,
                            isLatestChessMessage = isLatestChessForRow,
                            onRespondToChessInvite = { accepted ->
                                val gameId = chessEnvelopeForRow?.gameId
                                if (gameId != null) {
                                    chatViewModel.respondToChessInvite(contactId, gameId, accepted)
                                }
                            },
                            onOpenChessGame = { gameId -> navController.navigate("chess_game/$contactId/$gameId") },
                            contactAvatarUrl = conversation?.contact?.knsAvatarUrl,
                            contactAvatarFallback = conversation?.contact?.alias ?: contactId.takeLast(8),
                            myAvatarUrl = myKnsProfile?.avatarUrl,
                            myAvatarFallback = myAddress?.takeLast(8) ?: "",
                            isPendingRequest = msg.type == MessageProtocol.TYPE_HANDSHAKE &&
                                msg.direction == "received" &&
                                conversation?.contact?.conversationStatus == "pending",
                            isHandshakeComplete = conversation?.contact?.conversationStatus == "active",
                            onAccept = { chatViewModel.acceptHandshake(contactId) },
                            onDecline = { chatViewModel.declineHandshake(contactId) },
                            onRetry = { chatViewModel.retrySendMessage(msg) },
                            onReply = { chatViewModel.startReplyTo(msg) },
                            onSavePhoto = savePhotoIfPermitted,
                            revealOffsetPx = revealOffsetPx,
                            maxRevealOffsetPx = maxRevealOffsetPx,
                            photosBlocked = !com.kachat.app.repository.ChatRepository.shouldAutoDisplayPhotos(
                                conversation?.contact
                            ),
                            isPhotoRevealed = msg.id in revealedPhotoTxIds,
                            onRevealPhoto = { chatViewModel.revealPhoto(msg.id) },
                            kaspaExplorer = kaspaExplorer,
                            onJumpToReply = jumpToReply,
                            isHighlighted = msg.id == highlightedMessageId
                        )
                    }
                    if (ChatViewModel.shouldShowUnnotifiedWarning(messages)) {
                        item { UnnotifiedMessageBanner() }
                    }
                }
            }

            if (showScrollToBottom && messages.isNotEmpty()) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { scrollState.animateScrollToItem(messages.size - 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .background(LocalAppColors.current.surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.scroll_to_latest),
                        tint = LocalAppColors.current.textPrimary
                    )
                }
            }
        }
    }

    if (showFeeEditor) {
        AlertDialog(
            onDismissRequest = { showFeeEditor = false },
            containerColor = LocalAppColors.current.surface,
            title = { Text(stringResource(R.string.adjust_network_fee), color = LocalAppColors.current.textPrimary) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.if_the_network_is_busy_a),
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feeEditorInput,
                        onValueChange = { feeEditorInput = it },
                        label = { Text(stringResource(R.string.fee_kas)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LocalAppColors.current.textPrimary,
                            unfocusedTextColor = LocalAppColors.current.textPrimary,
                            focusedBorderColor = KaspaTeal,
                            unfocusedBorderColor = LocalAppColors.current.textSecondary,
                            focusedLabelColor = KaspaTeal,
                            unfocusedLabelColor = LocalAppColors.current.textSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val kas = feeEditorInput.toDoubleOrNull()
                    val currentFeeSompi = estimatedFee ?: 0L
                    if (kas != null && kas > 0 && currentFeeSompi > 0 && effectiveRate > 0) {
                        val impliedMass = currentFeeSompi / effectiveRate
                        val desiredFeeSompi = Math.round(kas * 100_000_000.0)
                        chatViewModel.setFeeRateOverride(kotlin.math.ceil(desiredFeeSompi / impliedMass).toLong())
                    } else {
                        chatViewModel.setFeeRateOverride(null)
                    }
                    showFeeEditor = false
                }) {
                    Text(stringResource(R.string.save), color = KaspaTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { chatViewModel.setFeeRateOverride(null); showFeeEditor = false }) {
                        Text(stringResource(R.string.use_default), color = LocalAppColors.current.textSecondary)
                    }
                    TextButton(onClick = { showFeeEditor = false }) {
                        Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun UnnotifiedMessageBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFF9500).copy(alpha = 0.08f))
            .border(0.5.dp, Color(0xFFFF9500).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFF9500),
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.when_you_message_someone_new_on),
            color = LocalAppColors.current.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** See the truncation check inside [MessageBubble]'s plain-text branch for why this exists. Also used by BroadcastScreens.kt's room bubble. */
const val MESSAGE_TEXT_TRUNCATION_THRESHOLD = 2_000
const val MESSAGE_TEXT_PREVIEW_LENGTH = 500

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    contactAvatarUrl: String? = null,
    contactAvatarFallback: String = "",
    myAvatarUrl: String? = null,
    myAvatarFallback: String = "",
    isPendingRequest: Boolean = false,
    isHandshakeComplete: Boolean = false,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    onRetry: () -> Unit = {},
    onReply: () -> Unit = {},
    onSavePhoto: (ByteArray, String) -> Unit = { _, _ -> },
    revealOffsetPx: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
    maxRevealOffsetPx: Float = 1f,
    photosBlocked: Boolean = false,
    isPhotoRevealed: Boolean = false,
    onRevealPhoto: () -> Unit = {},
    kaspaExplorer: com.kachat.app.models.KaspaExplorer = com.kachat.app.models.KaspaExplorer.default,
    /** Tapping the reply quote (if any) jumps to and highlights the original message. */
    onJumpToReply: (String) -> Unit = {},
    isHighlighted: Boolean = false,
    /** Current game state for this message's chess envelope, if it has one - computed by the
     *  caller (needs the full conversation's messages, which this composable doesn't have). */
    chessSummary: com.kachat.app.util.ChessGameSummary? = null,
    /** True only for the most recent chess message belonging to its game - see
     *  ChessGameEngine.isLatestChessMessage. */
    isLatestChessMessage: Boolean = false,
    onRespondToChessInvite: (Boolean) -> Unit = {},
    onOpenChessGame: (String) -> Unit = {}
) {
    val isSent = message.direction == "sent"
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Offset.Zero) }
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val replyContent = remember(message.plaintextBody) { MessageReply.parseOrNull(message.plaintextBody) }
    val displayBody = replyContent?.text ?: message.plaintextBody
    val imageContent = remember(displayBody) { ImageMessage.parseOrNull(displayBody) }
    val chessEnvelope = remember(displayBody) { com.kachat.app.util.ChessMessage.parseOrNull(displayBody) }

    if (isPendingRequest) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Surface(
                color = LocalAppColors.current.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.request_to_communicate),
                    color = LocalAppColors.current.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Surface(
                color = LocalAppColors.current.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.str_3), fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.contact_has_requested_permission_to_communicate),
                            color = LocalAppColors.current.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.accept), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.decline), color = LocalAppColors.current.textSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    val highlightColor by animateColorAsState(
        if (isHighlighted) KaspaTeal.copy(alpha = 0.18f) else Color.Transparent,
        label = "messageHighlight"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightColor, RoundedCornerShape(12.dp))
    ) {
        Text(
            text = remember(message.blockTimestamp) { ChatTimeFormat.formatMessageTime(message.blockTimestamp) },
            color = LocalAppColors.current.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .alpha((-revealOffsetPx.value / maxRevealOffsetPx).coerceIn(0f, 1f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(revealOffsetPx.value.toInt(), 0) },
            horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
        if (!isSent) {
            ContactAvatar(imageUrl = contactAvatarUrl, fallbackText = contactAvatarFallback, size = 32.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (isSent) Alignment.End else Alignment.Start,
            modifier = Modifier.onGloballyPositioned { coords ->
                menuAnchor = coords.positionInWindow() + Offset(0f, coords.size.height.toFloat())
            }
        ) {
        if (replyContent != null) {
            Surface(
                color = LocalAppColors.current.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .widthIn(max = 240.dp)
                    .clickable { onJumpToReply(replyContent.replyToId) }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        if (replyContent.replyToSender == message.walletAddress) "You" else "Them",
                        color = KaspaTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        replyContent.replyToPreview,
                        color = LocalAppColors.current.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box {
            if (message.type == "pay") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(painterResource(R.drawable.ic_kaspa_logo), null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.payment), color = Color(0xFFF39C12), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = if (isSent) KaspaTeal else LocalAppColors.current.surface,
                    shape = RoundedCornerShape(20.dp),
                    // Same off-screen-avatar risk as the plain text bubble — a long payment memo
                    // needs the same cap.
                    modifier = Modifier.widthIn(max = 280.dp).combinedClickable(onClick = {}, onLongClick = { showMenu = true })
                ) {
                    Text(
                        text = message.plaintextBody ?: "Payment",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (isSent) Color.Black else LocalAppColors.current.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (message.type == MessageProtocol.TYPE_HANDSHAKE) {
                // Matches the iOS bubble for any handshake message once it's not the
                // pending accept/decline card anymore (sent, or an already-accepted
                // received one) — a small pill above a "[Request to communicate]" bubble.
                // A message I sent is always framed as my outreach/response and never
                // changes. Their message only flips to "completed" once the connection
                // is actually live — i.e. once I've received their side of it.
                val showCompleted = !isSent && isHandshakeComplete
                val pillText = if (showCompleted) "🤝 Handshake completed" else "👋 Request to communicate"
                val bodyText = if (showCompleted) "[Handshake completed]" else "[Request to communicate]"
                Column(horizontalAlignment = if (isSent) Alignment.End else Alignment.Start) {
                    Surface(
                        color = LocalAppColors.current.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = pillText,
                            color = LocalAppColors.current.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = if (isSent) KaspaTeal else LocalAppColors.current.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { showMenu = true })
                    ) {
                        Text(
                            text = bodyText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = if (isSent) Color.Black else LocalAppColors.current.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (chessEnvelope != null) {
                ChessBubble(
                    envelope = chessEnvelope,
                    summary = chessSummary,
                    isLatest = isLatestChessMessage,
                    isSent = isSent,
                    messageId = message.id,
                    onRespond = onRespondToChessInvite,
                    onOpen = { onOpenChessGame(chessEnvelope.gameId) },
                    onLongPress = { showMenu = true }
                )
            } else if (VoiceMessage.parseOrNull(displayBody) != null) {
                AudioBubble(
                    voiceContent = VoiceMessage.parseOrNull(displayBody)!!,
                    isSent = isSent,
                    onLongPress = { showMenu = true },
                    onDoubleClick = onReply
                )
            } else if (ImageMessage.parseOrNull(displayBody) != null) {
                ImageBubble(
                    imageContent = ImageMessage.parseOrNull(displayBody)!!,
                    isSent = isSent,
                    onLongPress = { showMenu = true },
                    onDoubleClick = onReply,
                    photosBlocked = !isSent && photosBlocked,
                    senderDisplayName = contactAvatarFallback,
                    isRevealed = isPhotoRevealed,
                    onReveal = onRevealPhoto
                )
            } else {
                val bodyText = displayBody ?: ""

                // Above this, render a truncated tap-to-expand preview instead of laying out the
                // full text inline - matches iMessage's behavior for very long messages, and
                // specifically guards against a huge wall of text (e.g. raw base64 that ended up
                // as plain message content instead of being recognized as a file/image envelope)
                // making the whole chat scroll janky. Checked before running TextLinkify.findUrls
                // below, since scanning a huge string for links is itself wasted work here.
                if (bodyText.length > MESSAGE_TEXT_TRUNCATION_THRESHOLD) {
                    var showFullText by remember { mutableStateOf(false) }
                    Surface(
                        color = if (isSent) KaspaTeal else LocalAppColors.current.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .combinedClickable(
                                onClick = { showFullText = true },
                                onLongClick = { showMenu = true },
                                onDoubleClick = onReply
                            )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                text = bodyText.take(MESSAGE_TEXT_PREVIEW_LENGTH) + "…",
                                color = if (isSent) Color.Black else LocalAppColors.current.textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.show_more),
                                color = if (isSent) LocalAppColors.current.divider else KaspaTeal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    if (showFullText) {
                        FullMessageTextDialog(
                            text = bodyText,
                            onDismiss = { showFullText = false },
                            onCopy = { clipboardManager.setText(AnnotatedString(bodyText)) }
                        )
                    }
                } else {
                    var textLayoutResult by remember(bodyText) { mutableStateOf<TextLayoutResult?>(null) }
                    // Sent bubbles are teal (matching broadcast rooms' sent-message color) with black
                    // text/links for contrast — a teal link on a teal background would be unreadable.
                    val linkColor = if (isSent) Color.Black else KaspaTeal
                    val annotatedBody = remember(bodyText, isSent) {
                        buildAnnotatedString {
                            append(bodyText)
                            for (match in TextLinkify.findUrls(bodyText)) {
                                addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
                                addStringAnnotation("URL", match.uri, match.range.first, match.range.last + 1)
                            }
                        }
                    }
                    Surface(
                        color = if (isSent) KaspaTeal else LocalAppColors.current.surface,
                        shape = RoundedCornerShape(20.dp),
                        // Without a cap, a long message claims the outer Row's full width before the
                        // avatar sibling ever gets measured, pushing the avatar off-screen entirely —
                        // matches the same 280.dp cap broadcast rooms' equivalent bubble already uses.
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = annotatedBody,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .pointerInput(annotatedBody) {
                                    detectTapGestures(
                                        onLongPress = { showMenu = true },
                                        onDoubleTap = { onReply() },
                                        onTap = { offset ->
                                            val layout = textLayoutResult ?: return@detectTapGestures
                                            val charOffset = layout.getOffsetForPosition(offset)
                                            annotatedBody.getStringAnnotations("URL", charOffset, charOffset)
                                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                                        }
                                    )
                                },
                            onTextLayout = { textLayoutResult = it },
                            color = if (isSent) Color.Black else LocalAppColors.current.textPrimary
                        )
                    }
                    TextLinkify.findUrls(bodyText).firstOrNull()?.let { match ->
                        LinkPreviewCard(url = match.uri, txId = message.id, kaspaExplorer = kaspaExplorer)
                    }
                }
            }

            if (showMenu) {
                CenteredOptionsMenu(onDismissRequest = { showMenu = false }, anchor = menuAnchor) {
                    PopupMenuRow(Icons.Default.ContentCopy, stringResource(R.string.copy_message)) {
                        clipboardManager.setText(AnnotatedString(displayBody ?: ""))
                        showMenu = false
                    }
                    HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                    PopupMenuRow(Icons.Default.Tag, stringResource(R.string.go_to_explorer)) {
                        uriHandler.openUri(kaspaExplorer.txUrl(message.id))
                        showMenu = false
                    }
                    if (imageContent != null) {
                        HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                        PopupMenuRow(Icons.Default.Download, stringResource(R.string.save_photo)) {
                            try {
                                val bytes = android.util.Base64.decode(ImageMessage.base64Payload(imageContent), android.util.Base64.DEFAULT)
                                onSavePhoto(bytes, "kachat_${message.id}.jpg")
                            } catch (e: Exception) {
                                android.util.Log.e("MessageBubble", "Could not decode photo for saving", e)
                            }
                            showMenu = false
                        }
                    }
                    if (ChatViewModel.shouldShowRetryOption(message)) {
                        HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                        PopupMenuRow(Icons.Default.Refresh, stringResource(R.string.retry_send)) {
                            onRetry()
                            showMenu = false
                        }
                    }
                }
            }
        }

        if (isSent) {
            Row(modifier = Modifier.padding(top = 4.dp)) {
                when (message.deliveryStatus) {
                    "failed" -> Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(R.string.failed_to_send),
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(12.dp)
                    )
                    "pending" -> Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = stringResource(R.string.sending),
                        tint = LocalAppColors.current.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CD964),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        }
        if (isSent) {
            Spacer(Modifier.width(8.dp))
            ContactAvatar(imageUrl = myAvatarUrl, fallbackText = myAvatarFallback, size = 32.dp)
        }
        }
    }
}

/**
 * Full text of a message too long to render inline (see [MESSAGE_TEXT_TRUNCATION_THRESHOLD]) - a
 * full-screen scrollable, selectable text view, matching iMessage's "tap to see more" behavior.
 * Shared with BroadcastScreens.kt's room bubble, not just private-chat messages.
 */
@Composable
fun FullMessageTextDialog(text: String, onDismiss: () -> Unit, onCopy: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.message), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy), tint = KaspaTeal)
                }
            }
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    Text(text, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/**
 * A voice message bubble — decodes the embedded base64 audio to a temp file once, then plays it
 * with [android.media.MediaPlayer] (which decodes WebM/Opus natively, no manual PCM handling
 * needed for playback). No waveform visualization — the [VoiceMessageContent] format doesn't
 * carry sample data, and re-decoding to PCM just to draw bars isn't worth the extra native-audio
 * surface area for a cosmetic detail; play/pause + duration covers the actual "does it work" bar.
 */
/**
 * "Play Chess" 1:1 feature - dispatches to an invite card (Accept/Decline, mirroring the
 * handshake pending-request card above), a live status card (board thumbnail + status, for the
 * most recent chess message in its game), or a compact one-line log entry (earlier moves in the
 * same game, so a long game doesn't repeat a full board on every message).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChessBubble(
    envelope: com.kachat.app.util.ChessEnvelope,
    summary: com.kachat.app.util.ChessGameSummary?,
    isLatest: Boolean,
    isSent: Boolean,
    messageId: String,
    onRespond: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    when (envelope) {
        is com.kachat.app.util.ChessEnvelope.Invite -> ChessInviteBubble(isSent, summary, onRespond, onOpen, onLongPress)
        else -> {
            if (isLatest && summary != null) {
                ChessLiveCard(summary, onOpen, onLongPress)
            } else {
                ChessLogEntry(envelope, summary, messageId, onOpen, onLongPress)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChessInviteBubble(
    isSent: Boolean,
    summary: com.kachat.app.util.ChessGameSummary?,
    onRespond: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val showsResponseButtons = !isSent && summary?.status?.kind == com.kachat.app.util.ChessGameStatusKind.PENDING_RESPONSE
    Surface(
        color = LocalAppColors.current.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(max = 280.dp)
            .then(
                if (!showsResponseButtons) {
                    Modifier.combinedClickable(onClick = onOpen, onLongClick = onLongPress)
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.str_2), fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSent) "Chess game invite sent" else "Invited you to a game of chess",
                    color = LocalAppColors.current.textPrimary,
                    fontSize = 14.sp
                )
            }
            if (showsResponseButtons) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onRespond(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.accept), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onRespond(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3C)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.decline), color = LocalAppColors.current.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (summary != null) {
                Spacer(Modifier.height(4.dp))
                Text(summary.statusText, color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChessLiveCard(summary: com.kachat.app.util.ChessGameSummary, onOpen: () -> Unit, onLongPress: () -> Unit = {}) {
    Surface(
        color = LocalAppColors.current.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = onLongPress)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ChessBoardThumbnail(board = summary.board)
            Spacer(Modifier.height(8.dp))
            Text(
                summary.statusText,
                color = if (summary.status.isGameOver) LocalAppColors.current.textSecondary else LocalAppColors.current.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChessLogEntry(
    envelope: com.kachat.app.util.ChessEnvelope,
    summary: com.kachat.app.util.ChessGameSummary?,
    messageId: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    // A plain Unicode glyph character embedded in text renders in the ambient text color (see
    // the see-through-white-pieces fix on ChessPiece.glyph), so it can't convey white vs black on
    // its own - a move's piece is rendered via a real ChessPieceGlyph composable (fill/outline
    // colored by piece.color) placed next to the text instead.
    val record = if (envelope is com.kachat.app.util.ChessEnvelope.Move) {
        summary?.moveHistory?.firstOrNull { it.messageId == messageId }
    } else {
        null
    }
    val text = when (envelope) {
        is com.kachat.app.util.ChessEnvelope.Move -> {
            val promo = envelope.content.promotion?.let { " (${it.uppercase()})" } ?: ""
            "${envelope.content.from} → ${envelope.content.to}$promo"
        }
        is com.kachat.app.util.ChessEnvelope.Resign -> "Resigned"
        is com.kachat.app.util.ChessEnvelope.Response -> if (envelope.content.accepted) "Accepted the game" else "Declined the game"
        is com.kachat.app.util.ChessEnvelope.Invite -> "Chess invite"
    }
    Surface(
        color = LocalAppColors.current.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = onLongPress)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (record != null) {
                ChessPieceGlyph(
                    com.kachat.app.util.ChessPiece(record.pieceType, record.color),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(text, color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
        }
    }
}

/** Small, non-interactive board render - shared by the in-chat live card and (with its own
 *  square size) the full-screen ChessGameScreen. */
@Composable
fun ChessBoardThumbnail(board: com.kachat.app.util.ChessBoard, sizeDp: Dp = 160.dp) {
    val squareSize = sizeDp / 8
    Column {
        for (rank in 7 downTo 0) {
            Row {
                for (file in 0..7) {
                    val isLight = (file + rank) % 2 != 0
                    Box(
                        modifier = Modifier
                            .size(squareSize)
                            .background(if (isLight) ChessLightSquareColor else ChessDarkSquareColor),
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = board.piece(com.kachat.app.util.ChessSquare(file, rank))
                        if (piece != null) {
                            ChessPieceGlyph(piece, fontSize = (squareSize.value * 0.6f).sp)
                        }
                    }
                }
            }
        }
    }
}

/** Classic wood-tone board colors (matches chess.com/lichess's default theme) - shared by the
 *  in-chat thumbnail and the full-screen board in ChessGameScreen.kt. */
val ChessLightSquareColor = Color(0xFFEFD9B4)
val ChessDarkSquareColor = Color(0xFFB58863)

/** Renders a single chess piece glyph with an explicit white/black fill plus a crisp
 *  contrasting outline (four offset copies of the glyph drawn behind the fill, a standard
 *  lightweight text-stroke trick), rather than relying on the bare Unicode glyph's
 *  outline-vs-filled shape alone to distinguish sides - at typical board sizes that distinction
 *  was too subtle to read at a glance, especially on a same-toned square. */
@Composable
fun ChessPieceGlyph(piece: com.kachat.app.util.ChessPiece, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    val fillColor = if (piece.color == com.kachat.app.util.ChessColor.WHITE) Color(0xFFFCFCFC) else Color(0xFF121212)
    val outlineColor = if (piece.color == com.kachat.app.util.ChessColor.WHITE) Color(0xFF121212) else Color(0xFFFCFCFC)
    val d = 0.9.dp
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(piece.glyph, fontSize = fontSize, color = outlineColor, modifier = Modifier.offset(x = d))
        Text(piece.glyph, fontSize = fontSize, color = outlineColor, modifier = Modifier.offset(x = -d))
        Text(piece.glyph, fontSize = fontSize, color = outlineColor, modifier = Modifier.offset(y = d))
        Text(piece.glyph, fontSize = fontSize, color = outlineColor, modifier = Modifier.offset(y = -d))
        Text(piece.glyph, fontSize = fontSize, color = fillColor)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioBubble(voiceContent: VoiceMessageContent, isSent: Boolean, onLongPress: () -> Unit, onDoubleClick: () -> Unit = {}) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0) }
    var isReady by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    DisposableEffect(voiceContent.content) {
        var tempFile: java.io.File? = null
        try {
            val bytes = android.util.Base64.decode(VoiceMessage.base64Payload(voiceContent), android.util.Base64.DEFAULT)
            val file = java.io.File(context.cacheDir, "voice_playback_${System.nanoTime()}.webm")
            file.writeBytes(bytes)
            tempFile = file
            val player = android.media.MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener {
                durationMs = it.duration
                isReady = true
            }
            player.setOnCompletionListener { isPlaying = false }
            player.prepareAsync()
            mediaPlayer = player
        } catch (e: Exception) {
            android.util.Log.e("AudioBubble", "Could not prepare voice message for playback", e)
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            tempFile?.delete()
        }
    }

    Surface(
        color = if (isSent) LocalAppColors.current.surfaceVariant else LocalAppColors.current.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress, onDoubleClick = onDoubleClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .widthIn(min = 150.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = isReady,
                onClick = {
                    val player = mediaPlayer ?: return@IconButton
                    if (isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        if (player.currentPosition >= player.duration) player.seekTo(0)
                        player.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isReady) KaspaTeal else Color.Gray
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isReady) VoiceMessage.formatDuration(durationMs) else "...",
                color = LocalAppColors.current.textPrimary,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Small in-memory cache of already-decoded chat-photo bitmaps, keyed by the message's raw base64
 * content string - avoids re-decoding on every recomposition/scroll-back-into-view for a photo
 * that's already been shown once. Sized like iOS's equivalent `thumbnailCache` (24MB).
 */
private val incomingPhotoBitmapCache = object : android.util.LruCache<String, android.graphics.Bitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap) = value.byteCount
}

/**
 * Bounds how many photo decodes run at once. Without this, opening a chat with many photos -
 * especially scrolling straight to the bottom of a long history, as happens when a notification
 * tap opens straight into a photo-heavy chat - decoded every visible bubble's bitmap at once. Each
 * decode is cheap individually, but a burst of them can still contend with the main thread's own
 * layout/scroll work for CPU time on a busy launch. Matches iOS's `ImageDecodeLimiter`.
 */
private val incomingPhotoDecodeLimiter = Semaphore(3)

/**
 * Decodes an incoming photo's raw bytes, trying [ImageDecoder] as a fallback if [BitmapFactory]
 * returns null. Both platforms only ever send plain JPEG chat photos now (see
 * `ImagePrep.prepareForChatMessage`'s doc comment for why AVIF was tried and removed), so this is
 * mostly future-proofing/defense-in-depth for any other format that could reach this decode path -
 * `ImageDecoder` has occasionally succeeded where `BitmapFactory` fails on the same bytes on some
 * OEM builds.
 */
private fun decodeIncomingPhotoBitmap(bytes: ByteArray): android.graphics.Bitmap? {
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return try {
        val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
        android.graphics.ImageDecoder.decodeBitmap(source)
    } catch (e: Exception) {
        null
    }
}

/**
 * A photo message bubble — decodes the embedded base64 image to a [Bitmap] once, renders it inline
 * capped to a max width, and opens a tap-to-dismiss full-screen viewer on tap. Same interaction
 * contract as [AudioBubble] (long-press for the context menu, double-click to reply).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBubble(
    imageContent: VoiceMessageContent,
    isSent: Boolean,
    onLongPress: () -> Unit,
    onDoubleClick: () -> Unit = {},
    photosBlocked: Boolean = false,
    senderDisplayName: String = "",
    isRevealed: Boolean = false,
    onReveal: () -> Unit = {}
) {
    // Hidden behind a manual reveal - mirrors iOS's LazyImageBubble.hiddenBubble. Skips decoding
    // the bitmap entirely until revealed, matching the "don't auto-render" intent, not just
    // "don't show" - see ChatRepository.shouldAutoDisplayPhotos.
    if (photosBlocked && !isRevealed) {
        Surface(
            color = LocalAppColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 220.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).widthIn(min = 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.VisibilityOff, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "$senderDisplayName sent a photo",
                    color = LocalAppColors.current.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onReveal,
                    colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.show_photo), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var showFullScreen by remember { mutableStateOf(false) }
    val cachedBitmap = remember(imageContent.content) { incomingPhotoBitmapCache.get(imageContent.content) }
    var isDecoding by remember(imageContent.content) { mutableStateOf(cachedBitmap == null) }
    // Decoded off the main thread and rate-limited (see incomingPhotoDecodeLimiter's doc comment)
    // - this used to be a plain `remember(imageContent.content) { ... }`, which runs its block
    // synchronously during composition on the main thread. Opening a chat with many photos at
    // once (e.g. scrolling straight to the bottom via a notification tap) decoded every visible
    // bubble's bitmap inline, one after another, blocking Compose's own layout/draw pass for as
    // long as all of them took combined - a guaranteed freeze, not just a contention risk.
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = cachedBitmap, key1 = imageContent.content) {
        if (value != null) {
            isDecoding = false
            return@produceState
        }
        value = incomingPhotoDecodeLimiter.withPermit {
            withContext(Dispatchers.Default) {
                incomingPhotoBitmapCache.get(imageContent.content)?.let { return@withContext it }
                try {
                    val bytes = android.util.Base64.decode(ImageMessage.base64Payload(imageContent), android.util.Base64.DEFAULT)
                    decodeIncomingPhotoBitmap(bytes)?.also {
                        incomingPhotoBitmapCache.put(imageContent.content, it)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImageBubble", "Could not decode photo message", e)
                    null
                }
            }
        }
        isDecoding = false
    }

    if (isDecoding) {
        Surface(
            color = if (isSent) LocalAppColors.current.surfaceVariant else LocalAppColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(width = 220.dp, height = 160.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KaspaTeal, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }
        return
    }

    // A plain local val snapshot — `bitmap` itself is a delegated property (`by produceState`),
    // so Kotlin can't smart-cast it to non-null below just from the `== null` check on this line;
    // each read of a delegated property calls its getValue() again, and the compiler can't prove
    // it won't return a different (possibly null) value between the check and later reads.
    val resolvedBitmap = bitmap
    if (resolvedBitmap == null) {
        Surface(
            color = if (isSent) LocalAppColors.current.surfaceVariant else LocalAppColors.current.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress, onDoubleClick = onDoubleClick)
        ) {
            Text(
                text = stringResource(R.string.photo_unavailable),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (isSent) Color.Black else LocalAppColors.current.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(max = 220.dp)
            .combinedClickable(onClick = { showFullScreen = true }, onLongClick = onLongPress, onDoubleClick = onDoubleClick)
    ) {
        Image(
            bitmap = resolvedBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.photo_message),
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth
        )
    }

    if (showFullScreen) {
        Dialog(onDismissRequest = { showFullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalAppColors.current.background)
                    .clickable { showFullScreen = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = resolvedBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.photo_message_full_screen),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ChatActionButton(icon: ImageVector, onClick: () -> Unit = {}) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(LocalAppColors.current.surface, CircleShape)
    ) {
        Icon(icon, null, tint = KaspaTeal, modifier = Modifier.size(20.dp))
    }
}

/** "0:07" — the composer's live recording timer, capped display-wise the same way the recording itself is capped at 10s. */
fun formatRecordingElapsed(elapsedMs: Long): String = VoiceMessage.formatDuration(elapsedMs.toInt())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.contacts)) }) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.contact_list_phase_5), color = KaspaSubtext)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: WalletViewModel,
    navController: NavController,
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    portfolioViewModel: com.kachat.app.viewmodels.PortfolioViewModel = hiltViewModel()
) {
    val address by viewModel.address.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val balance by viewModel.fullBalance.collectAsState()
    val showSetupGuides by viewModel.showSetupGuides.collectAsState()
    val dotColorHex by connectionViewModel.dotColorHex.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val spendingAddress by viewModel.spendingAddress.collectAsState()
    val spendingBalance by viewModel.spendingBalance.collectAsState()
    val manageAddresses by viewModel.manageAddresses.collectAsState()
    val primarySpendingEntry = manageAddresses.firstOrNull { it.isCurrent }
    var showFundIdentityQr by remember { mutableStateOf(false) }
    // Its own state (not shared with any other QR overlay) so its bigger green border can't
    // accidentally affect an unrelated overlay reusing the same flag.
    var showAcceptPaymentQr by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showSpendingWithdrawDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val knsInscribeState by viewModel.knsInscribeState.collectAsState()
    val pendingKnsCommit by viewModel.pendingKnsCommit.collectAsState()

    val profileAssetId by viewModel.profileDomainAssetId.collectAsState()
    val knsProfile by viewModel.knsProfile.collectAsState()
    val activeProfileDomainName = viewModel.activeProfileDomainName.collectAsState().value
    val hasAnyProfileData = knsProfile != null && listOf(
        knsProfile?.bio, knsProfile?.x, knsProfile?.website, knsProfile?.telegram,
        knsProfile?.discord, knsProfile?.contactEmail, knsProfile?.github, knsProfile?.redirectUrl
    ).any { !it.isNullOrBlank() }

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
        viewModel.refreshOwnedDomains()
        viewModel.refreshSpendingAddress()
        viewModel.loadManageAddresses()
    }

    val pullRefreshState = rememberPullToRefreshState()
    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.refreshBalanceAndAwait()
            viewModel.refreshSpendingBalanceAndAwait()
            pullRefreshState.endRefresh()
        }
    }

    // Re-tapping the Profile tab while already on it (e.g. to back out of a full-screen QR
    // overlay) doesn't re-navigate/recompose this screen — see WalletViewModel.notifyTabReselected.
    val tabReselectSignal by viewModel.tabReselectSignal.collectAsState()
    LaunchedEffect(tabReselectSignal) {
        if (tabReselectSignal.second == "profile") {
            showFundIdentityQr = false
            showAcceptPaymentQr = false
            showWithdrawDialog = false
        }
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(LocalAppColors.current.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                TopStatusBar(
                    balance = balance,
                    onStatusClick = { navController.navigate("connection_status") },
                    dotColorHex = dotColorHex,
                    showAddButton = false,
                    showSettingsButton = true,
                    onSettingsClick = { navController.navigate("settings") }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                // Still scrollable as a safety net (smaller phones, larger system font scale),
                // but every element below is sized to comfortably fit a typical phone screen
                // without needing it.
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (pendingKnsCommit != null) {
                SettingsSection(title = stringResource(R.string.unfinished_inscription)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.a_kns_inscription_s_commit_transaction),
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.retryPendingKnsReveal() },
                            colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal),
                            enabled = knsInscribeState.status != WalletViewModel.KnsInscribeUiStatus.SUBMITTING_REVEAL
                        ) {
                            Text(stringResource(R.string.retry_inscription_reveal), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!accountName.isNullOrBlank()) {
                SettingsSection(title = stringResource(R.string.account)) {
                    Text(
                        accountName!!,
                        color = LocalAppColors.current.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.kns_profile)) {
                if (profileAssetId == null || activeProfileDomainName == null) {
                    // No domain yet - nothing to show an avatar/chevron-row for (there's no
                    // profile data at all), so this collapses to a single centered call-to-action
                    // that opens the guided creation wizard instead of the editor built for
                    // *existing* profiles.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("create_kns_profile") }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.create_kns_profile),
                            color = KaspaTeal,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    val clipboardManager = LocalClipboardManager.current
                    Column(
                        modifier = Modifier.clickable { navController.navigate("edit_kns_profile") }
                    ) {
                        val bannerUrl = knsProfile?.bannerUrl?.takeIf { it.isNotBlank() }
                        if (bannerUrl != null) {
                            SubcomposeAsyncImage(
                                model = bannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(LocalAppColors.current.surface)
                            )
                        }
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ContactAvatar(
                                imageUrl = knsProfile?.avatarUrl,
                                fallbackText = activeProfileDomainName,
                                size = 48.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(activeProfileDomainName, color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                val bio = knsProfile?.bio?.takeIf { it.isNotBlank() }
                                if (bio != null) {
                                    Text(
                                        text = bio,
                                        color = LocalAppColors.current.textPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(bio)) }
                                    )
                                } else {
                                    Text(
                                        text = if (hasAnyProfileData) "On-chain profile data available." else "No on-chain profile data yet.",
                                        color = LocalAppColors.current.textSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(20.dp))
                        }
                        val knsProfileSnapshot = knsProfile
                        val hasMoreInfo = knsProfileSnapshot != null && listOf(
                            knsProfileSnapshot.bio, knsProfileSnapshot.x, knsProfileSnapshot.website, knsProfileSnapshot.telegram,
                            knsProfileSnapshot.discord, knsProfileSnapshot.contactEmail, knsProfileSnapshot.github
                        ).any { !it.isNullOrBlank() }
                        if (hasMoreInfo) {
                            HorizontalDivider(color = LocalAppColors.current.divider)
                        }
                        var moreInfoExpanded by remember { mutableStateOf(false) }
                        if (hasMoreInfo) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { moreInfoExpanded = !moreInfoExpanded }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.more_info), color = KaspaTeal, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(
                                    if (moreInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (moreInfoExpanded) "Collapse" else "Expand",
                                    tint = KaspaTeal
                                )
                            }
                        }
                        if (moreInfoExpanded) {
                            HorizontalDivider(color = LocalAppColors.current.divider)
                            val context = LocalContext.current
                            Column(modifier = Modifier.padding(16.dp)) {

                                val socialLinks = listOfNotNull(
                                    knsProfile?.x?.takeIf { it.isNotBlank() }?.let { "X" to it },
                                    knsProfile?.website?.takeIf { it.isNotBlank() }?.let { "Website" to it },
                                    knsProfile?.telegram?.takeIf { it.isNotBlank() }?.let { "Telegram" to it },
                                    knsProfile?.discord?.takeIf { it.isNotBlank() }?.let { "Discord" to it },
                                    knsProfile?.contactEmail?.takeIf { it.isNotBlank() }?.let { "Email" to it },
                                    knsProfile?.github?.takeIf { it.isNotBlank() }?.let { "GitHub" to it }
                                )
                                socialLinks.forEachIndexed { index, (label, value) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val url = if (value.startsWith("http")) value else "https://$value"
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                } catch (e: Exception) { /* no browser available */ }
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(label, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                                        Text(value, color = KaspaTeal, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (index < socialLinks.lastIndex) {
                                        HorizontalDivider(color = LocalAppColors.current.divider)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showSetupGuides) {
                SettingsSection(title = stringResource(R.string.welcome_guide)) {
                    SettingsNavigationItem(stringResource(R.string.welcome_guide), Icons.Default.WavingHand, onClick = {
                        navController.navigate("welcome_guide")
                    })
                }
            }

            run {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    ProfileCircleAction(
                        icon = Icons.Default.QrCode,
                        label = stringResource(R.string.receive_kaspa),
                        modifier = Modifier.weight(1f)
                    ) {
                        showAcceptPaymentQr = true
                    }
                    ProfileCircleAction(
                        icon = Icons.Default.QrCode,
                        label = "Chatting Address",
                        modifier = Modifier.weight(1f)
                    ) {
                        showFundIdentityQr = true
                    }
                }
            }

            val chattingAddressClipboardManager = LocalClipboardManager.current
            CollapsibleAddressSection(title = "Chatting Address", balance = balance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                address?.let { chattingAddressClipboardManager.setText(AnnotatedString(it)) }
                                Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.copy_address), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = LocalAppColors.current.divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWithdrawDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.withdraw_kaspa), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                    if (address != null) {
                        HorizontalDivider(color = LocalAppColors.current.divider)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("cold_storage_tx_history/$address") }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Receipt, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.transaction_history), color = KaspaTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Separate from the identity address above, purely for payment privacy — "Pay in
            // Kaspa" sends always come out of this address, never the identity one above. It
            // rotates to a freshly derived address after every send (see WalletManager's
            // spending-address doc comment), so this always shows whichever one is current.
            val spendingAddressClipboardManager = LocalClipboardManager.current
            CollapsibleAddressSection(title = "Spending Address", balance = spendingBalance) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                spendingAddress?.let { spendingAddressClipboardManager.setText(AnnotatedString(it)) }
                                Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.copy_address), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = LocalAppColors.current.divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSpendingWithdrawDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.send_kaspa), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = LocalAppColors.current.divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("manage_addresses") }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.manage_addresses), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom-most section on Profile - merges what used to be a separate "Info" section
            // (just "Created") with Settings' old "About" section (Version/Website/Support
            // Email/Donate), now reached without needing to open Settings at all.
            SettingsSection(title = stringResource(R.string.about)) {
                SettingsInfoItem(stringResource(R.string.created), "Apr 22, 2026 at 8:33 AM")
                SettingsDivider()
                SettingsInfoItem(stringResource(R.string.version), com.kachat.app.BuildConfig.VERSION_NAME)
                SettingsDivider()
                SettingsInfoItem(
                    stringResource(R.string.website),
                    "https://linktr.ee/Kachat_",
                    KaspaTeal,
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://linktr.ee/Kachat_")))
                        } catch (e: Exception) { /* no browser available */ }
                    }
                )
                SettingsDivider()
                SettingsInfoItem(
                    stringResource(R.string.support_email),
                    "kaspasilver@gmail.com",
                    KaspaTeal,
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:kaspasilver@gmail.com")))
                        } catch (e: Exception) { /* no email app available */ }
                    }
                )
                SettingsDivider()
                SettingsInfoItem(
                    stringResource(R.string.donate),
                    ChatViewModel.DONATION_KNS_DOMAIN,
                    KaspaTeal,
                    onClick = {
                        chatViewModel.startDonationChat(
                            onResolved = { donateAddress -> navController.navigate("chat/$donateAddress?paymentMode=true") },
                            onError = {
                                Toast.makeText(context, "Couldn't reach ${ChatViewModel.DONATION_KNS_DOMAIN} right now. Try again later", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }

            // Moved here from Settings > Actions - Profile is where the rest of the
            // account-level actions (address management, About) already live.
            SettingsSection(title = stringResource(R.string.actions)) {
                SettingsActionItem(stringResource(R.string.log_out), Icons.AutoMirrored.Filled.Logout, Color.Red) {
                    showLogoutConfirmation = true
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (showFundIdentityQr) {
            QrCodeOverlay(
                value = address ?: "",
                onDismiss = { showFundIdentityQr = false },
                message = "Just send 5-10 KAS at a time, that's plenty to cover chat fees for a while (about 500 messages per KAS)",
                borderColor = KaspaTeal,
                borderWidth = 4.dp
            )
        }
        if (showAcceptPaymentQr) {
            QrCodeOverlay(
                value = spendingAddress ?: "",
                onDismiss = { showAcceptPaymentQr = false },
                message = "Only accept Kaspa you intend to use as money to this address.",
                borderColor = KaspaTeal,
                borderWidth = 4.dp
            )
        }
        }
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            containerColor = LocalAppColors.current.surface,
            title = { Text(stringResource(R.string.log_out), color = LocalAppColors.current.textPrimary) },
            text = {
                Text(
                    stringResource(R.string.this_signs_out_of_your_account),
                    color = LocalAppColors.current.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmation = false
                    viewModel.logout()
                }) {
                    Text(stringResource(R.string.log_out), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                }
            }
        )
    }

    if (showWithdrawDialog) {
        var recipientInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }
        var feeRateOverrideSompi by remember { mutableStateOf<Long?>(null) }
        var showFeeEditor by remember { mutableStateOf(false) }
        var feeEditorInput by remember { mutableStateOf("") }
        val isSending by viewModel.isSending.collectAsState()
        val sendResult by viewModel.sendResult.collectAsState()
        val identityBalanceSompi by viewModel.balanceSompi.collectAsState()
        val fiatPriceInCurrency by portfolioViewModel.currentPriceUsd.collectAsState()
        val fiatCurrencyCode by portfolioViewModel.currency.collectAsState()
        val fiatAmountState = com.kachat.app.util.rememberKaspaFiatAmountState(onKasTextChange = { amountInput = it })
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current

        val estimatedMass = remember {
            com.kachat.app.util.KaspaMass.calculateMass(numInputs = 1, outputScriptLens = listOf(34, 34), payloadSize = 0)
        }
        val defaultFeeSompi = com.kachat.app.util.KaspaMass.calculateFee(estimatedMass, com.kachat.app.util.KaspaMass.MINIMUM_FEE_RATE_SOMPI_PER_GRAM)
        val effectiveFeeSompi = feeRateOverrideSompi?.let { com.kachat.app.util.KaspaMass.calculateFee(estimatedMass, it) } ?: defaultFeeSompi

        LaunchedEffect(sendResult) {
            val result = sendResult ?: return@LaunchedEffect
            if (result.isSuccess) {
                Toast.makeText(context, context.getString(R.string.withdrawal_sent), Toast.LENGTH_SHORT).show()
                showWithdrawDialog = false
            } else {
                Toast.makeText(context, result.exceptionOrNull()?.message ?: context.getString(R.string.withdrawal_failed), Toast.LENGTH_SHORT).show()
            }
            viewModel.clearSendResult()
        }

        if (showScanner) {
            BackHandler { showScanner = false }
            QrScannerOverlay(
                onScanned = { scanned -> recipientInput = scanned.trim(); showScanner = false },
                onDismiss = { showScanner = false }
            )
            return
        }

        val amountSompi = amountInput.toDoubleOrNull()?.let { Math.round(it * 100_000_000.0) }
        val isValidAddress = remember(recipientInput) { KaspaAddress.isValid(recipientInput) }

        AlertDialog(
            onDismissRequest = { if (!isSending) showWithdrawDialog = false },
            containerColor = LocalAppColors.current.surface,
            title = { Text(stringResource(R.string.withdraw_kaspa), color = LocalAppColors.current.textPrimary) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.sends_kas_out_of_your_identity),
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text(stringResource(R.string.recipient_address)) },
                        singleLine = true,
                        enabled = !isSending,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LocalAppColors.current.textPrimary,
                            unfocusedTextColor = LocalAppColors.current.textPrimary,
                            focusedBorderColor = KaspaTeal,
                            unfocusedBorderColor = LocalAppColors.current.textSecondary,
                            focusedLabelColor = KaspaTeal,
                            unfocusedLabelColor = LocalAppColors.current.textSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { clipboardManager.getText()?.text?.let { recipientInput = it.trim() } },
                            enabled = !isSending
                        ) {
                            Text(stringResource(R.string.paste_from_clipboard), color = KaspaTeal, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(
                            onClick = { showScanner = true },
                            enabled = !isSending
                        ) {
                            Text(stringResource(R.string.scan_qr_code), color = KaspaTeal, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = fiatAmountState.displayText,
                        onValueChange = { fiatAmountState.onDisplayTextChange(it, fiatPriceInCurrency) },
                        label = { Text(if (fiatAmountState.isFiatMode) fiatCurrencyCode.uppercase() else stringResource(R.string.amount_kas)) },
                        singleLine = true,
                        enabled = !isSending,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                fiatAmountState.conversionLabelText(fiatPriceInCurrency, fiatCurrencyCode)?.let { label ->
                                    Text(
                                        label,
                                        color = LocalAppColors.current.textSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .clickable(enabled = !isSending) { fiatAmountState.toggleMode(fiatPriceInCurrency) }
                                            .padding(end = 8.dp)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val maxSompi = (identityBalanceSompi - effectiveFeeSompi).coerceAtLeast(0L)
                                        fiatAmountState.setMaxKas(maxSompi / 100_000_000.0, fiatPriceInCurrency)
                                    },
                                    enabled = !isSending
                                ) {
                                    Text(stringResource(R.string.max), color = KaspaTeal)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LocalAppColors.current.textPrimary,
                            unfocusedTextColor = LocalAppColors.current.textPrimary,
                            focusedBorderColor = KaspaTeal,
                            unfocusedBorderColor = LocalAppColors.current.textSecondary,
                            focusedLabelColor = KaspaTeal,
                            unfocusedLabelColor = LocalAppColors.current.textSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Available: $balance", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "·  Fee: %.8f KAS".format(java.util.Locale.US, effectiveFeeSompi / 100_000_000.0),
                            color = KaspaTeal,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            modifier = Modifier.clickable(enabled = !isSending) {
                                feeEditorInput = "%.8f".format(java.util.Locale.US, effectiveFeeSompi / 100_000_000.0)
                                showFeeEditor = true
                            }
                        )
                    }
                    if (isSending) {
                        Spacer(Modifier.height(12.dp))
                        InscribeProgressRow(stringResource(R.string.sending_2))
                    }
                }
            },
            confirmButton = {
                val canSend = !isSending && isValidAddress && (amountSompi ?: 0) > 0
                TextButton(
                    onClick = { amountSompi?.let { viewModel.onSendClicked(recipientInput.trim(), it, feeRateOverrideSompi) } },
                    enabled = canSend
                ) {
                    Text(stringResource(R.string.withdraw), color = if (canSend) KaspaTeal else Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSending) {
                    TextButton(onClick = { showWithdrawDialog = false }) {
                        Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                    }
                }
            }
        )

        if (showFeeEditor) {
            AlertDialog(
                onDismissRequest = { showFeeEditor = false },
                containerColor = LocalAppColors.current.surface,
                title = { Text(stringResource(R.string.adjust_network_fee), color = LocalAppColors.current.textPrimary) },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.if_the_network_is_busy_a),
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = feeEditorInput,
                            onValueChange = { feeEditorInput = it },
                            label = { Text(stringResource(R.string.fee_kas)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LocalAppColors.current.textPrimary,
                                unfocusedTextColor = LocalAppColors.current.textPrimary,
                                focusedBorderColor = KaspaTeal,
                                unfocusedBorderColor = LocalAppColors.current.textSecondary,
                                focusedLabelColor = KaspaTeal,
                                unfocusedLabelColor = LocalAppColors.current.textSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Default: %.8f KAS".format(java.util.Locale.US, defaultFeeSompi / 100_000_000.0),
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val kas = feeEditorInput.toDoubleOrNull()
                        feeRateOverrideSompi = if (kas != null && kas > 0) {
                            val desiredFeeSompi = Math.round(kas * 100_000_000.0)
                            kotlin.math.ceil(desiredFeeSompi.toDouble() / estimatedMass).toLong()
                        } else {
                            null
                        }
                        showFeeEditor = false
                    }) {
                        Text(stringResource(R.string.save), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { feeRateOverrideSompi = null; showFeeEditor = false }) {
                            Text(stringResource(R.string.use_default), color = LocalAppColors.current.textSecondary)
                        }
                        TextButton(onClick = { showFeeEditor = false }) {
                            Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                        }
                    }
                }
            )
        }
    }

    if (showSpendingWithdrawDialog) {
        primarySpendingEntry?.let { entry ->
            WithdrawFromAddressDialog(
                viewModel = viewModel,
                entry = entry,
                onDismiss = { showSpendingWithdrawDialog = false }
            )
        }
    }
}

/**
 * Dedicated KNS domain-management screen — the owned-domain list (star to mark primary, swap
 * icon to transfer), plus inscribing a new domain. Used to live inline as a collapsible section
 * on [ProfileScreen] itself; broken out once the list plus its two dialogs (inscribe/transfer)
 * made that screen too crowded to scan at a glance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnsDomainsScreen(viewModel: WalletViewModel, onBack: () -> Unit) {
    val ownedDomainAssets by viewModel.ownedDomainAssets.collectAsState()
    val primaryDomainName by viewModel.primaryDomainName.collectAsState()
    val setPrimaryState by viewModel.setPrimaryState.collectAsState()
    val domainPreview by viewModel.domainPreview.collectAsState()
    val knsInscribeState by viewModel.knsInscribeState.collectAsState()
    var showInscribeDialog by remember { mutableStateOf(false) }
    var domainLabelInput by remember { mutableStateOf("") }
    var transferDialogDomain by remember { mutableStateOf<com.kachat.app.services.KnsAsset?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshOwnedDomains()
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.kns_domains), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaspaTeal)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    domainLabelInput = ""
                    viewModel.clearDomainPreview()
                    viewModel.resetKnsInscribeState()
                    showInscribeDialog = true
                },
                containerColor = KaspaTeal,
                contentColor = Color.Black,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 120.dp)
            ) {
                Text(
                    stringResource(R.string.inscribe_new_domain),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            if (ownedDomainAssets.isEmpty()) {
                Text(text = stringResource(R.string.no_domains_yet), color = LocalAppColors.current.textSecondary, modifier = Modifier.padding(16.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LocalAppColors.current.surface)
                        .padding(16.dp)
                ) {
                    ownedDomainAssets.forEachIndexed { index, domainAsset ->
                        val name = domainAsset.asset ?: return@forEachIndexed
                        val assetId = domainAsset.assetId
                        val isPrimary = name == primaryDomainName
                        val settingThisOne = setPrimaryState.inFlight && setPrimaryState.assetId == assetId
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (settingThisOne) {
                                CircularProgressIndicator(color = KaspaTeal, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    if (isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (isPrimary) "Primary domain" else "Set as primary",
                                    tint = if (isPrimary) KaspaTeal else Color.Gray,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .then(
                                            if (!isPrimary && assetId != null) Modifier.clickable { viewModel.setPrimaryDomain(assetId) }
                                            else Modifier
                                        )
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(name, color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (assetId != null) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = stringResource(R.string.transfer_domain),
                                    tint = LocalAppColors.current.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            viewModel.resetTransferDomainState()
                                            transferDialogDomain = domainAsset
                                        }
                                )
                            }
                        }
                        val setPrimaryError = setPrimaryState.errorMessage
                        if (setPrimaryState.assetId == assetId && setPrimaryError != null) {
                            Text(setPrimaryError, color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodySmall)
                        }
                        if (index < ownedDomainAssets.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showInscribeDialog) {
        val inFlight = knsInscribeState.status !in listOf(
            WalletViewModel.KnsInscribeUiStatus.IDLE,
            WalletViewModel.KnsInscribeUiStatus.SUCCESS,
            WalletViewModel.KnsInscribeUiStatus.FAILED
        )
        AlertDialog(
            onDismissRequest = { if (!inFlight) showInscribeDialog = false },
            title = {
                Text(
                    when (knsInscribeState.status) {
                        WalletViewModel.KnsInscribeUiStatus.SUCCESS -> "Domain Registered"
                        WalletViewModel.KnsInscribeUiStatus.FAILED -> "Inscription Failed"
                        else -> "Inscribe New Domain"
                    },
                    color = LocalAppColors.current.textPrimary
                )
            },
            containerColor = LocalAppColors.current.surface,
            text = {
                Column {
                    when (knsInscribeState.status) {
                        WalletViewModel.KnsInscribeUiStatus.IDLE -> {
                            OutlinedTextField(
                                value = domainLabelInput,
                                onValueChange = {
                                    domainLabelInput = it
                                    viewModel.checkDomainLabel(it)
                                },
                                label = { Text(stringResource(R.string.domain_name)) },
                                suffix = { Text(stringResource(R.string.kas)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LocalAppColors.current.textPrimary,
                                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                                    focusedBorderColor = KaspaTeal,
                                    unfocusedBorderColor = LocalAppColors.current.textSecondary,
                                    focusedLabelColor = KaspaTeal,
                                    unfocusedLabelColor = LocalAppColors.current.textSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            domainPreview?.let { preview ->
                                when {
                                    preview.checking -> Text(stringResource(R.string.checking_availability), color = LocalAppColors.current.textSecondary)
                                    preview.errorMessage != null -> Text(preview.errorMessage, color = Color(0xFFFF3B30))
                                    preview.available == false -> Text("${preview.label}.kas is not available", color = Color(0xFFFF3B30))
                                    preview.available == true && preview.isReserved -> {
                                        Text("${preview.label}.kas is available", color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.reserved_domain_no_registration_fee_only), color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    preview.available == true -> {
                                        Text("${preview.label}.kas is available", color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        val revealKas = preview.revealKas ?: 0.0
                                        val commitKas = preview.commitKas ?: 0.0
                                        Text(
                                            "Registration fee: ${"%.2f".format(revealKas)} KAS",
                                            color = LocalAppColors.current.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "You'll send ~${"%.2f".format(commitKas)} KAS total; ~${"%.2f".format((commitKas - revealKas).coerceAtLeast(0.0))} KAS comes back as change, the rest covers the fee and network costs.",
                                            color = LocalAppColors.current.textSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        WalletViewModel.KnsInscribeUiStatus.CHECKING_AVAILABILITY -> InscribeProgressRow(stringResource(R.string.checking_availability))
                        WalletViewModel.KnsInscribeUiStatus.FETCHING_FEE -> InscribeProgressRow(stringResource(R.string.calculating_fee))
                        WalletViewModel.KnsInscribeUiStatus.SUBMITTING_COMMIT -> InscribeProgressRow(stringResource(R.string.submitting_commit_transaction))
                        WalletViewModel.KnsInscribeUiStatus.SUBMITTING_REVEAL -> InscribeProgressRow(stringResource(R.string.submitting_reveal_transaction))
                        WalletViewModel.KnsInscribeUiStatus.VERIFYING -> InscribeProgressRow(stringResource(R.string.verifying_on_chain_this_can_take))
                        WalletViewModel.KnsInscribeUiStatus.SUCCESS -> {
                            val result = knsInscribeState.result
                            Text("${result?.domain} is now yours.", color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Commit tx: ${result?.commitTxId}", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Text("Reveal tx: ${result?.revealTxId}", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            if (result?.verified == false) {
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.still_indexing_it_ll_show_up), color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        WalletViewModel.KnsInscribeUiStatus.FAILED -> {
                            Text(knsInscribeState.errorMessage ?: "Something went wrong", color = Color(0xFFFF3B30))
                        }
                    }
                }
            },
            confirmButton = {
                when (knsInscribeState.status) {
                    WalletViewModel.KnsInscribeUiStatus.IDLE -> {
                        val preview = domainPreview
                        TextButton(
                            onClick = { viewModel.inscribeDomain(preview?.label ?: domainLabelInput) },
                            enabled = preview?.available == true
                        ) {
                            val costLabel = preview?.commitKas?.let { " (pay ~${"%.2f".format(it)} KAS)" } ?: ""
                            Text("Inscribe$costLabel", color = if (preview?.available == true) KaspaTeal else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    WalletViewModel.KnsInscribeUiStatus.SUCCESS, WalletViewModel.KnsInscribeUiStatus.FAILED -> {
                        TextButton(onClick = { showInscribeDialog = false }) {
                            Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                if (!inFlight && knsInscribeState.status == WalletViewModel.KnsInscribeUiStatus.IDLE) {
                    TextButton(onClick = { showInscribeDialog = false }) {
                        Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                    }
                }
            }
        )
    }

    transferDialogDomain?.let { domain ->
        val recipientPreview by viewModel.transferRecipientPreview.collectAsState()
        val transferState by viewModel.transferDomainState.collectAsState()
        var recipientInput by remember(domain) { mutableStateOf("") }
        var confirmStep by remember(domain) { mutableStateOf(false) }
        val inFlight = transferState.status !in listOf(
            WalletViewModel.KnsInscribeUiStatus.IDLE,
            WalletViewModel.KnsInscribeUiStatus.SUCCESS,
            WalletViewModel.KnsInscribeUiStatus.FAILED
        )
        val domainName = domain.asset ?: ""
        val assetId = domain.assetId ?: ""

        AlertDialog(
            onDismissRequest = { if (!inFlight) transferDialogDomain = null },
            title = {
                Text(
                    when {
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.SUCCESS -> "Domain Transferred"
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.FAILED -> "Transfer Failed"
                        confirmStep -> "Confirm Transfer"
                        else -> "Transfer $domainName"
                    },
                    color = LocalAppColors.current.textPrimary
                )
            },
            containerColor = LocalAppColors.current.surface,
            text = {
                Column {
                    when {
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.SUBMITTING_COMMIT -> InscribeProgressRow(stringResource(R.string.submitting_commit_transaction))
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.SUBMITTING_REVEAL -> InscribeProgressRow(stringResource(R.string.submitting_reveal_transaction))
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.VERIFYING -> InscribeProgressRow(stringResource(R.string.verifying_new_ownership_on_chain_this))
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.SUCCESS -> {
                            val result = transferState.result
                            Text("$domainName now belongs to ${result?.toAddress}.", color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Commit tx: ${result?.commitTxId}", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Text("Reveal tx: ${result?.revealTxId}", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            if (result?.verified == false) {
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.still_indexing_ownership_will_update_shortly), color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        transferState.status == WalletViewModel.KnsInscribeUiStatus.FAILED -> {
                            Text(transferState.errorMessage ?: "Something went wrong", color = Color(0xFFFF3B30))
                        }
                        confirmStep -> {
                            Text("This will permanently transfer ownership of $domainName to:", color = LocalAppColors.current.textPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                recipientPreview?.resolvedAddress ?: "",
                                color = KaspaTeal,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.this_action_cannot_be_undone_double),
                                color = Color(0xFFFF3B30),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Text("Inscription: $assetId", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = recipientInput,
                                onValueChange = {
                                    recipientInput = it
                                    viewModel.checkTransferRecipient(it)
                                },
                                label = { Text(stringResource(R.string.recipient_address_or_kas_name)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LocalAppColors.current.textPrimary,
                                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                                    focusedBorderColor = KaspaTeal,
                                    unfocusedBorderColor = LocalAppColors.current.textSecondary,
                                    focusedLabelColor = KaspaTeal,
                                    unfocusedLabelColor = LocalAppColors.current.textSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            recipientPreview?.let { preview ->
                                when {
                                    preview.checking -> Text(stringResource(R.string.resolving), color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    preview.errorMessage != null -> Text(preview.errorMessage, color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodySmall)
                                    preview.resolvedAddress != null -> Text("Resolves to: ${preview.resolvedAddress}", color = Color(0xFF4CD964), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                when {
                    transferState.status == WalletViewModel.KnsInscribeUiStatus.SUCCESS || transferState.status == WalletViewModel.KnsInscribeUiStatus.FAILED -> {
                        TextButton(onClick = { transferDialogDomain = null }) {
                            Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                    confirmStep -> {
                        TextButton(onClick = { viewModel.transferDomain(domainName, assetId) }) {
                            Text(stringResource(R.string.confirm_transfer), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                        }
                    }
                    transferState.status == WalletViewModel.KnsInscribeUiStatus.IDLE -> {
                        TextButton(
                            onClick = { confirmStep = true },
                            enabled = recipientPreview?.resolvedAddress != null
                        ) {
                            Text(stringResource(R.string.next), color = if (recipientPreview?.resolvedAddress != null) KaspaTeal else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                when {
                    confirmStep && !inFlight -> {
                        TextButton(onClick = { confirmStep = false }) {
                            Text(stringResource(R.string.back), color = LocalAppColors.current.textSecondary)
                        }
                    }
                    !inFlight && transferState.status == WalletViewModel.KnsInscribeUiStatus.IDLE -> {
                        TextButton(onClick = { transferDialogDomain = null }) {
                            Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                        }
                    }
                }
            }
        )
    }
}

/**
 * Every spending-chain address derived so far, plus the identity address shown first (grayed
 * out, tapping it warns rather than lets you copy it) — since paying the identity address
 * instead of the current spending address defeats the whole point of keeping them separate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToTxHistory: (String) -> Unit = {},
    onNavigateToHidden: () -> Unit = {},
    onAddressPicked: ((com.kachat.app.services.WalletService.SpendingAddressEntry) -> Unit)? = null
) {
    val identityAddress by viewModel.address.collectAsState()
    val addresses by viewModel.manageAddresses.collectAsState()
    val loading by viewModel.manageAddressesLoading.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showIdentityWarning by remember { mutableStateOf(false) }
    var activateIndex by remember { mutableStateOf<Int?>(null) }
    var qrAddress by remember { mutableStateOf<String?>(null) }
    var withdrawEntry by remember { mutableStateOf<com.kachat.app.services.WalletService.SpendingAddressEntry?>(null) }
    var renamingEntry by remember { mutableStateOf<com.kachat.app.services.WalletService.SpendingAddressEntry?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showActionsMenu by remember { mutableStateOf(false) }
    var actionsMenuAnchor by remember { mutableStateOf(Offset.Zero) }
    var showConsolidateConfirm by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val consolidateState by viewModel.consolidateState.collectAsState()

    // Funded addresses always sort to the top; within each group, newest (highest index) first —
    // so a freshly generated (zero-balance) address lands right below the last funded one rather
    // than jumping above it just for being newest.
    val visibleAddresses = remember(addresses) {
        addresses.filterNot { it.hidden }
            .sortedWith(compareByDescending<com.kachat.app.services.WalletService.SpendingAddressEntry> { it.balanceSompi > 0 }.thenByDescending { it.index })
    }
    val hiddenAddresses = remember(addresses) { addresses.filter { it.hidden } }

    LaunchedEffect(Unit) {
        viewModel.loadManageAddresses()
    }

    LaunchedEffect(consolidateState.status) {
        when (consolidateState.status) {
            WalletViewModel.ConsolidateStatus.SUCCESS -> {
                val count = consolidateState.sweptCount
                Toast.makeText(
                    context,
                    if (count > 0) "Consolidated $count address${if (count == 1) "" else "es"}" else context.getString(R.string.nothing_to_consolidate),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetConsolidateState()
            }
            WalletViewModel.ConsolidateStatus.FAILED -> {
                Toast.makeText(context, consolidateState.errorMessage ?: context.getString(R.string.consolidation_failed), Toast.LENGTH_SHORT).show()
                viewModel.resetConsolidateState()
            }
            else -> {}
        }
    }

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.loadManageAddresses()
        }
    }

    LaunchedEffect(loading) {
        if (!loading && pullRefreshState.isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.manage_addresses), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaspaTeal)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            // Hidden while the QR overlay is up — its Dialog window doesn't fully cover the
            // screen, so the FAB would otherwise still show through around the QR card.
            if (qrAddress == null) {
            FloatingActionButton(
                onClick = { showActionsMenu = true },
                containerColor = KaspaTeal,
                contentColor = Color.Black,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .height(56.dp)
                    .onGloballyPositioned { coords -> actionsMenuAnchor = coords.positionInWindow() }
            ) {
                val addressActionsContentDescription = stringResource(R.string.address_actions_2)
                Text(
                    stringResource(R.string.address_actions),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .semantics { contentDescription = addressActionsContentDescription }
                )
            }
            }
            // A regular DropdownMenu anchors to (and can get pushed off to one side of) this
            // now screen-centered FAB rather than the screen itself — see CenteredOptionsMenu's
            // doc comment for why this uses a real Dialog instead. Anchored to the FAB's top edge
            // (no height offset) so the card grows upward and sits just above the button, matching
            // the composer "+" menu's same near-bottom-of-screen anchoring. centerHorizontally
            // overrides the usual left/right-edge hugging (which assumes the anchor sits near a
            // screen edge) since this FAB is itself screen-centered.
            if (showActionsMenu) {
                CenteredOptionsMenu(
                    onDismissRequest = { showActionsMenu = false },
                    anchor = actionsMenuAnchor,
                    centerHorizontally = true
                ) {
                    PopupMenuRow(Icons.Default.AddCircleOutline, stringResource(R.string.generate_new_spending_address)) {
                        showActionsMenu = false
                        viewModel.generateNewSpendingAddress()
                    }
                    HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                    PopupMenuRow(Icons.Default.Search, stringResource(R.string.discover_addresses)) {
                        showActionsMenu = false
                        viewModel.discoverSpendingAddresses { count ->
                            Toast.makeText(
                                context,
                                if (count > 0) "Found $count used address${if (count == 1) "" else "es"}" else "No additional used addresses found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                    PopupMenuRow(Icons.AutoMirrored.Filled.CallMerge, stringResource(R.string.send_all_kaspa_to_primary_spend)) {
                        showActionsMenu = false
                        showConsolidateConfirm = true
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(pullRefreshState.nestedScrollConnection)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onAddressPicked != null) {
                item {
                    Text(
                        stringResource(R.string.tap_an_address_below_to_swap),
                        color = KaspaTeal,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalAppColors.current.surface)
                        .clickable { showIdentityWarning = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.chatting_address), color = LocalAppColors.current.textSecondary, fontWeight = FontWeight.Bold)
                        Text(
                            text = identityAddress ?: "Loading...",
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (hiddenAddresses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToHidden)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Hidden (${hiddenAddresses.size})",
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LocalAppColors.current.textSecondary)
                    }
                }
            }

            if (loading && addresses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KaspaTeal)
                    }
                }
            } else {
                items(visibleAddresses, key = { it.index }) { entry ->
                    ManageAddressRow(
                        entry = entry,
                        onClick = { if (onAddressPicked != null) onAddressPicked(entry) else onNavigateToTxHistory(entry.address) },
                        onCopyClick = { clipboardManager.setText(AnnotatedString(entry.address)) },
                        onQrClick = { qrAddress = entry.address },
                        onActivateClick = { if (!entry.isCurrent) activateIndex = entry.index },
                        onWithdrawClick = { if (entry.balanceSompi > 0) withdrawEntry = entry },
                        onHideToggleClick = { viewModel.setManageAddressHidden(entry.index, true) },
                        onRenameClick = { renamingEntry = entry; renameInput = entry.label ?: "" }
                    )
                }
            }

            item {
                // Leaves room so the last address row isn't hidden behind the FAB.
                Spacer(Modifier.height(64.dp))
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        qrAddress?.let { address ->
            QrCodeOverlay(value = address, onDismiss = { qrAddress = null })
        }
        }
    }

    if (showIdentityWarning) {
        AlertDialog(
            onDismissRequest = { showIdentityWarning = false },
            containerColor = LocalAppColors.current.surface,
            title = { Text(stringResource(R.string.chatting_address), color = LocalAppColors.current.textPrimary) },
            text = {
                Text(
                    stringResource(R.string.never_send_kaspa_you_intend_to),
                    color = LocalAppColors.current.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showIdentityWarning = false }) {
                    Text(stringResource(R.string.ok), color = KaspaTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    activateIndex?.let { index ->
        ActivateAddressDialog(viewModel = viewModel, index = index, onDismiss = { activateIndex = null })
    }

    if (showConsolidateConfirm) {
        val consolidating = consolidateState.status == WalletViewModel.ConsolidateStatus.RUNNING
        AlertDialog(
            onDismissRequest = { if (!consolidating) showConsolidateConfirm = false },
            containerColor = LocalAppColors.current.surface,
            title = { Text(stringResource(R.string.send_all_kaspa_to_primary_spend), color = LocalAppColors.current.textPrimary) },
            text = {
                Text(
                    stringResource(R.string.sends_every_other_spending_address_s),
                    color = LocalAppColors.current.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !consolidating,
                    onClick = {
                        viewModel.consolidateSpendingAddresses()
                        showConsolidateConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = KaspaTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !consolidating, onClick = { showConsolidateConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                }
            }
        )
    }

    withdrawEntry?.let { entry ->
        WithdrawFromAddressDialog(viewModel = viewModel, entry = entry, onDismiss = { withdrawEntry = null })
    }

    renamingEntry?.let { entry ->
        RenameAddressDialog(
            index = entry.index,
            nameInput = renameInput,
            onNameChange = { renameInput = it },
            onDismiss = { renamingEntry = null },
            onSave = {
                viewModel.setManageAddressLabel(entry.index, renameInput)
                renamingEntry = null
            }
        )
    }
}

/** Rename dialog shared by [ManageAddressesScreen] and [ManageAddressesHiddenScreen] — an empty/blank name clears back to the default "Address #N". */
@Composable
private fun RenameAddressDialog(
    index: Int,
    nameInput: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalAppColors.current.surface,
        title = { Text(stringResource(R.string.rename_address), color = LocalAppColors.current.textPrimary) },
        text = {
            Column {
                Text("Address #$index", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.name)) },
                    placeholder = { Text("Address #$index") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalAppColors.current.textPrimary,
                        unfocusedTextColor = LocalAppColors.current.textPrimary,
                        focusedBorderColor = KaspaTeal,
                        unfocusedBorderColor = LocalAppColors.current.textSecondary,
                        focusedLabelColor = KaspaTeal,
                        unfocusedLabelColor = LocalAppColors.current.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save), color = KaspaTeal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
            }
        }
    )
}

/**
 * Every hidden spending address, reached via the "Hidden (N)" link on [ManageAddressesScreen] —
 * the only place a hidden address can be unhidden again. Shares [viewModel]'s own `manageAddresses`
 * list rather than loading a separate one, so it's always in sync with the main screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesHiddenScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToTxHistory: (String) -> Unit = {},
    onAddressPicked: ((com.kachat.app.services.WalletService.SpendingAddressEntry) -> Unit)? = null
) {
    val addresses by viewModel.manageAddresses.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var activateIndex by remember { mutableStateOf<Int?>(null) }
    var qrAddress by remember { mutableStateOf<String?>(null) }
    var withdrawEntry by remember { mutableStateOf<com.kachat.app.services.WalletService.SpendingAddressEntry?>(null) }
    var renamingEntry by remember { mutableStateOf<com.kachat.app.services.WalletService.SpendingAddressEntry?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val hiddenAddresses = remember(addresses) {
        addresses.filter { it.hidden }
            .sortedWith(compareByDescending<com.kachat.app.services.WalletService.SpendingAddressEntry> { it.balanceSompi > 0 }.thenByDescending { it.index })
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.hidden_addresses), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaspaTeal)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        if (hiddenAddresses.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.VisibilityOff, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.no_hidden_addresses), color = LocalAppColors.current.textSecondary, textAlign = TextAlign.Center)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onAddressPicked != null) {
                        item {
                            Text(
                                stringResource(R.string.tap_an_address_below_to_use),
                                color = KaspaTeal,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    items(hiddenAddresses, key = { it.index }) { entry ->
                        ManageAddressRow(
                            entry = entry,
                            onClick = { if (onAddressPicked != null) onAddressPicked(entry) else onNavigateToTxHistory(entry.address) },
                            onCopyClick = { clipboardManager.setText(AnnotatedString(entry.address)) },
                            onQrClick = { qrAddress = entry.address },
                            onActivateClick = { if (!entry.isCurrent) activateIndex = entry.index },
                            onWithdrawClick = { if (entry.balanceSompi > 0) withdrawEntry = entry },
                            onHideToggleClick = { viewModel.setManageAddressHidden(entry.index, false) },
                            onRenameClick = { renamingEntry = entry; renameInput = entry.label ?: "" }
                        )
                    }
                }
                qrAddress?.let { address ->
                    QrCodeOverlay(value = address, onDismiss = { qrAddress = null })
                }
            }
        }
    }

    activateIndex?.let { index ->
        ActivateAddressDialog(viewModel = viewModel, index = index, onDismiss = { activateIndex = null })
    }

    withdrawEntry?.let { entry ->
        WithdrawFromAddressDialog(viewModel = viewModel, entry = entry, onDismiss = { withdrawEntry = null })
    }

    renamingEntry?.let { entry ->
        RenameAddressDialog(
            index = entry.index,
            nameInput = renameInput,
            onNameChange = { renameInput = it },
            onDismiss = { renamingEntry = null },
            onSave = {
                viewModel.setManageAddressLabel(entry.index, renameInput)
                renamingEntry = null
            }
        )
    }
}

/** "Make Active Address" confirmation — shared by [ManageAddressesScreen] and [ManageAddressesHiddenScreen]. */
@Composable
private fun ActivateAddressDialog(viewModel: WalletViewModel, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalAppColors.current.surface,
        title = { Text(stringResource(R.string.make_active_address), color = LocalAppColors.current.textPrimary) },
        text = {
            Text(
                stringResource(R.string.spending_kaspa_on_kachat_will_come),
                color = LocalAppColors.current.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.setActiveSpendingAddress(index)
                onDismiss()
            }) {
                Text(stringResource(R.string.confirm), color = KaspaTeal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
            }
        }
    )
}

/** Withdraw-from-this-address dialog — shared by [ManageAddressesScreen] and [ManageAddressesHiddenScreen]. */
@Composable
private fun WithdrawFromAddressDialog(
    viewModel: WalletViewModel,
    entry: com.kachat.app.services.WalletService.SpendingAddressEntry,
    onDismiss: () -> Unit,
    portfolioViewModel: com.kachat.app.viewmodels.PortfolioViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val kaspaExplorer by viewModel.kaspaExplorer.collectAsState()
    val fiatPriceInCurrency by portfolioViewModel.currentPriceUsd.collectAsState()
    val fiatCurrencyCode by portfolioViewModel.currency.collectAsState()
    var recipientInput by remember(entry) { mutableStateOf("") }
    var amountInput by remember(entry) { mutableStateOf("") }
    val fiatAmountState = com.kachat.app.util.rememberKaspaFiatAmountState(resetKey = entry, onKasTextChange = { amountInput = it })
    var showScanner by remember { mutableStateOf(false) }
    var feeRateOverrideSompi by remember { mutableStateOf<Long?>(null) }
    var showFeeEditor by remember { mutableStateOf(false) }
    var feeEditorInput by remember { mutableStateOf("") }
    var sentTxId by remember { mutableStateOf<String?>(null) }
    val isSending by viewModel.isSending.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()

    // Same simplified single-input, two-output shape the "Max" button has always estimated
    // against — real UTXO selection can differ slightly, but this is close enough to preview
    // and to translate a user-entered KAS fee back into a sompi-per-gram rate.
    val estimatedMass = remember {
        com.kachat.app.util.KaspaMass.calculateMass(numInputs = 1, outputScriptLens = listOf(34, 34), payloadSize = 0)
    }
    val defaultFeeSompi = com.kachat.app.util.KaspaMass.calculateFee(estimatedMass, com.kachat.app.util.KaspaMass.MINIMUM_FEE_RATE_SOMPI_PER_GRAM)
    val effectiveFeeSompi = feeRateOverrideSompi?.let { com.kachat.app.util.KaspaMass.calculateFee(estimatedMass, it) } ?: defaultFeeSompi

    LaunchedEffect(sendResult) {
        val result = sendResult ?: return@LaunchedEffect
        if (result.isSuccess) {
            sentTxId = result.getOrNull()
        } else {
            Toast.makeText(context, result.exceptionOrNull()?.message ?: context.getString(R.string.withdrawal_failed), Toast.LENGTH_SHORT).show()
        }
        viewModel.clearSendResult()
    }

    if (showScanner) {
        BackHandler { showScanner = false }
        QrScannerOverlay(
            onScanned = { scanned -> recipientInput = scanned.trim(); showScanner = false },
            onDismiss = { showScanner = false }
        )
        return
    }

    sentTxId?.let { txId ->
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = LocalAppColors.current.surface,
            title = null,
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CD964).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CD964), modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.sent), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.transaction_id), color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
                    Text(
                        txId,
                        color = KaspaTeal,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { uriHandler.openUri(kaspaExplorer.txUrl(txId)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
        return
    }

    val amountSompi = amountInput.toDoubleOrNull()?.let { Math.round(it * 100_000_000.0) }
    val isValidAddress = remember(recipientInput) { KaspaAddress.isValid(recipientInput) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        containerColor = LocalAppColors.current.surface,
        title = { Text(stringResource(R.string.withdraw_from_this_address), color = LocalAppColors.current.textPrimary) },
        text = {
            Column {
                Text(entry.address, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recipientInput,
                    onValueChange = { recipientInput = it },
                    label = { Text(stringResource(R.string.recipient_address)) },
                    singleLine = true,
                    enabled = !isSending,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalAppColors.current.textPrimary,
                        unfocusedTextColor = LocalAppColors.current.textPrimary,
                        focusedBorderColor = KaspaTeal,
                        unfocusedBorderColor = LocalAppColors.current.textSecondary,
                        focusedLabelColor = KaspaTeal,
                        unfocusedLabelColor = LocalAppColors.current.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { clipboardManager.getText()?.text?.let { recipientInput = it.trim() } },
                        enabled = !isSending
                    ) {
                        Text(stringResource(R.string.paste_from_clipboard), color = KaspaTeal, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        onClick = { showScanner = true },
                        enabled = !isSending
                    ) {
                        Text(stringResource(R.string.scan_qr_code), color = KaspaTeal, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = fiatAmountState.displayText,
                    onValueChange = { fiatAmountState.onDisplayTextChange(it, fiatPriceInCurrency) },
                    label = { Text(if (fiatAmountState.isFiatMode) fiatCurrencyCode.uppercase() else stringResource(R.string.amount_kas)) },
                    singleLine = true,
                    enabled = !isSending,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            fiatAmountState.conversionLabelText(fiatPriceInCurrency, fiatCurrencyCode)?.let { label ->
                                Text(
                                    label,
                                    color = LocalAppColors.current.textSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable(enabled = !isSending) { fiatAmountState.toggleMode(fiatPriceInCurrency) }
                                        .padding(end = 8.dp)
                                )
                            }
                            TextButton(
                                onClick = {
                                    val maxSompi = (entry.balanceSompi - effectiveFeeSompi).coerceAtLeast(0L)
                                    fiatAmountState.setMaxKas(maxSompi / 100_000_000.0, fiatPriceInCurrency)
                                },
                                enabled = !isSending
                            ) {
                                Text(stringResource(R.string.max), color = KaspaTeal)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalAppColors.current.textPrimary,
                        unfocusedTextColor = LocalAppColors.current.textPrimary,
                        focusedBorderColor = KaspaTeal,
                        unfocusedBorderColor = LocalAppColors.current.textSecondary,
                        focusedLabelColor = KaspaTeal,
                        unfocusedLabelColor = LocalAppColors.current.textSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Available: %.8f KAS".format(java.util.Locale.US, entry.balanceSompi / 100_000_000.0),
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "·  Fee: %.8f KAS".format(java.util.Locale.US, effectiveFeeSompi / 100_000_000.0),
                        color = KaspaTeal,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.clickable(enabled = !isSending) {
                            feeEditorInput = "%.8f".format(java.util.Locale.US, effectiveFeeSompi / 100_000_000.0)
                            showFeeEditor = true
                        }
                    )
                }
                if (isSending) {
                    Spacer(Modifier.height(12.dp))
                    InscribeProgressRow(stringResource(R.string.sending_2))
                }
            }
        },
        confirmButton = {
            val canSend = !isSending && isValidAddress && (amountSompi ?: 0) > 0
            TextButton(
                onClick = {
                    amountSompi?.let {
                        viewModel.withdrawFromSpendingAddress(entry.index, recipientInput.trim(), it, feeRateOverrideSompi)
                    }
                },
                enabled = canSend
            ) {
                Text(stringResource(R.string.withdraw), color = if (canSend) KaspaTeal else Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!isSending) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                }
            }
        }
    )

    if (showFeeEditor) {
        AlertDialog(
            onDismissRequest = { showFeeEditor = false },
            containerColor = LocalAppColors.current.surface,
            title = { Text(stringResource(R.string.adjust_network_fee), color = LocalAppColors.current.textPrimary) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.if_the_network_is_busy_a),
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feeEditorInput,
                        onValueChange = { feeEditorInput = it },
                        label = { Text(stringResource(R.string.fee_kas)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LocalAppColors.current.textPrimary,
                            unfocusedTextColor = LocalAppColors.current.textPrimary,
                            focusedBorderColor = KaspaTeal,
                            unfocusedBorderColor = LocalAppColors.current.textSecondary,
                            focusedLabelColor = KaspaTeal,
                            unfocusedLabelColor = LocalAppColors.current.textSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Default: %.8f KAS".format(java.util.Locale.US, defaultFeeSompi / 100_000_000.0),
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val kas = feeEditorInput.toDoubleOrNull()
                    feeRateOverrideSompi = if (kas != null && kas > 0) {
                        val desiredFeeSompi = Math.round(kas * 100_000_000.0)
                        kotlin.math.ceil(desiredFeeSompi.toDouble() / estimatedMass).toLong()
                    } else {
                        null
                    }
                    showFeeEditor = false
                }) {
                    Text(stringResource(R.string.save), color = KaspaTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { feeRateOverrideSompi = null; showFeeEditor = false }) {
                        Text(stringResource(R.string.use_default), color = LocalAppColors.current.textSecondary)
                    }
                    TextButton(onClick = { showFeeEditor = false }) {
                        Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                    }
                }
            }
        )
    }
}

/**
 * One spending address row in [ManageAddressesScreen] — shared by the main and "Hidden" sections,
 * differing only in whether [WalletService.SpendingAddressEntry.hidden] shows a hide or an unhide
 * swipe action. Hiding is a swipe-left action (matching Chats' swipe-to-delete and Cold Storage's
 * address rows) rather than a permanent icon button, since it's reached for less often than the
 * actions in the overflow menu. Unhiding is always available, but an address can't be hidden while
 * it still holds a balance or is the primary ("Pay in Kaspa") spending address — see
 * [WalletViewModel.setManageAddressHidden], which enforces the same rule as a backstop.
 *
 * Everything besides hide/unhide (copy, QR, set primary, withdraw, rename) lives behind a single
 * overflow button's [CenteredOptionsMenu] rather than a row of icons, so the address itself has
 * room to sit on its own line instead of being squeezed by four icon buttons.
 */
@Composable
private fun ManageAddressRow(
    entry: com.kachat.app.services.WalletService.SpendingAddressEntry,
    onClick: () -> Unit,
    onCopyClick: () -> Unit,
    onQrClick: () -> Unit,
    onActivateClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onHideToggleClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    val kas = entry.balanceSompi / 100_000_000.0
    val canHide = entry.hidden || (entry.balanceSompi == 0L && !entry.isCurrent)
    var showMenu by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Offset.Zero) }

    SwipeActionRow(
        enabled = canHide,
        cornerRadius = 12.dp,
        trailingIcon = if (entry.hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
        trailingLabel = if (entry.hidden) "Unhide" else "Hide",
        trailingColor = Color(0xFF48484A),
        onTrailingClick = onHideToggleClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LocalAppColors.current.surface)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.label?.takeIf { it.isNotBlank() } ?: "Address #${entry.index}",
                        color = LocalAppColors.current.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.isCurrent) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Star, "Primary address", tint = KaspaTeal, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${entry.address.take(14)}...${entry.address.takeLast(6)}",
                    color = LocalAppColors.current.textPrimary,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%.8f KAS".format(java.util.Locale.US, kas),
                    color = LocalAppColors.current.textPrimary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (entry.everUsed) "Used" else "Unused",
                    color = if (entry.everUsed) Color(0xFFF39C12) else Color(0xFF4CD964),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(44.dp)
                    .onGloballyPositioned { coords ->
                        menuAnchor = coords.positionInWindow() + Offset(0f, coords.size.height.toFloat())
                    }
            ) {
                Icon(Icons.Default.MoreVert, "Address actions", tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(28.dp))
            }
        }
    }

    if (showMenu) {
        CenteredOptionsMenu(onDismissRequest = { showMenu = false }, anchor = menuAnchor) {
            PopupMenuRow(Icons.Default.ContentCopy, stringResource(R.string.copy_address)) {
                showMenu = false
                onCopyClick()
            }
            HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
            PopupMenuRow(Icons.Default.QrCode, stringResource(R.string.show_qr_code)) {
                showMenu = false
                onQrClick()
            }
            if (!entry.isCurrent) {
                HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                PopupMenuRow(Icons.Default.Star, stringResource(R.string.set_as_primary_address)) {
                    showMenu = false
                    onActivateClick()
                }
            }
            if (entry.balanceSompi > 0) {
                HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
                PopupMenuRow(Icons.AutoMirrored.Filled.Send, stringResource(R.string.withdraw)) {
                    showMenu = false
                    onWithdrawClick()
                }
            }
            HorizontalDivider(color = LocalAppColors.current.textPrimary.copy(alpha = 0.08f))
            PopupMenuRow(Icons.Default.Edit, stringResource(R.string.rename_address)) {
                showMenu = false
                onRenameClick()
            }
        }
    }
}

/**
 * A small options card, positioned via a real [Dialog] rather than an anchored
 * `DropdownMenu`/`Popup` — see [ManageAddressesScreen]'s Address Actions menu for why: a floating
 * anchored Popup can dismiss itself on the very touch that opened it, since it shares the same
 * composition/touch pass as the button that triggered it. A Dialog is backed by its own Android
 * window, added only once that opening gesture has fully finished, so it doesn't race. It's also
 * how [ChatsScreen]/broadcast rooms' message and avatar context menus avoid a second, unrelated
 * bug: Material3's stock `DropdownMenu` clips its content to its own fixed small internal shape
 * token no matter what shape you pass it, which is what made those menus render with visibly
 * square corners.
 *
 * [anchor], if given, is the window-relative pixel position (`LayoutCoordinates.positionInWindow()`)
 * to hug a corner of instead of centering — e.g. just below a tapped avatar or message, or just
 * above the composer's "+" button. Which corner is picked (and therefore which direction the card
 * grows in) flips per axis based on which half of the screen [anchor] falls in, so it never grows
 * off-screen; since the card's own size isn't known until it's laid out, that placement happens in
 * a second pass, via [Modifier.onSizeChanged], once the real size is measured — the on-screen jump
 * from the first frame's guess is a single frame and not noticeable. With no anchor, the card is
 * centered at the bottom of the screen instead, for a FAB-triggered menu like Address Actions
 * where there's no single on-screen element to sit next to.
 *
 * [content] should be one or more [PopupMenuRow]s, optionally separated by [HorizontalDivider]s.
 */
@Composable
fun CenteredOptionsMenu(
    onDismissRequest: () -> Unit,
    anchor: Offset? = null,
    centerHorizontally: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        val density = LocalDensity.current
        val cardWidthDp = 280.dp
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            // A Dialog's window normally takes input focus when it appears, which dismisses the
            // soft keyboard if the composer's TextField had it up — these flags are the standard
            // "popup that doesn't steal focus" combo, so the keyboard (and whatever layout shift
            // it caused, which [anchor] was captured after) stays exactly as it was.
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            // FLAG_NOT_FOCUSABLE (needed above so the keyboard doesn't close) also breaks the
            // Dialog's own built-in dismissOnClickOutside — it stops delivering ACTION_OUTSIDE once
            // the window can't take focus. Every branch below rolls its own tap-anywhere-outside
            // dismiss instead (a full-size scrim behind the card), so every branch's window needs
            // to actually span the full screen for there to be anything for that scrim to cover.
            window.setGravity(Gravity.TOP or Gravity.START)
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
        if (anchor != null) {
            val metrics = view.context.resources.displayMetrics
            val marginPx = with(density) { 8.dp.toPx() }
            val horizontalEnd = anchor.x > metrics.widthPixels / 2f
            val verticalBottom = anchor.y > metrics.heightPixels / 2f
            var cardSize by remember { mutableStateOf(IntSize.Zero) }
            val offsetX = if (centerHorizontally) {
                ((metrics.widthPixels - cardSize.width) / 2f).coerceAtLeast(marginPx)
            } else {
                (if (horizontalEnd) anchor.x - cardSize.width else anchor.x).coerceAtLeast(marginPx)
            }
            val offsetY = (if (verticalBottom) anchor.y - cardSize.height - marginPx else anchor.y + marginPx).coerceAtLeast(marginPx)
            // This window now spans the full screen (so the card can be offset to hug whichever
            // corner it needs to), which leaves no genuine "outside the window" area for the
            // Dialog's own dismissOnClickOutside to detect — same underlying reason as the
            // FAB-triggered branch needing none of this. A manual full-size scrim stands in for
            // it instead; each [PopupMenuRow]'s own clickable consumes its tap before it can reach
            // this one, so only taps on the blank area around the card actually dismiss it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismissRequest
                    )
            ) {
                Surface(
                    color = LocalAppColors.current.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = cardWidthDp)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .onSizeChanged { cardSize = it }
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max), content = content)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismissRequest
                    )
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = LocalAppColors.current.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.widthIn(min = 160.dp, max = cardWidthDp)
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max), content = content)
                }
            }
        }
    }
}

/** One row of a [CenteredOptionsMenu] — icon fixed at a consistent left offset so it lines up the same across every row regardless of label length. */
@Composable
fun PopupMenuRow(
    icon: ImageVector,
    label: String,
    labelColor: Color = LocalAppColors.current.textPrimary,
    iconTint: Color = KaspaTeal,
    onClick: () -> Unit
) {
    PopupMenuRowContent(label, labelColor, onClick) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

/** [PopupMenuRow] overload for a custom drawable (e.g. the Kaspa "K" mark) instead of a Material [ImageVector]. */
@Composable
fun PopupMenuRow(
    icon: Painter,
    label: String,
    labelColor: Color = LocalAppColors.current.textPrimary,
    iconTint: Color = KaspaTeal,
    onClick: () -> Unit
) {
    PopupMenuRowContent(label, labelColor, onClick) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PopupMenuRowContent(label: String, labelColor: Color, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(label, color = labelColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InscribeProgressRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = KaspaTeal, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, color = LocalAppColors.current.textPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKnsProfileScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToDomains: () -> Unit = {},
    onNavigateToSetupGuide: () -> Unit = {}
) {
    val knsProfile by viewModel.knsProfile.collectAsState()
    val activeProfileDomainName by viewModel.activeProfileDomainName.collectAsState()
    val profileAssetId by viewModel.profileDomainAssetId.collectAsState()
    val pendingAvatarUri by viewModel.pendingAvatarUri.collectAsState()
    val pendingBannerUri by viewModel.pendingBannerUri.collectAsState()
    val editState by viewModel.editProfileState.collectAsState()
    val showSetupGuides by viewModel.showSetupGuides.collectAsState()

    var bio by remember { mutableStateOf("") }
    var x by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var discord by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var github by remember { mutableStateOf("") }
    var redirect by remember { mutableStateOf("") }
    var seeded by remember { mutableStateOf(false) }

    LaunchedEffect(knsProfile) {
        if (!seeded) {
            bio = knsProfile?.bio ?: ""
            x = knsProfile?.x ?: ""
            website = knsProfile?.website ?: ""
            telegram = knsProfile?.telegram ?: ""
            discord = knsProfile?.discord ?: ""
            email = knsProfile?.contactEmail ?: ""
            github = knsProfile?.github ?: ""
            redirect = knsProfile?.redirectUrl ?: ""
            seeded = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetEditProfileState() }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) viewModel.setPendingAvatar(uri) }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) viewModel.setPendingBanner(uri) }

    var showSaveDialog by remember { mutableStateOf(false) }

    // What "Save" is actually about to submit — each entry becomes its own on-chain commit/reveal
    // transaction, so this doubles as both the confirm dialog's change list and its cost count.
    val pendingChanges = remember(bio, x, website, telegram, discord, email, github, redirect, pendingAvatarUri, pendingBannerUri, knsProfile) {
        buildList {
            if (pendingAvatarUri != null) add("Avatar")
            if (pendingBannerUri != null) add("Banner")
            if (bio.trim() != (knsProfile?.bio ?: "")) add("Bio")
            if (x.trim() != (knsProfile?.x ?: "")) add("X")
            if (website.trim() != (knsProfile?.website ?: "")) add("Website")
            if (telegram.trim() != (knsProfile?.telegram ?: "")) add("Telegram")
            if (discord.trim() != (knsProfile?.discord ?: "")) add("Discord")
            if (email.trim() != (knsProfile?.contactEmail ?: "")) add("Email")
            if (github.trim() != (knsProfile?.github ?: "")) add("GitHub")
            if (redirect.trim() != (knsProfile?.redirectUrl ?: "")) add("Redirect")
        }
    }

    val inFlight = editState.step !in listOf(
        WalletViewModel.EditProfileStep.IDLE,
        WalletViewModel.EditProfileStep.SUCCESS,
        WalletViewModel.EditProfileStep.PARTIAL_FAILURE,
        WalletViewModel.EditProfileStep.FAILED
    )

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_kns_profile), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack, enabled = !inFlight) {
                        Text(stringResource(R.string.cancel), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (pendingChanges.isNotEmpty()) showSaveDialog = true },
                        enabled = !inFlight && pendingChanges.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save), color = if (!inFlight && pendingChanges.isNotEmpty()) KaspaTeal else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Re-enters the same guided wizard used to create a profile from scratch - it
            // already knows (via the domain/profile it fetches) to offer skipping domain
            // registration and pre-fill the banner/avatar/detail steps with whatever's already
            // inscribed, so this is a safe re-entry point regardless of how much of a profile
            // already exists. Lives here (rather than next to "KNS Profile" on the Profile tab)
            // since that spot sits directly beside the banner image, which made it unclickable
            // whenever a banner was set.
            if (showSetupGuides) {
                SettingsSection(title = stringResource(R.string.setup_guide)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSetupGuide() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.walk_through_setting_up_your_domain),
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.avatar)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ContactAvatar(
                        imageUrl = pendingAvatarUri?.toString() ?: knsProfile?.avatarUrl,
                        fallbackText = activeProfileDomainName ?: "?",
                        size = 64.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = { avatarPicker.launch("image/*") }, enabled = !inFlight) {
                            Text(stringResource(R.string.choose_avatar), color = KaspaTeal, fontWeight = FontWeight.Bold)
                        }
                        if (pendingAvatarUri != null) {
                            TextButton(onClick = { viewModel.setPendingAvatar(null) }, enabled = !inFlight) {
                                Text(stringResource(R.string.remove), color = Color(0xFFFF3B30))
                            }
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.banner)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val previewUrl = pendingBannerUri?.toString() ?: knsProfile?.bannerUrl
                    if (previewUrl != null) {
                        SubcomposeAsyncImage(
                            model = previewUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalAppColors.current.surface)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = { bannerPicker.launch("image/*") }, enabled = !inFlight) {
                            Text(stringResource(R.string.choose_banner), color = KaspaTeal, fontWeight = FontWeight.Bold)
                        }
                        if (pendingBannerUri != null) {
                            TextButton(onClick = { viewModel.setPendingBanner(null) }, enabled = !inFlight) {
                                Text(stringResource(R.string.remove), color = Color(0xFFFF3B30))
                            }
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.profile)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditProfileTextField(stringResource(R.string.bio), bio, { bio = it }, enabled = !inFlight, singleLine = false)
                    EditProfileTextField(stringResource(R.string.x), x, { x = it }, enabled = !inFlight)
                    EditProfileTextField(stringResource(R.string.website), website, { website = it }, enabled = !inFlight)
                    EditProfileTextField(stringResource(R.string.telegram), telegram, { telegram = it }, enabled = !inFlight)
                    EditProfileTextField(stringResource(R.string.discord), discord, { discord = it }, enabled = !inFlight)
                    EditProfileTextField(stringResource(R.string.email), email, { email = it }, enabled = !inFlight)
                    EditProfileTextField(stringResource(R.string.github), github, { github = it }, enabled = !inFlight)
                    EditProfileTextField(stringResource(R.string.redirect), redirect, { redirect = it }, enabled = !inFlight)
                }
            }

            SettingsSection(title = stringResource(R.string.domains)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDomains() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeProfileDomainName ?: "—", color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(profileAssetId ?: "", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }

    if (showSaveDialog) {
        val terminal = editState.step in listOf(
            WalletViewModel.EditProfileStep.SUCCESS,
            WalletViewModel.EditProfileStep.PARTIAL_FAILURE,
            WalletViewModel.EditProfileStep.FAILED
        )
        fun closeDialog() {
            showSaveDialog = false
            viewModel.resetEditProfileState()
        }
        AlertDialog(
            onDismissRequest = {
                when (editState.step) {
                    WalletViewModel.EditProfileStep.IDLE -> showSaveDialog = false
                    else -> if (terminal) closeDialog()
                }
            },
            containerColor = LocalAppColors.current.surface,
            title = {
                Text(
                    when (editState.step) {
                        WalletViewModel.EditProfileStep.IDLE -> "Confirm Changes"
                        WalletViewModel.EditProfileStep.SUCCESS -> "Saved"
                        WalletViewModel.EditProfileStep.PARTIAL_FAILURE -> "Some Changes Failed"
                        WalletViewModel.EditProfileStep.FAILED -> "Save Failed"
                        else -> "Saving..."
                    },
                    color = LocalAppColors.current.textPrimary
                )
            },
            text = {
                when (editState.step) {
                    WalletViewModel.EditProfileStep.IDLE -> Column {
                        Text(
                            "${pendingChanges.size} change${if (pendingChanges.size == 1) "" else "s"}. Each is submitted as its own on-chain transaction from your spending address:",
                            color = LocalAppColors.current.textPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        pendingChanges.forEach { Text("• $it", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.each_transaction_temporarily_uses_2_kas),
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    WalletViewModel.EditProfileStep.UPLOADING_AVATAR,
                    WalletViewModel.EditProfileStep.UPLOADING_BANNER,
                    WalletViewModel.EditProfileStep.SUBMITTING_FIELD -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = KaspaTeal)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when (editState.step) {
                                WalletViewModel.EditProfileStep.UPLOADING_AVATAR -> "Uploading avatar..."
                                WalletViewModel.EditProfileStep.UPLOADING_BANNER -> "Uploading banner..."
                                else -> "Submitting ${editState.currentFieldLabel}..."
                            },
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    WalletViewModel.EditProfileStep.SUCCESS -> Text(
                        if (editState.fieldResults.isEmpty()) "Nothing to save." else "All changes saved.",
                        color = Color(0xFF4CD964),
                        fontWeight = FontWeight.Bold
                    )
                    WalletViewModel.EditProfileStep.PARTIAL_FAILURE -> Column {
                        Text(stringResource(R.string.some_changes_failed_to_save), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                        editState.fieldResults.filter { !it.success }.forEach {
                            Text("${it.fieldKey}: ${it.errorMessage ?: "failed"}", color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    WalletViewModel.EditProfileStep.FAILED -> Text(
                        editState.fieldResults.firstOrNull { !it.success }?.errorMessage ?: "Save failed",
                        color = Color(0xFFFF3B30)
                    )
                }
            },
            confirmButton = {
                when (editState.step) {
                    WalletViewModel.EditProfileStep.IDLE -> TextButton(
                        onClick = {
                            viewModel.saveKnsProfile(
                                mapOf(
                                    "bio" to bio,
                                    "x" to x,
                                    "website" to website,
                                    "telegram" to telegram,
                                    "discord" to discord,
                                    "contactEmail" to email,
                                    "github" to github,
                                    "redirectUrl" to redirect
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.confirm), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                    WalletViewModel.EditProfileStep.SUCCESS,
                    WalletViewModel.EditProfileStep.PARTIAL_FAILURE,
                    WalletViewModel.EditProfileStep.FAILED -> TextButton(onClick = { closeDialog() }) {
                        Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                    else -> {}
                }
            },
            dismissButton = {
                if (editState.step == WalletViewModel.EditProfileStep.IDLE) {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun EditProfileTextField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = LocalAppColors.current.textPrimary,
            unfocusedTextColor = LocalAppColors.current.textPrimary,
            disabledTextColor = Color.Gray,
            focusedBorderColor = KaspaTeal,
            unfocusedBorderColor = LocalAppColors.current.textSecondary,
            focusedLabelColor = KaspaTeal,
            unfocusedLabelColor = LocalAppColors.current.textSecondary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SeedPhraseScreen(viewModel: WalletViewModel, onBack: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    val mnemonic = remember { viewModel.getActiveMnemonic() ?: "" }
    val privateKey = remember { viewModel.getPrivateKeyHex() }
    val words = remember { mnemonic.split(" ") }
    val clipboardManager = LocalClipboardManager.current

    // Blocks screenshots and screen recording of the seed phrase / private key for as long as
    // this screen is on-screen (window-level flag, the standard Android mechanism - unlike iOS,
    // which has no API to block a screenshot outright, only to detect active screen recording
    // after the fact via UIScreen.isCaptured). Cleared on leaving so it doesn't leak onto other
    // screens.
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Auto-hides the words again after a short window instead of leaving them on screen
    // indefinitely once revealed — someone glancing at the phone later shouldn't still see them.
    LaunchedEffect(revealed) {
        if (revealed) {
            delay(7000)
            revealed = false
        }
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.seed_phrase), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C1E1E))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF39C12),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.security_warning),
                        color = Color(0xFFF39C12),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.anyone_with_your_seed_phrase_can),
                        color = Color(0xFF948B8B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!revealed) {
                Column(
                    modifier = Modifier
                        .clickable { revealed = true }
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = LocalAppColors.current.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.tap_to_reveal_seed_phrase),
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    words.forEachIndexed { index, word ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(LocalAppColors.current.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = LocalAppColors.current.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                text = word,
                                color = LocalAppColors.current.textPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (revealed) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(mnemonic))
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.copy_seed_phrase), color = KaspaTeal)
                        }
                    }
                    TextButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(privateKey))
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tag, null, tint = KaspaTeal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.copy_private_key_hex), color = KaspaTeal)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Unwraps a Compose [android.content.Context] (often a ContextWrapper) to find the real hosting Activity — needed for Credential Manager / Drive authorization, which require an Activity, not just any Context. */
private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    walletViewModel: WalletViewModel = hiltViewModel(),
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val balance by walletViewModel.fullBalance.collectAsState()
    val dotColorHex by connectionViewModel.dotColorHex.collectAsState()
    val darkModeEnabled by walletViewModel.darkModeEnabled.collectAsState()
    val showSetupGuides by walletViewModel.showSetupGuides.collectAsState()
    val currencyCode by walletViewModel.currency.collectAsState()
    val biometricSeedPhraseEnabled by walletViewModel.biometricSeedPhraseEnabled.collectAsState()
    val biometricAccountLoginEnabled by walletViewModel.biometricAccountLoginEnabled.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val showFeeEstimate by settingsViewModel.showFeeEstimate.collectAsState()
    val chatPhotoQualityPreset by chatViewModel.chatPhotoQualityPreset.collectAsState()
    val kaspaExplorer by chatViewModel.kaspaExplorer.collectAsState()
    val syncSystemContactsEnabled by chatViewModel.syncSystemContactsEnabled.collectAsState()
    val autoCreateSystemContactsEnabled by chatViewModel.autoCreateSystemContactsEnabled.collectAsState()
    val exportChatHistoryState by chatViewModel.exportState.collectAsState()
    val importChatHistoryState by chatViewModel.importState.collectAsState()
    val diagnosticsExportState by chatViewModel.diagnosticsExportState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val syncContactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val autoCreatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    val importChatHistoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) chatViewModel.importChatHistory(uri)
    }

    LaunchedEffect(Unit) {
        walletViewModel.refreshBalance()
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .background(LocalAppColors.current.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                TopStatusBar(
                    balance = balance,
                    onStatusClick = { navController.navigate("connection_status") },
                    dotColorHex = dotColorHex,
                    showAddButton = false
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = stringResource(R.string.customization)) {
                SettingsSwitchItem(stringResource(R.string.dark_mode), darkModeEnabled) { enabled ->
                    walletViewModel.setDarkModeEnabled(enabled)
                }
                SettingsDivider()
                SettingsNavigationItem(stringResource(R.string.menu), Icons.Default.Apps, onClick = {
                    navController.navigate("settings_menu")
                })
                SettingsDivider()
                SettingsNavigationItem(stringResource(R.string.language), Icons.Default.Translate, onClick = {
                    navController.navigate("language_settings")
                })
                SettingsDivider()
                SettingsNavigationItem(stringResource(R.string.currency), Icons.Default.AttachMoney, currencyCode.uppercase(), onClick = {
                    navController.navigate("currency_settings")
                })
                SettingsDivider()
                SettingsSwitchItem(stringResource(R.string.show_setup_guides), showSetupGuides) { enabled ->
                    walletViewModel.setShowSetupGuides(enabled)
                }
            }

            SettingsSection(title = stringResource(R.string.security)) {
                SettingsSwitchItem(stringResource(R.string.biometrics_for_seed_phrase), biometricSeedPhraseEnabled) { enabled ->
                    walletViewModel.setBiometricSeedPhraseEnabled(enabled)
                }
                SettingsDivider()
                SettingsSwitchItem(stringResource(R.string.biometrics_for_account_login), biometricAccountLoginEnabled) { enabled ->
                    walletViewModel.setBiometricAccountLoginEnabled(enabled)
                }
            }

            SettingsSection(title = stringResource(R.string.connection)) {
                SettingsNavigationItem(stringResource(R.string.connection_settings), Icons.Default.Language, "Mainnet", onClick = {
                    navController.navigate("connection_settings")
                })
                SettingsDivider()
                SettingsNavigationItem(stringResource(R.string.kaspa_explorer), Icons.Default.Explore, kaspaExplorer.displayName, onClick = {
                    navController.navigate("kaspa_explorer_settings")
                })
            }

            SettingsSection(title = stringResource(R.string.chats)) {
                SettingsSwitchItem(stringResource(R.string.show_fee_estimate), showFeeEstimate) { enabled ->
                    settingsViewModel.setShowFeeEstimate(enabled)
                }
                SettingsDivider()
                SettingsNavigationItem(
                    stringResource(R.string.photo_quality),
                    Icons.Default.Photo,
                    chatPhotoQualityPreset.displayName,
                    onClick = { navController.navigate("photo_quality_settings") }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    stringResource(R.string.notifications),
                    Icons.Default.NotificationsNone,
                    if (notificationsEnabled) "On" else "Off",
                    onClick = { navController.navigate("notification_settings") }
                )
            }

            SettingsSection(title = stringResource(R.string.contacts)) {
                SettingsSwitchItem(stringResource(R.string.sync_system_contacts), syncSystemContactsEnabled) { enabled ->
                    chatViewModel.setSyncSystemContactsEnabled(enabled)
                    if (enabled) syncContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
                SettingsDivider()
                SettingsSwitchItem(stringResource(R.string.autocreate_system_contacts), autoCreateSystemContactsEnabled) { enabled ->
                    chatViewModel.setAutoCreateSystemContactsEnabled(enabled)
                    if (enabled) {
                        autoCreatePermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                        )
                    }
                }
                SettingsFooter(stringResource(R.string.uses_your_device_contacts_to_match))
            }

            SettingsSection(title = stringResource(R.string.storage)) {
                val googleBackupEnabled by chatViewModel.googleBackupEnabled.collectAsState()
                val googleBackupOpState by chatViewModel.googleBackupOpState.collectAsState()
                val restoreState by chatViewModel.restoreState.collectAsState()
                val pendingConsentIntent by chatViewModel.pendingConsentIntent.collectAsState()
                val activity = LocalContext.current.findActivity()

                val consentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    chatViewModel.consentIntentLaunched()
                    result.data?.let { chatViewModel.completeGoogleDriveAuthorization(it) }
                }

                LaunchedEffect(pendingConsentIntent) {
                    pendingConsentIntent?.let { pendingIntent ->
                        consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    }
                }

                val backupInFlight = googleBackupOpState.status == ChatViewModel.GoogleBackupOpStatus.IN_PROGRESS
                val restoreInFlight = restoreState.status == ChatViewModel.ChatHistoryOpStatus.IN_PROGRESS

                SettingsSwitchItem(
                    stringResource(R.string.back_up_to_google_drive),
                    checked = googleBackupEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            activity?.let { chatViewModel.enableGoogleDriveBackup(it) }
                        } else {
                            chatViewModel.disableGoogleDriveBackup()
                        }
                    }
                )
                val backupFooterText = when {
                    backupInFlight -> "Working..."
                    googleBackupOpState.status == ChatViewModel.GoogleBackupOpStatus.FAILED -> googleBackupOpState.message ?: "Something went wrong"
                    googleBackupEnabled && googleBackupOpState.signedInEmail != null -> "Signed in as ${googleBackupOpState.signedInEmail}"
                    else -> "Off by default. Backs up chat history to your own Google Drive as hidden storage, not visible in your regular Drive files."
                }
                SettingsFooter(backupFooterText)

                // Local vs. cloud storage — mirrors iOS's "Local storage used"/"iCloud storage
                // used" split. Android has no live per-record cloud sync like CloudKit; Google
                // Drive backup is one flat JSON file per account, so "cloud" here is just that
                // one file's current size in Drive.
                SettingsDivider()
                val context = LocalContext.current
                val localStorageSizeBytes by chatViewModel.localStorageSizeBytes.collectAsState()
                LaunchedEffect(Unit) { chatViewModel.refreshLocalStorageSize() }
                SettingsInfoItem(
                    label = "Local storage used",
                    value = localStorageSizeBytes?.let { android.text.format.Formatter.formatShortFileSize(context, it) }
                        ?: "Calculating..."
                )

                SettingsDivider()
                val driveBackupSizeState by chatViewModel.driveBackupSizeState.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.google_drive_backup_used), color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = when (driveBackupSizeState.status) {
                                ChatViewModel.DriveSizeStatus.IDLE -> "Not checked"
                                ChatViewModel.DriveSizeStatus.LOADING -> "Checking..."
                                ChatViewModel.DriveSizeStatus.LOADED -> driveBackupSizeState.bytes?.let {
                                    android.text.format.Formatter.formatShortFileSize(context, it)
                                } ?: "No backup found"
                                ChatViewModel.DriveSizeStatus.FAILED -> "Unavailable"
                            },
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (driveBackupSizeState.status == ChatViewModel.DriveSizeStatus.LOADING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KaspaTeal, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { chatViewModel.refreshDriveBackupSize() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.check_drive_backup_size), tint = KaspaTeal)
                        }
                    }
                }

                if (googleBackupEnabled) {
                    SettingsDivider()
                    SettingsActionItem(
                        label = if (backupInFlight) "Backing Up..." else "Back Up Now",
                        icon = Icons.Default.CloudUpload,
                        color = if (backupInFlight) Color.Gray else KaspaTeal
                    ) {
                        if (!backupInFlight) chatViewModel.backupNow()
                    }
                    SettingsDivider()
                    SettingsActionItem(
                        label = if (restoreInFlight) "Restoring..." else "Restore from Google Drive",
                        icon = Icons.Default.CloudDownload,
                        color = if (restoreInFlight) Color.Gray else KaspaTeal
                    ) {
                        if (!restoreInFlight) chatViewModel.restoreFromGoogleDrive()
                    }
                    if (restoreState.status == ChatViewModel.ChatHistoryOpStatus.SUCCESS) {
                        SettingsFooter(restoreState.message ?: "Restore complete.")
                    }
                    if (restoreState.status == ChatViewModel.ChatHistoryOpStatus.FAILED) {
                        SettingsFooter(restoreState.message ?: "Restore failed.")
                    }

                    SettingsDivider()

                    val backupRetention by chatViewModel.backupRetention.collectAsState()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.retention), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf(
                                Triple("Forever", BackupRetention.FOREVER, 0),
                                Triple("30 Days", BackupRetention.DAYS_30, 1),
                                Triple("90 Days", BackupRetention.DAYS_90, 2)
                            )
                            options.forEach { (label, value, index) ->
                                SegmentedButton(
                                    selected = backupRetention == value,
                                    onClick = { chatViewModel.setBackupRetention(value) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = LocalAppColors.current.surfaceVariant,
                                        activeContentColor = LocalAppColors.current.textPrimary,
                                        inactiveContainerColor = LocalAppColors.current.surface,
                                        inactiveContentColor = LocalAppColors.current.textSecondary
                                    )
                                ) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (backupRetention == BackupRetention.FOREVER) {
                                "Chat history is kept forever and backed up as-is."
                            } else {
                                "Messages older than ${backupRetention.days} days are permanently deleted from this device, not just excluded from the backup. This cannot be undone."
                            },
                            color = if (backupRetention == BackupRetention.FOREVER) Color.Gray else Color(0xFFFF3B30),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.chat_history)) {
                val exportInFlight = exportChatHistoryState.status == ChatViewModel.ChatHistoryOpStatus.IN_PROGRESS
                val importInFlight = importChatHistoryState.status == ChatViewModel.ChatHistoryOpStatus.IN_PROGRESS

                SettingsActionItem(
                    label = if (exportInFlight) "Exporting..." else "Export Chat History",
                    icon = Icons.Default.FileUpload,
                    color = if (exportInFlight) Color.Gray else KaspaTeal
                ) {
                    if (!exportInFlight) {
                        chatViewModel.exportChatHistory { uri ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Chat History"))
                        }
                    }
                }
                if (exportChatHistoryState.status == ChatViewModel.ChatHistoryOpStatus.FAILED) {
                    Text(
                        exportChatHistoryState.message ?: "Export failed",
                        color = Color(0xFFFF3B30),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                SettingsDivider()

                SettingsActionItem(
                    label = if (importInFlight) "Importing..." else "Import Chat History",
                    icon = Icons.Default.FileDownload,
                    color = if (importInFlight) Color.Gray else KaspaTeal
                ) {
                    if (!importInFlight) {
                        importChatHistoryLauncher.launch(arrayOf("application/json"))
                    }
                }
                if (importChatHistoryState.status == ChatViewModel.ChatHistoryOpStatus.SUCCESS) {
                    Text(
                        importChatHistoryState.message ?: "Import complete",
                        color = Color(0xFF4CD964),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                if (importChatHistoryState.status == ChatViewModel.ChatHistoryOpStatus.FAILED) {
                    Text(
                        importChatHistoryState.message ?: "Import failed",
                        color = Color(0xFFFF3B30),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                SettingsFooter(stringResource(R.string.exports_a_plaintext_json_file_of))
            }

            SettingsSection(title = stringResource(R.string.diagnostics)) {
                val diagnosticsExportInFlight = diagnosticsExportState.status == ChatViewModel.ChatHistoryOpStatus.IN_PROGRESS

                SettingsActionItem(
                    label = if (diagnosticsExportInFlight) "Exporting..." else "Export Diagnostics Archive",
                    icon = Icons.Default.BugReport,
                    color = if (diagnosticsExportInFlight) Color.Gray else KaspaTeal
                ) {
                    if (!diagnosticsExportInFlight) {
                        chatViewModel.exportDiagnostics { uri ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Diagnostics Archive"))
                        }
                    }
                }
                if (diagnosticsExportState.status == ChatViewModel.ChatHistoryOpStatus.FAILED) {
                    Text(
                        diagnosticsExportState.message ?: "Export failed",
                        color = Color(0xFFFF3B30),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                SettingsFooter(stringResource(R.string.exports_app_device_info_connection_settings))
            }

            SettingsSection(title = stringResource(R.string.actions)) {
                SettingsActionItem(stringResource(R.string.view_seed_phrase), Icons.Default.Key, KaspaTeal) {
                    if (biometricSeedPhraseEnabled) {
                        context.authenticateWithDeviceCredential(
                            title = "Unlock to View Seed Phrase",
                            onSuccess = { navController.navigate("seed_phrase") }
                        )
                    } else {
                        navController.navigate("seed_phrase")
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.danger_zone)) {
                val activeAddress by walletViewModel.address.collectAsState()
                val wipeIncomingState by chatViewModel.wipeIncomingState.collectAsState()
                val wipeAccountState by chatViewModel.wipeAccountState.collectAsState()
                var showWipeIncomingConfirm by remember { mutableStateOf(false) }
                var showWipeAccountConfirm by remember { mutableStateOf(false) }
                var showWipeAccountCloudConfirm by remember { mutableStateOf(false) }

                val wipeIncomingInFlight = wipeIncomingState.status == ChatViewModel.DangerZoneOpStatus.IN_PROGRESS
                val wipeAccountInFlight = wipeAccountState.status == ChatViewModel.DangerZoneOpStatus.IN_PROGRESS

                SettingsActionItem(
                    label = if (wipeIncomingInFlight) "Wiping..." else "Wipe and re-sync incoming messages",
                    icon = Icons.Default.Cached,
                    color = if (wipeIncomingInFlight) Color.Gray else Color.Red
                ) {
                    if (!wipeIncomingInFlight) showWipeIncomingConfirm = true
                }
                if (wipeIncomingState.status == ChatViewModel.DangerZoneOpStatus.SUCCESS || wipeIncomingState.status == ChatViewModel.DangerZoneOpStatus.FAILED) {
                    SettingsFooter(wipeIncomingState.message ?: "Done")
                }
                SettingsDivider()
                SettingsActionItem(
                    label = if (wipeAccountInFlight) "Wiping..." else "Wipe account & messages",
                    icon = Icons.Default.PersonRemoveAlt1,
                    color = if (wipeAccountInFlight) Color.Gray else Color.Red
                ) {
                    if (!wipeAccountInFlight && activeAddress != null) showWipeAccountConfirm = true
                }
                SettingsDivider()
                SettingsActionItem(
                    label = if (wipeAccountInFlight) "Wiping..." else "Wipe account & messages & Cloud",
                    icon = Icons.Default.CloudOff,
                    color = if (wipeAccountInFlight) Color.Gray else Color.Red
                ) {
                    if (!wipeAccountInFlight && activeAddress != null) showWipeAccountCloudConfirm = true
                }
                if (wipeAccountState.status == ChatViewModel.DangerZoneOpStatus.FAILED) {
                    SettingsFooter(wipeAccountState.message ?: "Failed")
                }

                if (showWipeIncomingConfirm) {
                    AlertDialog(
                        onDismissRequest = { showWipeIncomingConfirm = false },
                        containerColor = LocalAppColors.current.surface,
                        title = { Text(stringResource(R.string.wipe_and_re_sync_incoming_messages), color = LocalAppColors.current.textPrimary) },
                        text = {
                            Text(
                                stringResource(R.string.this_removes_all_incoming_messages_locally),
                                color = LocalAppColors.current.textSecondary
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showWipeIncomingConfirm = false
                                chatViewModel.wipeIncomingMessages()
                            }) {
                                Text(stringResource(R.string.wipe_incoming_messages), color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWipeIncomingConfirm = false }) {
                                Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                            }
                        }
                    )
                }

                if (showWipeAccountConfirm) {
                    val address = activeAddress
                    AlertDialog(
                        onDismissRequest = { showWipeAccountConfirm = false },
                        containerColor = LocalAppColors.current.surface,
                        title = { Text(stringResource(R.string.wipe_account_messages), color = LocalAppColors.current.textPrimary) },
                        text = {
                            Text(
                                stringResource(R.string.this_permanently_deletes_this_account_s),
                                color = LocalAppColors.current.textSecondary
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showWipeAccountConfirm = false
                                if (address != null) {
                                    chatViewModel.wipeAccountAndMessages(address, alsoDeleteCloud = false) {
                                        walletViewModel.deleteWallet(address)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.wipe_account), color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWipeAccountConfirm = false }) {
                                Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                            }
                        }
                    )
                }

                if (showWipeAccountCloudConfirm) {
                    val address = activeAddress
                    AlertDialog(
                        onDismissRequest = { showWipeAccountCloudConfirm = false },
                        containerColor = LocalAppColors.current.surface,
                        title = { Text(stringResource(R.string.wipe_account_messages_cloud), color = LocalAppColors.current.textPrimary) },
                        text = {
                            Text(
                                stringResource(R.string.this_permanently_deletes_this_account_s_2),
                                color = LocalAppColors.current.textSecondary
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showWipeAccountCloudConfirm = false
                                if (address != null) {
                                    chatViewModel.wipeAccountAndMessages(address, alsoDeleteCloud = true) {
                                        walletViewModel.deleteWallet(address)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.wipe_everything), color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWipeAccountCloudConfirm = false }) {
                                Text(stringResource(R.string.cancel), color = LocalAppColors.current.textSecondary)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, headerAction: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LocalAppColors.current.textSecondary,
                modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 8.dp)
            )
            headerAction?.invoke()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LocalAppColors.current.surface)
        ) {
            content()
        }
    }
}

/**
 * Same card chrome as [SettingsSection], but starts collapsed to a single summary row — for
 * sections whose content (KNS domains/profile fields) isn't relevant on every visit to the
 * Profile screen and would otherwise push more useful sections further down.
 */
@Composable
fun CollapsibleSettingsSection(
    title: String,
    summary: String,
    description: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)) {
                    append(title)
                }
                if (!description.isNullOrBlank()) {
                    append("  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, fontSize = MaterialTheme.typography.bodySmall.fontSize)) {
                        append(description)
                    }
                }
            },
            color = LocalAppColors.current.textSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LocalAppColors.current.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    summary,
                    color = LocalAppColors.current.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = KaspaTeal
                )
            }
            if (expanded) {
                HorizontalDivider(color = LocalAppColors.current.divider)
                content()
            }
        }
    }
}

/** A circular icon button with its label centered underneath — used in pairs on [ProfileScreen] (Accept Kaspa As Payment / Fund Chatting Address). */
@Composable
private fun ProfileCircleAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(KaspaTeal)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = LocalAppColors.current.textPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Same card chrome as [CollapsibleSettingsSection], but the title itself (with its optional
 * description) is the clickable dropdown header — there's no separate outer label or truncated
 * preview line, since revealing the address is the entire point of expanding.
 */
@Composable
fun CollapsibleAddressSection(
    title: String,
    balance: String? = null,
    description: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAppColors.current.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(description, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!balance.isNullOrBlank()) {
                Text(balance, color = KaspaTeal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = KaspaTeal
            )
        }
        if (expanded) {
            HorizontalDivider(color = LocalAppColors.current.divider)
            content()
        }
    }
}

@Composable
fun SettingsSwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = LocalAppColors.current.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KaspaTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun SettingsNavigationItem(label: String, icon: ImageVector?, value: String = "", showIcon: Boolean = true, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon && icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = KaspaTeal, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(text = label, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(text = value, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun SettingsActionItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsInfoItem(label: String, value: String, valueColor: Color = LocalAppColors.current.textSecondary, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, color = valueColor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = LocalAppColors.current.divider, thickness = 0.5.dp)
}

@Composable
fun SettingsFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalAppColors.current.textSecondary,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun TopStatusBar(
    balance: String,
    onStatusClick: () -> Unit,
    onAddClick: () -> Unit = {},
    dotColorHex: Long = 0xFF4CD964,
    // Chats has this moved to a floating action button instead (see ChatsScreen.kt), and
    // Profile/Settings have no "add chat" action of their own - every current caller passes
    // false explicitly, so this default only matters for a future screen that doesn't.
    showAddButton: Boolean = true,
    showEditButton: Boolean = false,
    isEditing: Boolean = false,
    onEditClick: () -> Unit = {},
    selectAllLabel: String? = null,
    onSelectAllClick: () -> Unit = {},
    // Profile-only (matches iOS's gear button in ProfileView's toolbar) — Settings isn't a
    // bottom-tab destination, this is its only entry point.
    showSettingsButton: Boolean = false,
    onSettingsClick: () -> Unit = {}
) {
    val statusColor = Color(dotColorHex)

    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onStatusClick,
            modifier = Modifier
                .size(40.dp)
                .background(LocalAppColors.current.surface, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
        }

        Text(
            text = balance,
            color = LocalAppColors.current.textPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEditing && selectAllLabel != null) {
                TextButton(onClick = onSelectAllClick) {
                    Text(selectAllLabel, color = KaspaTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(4.dp))
            }
            if (showEditButton) {
                // Text toggle instead of a pen icon, matching iOS's "Select"/"Cancel" toolbar
                // button (ChatListView.swift) rather than a Material edit-pencil affordance.
                TextButton(onClick = onEditClick) {
                    Text(
                        text = if (isEditing) "Cancel" else "Select",
                        color = KaspaTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                if (!isEditing) Spacer(Modifier.width(8.dp))
            }
            if (!isEditing) {
                if (showAddButton) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(LocalAppColors.current.surface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAddAlt1,
                            contentDescription = stringResource(R.string.add_contact),
                            tint = KaspaTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (showSettingsButton) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(LocalAppColors.current.surface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = KaspaTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (!showEditButton) {
                    // Keeps the balance text centered between the two ends, same as when the button is shown.
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionStatusScreen(onBack: () -> Unit, viewModel: ConnectionViewModel = hiltViewModel()) {
    val network by viewModel.network.collectAsState()
    val indexerUrl by viewModel.indexerUrl.collectAsState()
    val pushIndexerUrl by viewModel.pushIndexerUrl.collectAsState()

    val activeNodes by viewModel.activeNodes.collectAsState()
    val allNodes by viewModel.allNodes.collectAsState()
    // "Other Nodes" means genuinely other than what's already listed above under Active
    // Nodes — allNodes includes every known node (active and not), so without this filter
    // the same active nodes showed up twice, once in each section.
    val otherNodes = remember(allNodes) { allNodes.filterNot { it.status == "Active" } }
    val status by viewModel.status.collectAsState()
    val dotColorHex by viewModel.dotColorHex.collectAsState()
    val lastSyncAt by viewModel.lastSyncAt.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // status is derived from the exact same latency threshold as dotColorHex, so
    // the text here can never contradict the dot's color: green only says
    // Connected/Healthy, orange only says Degraded, red only says Disconnected/Unhealthy.
    val statusColor = Color(dotColorHex)
    val statusText = when (status) {
        ConnStatus.CONNECTED -> "Connected"
        ConnStatus.DEGRADED -> "Degraded"
        ConnStatus.DISCONNECTED -> "Disconnected"
    }
    val poolHealthText = when (status) {
        ConnStatus.CONNECTED -> "Healthy"
        ConnStatus.DEGRADED -> "Degraded"
        ConnStatus.DISCONNECTED -> "Unhealthy"
    }
    // "Verified" = currently reachable at all (Active or Suspect), a broader real count
    // than "Active" (Active additionally requires being in-sync and not recently failing).
    // Excludes nodes that haven't been probed even once yet (latency == "—") — those also
    // report as "Suspect" (see NodeRegistry.statusOf's lastProbe == null branch), but counting
    // them as "Verified" is misleading: right after a fresh launch/DNS-seed resolution this made
    // the whole pool look "Verified" before a single probe had actually completed.
    val verifiedCount = allNodes.count { (it.status == "Active" || it.status == "Suspect") && it.latency != "—" }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.connection_status), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.done), color = KaspaTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = stringResource(R.string.connection_status)) {
                ConnectionInfoRow(stringResource(R.string.status), statusText, statusColor)
                SettingsDivider()
                ConnectionInfoRow(
                    stringResource(R.string.protocol),
                    if (activeNodes.firstOrNull()?.ip?.let { com.kachat.app.services.grpc.parseNodeAddress(it)?.secure } == true) {
                        "gRPC (secure)"
                    } else {
                        "gRPC (plaintext)"
                    }
                )
                SettingsDivider()
                ConnectionInfoRow(stringResource(R.string.connected_node), activeNodes.firstOrNull()?.ip ?: "None")
                SettingsDivider()
                ConnectionInfoRow(stringResource(R.string.latency), activeNodes.firstOrNull()?.latency ?: "—", statusColor)
                SettingsDivider()
                ConnectionInfoRow(stringResource(R.string.indexer), indexerUrl.substringAfter("://").substringBefore("/"))
                SettingsDivider()
                ConnectionInfoRow(stringResource(R.string.push_register), pushIndexerUrl.substringAfter("://").substringBefore("/"))
                SettingsDivider()
                ConnectionInfoRow(stringResource(R.string.last_sync), lastSyncAt)
            }

            SettingsSection(title = stringResource(R.string.pool_status)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    PoolStatItem(stringResource(R.string.active), activeNodes.size.toString(), Color(0xFF4CD964))
                    PoolStatItem(stringResource(R.string.verified), verifiedCount.toString(), Color(0xFF2196F3))
                    PoolStatItem(stringResource(R.string.total), allNodes.size.toString(), Color.Gray)
                }
                SettingsDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.pool_health), color = LocalAppColors.current.textPrimary)
                    Text(poolHealthText, color = statusColor)
                }
            }

            SettingsSection(title = stringResource(R.string.actions)) {
                SettingsActionItem(stringResource(R.string.refresh_pool), Icons.Default.Refresh, KaspaTeal, onClick = {
                    viewModel.refreshPool()
                    coroutineScope.launch { snackbarHostState.showSnackbar("Refreshing pool…") }
                })
                SettingsDivider()
                SettingsActionItem(stringResource(R.string.clear_connection_pool), Icons.Default.DeleteSweep, Color.Red, onClick = {
                    viewModel.clearPool()
                    coroutineScope.launch { snackbarHostState.showSnackbar("Pool cleared, reconnecting to seed nodes") }
                })
                SettingsDivider()
                SettingsActionItem(stringResource(R.string.reconnect), Icons.Default.Replay, KaspaTeal, onClick = {
                    viewModel.reconnect()
                    coroutineScope.launch { snackbarHostState.showSnackbar("Reconnecting…") }
                })
            }

            Text(
                text = "Primary: ${activeNodes.firstOrNull()?.ip ?: "None"}",
                color = LocalAppColors.current.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CD964), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.active_nodes), style = MaterialTheme.typography.titleMedium, color = LocalAppColors.current.textPrimary)
                }
                Text(text = activeNodes.size.toString(), color = LocalAppColors.current.textSecondary)
            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LocalAppColors.current.surface)
            ) {
                activeNodes.forEachIndexed { index, node ->
                    ActiveNodeRow(node)
                    if (index < activeNodes.size - 1) SettingsDivider()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.other_nodes), style = MaterialTheme.typography.titleMedium, color = LocalAppColors.current.textPrimary)
                }
                Text(text = otherNodes.size.toString(), color = LocalAppColors.current.textSecondary)
            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LocalAppColors.current.surface)
            ) {
                otherNodes.forEachIndexed { index, node ->
                    AllNodeRow(node)
                    if (index < otherNodes.size - 1) SettingsDivider()
                }
            }

            Text(
                text = stringResource(R.string.all_discovered_nodes_sorted_by_state),
                style = MaterialTheme.typography.bodySmall,
                color = LocalAppColors.current.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.notificationSoundEnabled.collectAsState()
    val vibrationEnabled by viewModel.notificationVibrationEnabled.collectAsState()

    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.notifications), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (!permissionGranted) {
                Surface(
                    color = Color(0xFF2C1C1C),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.notifications_are_off_in_system_settings), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.kachat_can_t_show_notifications_until),
                            color = LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        }) {
                            Text(stringResource(R.string.open_settings), color = KaspaTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.push_notifications)) {
                SettingsSwitchItem(stringResource(R.string.notifications), notificationsEnabled && permissionGranted) {
                    viewModel.setNotificationsEnabled(it)
                }
                SettingsFooter(
                    if (notificationsEnabled && permissionGranted)
                        "Receive notifications when contacts send messages while KaChat is running. There's no remote push server yet, so nothing arrives once the app has been fully closed by the system."
                    else
                        "Notifications are disabled."
                )
            }

            if (notificationsEnabled && permissionGranted) {
                SettingsSection(title = stringResource(R.string.sound_vibration)) {
                    SettingsSwitchItem(stringResource(R.string.play_sound), soundEnabled) {
                        viewModel.setNotificationSoundEnabled(it)
                    }
                    SettingsDivider()
                    SettingsSwitchItem(stringResource(R.string.vibration), vibrationEnabled) {
                        viewModel.setNotificationVibrationEnabled(it)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Global default photo-compression quality for chat photos — mirrors iOS's
 * `PhotoQualitySettingsSheet`/`ChatPhotoQualitySlider`. Writes take effect immediately (no
 * separate Save step), matching every other row in [SettingsScreen]; only affects photos attached
 * after the change, never a photo already staged in a chat's composer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoQualitySettingsScreen(onBack: () -> Unit, chatViewModel: ChatViewModel = hiltViewModel()) {
    val preset by chatViewModel.chatPhotoQualityPreset.collectAsState()
    val presets = com.kachat.app.models.ChatPhotoQualityPreset.entries
    val sliderPosition = presets.indexOf(preset).coerceAtLeast(0).toFloat()

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.photo_quality), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.controls_how_much_photos_are_compressed),
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection(title = stringResource(R.string.chats)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.photo_quality_2), color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(preset.summaryText, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            chatViewModel.updateChatPhotoQualityPreset(
                                com.kachat.app.models.ChatPhotoQualityPreset.fromSliderValue(it.toInt())
                            )
                        },
                        valueRange = 0f..(presets.size - 1).toFloat(),
                        steps = presets.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = KaspaTeal,
                            inactiveTrackColor = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaspaExplorerSettingsScreen(onBack: () -> Unit, chatViewModel: ChatViewModel = hiltViewModel()) {
    val kaspaExplorer by chatViewModel.kaspaExplorer.collectAsState()

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.kaspa_explorer), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.transaction_links_in_message_menus_and),
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection(title = stringResource(R.string.explorer)) {
                com.kachat.app.models.KaspaExplorer.entries.forEachIndexed { index, explorer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chatViewModel.updateKaspaExplorer(explorer) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(explorer.displayName, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (kaspaExplorer == explorer) {
                            Icon(Icons.Default.Check, null, tint = KaspaTeal)
                        }
                    }
                    if (index < com.kachat.app.models.KaspaExplorer.entries.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(onBack: () -> Unit, viewModel: ConnectionViewModel = hiltViewModel()) {
    val network by viewModel.network.collectAsState()
    val indexerUrl by viewModel.indexerUrl.collectAsState()
    val pushIndexerUrl by viewModel.pushIndexerUrl.collectAsState()
    val knsApiUrl by viewModel.knsApiUrl.collectAsState()
    val kaspaRestApiUrl by viewModel.kaspaRestApiUrl.collectAsState()
    val trustedNodeAddress by viewModel.trustedNodeAddress.collectAsState()
    val savedNodeAddresses by viewModel.savedNodeAddresses.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.connection_settings), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.surfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp).padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = stringResource(R.string.network)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.network), color = LocalAppColors.current.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(network, color = KaspaTeal)
                        Icon(Icons.Default.UnfoldMore, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                SettingsFooter(stringResource(R.string.select_mainnet_for_real_transactions_or))
            }

            SettingsSection(title = stringResource(R.string.kachat_indexer)) {
                ConnectionUrlField(label = "Indexer URL", value = indexerUrl)
                SettingsFooter(stringResource(R.string.message_indexer_service_for_chat_functionality))
            }

            SettingsSection(title = stringResource(R.string.push_registration)) {
                ConnectionUrlField(label = "Push Indexer URL", value = pushIndexerUrl)
                SettingsFooter(stringResource(R.string.used_only_for_push_registration_and))
            }

            SettingsSection(title = stringResource(R.string.kaspa_name_service)) {
                ConnectionUrlField(label = "KNS API URL", value = knsApiUrl)
                SettingsFooter(stringResource(R.string.kns_domain_resolution_service))
            }

            SettingsSection(title = stringResource(R.string.kaspa_explorer_api)) {
                ConnectionUrlField(label = "Kaspa REST API URL", value = kaspaRestApiUrl)
                SettingsFooter(stringResource(R.string.rest_api_for_transaction_history_and))
            }

            SettingsSection(title = stringResource(R.string.kaspa_node)) {
                var endpoint by remember(trustedNodeAddress) { mutableStateOf(trustedNodeAddress) }
                var validationError by remember { mutableStateOf<String?>(null) }
                val keyboardController = LocalSoftwareKeyboardController.current

                fun save() {
                    val trimmed = endpoint.trim()
                    if (trimmed.isEmpty()) {
                        validationError = null
                        viewModel.setTrustedNodeAddress("")
                    } else if (com.kachat.app.services.grpc.parseNodeAddress(trimmed) == null) {
                        validationError = "Enter as host:port or grpcs://host"
                    } else {
                        validationError = null
                        viewModel.setTrustedNodeAddress(trimmed)
                    }
                    keyboardController?.hide()
                }

                TextField(
                    value = endpoint,
                    onValueChange = { endpoint = it; validationError = null },
                    placeholder = { Text(stringResource(R.string.host_port_or_grpcs_host), color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp).clip(RoundedCornerShape(12.dp)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
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
                TextButton(
                    onClick = {
                        endpoint = com.kachat.app.repository.AppSettingsRepository.DEFAULT_TRUSTED_NODE_ADDRESS
                        validationError = null
                        viewModel.setTrustedNodeAddress(com.kachat.app.repository.AppSettingsRepository.DEFAULT_TRUSTED_NODE_ADDRESS)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Use Default (${com.kachat.app.repository.AppSettingsRepository.DEFAULT_TRUSTED_NODE_ADDRESS})", color = KaspaTeal)
                }
                validationError?.let {
                    Text(it, color = Color(0xFFFF3B30), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                if (trustedNodeAddress.isNotBlank()) {
                    SettingsDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.connected_only_to_this_node), color = KaspaTeal, fontSize = 13.sp)
                        TextButton(onClick = {
                            endpoint = ""
                            validationError = null
                            viewModel.setTrustedNodeAddress("")
                        }) {
                            Text(stringResource(R.string.clear), color = Color(0xFFFF3B30))
                        }
                    }
                }
                SettingsFooter(
                    if (trustedNodeAddress.isNotBlank()) {
                        "KaChat connects only to this node for sending and message scanning, and won't search for or fall back to other nodes. Clear, or edit and tap Done on the keyboard, to change it."
                    } else {
                        "Enter a trusted node (host:port for plaintext, or grpcs://host for TLS-secured nodes like Kaspium's) and tap Done on the keyboard to always connect to it instead of automatic discovery. Doesn't affect the Indexer/KNS/REST API URLs above."
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.ip_address_book)) {
                var newLabel by remember { mutableStateOf("") }
                var newAddress by remember { mutableStateOf("") }
                var addError by remember { mutableStateOf<String?>(null) }
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current

                TextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    placeholder = { Text(stringResource(R.string.label_optional), color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(50.dp).clip(RoundedCornerShape(12.dp)),
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newAddress,
                        onValueChange = { newAddress = it; addError = null },
                        placeholder = { Text(stringResource(R.string.host_port_or_grpcs_host), color = Color.DarkGray) },
                        modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(12.dp)),
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
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            val trimmed = newAddress.trim()
                            if (trimmed.isEmpty()) return@IconButton
                            if (com.kachat.app.services.grpc.parseNodeAddress(trimmed) == null) {
                                addError = "Enter as host:port or grpcs://host"
                            } else {
                                addError = null
                                viewModel.addSavedNodeAddress(newLabel.trim(), trimmed)
                                newLabel = ""
                                newAddress = ""
                            }
                        },
                        modifier = Modifier.size(40.dp).background(KaspaTeal, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black)
                    }
                }
                addError?.let {
                    Text(it, color = Color(0xFFFF3B30), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }

                if (savedNodeAddresses.isEmpty()) {
                    Text(
                        stringResource(R.string.no_saved_addresses),
                        color = LocalAppColors.current.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    savedNodeAddresses.forEachIndexed { index, entry ->
                        if (index > 0) SettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(entry.address))
                                    Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                if (entry.label.isNotBlank()) {
                                    Text(entry.label, color = LocalAppColors.current.textPrimary)
                                    Text(
                                        entry.address,
                                        color = LocalAppColors.current.textSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Text(entry.address, color = LocalAppColors.current.textPrimary, fontFamily = FontFamily.Monospace)
                                }
                            }
                            IconButton(onClick = { viewModel.removeSavedNodeAddress(entry.id) }) {
                                Icon(Icons.Default.Delete, null, tint = LocalAppColors.current.textSecondary)
                            }
                        }
                    }
                }
                SettingsFooter(stringResource(R.string.save_your_own_node_addresses_here))
            }

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.reset_to_defaults), color = Color.Red, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/** Read-only for now — editing these let a mistyped URL crash the whole app (fixed at the NetworkService layer too, but not editable at all is safer). */
@Composable
fun ConnectionUrlField(label: String, value: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(label, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = LocalAppColors.current.textPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ConnectionInfoRow(label: String, value: String, valueColor: Color = LocalAppColors.current.textSecondary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (label == "Status") {
                Box(modifier = Modifier.size(8.dp).background(valueColor, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PoolStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActiveNodeRow(node: NodeInfo) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // weight(1f) here (not on the status badge) so a long address — real discovered
            // peers can be IPv6 literals like [2601:680:cc80:5630:e1e5:e6fa:c86b:b946]:16111,
            // much longer than the old hardcoded IPv4 seeds — elides instead of squeezing the
            // badge down to near-zero width, which forced its own text to wrap letter by letter.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Shield, null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(node.ip, color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.background(Color(0xFF1E3A1E), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(node.status, color = Color(node.color), fontSize = 10.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row {
                Text(node.type, color = Color(0xFF2196F3), fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                Text(node.latency, color = Color(0xFFF39C12), fontSize = 10.sp)
            }
            Text("DAA: ${node.daaScore}", color = LocalAppColors.current.textSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun AllNodeRow(node: NodeInfo) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // weight(1f) here (not on the latency/status side) so a long address — real discovered
        // peers can be IPv6 literals like [2601:680:cc80:5630:e1e5:e6fa:c86b:b946]:16111, much
        // longer than the old hardcoded IPv4 seeds — elides instead of squeezing the status
        // badge down to near-zero width, which forced its own text to wrap letter by letter.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(8.dp).background(Color(node.color), CircleShape))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Shield, null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(node.ip, color = LocalAppColors.current.textPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(node.latency, color = Color(0xFFF39C12), fontSize = 10.sp)
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.background(Color(0x33FF3B30), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(node.status, color = Color(node.color), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun rememberQrBitmapPainter(
    content: String,
    size: Int = 512,
    padding: Int = 0
): BitmapPainter {
    val density = LocalDensity.current
    val sizePx = with(density) { size.dp.roundToPx() }
    
    val bitmap = remember(content) {
        if (content.isEmpty()) {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        } else {
            val matrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                mapOf(EncodeHintType.MARGIN to padding)
            )
            val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bitmap.asImageBitmap()
        }
    }
    return BitmapPainter(bitmap)
}

/**
 * Byte-mode QR encoding for raw binary payloads (e.g. a KSPT transaction frame) — ISO-8859-1 maps
 * every byte 1:1 to a char and back, so wrapping [bytes] this way and forcing ZXing's
 * `CHARACTER_SET` hint round-trips the exact bytes through its String-based encoder in byte mode,
 * matching KasSigner's own raw-byte QR encoding (`qrcode::QrCode::new(&[u8])`) on the other end.
 */
@Composable
fun rememberQrBitmapPainter(
    bytes: ByteArray,
    size: Int = 512,
    padding: Int = 0
): BitmapPainter {
    val density = LocalDensity.current
    val sizePx = with(density) { size.dp.roundToPx() }

    val bitmap = remember(bytes) {
        if (bytes.isEmpty()) {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        } else {
            val content = String(bytes, Charsets.ISO_8859_1)
            val matrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                mapOf(EncodeHintType.MARGIN to padding, EncodeHintType.CHARACTER_SET to "ISO-8859-1")
            )
            val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bmp.asImageBitmap()
        }
    }
    return BitmapPainter(bitmap)
}

/**
 * Cycles through [frames] at a fixed interval — the display half of KasSigner's animated multi-
 * frame QR protocol (see [com.kachat.app.util.QrFrameChunker]). A single-frame list just renders
 * a static code with no play/pause controls or frame counter.
 */
@Composable
fun AnimatedQrDisplay(frames: List<ByteArray>, modifier: Modifier = Modifier, frameDelayMs: Long = 2500L) {
    var frameIndex by remember(frames) { mutableStateOf(0) }
    // Matches KasSee's own 2.5s auto-advance (kassee/web/js/app.js's displayKsptQr) — a scanning
    // camera needs real time to lock onto and decode each frame; a faster cycle (this used to
    // default to 200ms) skips past frames before the scanner ever catches them.
    var isPlaying by remember(frames) { mutableStateOf(true) }

    LaunchedEffect(frames, isPlaying) {
        if (frames.size <= 1 || !isPlaying) return@LaunchedEffect
        while (true) {
            delay(frameDelayMs)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val painter = rememberQrBitmapPainter(bytes = frames[frameIndex.coerceIn(frames.indices)])
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(2.dp, KaspaTeal, RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painter, contentDescription = stringResource(R.string.qr_code_2), modifier = Modifier.fillMaxSize())
        }
        if (frames.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                frames.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            // Fixed, not theme-based — this always sits on a forced-white card
                            // (see ColdSendFlow), so the inactive dot needs to read against white
                            // specifically rather than whatever the app's own surfaceVariant is.
                            .background(if (i == frameIndex) KaspaTeal else Color(0xFFD0D0D5))
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Manual stepping works whether playing or paused — tapping prev/next doesn't
                // require pausing first, matching KasSee's own frame-nav buttons.
                IconButton(onClick = { frameIndex = (frameIndex - 1 + frames.size) % frames.size }) {
                    Icon(Icons.Default.SkipPrevious, "Previous frame", tint = KaspaTeal)
                }
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        tint = KaspaTeal
                    )
                }
                IconButton(onClick = { frameIndex = (frameIndex + 1) % frames.size }) {
                    Icon(Icons.Default.SkipNext, "Next frame", tint = KaspaTeal)
                }
                Text("Frame ${frameIndex + 1} / ${frames.size}", color = Color(0xFF6B6B70), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Full-bleed QR overlay over the current screen's content area — matches iOS's push-navigated
 *  QR screens (full-screen white, dismissed via a back arrow/system back, not a tap-anywhere
 *  gesture that could be triggered by mistake). [message], if given, renders as a caption below
 *  the code (e.g. funding guidance). */
@Composable
fun QrCodeOverlay(
    value: String,
    onDismiss: () -> Unit,
    message: String? = null,
    borderColor: Color = KaspaTeal,
    borderWidth: Dp = 2.dp
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    BackHandler(onBack = onDismiss)

    // Full-screen white, not the app's own themed background — a bright, high-contrast quiet
    // zone around the code is what actually gets a reliable scan on another device's camera,
    // regardless of whether KaChat itself is in light or dark mode.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val qrPainter = rememberQrBitmapPainter(value)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = qrPainter,
                    contentDescription = stringResource(R.string.qr_code),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = value,
                color = Color.Black,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(value))
                Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, null, tint = KaspaTeal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.copy_address), color = KaspaTeal, fontWeight = FontWeight.Bold)
            }
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = Color(0xFF6B6B70),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatScreen(
    onBack: () -> Unit,
    onChatCreated: (String) -> Unit,
    onGroupCreated: (String) -> Unit = {},
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // Group chat mode — same screen, just a toggle: instead of one address, up to 10, plus a
    // group name instead of an optional contact nickname.
    var isGroupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupAddressRows by remember { mutableStateOf(listOf(GroupAddressRow())) }
    var scanningGroupRowId by remember { mutableStateOf<String?>(null) }
    var importingGroupRowId by remember { mutableStateOf<String?>(null) }
    val isCreatingGroup by chatViewModel.isCreatingGroup.collectAsState()
    val createGroupError by chatViewModel.createGroupError.collectAsState()
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val isValidRawAddress = remember(address) { KaspaAddress.isValid(address) }
    val looksLikeKnsDomain = remember(address) { com.kachat.app.services.KnsService.looksLikeDomain(address) }

    val knsResolvedAddress by chatViewModel.knsResolvedAddress.collectAsState()
    val isResolvingKns by chatViewModel.isResolvingKns.collectAsState()
    val knsError by chatViewModel.knsError.collectAsState()

    val context = LocalContext.current
    // Reads the picked contact's data via the /entities sub-path of the URI the system picker
    // itself returns — covered by the temporary read grant that comes with that URI, so no
    // READ_CONTACTS runtime permission is needed (matches ChatInfoScreen's "Link from Contacts"
    // picker, which relies on the same grant for its own, narrower query).
    val importContactMimeTypes = setOf(
        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
    )
    val pickContactForImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        val targetGroupRowId = importingGroupRowId
        importingGroupRowId = null
        if (uri == null) return@rememberLauncherForActivityResult
        val entityUri = Uri.withAppendedPath(uri, ContactsContract.Contacts.Entity.CONTENT_DIRECTORY)
        var foundAddress: String? = null
        var displayName: String? = null
        context.contentResolver.query(
            entityUri,
            arrayOf(
                ContactsContract.Contacts.Entity.MIMETYPE,
                ContactsContract.Contacts.Entity.DATA1,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            null, null, null
        )?.use { cursor ->
            val mimeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.Entity.MIMETYPE)
            val dataIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.Entity.DATA1)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                if (displayName == null) displayName = cursor.getString(nameIdx)
                if (foundAddress != null) continue
                val mime = cursor.getString(mimeIdx) ?: continue
                if (mime !in importContactMimeTypes) continue
                val value = cursor.getString(dataIdx) ?: continue
                foundAddress = com.kachat.app.services.SystemContactsSyncService.extractKaspaAddresses(value).firstOrNull()
            }
        }
        if (targetGroupRowId != null) {
            // Group mode: write into the row that requested the import, not the single-contact
            // address field. No inline error slot for this case besides the row's own KNS/
            // validity status line - a contact with no address just leaves the row untouched.
            if (foundAddress != null) {
                groupAddressRows = groupAddressRows.map {
                    if (it.id == targetGroupRowId) it.copy(text = foundAddress!!, knsError = null) else it
                }
            } else {
                groupAddressRows = groupAddressRows.map {
                    if (it.id == targetGroupRowId) it.copy(knsError = "No Kaspa address found in ${displayName ?: "that contact"}") else it
                }
            }
        } else if (foundAddress != null) {
            address = foundAddress!!
            if (name.isBlank()) name = displayName ?: ""
            importErrorMessage = null
        } else {
            importErrorMessage = "No Kaspa address found in ${displayName ?: "that contact"}"
        }
    }

    LaunchedEffect(address) {
        importErrorMessage = null
        chatViewModel.onCreateChatAddressChanged(address)
    }

    // The address actually used to create the contact — the resolved owner address
    // when the input is a KNS domain, otherwise whatever was typed directly.
    val effectiveAddress = if (looksLikeKnsDomain) knsResolvedAddress else address
    val isValidAddress = if (looksLikeKnsDomain) knsResolvedAddress != null else isValidRawAddress

    if (showScanner) {
        BackHandler { showScanner = false }
        QrScannerOverlay(
            onScanned = { scanned ->
                address = scanned
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
        return
    }

    scanningGroupRowId?.let { rowId ->
        BackHandler { scanningGroupRowId = null }
        QrScannerOverlay(
            onScanned = { scanned ->
                groupAddressRows = groupAddressRows.map {
                    if (it.id == rowId) it.copy(text = scanned.trim()) else it
                }
                scanningGroupRowId = null
            },
            onDismiss = { scanningGroupRowId = null }
        )
        return
    }

    val canCreateGroup = groupName.trim().isNotEmpty() &&
        groupAddressRows.filter { it.trimmedText.isNotEmpty() }.let { rows -> rows.isNotEmpty() && rows.all { it.isValid } }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isGroupMode) "New Group Chat" else "Create chat",
                        color = LocalAppColors.current.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.cancel), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (isGroupMode) {
                        if (isCreatingGroup) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KaspaTeal, strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                        } else {
                            TextButton(
                                onClick = {
                                    val resolvedAddresses = groupAddressRows.mapNotNull { it.effectiveAddress }
                                    chatViewModel.createGroupChat(groupName, resolvedAddresses) { groupId ->
                                        onGroupCreated(groupId)
                                    }
                                },
                                enabled = canCreateGroup
                            ) {
                                Text(stringResource(R.string.create), color = if (canCreateGroup) KaspaTeal else Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                val resolvedAddress = effectiveAddress ?: return@TextButton
                                chatViewModel.addContact(
                                    address = resolvedAddress,
                                    name = name,
                                    knsName = if (looksLikeKnsDomain) com.kachat.app.services.KnsService.normalizeDomain(address) else null
                                )
                                onChatCreated(resolvedAddress)
                            },
                            enabled = isValidAddress
                        ) {
                            Text(stringResource(R.string.add), color = if (isValidAddress) KaspaTeal else Color.DarkGray, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .let { if (isGroupMode) it.verticalScroll(rememberScrollState()) else it }
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalAppColors.current.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.group_chat), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold)
                Switch(
                    checked = isGroupMode,
                    onCheckedChange = {
                        isGroupMode = it
                        chatViewModel.clearCreateGroupError()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = KaspaTeal)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isGroupMode) {
                GroupChatCreationFields(
                    groupName = groupName,
                    onGroupNameChange = { groupName = it },
                    rows = groupAddressRows,
                    onRowsChange = { groupAddressRows = it },
                    onScanRequested = { rowId -> scanningGroupRowId = rowId },
                    onImportRequested = { rowId ->
                        importingGroupRowId = rowId
                        pickContactForImportLauncher.launch(null)
                    },
                    errorMessage = createGroupError,
                    chatViewModel = chatViewModel
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.address),
                color = LocalAppColors.current.textPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalAppColors.current.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.kaspa_address_or_kns_domain),
                    color = LocalAppColors.current.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text(stringResource(R.string.kaspa_qr_or_name_kas), color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = LocalAppColors.current.textPrimary,
                        unfocusedTextColor = LocalAppColors.current.textPrimary,
                        cursorColor = KaspaTeal,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                
                if (looksLikeKnsDomain && isResolvingKns) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = KaspaTeal, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.resolving_domain), color = LocalAppColors.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (looksLikeKnsDomain && knsError != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(knsError ?: "", color = Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (looksLikeKnsDomain && knsResolvedAddress != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CD964), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Resolved to ${knsResolvedAddress?.takeLast(12)}",
                            color = Color(0xFF4CD964),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (isValidAddress) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CD964),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.valid_address),
                            color = Color(0xFF4CD964),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CreateChatActionItem(Icons.Default.PersonAddAlt1, "Import") {
                        pickContactForImportLauncher.launch(null)
                    }
                    CreateChatActionItem(Icons.Default.ContentPaste, "Paste") {
                        clipboardManager.getText()?.text?.let { address = it.trim() }
                    }
                    CreateChatActionItem(Icons.Default.QrCodeScanner, "Scan QR") { showScanner = true }
                }

                importErrorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(message, color = Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.enter_a_kaspa_address_kaspa_or),
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.name_optional),
                color = LocalAppColors.current.textPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.contact_name_2), color = Color.DarkGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LocalAppColors.current.surface,
                    unfocusedContainerColor = LocalAppColors.current.surface,
                    focusedTextColor = LocalAppColors.current.textPrimary,
                    unfocusedTextColor = LocalAppColors.current.textPrimary,
                    cursorColor = KaspaTeal,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }
    }
}

@Composable
fun CreateChatActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = KaspaTeal, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = KaspaTeal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

private const val MAX_GROUP_MEMBERS = 10

/** One row in the group-member address list - supports both a raw Kaspa address and a KNS domain, resolved the same way the single-contact flow's address field does. */
data class GroupAddressRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val resolvedAddress: String? = null,
    val isResolvingKns: Boolean = false,
    val knsError: String? = null
) {
    val trimmedText: String get() = text.trim()
    val looksLikeDomain: Boolean get() = com.kachat.app.services.KnsService.looksLikeDomain(trimmedText)

    /** The actual address this row resolves to - resolved KNS owner address, or the raw typed/scanned address. Null while a domain hasn't resolved yet. */
    val effectiveAddress: String? get() = if (looksLikeDomain) resolvedAddress else trimmedText.ifEmpty { null }

    /**
     * Matches the single-contact flow's `isValidAddress` trust model exactly: a resolved KNS
     * domain is trusted outright (the KNS API is the source of truth for it), only a raw typed/
     * scanned/pasted address gets re-validated here. Re-running a resolved domain's address back
     * through `KaspaAddress.isValid` was the bug behind "KNS domains don't work in group mode" -
     * it isn't wrong exactly, but it's a stricter, redundant check the 1:1 flow deliberately
     * skips, and it was silently keeping "Add Address"/"Create" disabled even after a domain
     * resolved successfully.
     */
    val isValid: Boolean get() = if (looksLikeDomain) resolvedAddress != null else KaspaAddress.isValid(trimmedText)
}

@Composable
fun GroupChatCreationFields(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    rows: List<GroupAddressRow>,
    onRowsChange: (List<GroupAddressRow>) -> Unit,
    onScanRequested: (String) -> Unit,
    onImportRequested: (String) -> Unit,
    errorMessage: String?,
    chatViewModel: ChatViewModel
) {
    val clipboardManager = LocalClipboardManager.current

    // The one member "card" expanded for editing (text field + Import/Paste/Scan + Add Address)
    // - every other row shows collapsed (name/address + a remove button only). Tapping a
    // collapsed row re-expands it; committing the expanded one via "Add Address" collapses it
    // and expands a fresh blank row in its place. Resets whenever this composable enters
    // composition fresh (i.e. every time group mode is toggled on), same as `rows` itself.
    var editingRowId by remember { mutableStateOf(rows.firstOrNull()?.id) }

    fun isValidRow(row: GroupAddressRow): Boolean = row.isValid

    fun commitRow(id: String) {
        val row = rows.firstOrNull { it.id == id } ?: return
        if (!isValidRow(row)) return
        if (rows.size < MAX_GROUP_MEMBERS) {
            val newRow = GroupAddressRow()
            onRowsChange(rows + newRow)
            editingRowId = newRow.id
        } else {
            editingRowId = null
        }
    }

    fun setEditingRow(id: String) {
        val currentId = editingRowId
        if (currentId != null && currentId != id) {
            val current = rows.firstOrNull { it.id == currentId }
            if (current != null && current.trimmedText.isEmpty() && rows.size > 1) {
                onRowsChange(rows.filterNot { it.id == currentId })
            }
        }
        editingRowId = id
    }

    fun removeRow(id: String) {
        val wasEditing = editingRowId == id
        val newRows = rows.filterNot { it.id == id }
        if (wasEditing) editingRowId = null
        if (newRows.isEmpty() || (editingRowId == null && newRows.size < MAX_GROUP_MEMBERS)) {
            val newRow = GroupAddressRow()
            onRowsChange(newRows + newRow)
            editingRowId = newRow.id
        } else {
            onRowsChange(newRows)
        }
    }

    Text(
        text = stringResource(R.string.group_name),
        color = LocalAppColors.current.textPrimary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(12.dp))
    TextField(
        value = groupName,
        onValueChange = onGroupNameChange,
        placeholder = { Text(stringResource(R.string.group_name_2), color = Color.DarkGray) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LocalAppColors.current.surface,
            unfocusedContainerColor = LocalAppColors.current.surface,
            focusedTextColor = LocalAppColors.current.textPrimary,
            unfocusedTextColor = LocalAppColors.current.textPrimary,
            cursorColor = KaspaTeal,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = stringResource(R.string.members),
        color = LocalAppColors.current.textPrimary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAppColors.current.surface)
            .padding(16.dp)
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))

            // Debounced KNS resolution for this row, independent of every other row.
            LaunchedEffect(row.text) {
                val trimmed = row.text.trim()
                if (trimmed.isEmpty() || !com.kachat.app.services.KnsService.looksLikeDomain(trimmed)) {
                    if (row.resolvedAddress != null || row.knsError != null || row.isResolvingKns) {
                        onRowsChange(rows.map { if (it.id == row.id) it.copy(resolvedAddress = null, knsError = null, isResolvingKns = false) else it })
                    }
                    return@LaunchedEffect
                }
                onRowsChange(rows.map { if (it.id == row.id) it.copy(isResolvingKns = true, resolvedAddress = null, knsError = null) else it })
                kotlinx.coroutines.delay(500)
                val resolved = chatViewModel.resolveKnsDomain(trimmed)
                onRowsChange(rows.map {
                    if (it.id == row.id && it.trimmedText == trimmed) {
                        it.copy(isResolvingKns = false, resolvedAddress = resolved, knsError = if (resolved == null) "KNS domain not found" else null)
                    } else it
                })
            }

            if (row.id == editingRowId) {
                // The one expanded "card": address field, then Import/Paste/Scan (same
                // size/style as the single-contact flow), then Add Address to commit it and
                // open the next blank slot.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = row.text,
                        onValueChange = { newValue ->
                            onRowsChange(rows.map { if (it.id == row.id) it.copy(text = newValue) else it })
                        },
                        placeholder = { Text(stringResource(R.string.kaspa_qr_or_name_kas), color = Color.DarkGray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = LocalAppColors.current.textPrimary,
                            unfocusedTextColor = LocalAppColors.current.textPrimary,
                            cursorColor = KaspaTeal,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    if (rows.size > 1) {
                        IconButton(onClick = { removeRow(row.id) }) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = stringResource(R.string.remove), tint = Color(0xFFFF3B30))
                        }
                    }
                }

                if (row.trimmedText.isNotEmpty() || row.knsError != null) {
                    when {
                        row.isResolvingKns -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = KaspaTeal, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.resolving_kns_domain), color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
                        }
                        row.knsError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(row.knsError, color = Color(0xFFFF3B30), fontSize = 12.sp)
                        }
                        row.looksLikeDomain && row.resolvedAddress != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CD964), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Resolved: ${row.resolvedAddress.takeLast(12)}", color = Color(0xFF4CD964), fontSize = 12.sp)
                        }
                        !row.looksLikeDomain -> {
                            val isValid = KaspaAddress.isValid(row.trimmedText)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isValid) Color(0xFF4CD964) else Color(0xFFFF3B30),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isValid) "Valid address" else "Invalid address format",
                                    color = if (isValid) Color(0xFF4CD964) else Color(0xFFFF3B30),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = LocalAppColors.current.divider)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    CreateChatActionItem(Icons.Default.PersonAddAlt1, "Import") {
                        onImportRequested(row.id)
                    }
                    CreateChatActionItem(Icons.Default.ContentPaste, "Paste") {
                        clipboardManager.getText()?.text?.let { pasted ->
                            onRowsChange(rows.map { if (it.id == row.id) it.copy(text = pasted.trim()) else it })
                        }
                    }
                    CreateChatActionItem(Icons.Default.QrCodeScanner, "Scan QR") {
                        onScanRequested(row.id)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { commitRow(row.id) },
                    enabled = isValidRow(row),
                    colors = ButtonDefaults.buttonColors(containerColor = KaspaTeal, disabledContainerColor = LocalAppColors.current.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.add_address), color = if (isValidRow(row)) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                }
            } else {
                // Committed: collapsed to a single row - tap the name/address to edit it again,
                // or tap the red button to remove it outright.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setEditingRow(row.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.trimmedText,
                        color = LocalAppColors.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { removeRow(row.id) }) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = stringResource(R.string.remove), tint = Color(0xFFFF3B30))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Up to $MAX_GROUP_MEMBERS addresses or KNS domains. Anyone not already a contact will be added automatically.",
        color = LocalAppColors.current.textSecondary,
        style = MaterialTheme.typography.bodySmall
    )

    errorMessage?.let { message ->
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    contactId: String,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    fromBroadcast: Boolean = false,
    onNavigateToPhotoSettings: (String) -> Unit = {},
    onNavigateToNotificationSettings: (String) -> Unit = {},
    onNavigateToDomainSettings: (String) -> Unit = {}
) {
    val conversation = chatViewModel.conversations.collectAsState().value.find { it.contact.id == contactId }
    val messages by chatViewModel.getMessages(contactId).collectAsState(initial = emptyList())
    val knsProfile = chatViewModel.knsProfiles.collectAsState().value[contactId]
    val knsFields = knsProfile?.profile
    val ownedDomains = knsProfile?.ownedDomains.orEmpty()
    val hasMoreInfo = knsFields != null && listOf(
        knsFields.bio, knsFields.x, knsFields.website, knsFields.telegram,
        knsFields.discord, knsFields.contactEmail, knsFields.github, knsFields.redirectUrl
    ).any { !it.isNullOrBlank() }
    val systemContactId = conversation?.contact?.systemContactId
    val systemContactName = conversation?.contact?.systemContactName

    var contactName by remember { mutableStateOf("") }

    // Synchronize local state with database when it loads
    LaunchedEffect(conversation?.contact?.alias) {
        contactName = conversation?.contact?.alias ?: ""
    }

    // No custom nickname saved yet — once the contact's primary KNS domain resolves, prefill the
    // field with it (still fully editable) rather than leaving it blank. Guarded on contactName
    // still being blank at the moment this fires so it never clobbers something already typed.
    LaunchedEffect(knsProfile?.selectedDomain) {
        if (conversation?.contact?.alias == null && contactName.isBlank()) {
            knsProfile?.selectedDomain?.let { contactName = it }
        }
    }

    LaunchedEffect(contactId) {
        chatViewModel.refreshKnsProfile(contactId)
    }

    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    val context = LocalContext.current
    val pickContactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                if (lookupKey != null && displayName != null) {
                    contactName = displayName
                    chatViewModel.linkSystemContact(contactId, lookupKey, displayName)
                }
            }
        }
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (fromBroadcast) "User Info" else "Chat Info", color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.cancel), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        chatViewModel.updateContactName(contactId, contactName)
                        onBack()
                    }) {
                        Text(stringResource(R.string.save), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Contact-name card — only needed as a fallback when there's no KNS profile to show
            // instead; the "KNS Profile" section below takes over this role (avatar + editable
            // name) once a profile exists, rather than showing both.
            if (ownedDomains.isEmpty()) {
                Surface(
                    color = LocalAppColors.current.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(
                            imageUrl = knsFields?.avatarUrl,
                            fallbackText = conversation?.contact?.alias ?: contactId.takeLast(8),
                            size = 60.dp,
                            backgroundColor = LocalAppColors.current.surfaceVariant,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(16.dp))
                        TextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            placeholder = { Text(stringResource(R.string.contact_name), color = LocalAppColors.current.textSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = LocalAppColors.current.textPrimary,
                                unfocusedTextColor = LocalAppColors.current.textPrimary,
                                cursorColor = KaspaTeal,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth().offset(x = (-16).dp)
                        )
                    }
                }
            }

            if (ownedDomains.isNotEmpty()) {
                SettingsSection(title = stringResource(R.string.kns_profile)) {
                    Column {
                        val bannerUrl = knsFields?.bannerUrl?.takeIf { it.isNotBlank() }
                        if (bannerUrl != null) {
                            SubcomposeAsyncImage(
                                model = bannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(LocalAppColors.current.surfaceVariant)
                            )
                        }
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ContactAvatar(
                                imageUrl = knsFields?.avatarUrl,
                                fallbackText = knsProfile?.selectedDomain ?: contactId.takeLast(8),
                                size = 48.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TextField(
                                    value = contactName,
                                    onValueChange = { contactName = it },
                                    placeholder = { Text(knsProfile?.selectedDomain ?: "Contact Name", color = LocalAppColors.current.textSecondary) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = LocalAppColors.current.textPrimary,
                                        unfocusedTextColor = LocalAppColors.current.textPrimary,
                                        cursorColor = KaspaTeal,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(x = (-16).dp)
                                )
                                val bio = knsFields?.bio?.takeIf { it.isNotBlank() }
                                if (bio != null) {
                                    Text(
                                        text = bio,
                                        color = LocalAppColors.current.textPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(bio)) }
                                    )
                                } else {
                                    Text(
                                        text = if (hasMoreInfo) "On-chain profile data available." else "No on-chain profile data yet.",
                                        color = LocalAppColors.current.textSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        var moreInfoExpanded by remember(contactId) { mutableStateOf(false) }
                        if (hasMoreInfo) {
                            HorizontalDivider(color = LocalAppColors.current.divider)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { moreInfoExpanded = !moreInfoExpanded }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.more_info), color = KaspaTeal, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(
                                    if (moreInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (moreInfoExpanded) "Collapse" else "Expand",
                                    tint = KaspaTeal
                                )
                            }
                            if (moreInfoExpanded) {
                                HorizontalDivider(color = LocalAppColors.current.divider)
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val socialLinks = listOfNotNull(
                                        knsFields?.x?.takeIf { it.isNotBlank() }?.let { "X" to it },
                                        knsFields?.website?.takeIf { it.isNotBlank() }?.let { "Website" to it },
                                        knsFields?.telegram?.takeIf { it.isNotBlank() }?.let { "Telegram" to it },
                                        knsFields?.discord?.takeIf { it.isNotBlank() }?.let { "Discord" to it },
                                        knsFields?.contactEmail?.takeIf { it.isNotBlank() }?.let { "Email" to it },
                                        knsFields?.github?.takeIf { it.isNotBlank() }?.let { "GitHub" to it },
                                        knsFields?.redirectUrl?.takeIf { it.isNotBlank() }?.let { "Redirect" to it }
                                    )
                                    socialLinks.forEachIndexed { index, (label, value) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val url = if (value.startsWith("http")) value else "https://$value"
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                    } catch (e: Exception) { /* no browser available */ }
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(label, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                                            Text(value, color = KaspaTeal, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (index < socialLinks.lastIndex) {
                                            HorizontalDivider(color = LocalAppColors.current.divider)
                                        }
                                    }
                                }
                            }
                        }

                        // Which owned domain represents this contact — placed right under More
                        // Info, in the same card, rather than as its own separate section. A
                        // dedicated picker page rather than an inline list, matching Photos/
                        // Incoming Notifications' pattern.
                        HorizontalDivider(color = LocalAppColors.current.divider)
                        SettingsNavigationItem(
                            stringResource(R.string.domains),
                            Icons.Default.Language,
                            knsProfile?.selectedDomain ?: "",
                            onClick = { onNavigateToDomainSettings(contactId) }
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.address)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(contactId))
                            Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val qrPainter = rememberQrBitmapPainter(contactId)
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(2.dp, KaspaTeal, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(qrPainter, "QR Code", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = contactId,
                        color = LocalAppColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.system_contact)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (systemContactId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.linked), color = LocalAppColors.current.textPrimary)
                            Text(systemContactName ?: "", color = LocalAppColors.current.textSecondary)
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = LocalAppColors.current.divider)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text(stringResource(R.string.not_linked), color = LocalAppColors.current.textSecondary)
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.clickable { pickContactLauncher.launch(null) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAddAlt1, null, tint = KaspaTeal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.link_from_contacts), color = KaspaTeal, fontWeight = FontWeight.Bold)
                    }

                    if (systemContactId != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.clickable { chatViewModel.unlinkSystemContact(contactId) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.unlink), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!fromBroadcast) {
                SettingsSection(title = stringResource(R.string.incoming_notifications)) {
                    val notificationOverride = com.kachat.app.models.ContactNotificationMode.fromName(conversation?.contact?.notificationOverride)
                    SettingsNavigationItem(
                        stringResource(R.string.incoming_notifications),
                        Icons.Default.NotificationsNone,
                        notificationOverride?.displayName ?: "Default",
                        onClick = { onNavigateToNotificationSettings(contactId) }
                    )
                }

                SettingsSection(title = stringResource(R.string.photos)) {
                    val photoOverride = com.kachat.app.models.PhotoAutoDisplayMode.fromName(conversation?.contact?.photoAutoDisplayOverride)
                    SettingsNavigationItem(
                        stringResource(R.string.photos),
                        Icons.Default.Photo,
                        photoOverride.displayName,
                        onClick = { onNavigateToPhotoSettings(contactId) }
                    )
                }
            }

            if (!fromBroadcast) {
                SettingsSection(title = stringResource(R.string.info)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val addedDate = remember(conversation) {
                            conversation?.contact?.addedAt?.let {
                                java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US).format(java.util.Date(it))
                            } ?: "Unknown"
                        }

                        val lastMessageTime = remember(messages) {
                            messages.firstOrNull()?.blockTimestamp?.let {
                                val diff = System.currentTimeMillis() - it
                                val hours = diff / (1000 * 60 * 60)
                                val minutes = (diff / (1000 * 60)) % 60
                                if (hours > 0) "${hours} hr, ${minutes} min" else "${minutes} min"
                            } ?: "None"
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.added), color = LocalAppColors.current.textPrimary)
                            Text(addedDate, color = LocalAppColors.current.textSecondary)
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = LocalAppColors.current.divider)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.last_message), color = LocalAppColors.current.textPrimary)
                            Text(lastMessageTime, color = LocalAppColors.current.textSecondary)
                        }
                        Spacer(Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            InfoStatItem(label = "Sent", value = messages.count { it.direction == "sent" }.toString())
                            InfoStatItem(label = "Received", value = messages.count { it.direction == "received" }.toString())
                            InfoStatItem(label = "Total", value = messages.size.toString())
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/** Reached from Chat Info's "Photos" row — a selectable list of [PhotoAutoDisplayMode]s for this one contact, matching [KaspaExplorerSettingsScreen]'s picker pattern. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPhotoSettingsScreen(contactId: String, onBack: () -> Unit, chatViewModel: ChatViewModel = hiltViewModel()) {
    val conversation = chatViewModel.conversations.collectAsState().value.find { it.contact.id == contactId }
    val photoOverride = com.kachat.app.models.PhotoAutoDisplayMode.fromName(conversation?.contact?.photoAutoDisplayOverride)
    val automaticResolvesToShow = conversation?.contact?.conversationStatus == "active"

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.photos), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Automatic currently ${if (automaticResolvesToShow) "shows" else "hides"} photos from this contact. " +
                    "It hides photos from contacts you haven't added or messaged yet, until you tap to reveal them.",
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection(title = stringResource(R.string.photos)) {
                com.kachat.app.models.PhotoAutoDisplayMode.entries.forEachIndexed { index, mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chatViewModel.updateContactPhotoOverride(
                                    contactId,
                                    if (mode == com.kachat.app.models.PhotoAutoDisplayMode.AUTOMATIC) null else mode
                                )
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.displayName, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (photoOverride == mode) {
                            Icon(Icons.Default.Check, null, tint = KaspaTeal)
                        }
                    }
                    if (index < com.kachat.app.models.PhotoAutoDisplayMode.entries.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
}

/** Reached from Chat Info's "Incoming Notifications" row — a selectable list of [ContactNotificationMode]s for this one contact, matching [ContactPhotoSettingsScreen]'s picker pattern (a null override shows as "Default", the first row, rather than one of the enum's own cases). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactNotificationSettingsScreen(contactId: String, onBack: () -> Unit, chatViewModel: ChatViewModel = hiltViewModel()) {
    val conversation = chatViewModel.conversations.collectAsState().value.find { it.contact.id == contactId }
    val notificationOverride = com.kachat.app.models.ContactNotificationMode.fromName(conversation?.contact?.notificationOverride)

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.incoming_notifications), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.default_follows_settings_notifications_off_disables),
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection(title = stringResource(R.string.incoming_notifications)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { chatViewModel.updateContactNotificationOverride(contactId, null) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.default_option), color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (notificationOverride == null) {
                        Icon(Icons.Default.Check, null, tint = KaspaTeal)
                    }
                }
                com.kachat.app.models.ContactNotificationMode.entries.forEach { mode ->
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chatViewModel.updateContactNotificationOverride(contactId, mode) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.displayName, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (notificationOverride == mode) {
                            Icon(Icons.Default.Check, null, tint = KaspaTeal)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reached from Chat Info's "Domains" row — lets you pick which of this contact's owned KNS
 * domains represents them (also the default contact name when there's no custom nickname).
 * Domain strings from [ChatViewModel.KnsProfileUiState.ownedDomains] already carry their real
 * ".kas" suffix, so they're shown as-is here — never re-appended.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDomainSettingsScreen(contactId: String, onBack: () -> Unit, chatViewModel: ChatViewModel = hiltViewModel()) {
    val knsProfile = chatViewModel.knsProfiles.collectAsState().value[contactId]
    val ownedDomains = knsProfile?.ownedDomains.orEmpty()

    // This screen gets its own ChatViewModel instance (scoped to its own nav destination), so it
    // needs its own fetch — reading knsProfiles alone would only ever see whatever Chat Info's
    // separate instance happened to load, which is nothing the first time you open this page.
    LaunchedEffect(contactId) {
        chatViewModel.refreshKnsProfile(contactId)
    }

    Scaffold(
        containerColor = LocalAppColors.current.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.domains), color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LocalAppColors.current.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.pick_which_domain_you_want_to),
                color = LocalAppColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection(title = stringResource(R.string.domains)) {
                ownedDomains.forEachIndexed { index, domain ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chatViewModel.selectKnsDomain(contactId, domain) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(domain, color = LocalAppColors.current.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (domain == knsProfile?.selectedDomain) {
                            Icon(Icons.Default.Check, null, tint = KaspaTeal)
                        }
                    }
                    if (index < ownedDomains.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun InfoStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = LocalAppColors.current.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
    }
}

