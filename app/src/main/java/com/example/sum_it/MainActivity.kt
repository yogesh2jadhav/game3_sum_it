package com.example.sum_it

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sum_it.ui.theme.Sum_itTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

/**
 * The main activity for the Sum It game. This is the entry point of the application.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display to draw behind system bars.
        enableEdgeToEdge()
        setContent {
            Sum_itTheme {
                // Set the main composable for the screen.
                SumGridChallengeGame()
            }
        }
    }
}

/**
 * The main composable that holds the entire game screen.
 * It manages the overall game state and layout.
 */
@Composable
fun SumGridChallengeGame() {
    // Game State Management
    var score by remember { mutableIntStateOf(120) }
    var level by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(300) } // 5 minutes in seconds
    var isGameOver by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    // Game Logic State
    val solution = remember { mutableStateListOf<Int>() }
    val userInput = remember { mutableStateListOf(*Array(9) { "" }) }
    val rowSums = remember { mutableStateListOf(0, 0, 0) }
    val colSums = remember { mutableStateListOf(0, 0, 0) }

    /**
     * Initializes or resets the game to a new state based on the current level.
     * @param currentLevel The level to start.
     */
    fun startNewGame(currentLevel: Int) {
        val nums = (1..9).toList().shuffled()
        solution.clear()
        solution.addAll(nums)
        
        // Calculate the target sums for rows and columns based on the new solution.
        for (i in 0..2) {
            rowSums[i] = solution[i * 3] + solution[i * 3 + 1] + solution[i * 3 + 2]
            colSums[i] = solution[i] + solution[i + 3] + solution[i + 6]
        }
        
        userInput.fill("")
        
        // Pre-fill a certain number of cells with correct answers based on the level.
        val preFillCount = when(currentLevel) {
            1 -> 4
            2 -> 3
            3 -> 2
            4 -> 1
            else -> 0
        }
        
        if (preFillCount > 0) {
            val indices = (0..8).shuffled().take(preFillCount)
            indices.forEach { idx ->
                userInput[idx] = solution[idx].toString()
            }
        }

        // Reset timers and flags.
        timeLeft = 300
        isGameOver = false
        isSuccess = false
        showErrors = false
    }

    // Start the game for the first time.
    LaunchedEffect(Unit) {
        startNewGame(level)
    }

    // Game Timer Logic: Decrements the timer every second.
    LaunchedEffect(isGameOver, isSuccess) {
        while (timeLeft > 0 && !isSuccess && !isGameOver) {
            delay(1000)
            timeLeft--
            if (timeLeft == 0) isGameOver = true
        }
    }

    // Auto-Submit Logic: Checks if the board is completely correct and automatically triggers success.
    LaunchedEffect(userInput.toList(), solution.toList()) {
        if (solution.isNotEmpty() && !isSuccess && !isGameOver) {
            val allCorrect = userInput.indices.all { i ->
                userInput[i] == solution[i].toString()
            }
            if (allCorrect) {
                delay(500) // Brief delay to show the last correct cell.
                isSuccess = true
                score += 50
            }
        }
    }

    // Define a cool gradient background for the game.
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF4A148C), Color(0xFF1A237E))
    )

    // Main layout container.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Displays Score, Timer, and Level.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameStatCard("Score", score.toString())
                TimerCircle(timeLeft, size = 80.dp)
                GameStatCard("Level", level.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The main 3x3 puzzle grid.
            PuzzleGrid(
                userInput = userInput,
                solution = solution,
                rowTargets = rowSums,
                colTargets = colSums,
                showErrors = showErrors,
                level = level
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer: Contains action buttons like Submit, Reset, and Hint.
            val isGridComplete = userInput.all { it.isNotEmpty() } && userInput.toSet().size == 9
            
            GameActions(
                isSubmitEnabled = isGridComplete,
                onSubmit = {
                    // Manual submission logic.
                    val currentRows = (0..2).map { i ->
                        (userInput[i * 3].toIntOrNull() ?: 0) + 
                        (userInput[i * 3 + 1].toIntOrNull() ?: 0) + 
                        (userInput[i * 3 + 2].toIntOrNull() ?: 0)
                    }
                    val currentColumns = (0..2).map { i ->
                        (userInput[i].toIntOrNull() ?: 0) + 
                        (userInput[i + 3].toIntOrNull() ?: 0) + 
                        (userInput[i + 6].toIntOrNull() ?: 0)
                    }
                    
                    if (currentRows == rowSums.toList() && currentColumns == colSums.toList()) {
                        isSuccess = true
                        score += 50
                    } else {
                        showErrors = true
                    }
                },
                onReset = { startNewGame(level) },
                onHint = {
                    // Reveal a random empty cell as a hint.
                    val emptyIndices = userInput.indices.filter { userInput[it].isEmpty() }
                    if (emptyIndices.isNotEmpty()) {
                        val idx = emptyIndices.random()
                        userInput[idx] = solution[idx].toString()
                    }
                }
            )
        }

        // Overlay screen for "Game Over" or "Victory".
        if (isGameOver || isSuccess) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isSuccess) "VICTORY!" else "GAME OVER",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            if (isSuccess) {
                                // If victorious, advance to the next level.
                                val nextLevel = level + 1
                                level = nextLevel
                                startNewGame(nextLevel)
                            } else {
                                // Otherwise, restart the current level.
                                startNewGame(level)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("PLAY AGAIN", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

/**
 * A small card composable to display a game statistic like Score or Level.
 */
@Composable
fun GameStatCard(label: String, value: String) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * A circular progress indicator that visualizes the remaining time.
 */
@Composable
fun TimerCircle(seconds: Int, size: Dp = 110.dp) {
    // Animate the progress of the circle based on the remaining time.
    val progress by animateFloatAsState(targetValue = seconds / 300f, label = "timer")
    // Format the time as MM:SS.
    val timeStr = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            // The color of the progress bar turns red when time is low.
            color = if (seconds < 30) Color.Red else Color(0xFF00E5FF),
            strokeWidth = 6.dp,
            trackColor = Color.White.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(timeStr, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * The main component for the 3x3 grid and the surrounding sum targets.
 */
@Composable
fun PuzzleGrid(
    userInput: MutableList<String>,
    solution: List<Int>,
    rowTargets: List<Int>,
    colTargets: List<Int>,
    showErrors: Boolean,
    level: Int
) {
    // State to track indices of duplicate numbers for highlighting.
    var duplicateIndices by remember { mutableStateOf(setOf<Int>()) }
    // State to remember the last entered duplicate to clear it after a delay.
    var lastDuplicateIdx by remember { mutableIntStateOf(-1) }

    // Effect to clear duplicate highlights and the invalid number after a delay.
    LaunchedEffect(duplicateIndices) {
        if (duplicateIndices.isNotEmpty()) {
            delay(2000)
            if (lastDuplicateIdx != -1) {
                userInput[lastDuplicateIdx] = ""
                lastDuplicateIdx = -1
            }
            duplicateIndices = emptySet()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The 3x3 input grid.
            Column {
                for (r in 0..2) {
                    Row {
                        for (c in 0..2) {
                            val idx = r * 3 + c
                            // Check if the current user input for this cell matches the solution.
                            val isCorrectMatch = userInput[idx].isNotEmpty() && userInput[idx] == solution[idx].toString()
                            
                            // Determine if the green "correct" highlight should be shown based on the level.
                            val canShowHighlight = when {
                                level <= 5 -> true
                                level == 6 -> r == 0 || r == 2 // Top and bottom rows only
                                level == 7 -> r == 1          // Middle row only
                                else -> false                 // No highlights
                            }
                            
                            val isCorrect = isCorrectMatch && canShowHighlight

                            GridCell(
                                value = userInput[idx],
                                onValueChange = { newVal ->
                                    if (newVal.isEmpty()) {
                                        userInput[idx] = ""
                                        if (lastDuplicateIdx == idx) {
                                            lastDuplicateIdx = -1
                                            duplicateIndices = emptySet()
                                        }
                                    } else if (newVal.length == 1 && newVal[0] in '1'..'9') {
                                        val existingIndex = userInput.indexOf(newVal)
                                        if (existingIndex != -1 && existingIndex != idx) {
                                            // Duplicate found: highlight both cells and mark for removal.
                                            userInput[idx] = newVal
                                            duplicateIndices = setOf(existingIndex, idx)
                                            lastDuplicateIdx = idx
                                        } else {
                                            // Unique number: update the cell.
                                            userInput[idx] = newVal
                                            if (lastDuplicateIdx == idx) {
                                                lastDuplicateIdx = -1
                                                duplicateIndices = emptySet()
                                            }
                                        }
                                    }
                                },
                                isError = duplicateIndices.contains(idx),
                                isCorrect = isCorrect,
                                label = "${('A' + c)}${r + 1}"
                            )
                            if (c < 2) Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    if (r < 2) Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Display the target sums for each row.
            Column {
                rowTargets.forEachIndexed { i, target ->
                    val currentSum = (0..2).sumOf { userInput[i * 3 + it].toIntOrNull() ?: 0 }
                    val isWrong = showErrors && currentSum != target
                    Text(
                        "= $target",
                        color = if (isWrong) Color(0xFFFF5252) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.height(65.dp).wrapContentHeight()
                    )
                    if (i < 2) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display the target sums for each column.
        Row(modifier = Modifier.padding(end = 55.dp)) {
            colTargets.forEachIndexed { i, target ->
                val currentSum = (0..2).sumOf { userInput[it * 3 + i].toIntOrNull() ?: 0 }
                val isWrong = showErrors && currentSum != target
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(65.dp)
                ) {
                    Text("+", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    HorizontalDivider(
                        modifier = Modifier.width(35.dp).padding(vertical = 2.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        "$target",
                        color = if (isWrong) Color(0xFFFF5252) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (i < 2) Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * Represents a single cell in the 3x3 grid.
 * It handles its own value, styling (error, correct, default), and user input.
 */
@Composable
fun GridCell(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    isCorrect: Boolean = false
) {
    // Determine border and background colors based on the cell's state.
    val borderColor = when {
        isError -> Color.Red
        isCorrect -> Color(0xFF00C853) // Green for correct
        value.isNotEmpty() -> Color(0xFF2196F3) // Blue for filled
        else -> Color(0xFFE0E0E0) // Gray for empty
    }
    val backgroundColor = when {
        isError -> Color(0xFFFFEBEE) // Light red
        isCorrect -> Color(0xFFE8F5E9) // Light green
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(65.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Show a label (e.g., "A1") when the cell is empty.
        if (value.isEmpty()) {
            Text(label, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        }
        // The actual text field for user input.
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                // Determine the text color based on the cell's state.
                color = when {
                    isError -> Color.Red
                    isCorrect -> Color(0xFF2E7D32)
                    else -> Color(0xFF37474F)
                }
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

/**
 * A container for the main game action buttons.
 * @param isSubmitEnabled Controls whether the Submit button is active.
 */
@Composable
fun GameActions(isSubmitEnabled: Boolean, onSubmit: () -> Unit, onReset: () -> Unit, onHint: () -> Unit) {
    // A glowing effect for the submit button when it's enabled.
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // The main Submit button.
        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(if (isSubmitEnabled) 10.dp * glowAlpha else 0.dp, RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00C853),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                "SUBMIT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isSubmitEnabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row containing Reset and Hint buttons.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f).height(48.dp),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("RESET", fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onHint,
                modifier = Modifier.weight(1f).height(48.dp),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("HINT", fontSize = 14.sp)
            }
        }
    }
}
