package com.kachat.app.ui.screens

import com.kachat.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kachat.app.models.MessageEntity
import com.kachat.app.ui.theme.KaspaTeal
import com.kachat.app.ui.theme.LocalAppColors
import com.kachat.app.util.ChessColor
import com.kachat.app.util.ChessEngine
import com.kachat.app.util.ChessGameEngine
import com.kachat.app.util.ChessGameStatusKind
import com.kachat.app.util.ChessMessage
import com.kachat.app.util.ChessMove
import com.kachat.app.util.ChessPiece
import com.kachat.app.util.ChessPieceType
import com.kachat.app.util.ChessSquare
import com.kachat.app.util.ImageMessage
import com.kachat.app.util.MessageProtocol
import com.kachat.app.util.MessageReply
import com.kachat.app.util.VoiceMessage
import com.kachat.app.viewmodels.ChatViewModel
import com.kachat.app.viewmodels.WalletViewModel

/**
 * Full-screen interactive chess board, opened by tapping a chess card in a 1:1 chat
 * ([ChessBubble] in Screens.kt). Board state is entirely derived from the conversation's
 * messages ([ChessGameEngine.summarize]) - re-derived fresh from `chatViewModel.getMessages`
 * (Room-`Flow`-backed) on every recomposition, so a new move arriving while this screen is open
 * updates it automatically, the same way any other message-driven screen in this app stays live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessGameScreen(
    navController: NavController,
    contactId: String,
    gameId: String,
    chatViewModel: ChatViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val conversations by chatViewModel.conversations.collectAsState()
    val conversation = conversations.find { it.contact.id == contactId }
    val messages by chatViewModel.getMessages(contactId).collectAsState(initial = emptyList())
    val myAddress by walletViewModel.address.collectAsState()
    val focusManager = LocalFocusManager.current

    // Every non-chess message in the conversation - chess move/invite/response/resign envelopes
    // are deliberately excluded since the live board above already shows that state; repeating
    // it here as text would just be clutter.
    val chatMessages = remember(messages) {
        messages.filter { message ->
            val unwrapped = MessageReply.parseOrNull(message.plaintextBody)?.text ?: message.plaintextBody
            ChessMessage.parseOrNull(unwrapped) == null
        }.sortedBy { it.blockTimestamp }
    }
    var chatDraft by remember { mutableStateOf("") }

    val chessSourceMessages = remember(messages) {
        messages.map {
            ChessGameEngine.SimpleChessSourceMessage(
                id = it.id,
                plaintextBody = it.plaintextBody,
                isOutgoing = it.direction == "sent",
                blockTimestamp = it.blockTimestamp
            )
        }
    }
    val summary = remember(chessSourceMessages, myAddress) {
        val address = myAddress
        if (address != null) ChessGameEngine.summarize(gameId, chessSourceMessages, address, contactId) else null
    }
    val myColor = remember(summary, myAddress) {
        val address = myAddress
        if (address != null) summary?.colorFor(address) else null
    }
    val isMyTurn = summary != null && myColor != null && summary.status.kind == ChessGameStatusKind.IN_PROGRESS && summary.board.sideToMove == myColor

    var selectedSquare by remember { mutableStateOf<ChessSquare?>(null) }
    var pendingPromotionMove by remember { mutableStateOf<ChessMove?>(null) }
    var showResignConfirm by remember { mutableStateOf(false) }

    val legalDestinations = remember(selectedSquare, summary) {
        val square = selectedSquare
        val board = summary?.board
        if (square != null && board != null) ChessEngine.legalMoves(square, board).map { it.to } else emptyList()
    }

    fun send(move: ChessMove) {
        chatViewModel.sendChessMove(contactId, gameId, move)
    }

    fun handleTap(square: ChessSquare) {
        val board = summary?.board ?: return
        if (!isMyTurn) return
        val currentSelection = selectedSquare
        if (currentSelection != null) {
            if (legalDestinations.contains(square)) {
                val movingPiece = board.piece(currentSelection)
                val backRank = if (movingPiece?.color == ChessColor.WHITE) 7 else 0
                selectedSquare = null
                if (movingPiece?.type == ChessPieceType.PAWN && square.rank == backRank) {
                    pendingPromotionMove = ChessMove(currentSelection, square, null)
                } else {
                    send(ChessMove(currentSelection, square, null))
                }
                return
            }
            val piece = board.piece(square)
            selectedSquare = if (piece != null && piece.color == myColor) square else null
        } else {
            val piece = board.piece(square)
            if (piece != null && piece.color == myColor) selectedSquare = square
        }
    }

    fun sendChatMessage() {
        val text = chatDraft.trim()
        if (text.isEmpty()) return
        chatDraft = ""
        chatViewModel.sendMessage(contactId, text)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            conversation?.contact?.alias?.takeIf { it.isNotBlank() } ?: contactId.takeLast(8),
                            color = LocalAppColors.current.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (summary != null) {
                            Text(
                                summary.statusText,
                                color = if (summary.status.isGameOver) LocalAppColors.current.textSecondary else KaspaTeal,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                            R.string.back), tint = LocalAppColors.current.textPrimary)
                    }
                },
                actions = {
                    if (summary != null && summary.status.kind == ChessGameStatusKind.IN_PROGRESS) {
                        TextButton(onClick = { showResignConfirm = true }) {
                            Text(stringResource(R.string.resign), color = Color(0xFFFF3B30))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalAppColors.current.background)
            )
        },
        bottomBar = {
            if (summary != null) {
                // navigationBarsPadding() keeps the send row clear of the system nav bar when the
                // keyboard is closed; imePadding() on the Scaffold above handles the keyboard-open
                // case - matches the 1:1 chat composer's identical pattern in Screens.kt.
                // Text-only, deliberately - no mic, no "+" menu, no photos/payments/another chess
                // invite. This is a quick-chat surface for while a game's in progress, not a full
                // composer.
                Column(modifier = Modifier.background(LocalAppColors.current.background).navigationBarsPadding().padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = chatDraft,
                            onValueChange = { chatDraft = it },
                            placeholder = { Text(stringResource(R.string.message), color = LocalAppColors.current.textSecondary) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = LocalAppColors.current.surface,
                                unfocusedContainerColor = LocalAppColors.current.surface,
                                focusedTextColor = LocalAppColors.current.textPrimary,
                                unfocusedTextColor = LocalAppColors.current.textPrimary,
                                cursorColor = KaspaTeal,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4
                        )
                        IconButton(onClick = { sendChatMessage() }, enabled = chatDraft.isNotBlank()) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.send),
                                tint = if (chatDraft.isNotBlank()) KaspaTeal else LocalAppColors.current.textSecondary
                            )
                        }
                    }
                }
            }
        },
        containerColor = LocalAppColors.current.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Taps that land on an interactive child (a board square, a button) are already
                // consumed by that child's own `clickable`, so this only fires for taps on empty
                // space (header padding, captured-pieces bar background, the divider) - "tap
                // outside to dismiss the keyboard", without interfering with square selection.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (summary != null) {
                Spacer(Modifier.height(8.dp))
                CapturedPiecesBar(summary, myColor ?: ChessColor.WHITE)
                Spacer(Modifier.height(8.dp))
                InteractiveChessBoard(
                    board = summary.board,
                    orientation = myColor ?: ChessColor.WHITE,
                    selectedSquare = selectedSquare,
                    legalDestinations = legalDestinations,
                    lastMove = summary.moveHistory.lastOrNull(),
                    onSquareTap = ::handleTap
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                ChessChatHistory(messages = chatMessages, modifier = Modifier.weight(1f).fillMaxWidth())
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = KaspaTeal)
                }
            }
        }
    }

    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text(stringResource(R.string.resign_this_game)) },
            confirmButton = {
                TextButton(onClick = {
                    showResignConfirm = false
                    chatViewModel.resignChessGame(contactId, gameId)
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.resign), color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    val pendingMove = pendingPromotionMove
    if (pendingMove != null) {
        AlertDialog(
            onDismissRequest = { pendingPromotionMove = null },
            title = { Text(stringResource(R.string.promote_pawn_to)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    for (type in listOf(ChessPieceType.QUEEN, ChessPieceType.ROOK, ChessPieceType.BISHOP, ChessPieceType.KNIGHT)) {
                        ChessPieceGlyph(
                            piece = ChessPiece(type, myColor ?: ChessColor.WHITE),
                            fontSize = 36.sp,
                            modifier = Modifier.clickable {
                                pendingPromotionMove = null
                                send(pendingMove.copy(promotion = type))
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun InteractiveChessBoard(
    board: com.kachat.app.util.ChessBoard,
    orientation: ChessColor,
    selectedSquare: ChessSquare?,
    legalDestinations: List<ChessSquare>,
    lastMove: com.kachat.app.util.ChessMoveRecord?,
    onSquareTap: (ChessSquare) -> Unit
) {
    val squareSizeDp = 44.dp
    val labelSizeDp = 16.dp
    val ranks = if (orientation == ChessColor.WHITE) (7 downTo 0) else (0..7)
    val files = if (orientation == ChessColor.WHITE) (0..7) else (7 downTo 0)

    Column {
        FileLabelsRow(files, squareSizeDp, labelSizeDp)
        for (rank in ranks) {
            Row {
                RankLabel(rank, squareSizeDp, labelSizeDp)
                for (file in files) {
                    val square = ChessSquare(file, rank)
                    val isLight = (file + rank) % 2 != 0
                    val isSelected = selectedSquare == square
                    val isDestination = legalDestinations.contains(square)
                    val isLastMoveSquare = lastMove != null && (lastMove.from == square || lastMove.to == square)
                    Box(
                        modifier = Modifier
                            .size(squareSizeDp)
                            .background(if (isLight) ChessLightSquareColor else ChessDarkSquareColor)
                            .then(
                                if (isLastMoveSquare) Modifier.background(Color(0xFFFFEB3B).copy(alpha = 0.35f)) else Modifier
                            )
                            .then(
                                if (isSelected) Modifier.background(KaspaTeal.copy(alpha = 0.45f)) else Modifier
                            )
                            .clickable { onSquareTap(square) },
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = board.piece(square)
                        if (piece != null) {
                            ChessPieceGlyph(piece, fontSize = 28.sp)
                        }
                        if (isDestination) {
                            Box(
                                modifier = Modifier
                                    .size(squareSizeDp / 3)
                                    .background(KaspaTeal.copy(alpha = 0.6f), CircleShape)
                            )
                        }
                    }
                }
                RankLabel(rank, squareSizeDp, labelSizeDp)
            }
        }
        FileLabelsRow(files, squareSizeDp, labelSizeDp)
    }
}

/** File letters (a-h) shown above and below the board, in the current orientation's order. */
@Composable
private fun FileLabelsRow(files: IntProgression, squareSizeDp: androidx.compose.ui.unit.Dp, labelSizeDp: androidx.compose.ui.unit.Dp) {
    Row {
        Box(Modifier.size(labelSizeDp))
        for (file in files) {
            Box(modifier = Modifier.width(squareSizeDp).height(labelSizeDp), contentAlignment = Alignment.Center) {
                Text(('a' + file).toString(), fontSize = 10.sp, color = LocalAppColors.current.textSecondary)
            }
        }
        Box(Modifier.size(labelSizeDp))
    }
}

/** Rank number (1-8) shown to the left and right of a board row. */
@Composable
private fun RankLabel(rank: Int, squareSizeDp: androidx.compose.ui.unit.Dp, labelSizeDp: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.width(labelSizeDp).height(squareSizeDp), contentAlignment = Alignment.Center) {
        Text("${rank + 1}", fontSize = 10.sp, color = LocalAppColors.current.textSecondary)
    }
}

/** Captured-pieces tray: pieces the opponent has taken from me on the leading edge, pieces I've
 *  taken from them on the trailing edge - mirrors how online chess UIs show each side's haul next
 *  to their own info. */
@Composable
private fun CapturedPiecesBar(summary: com.kachat.app.util.ChessGameSummary, myColor: ChessColor) {
    val takenFromMe = if (myColor == ChessColor.WHITE) summary.capturedByBlack else summary.capturedByWhite
    val takenByMe = if (myColor == ChessColor.WHITE) summary.capturedByWhite else summary.capturedByBlack
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CapturedGroup(pieces = takenFromMe, pieceColor = myColor, label = "They captured")
        CapturedGroup(pieces = takenByMe, pieceColor = myColor.opposite, label = "You captured")
    }
}

@Composable
private fun CapturedGroup(pieces: List<ChessPieceType>, pieceColor: ChessColor, label: String) {
    Column {
        Text(label, fontSize = 10.sp, color = LocalAppColors.current.textSecondary)
        Row {
            if (pieces.isEmpty()) {
                Text(stringResource(R.string.str), fontSize = 10.sp, color = LocalAppColors.current.textSecondary)
            } else {
                for (type in pieces) {
                    ChessPieceGlyph(ChessPiece(type, pieceColor), fontSize = 14.sp)
                }
            }
        }
    }
}

/** Scrollable, auto-scroll-to-latest chat history under the board - lets you keep chatting
 *  without leaving the full-screen game. */
@Composable
private fun ChessChatHistory(messages: List<MessageEntity>, modifier: Modifier = Modifier) {
    val listState: LazyListState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            ChessChatRow(message)
        }
    }
}

@Composable
private fun ChessChatRow(message: MessageEntity) {
    val isSent = message.direction == "sent"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isSent) KaspaTeal else LocalAppColors.current.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                chessChatPreviewText(message),
                color = if (isSent) Color.Black else LocalAppColors.current.textPrimary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/** Condensed, text-only rendering for the mini history - unlike the normal message bubbles,
 *  non-text content (photos/voice/payments) collapses to a short label rather than fully
 *  rendering, to keep this secondary surface lightweight. */
private fun chessChatPreviewText(message: MessageEntity): String {
    if (message.type == MessageProtocol.TYPE_PAY) return "💰 Payment"
    if (message.type == MessageProtocol.TYPE_HANDSHAKE) return "👋 Handshake"
    val unwrapped = MessageReply.parseOrNull(message.plaintextBody)?.text ?: message.plaintextBody
    if (VoiceMessage.parseOrNull(unwrapped) != null) return "🎤 Voice message"
    if (ImageMessage.parseOrNull(unwrapped) != null) return "📷 Photo"
    return unwrapped ?: ""
}
