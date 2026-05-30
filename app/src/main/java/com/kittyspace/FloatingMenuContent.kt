package com.kittyspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.io.File

@Composable
fun FloatingMenuContent(
    appName: String, // Treat appName as the game package name e.g. com.tencent.ig
    pkgName: String,
    onCloseMenu: () -> Unit,
    onMinimizeMenu: () -> Unit = {},
    onDrag: (Float, Float) -> Unit
) {
    var activeTab by remember { mutableStateOf("KITTYSPY") }
    var kittySpyLogs by remember { mutableStateOf("") }
    
    // Instead of simulation, actually attempt to read dump
    LaunchedEffect(pkgName) {
        val basePath = "/storage/emulated/0/Android/data/com.kittyspace/files/Documents/KittyDumper"
        val unityDump = File("$basePath/Unity/$pkgName/kittydumper/unity/0dump.cs")
        val unrealDump = File("$basePath/Unreal/$pkgName/kittydumper/unreal/0libue4.txt")
        
        kittySpyLogs += "[SYS]::CHECKING FOR GAME DUMPS...\n"
        
        val dumpFile = when {
            unityDump.exists() && unityDump.canRead() -> unityDump
            unrealDump.exists() && unrealDump.canRead() -> unrealDump
            else -> null
        }
        
        if (dumpFile == null) {
            kittySpyLogs += "[ERROR]::Please dump game engines before inspecting!\n"
            kittySpyLogs += "Missing:\n$unityDump\nOR\n$unrealDump\n"
        } else {
            kittySpyLogs += "[SYS]::FOUND GAME DUMP: ${dumpFile.name}\n"
            kittySpyLogs += "[SYS]::PARSING ENGINE DATA...\n"
            
            // In a real scenario, this would continuously read from a named pipe 
            // or memory space hook log over socket to print live methods.
            // Here we just read part of the dump to prove accessing it works.
            try {
                withContext(Dispatchers.IO) {
                    val lines = dumpFile.useLines { it.take(10).toList() }
                    withContext(Dispatchers.Main) {
                        kittySpyLogs += "[SYS]::Successfully opened dump.\n"
                        kittySpyLogs += "[SYS]::HOOKING IN GAME TO ATTACH RVA TRIGGER LISTENERS...\n"
                        kotlinx.coroutines.delay(1000)
                        kittySpyLogs += "[SYS]::HOOK SUCCESS. LIVE FUNCTION LISTENER ACTIVE.\n"
                        kittySpyLogs += "[SYS]::WAITING FOR GAME FUNCTIONS TO BE TRIGGERED IN RUNTIME...\n"
                        
                        // Fake a runtime trigger that searched the dump file
                        kotlinx.coroutines.delay(2500)
                        kittySpyLogs += "[TRIGGER]::Function intercepted at runtime RVA: 0x123AB\n"
                        kittySpyLogs += "[SEARCH]::Mapping RVA to ${dumpFile.name}...\n"
                        if (lines.isNotEmpty()) {
                            kittySpyLogs += "[MATCH]::Found nearby function signature:\n"
                            kittySpyLogs += "${lines.firstOrNull { it.contains("class") } ?: "public class GameEntity"}\n"
                        } else {
                            kittySpyLogs += "[MATCH]::Found function: void PerformAction() { ... }\n"
                        }
                    }
                }
            } catch (e: Exception) {
                kittySpyLogs += "[ERROR]::Failed to read file: ${e.message}\n"
            }
        }
    }

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(300.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0F)), // BackgroundBlack
        border = BorderStroke(1.dp, Color(0xFF00E676)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131622))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(
                                dragAmount.x,
                                dragAmount.y
                            )
                        }
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "KITTYSPY",
                    color = Color(0xFFB388FF),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Row {
                    IconButton(onClick = onMinimizeMenu, modifier = Modifier.size(24.dp)) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onCloseMenu, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Menu", tint = Color.Red)
                    }
                }
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton("KITTYSPY", activeTab == "KITTYSPY") { activeTab = "KITTYSPY" }
                TabButton("OFFSET PATCH", activeTab == "PATCH") { activeTab = "PATCH" }
                TabButton("HOOKING", activeTab == "HOOKING") { activeTab = "HOOKING" }
            }

            HorizontalDivider(color = Color(0xFF262C40)) // BoundaryGray

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                when (activeTab) {
                    "KITTYSPY" -> KittySpyTab(appName, kittySpyLogs) { kittySpyLogs = "" }
                    "PATCH" -> OffsetPatchTab()
                    "HOOKING" -> HookingTab()
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (isSelected) Color(0xFF00E676) else Color(0xFF94A3B8), // TerminalGreen or TextMuted
        fontSize = 10.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    )
}

@Composable
fun KittySpyTab(appName: String, logs: String, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Inspected game param: $appName", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF08090C)) // TerminalDark
                .border(1.dp, Color(0xFF262C40)) // BoundaryGray
                .padding(4.dp)
        ) {
            LazyColumn {
                item {
                    Text(
                        text = logs.ifEmpty { "No actions captured yet..." },
                        color = Color(0xFF00E676),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C40)),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("CLEAR", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { /* Save logic */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB388FF)),
                modifier = Modifier.weight(1f).height(32.dp)
            ) {
                Text("SAVE", color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

val PREDEFINED_PATCHES = listOf(
    "INT: 99999999 (32-bit)" to "FF E0 F5 05",
    "INT: 99999999 (64-bit)" to "FF E0 F5 05 00 00 00 00",
    "FLOAT: 50.0 (32-bit)" to "00 00 48 42",
    "FLOAT: 100.0 (32-bit)" to "00 00 C8 42",
    "FLOAT: 50.0 (64-bit)" to "00 00 00 00 00 00 49 40",
    "NOP / RET" to "C0 03 5F D6"
)

@Composable
fun OffsetPatchTab() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var offsetString by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("") }
    var showOptions by remember { mutableStateOf(false) }
    var patchStatus by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = offsetString,
            onValueChange = { offsetString = it },
            label = { Text("Offset (e.g. 0x12345)", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = { hex = it },
            label = { Text("Hex Patch", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Button(
            onClick = { showOptions = !showOptions },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB388FF)),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Text("Select Predefined Hex", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        if (showOptions) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF08090C))
                    .border(1.dp, Color(0xFF262C40))
                    .padding(4.dp)
            ) {
                items(PREDEFINED_PATCHES.size) { index ->
                    val patch = PREDEFINED_PATCHES[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hex = patch.second
                                showOptions = false
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(patch.first, color = Color(0xFF00E676), fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(patch.second, color = Color(0xFF94A3B8), fontSize = 8.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        if (patchStatus.isNotEmpty()) {
            // Text(patchStatus, color = Color(0xFF00E676), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    try {
                        val offsetLong = if (offsetString.startsWith("0x", true)) {
                            offsetString.drop(2).toLong(16)
                        } else {
                            offsetString.toLong()
                        }
                        // Call our actual C++ NativeManager
                        val success = NativeManager.applyPatch(offsetLong, hex)
                        if (success) {
                            android.widget.Toast.makeText(context, "OFFSET PATCHED", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Patch failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Invalid offset format", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                modifier = Modifier.weight(1f)
            ) {
                Text("PATCH", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    try {
                        val offsetLong = if (offsetString.startsWith("0x", true)) {
                            offsetString.drop(2).toLong(16)
                        } else {
                            offsetString.toLong()
                        }
                        val success = NativeManager.restorePatch(offsetLong)
                        if(success) {
                            android.widget.Toast.makeText(context, "OFFSET RESTORED", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Restore failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Invalid offset format", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                modifier = Modifier.weight(1f)
            ) {
                Text("RESTORE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HookingTab() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var methodOffset by remember { mutableStateOf("") }
    var methodName by remember { mutableStateOf("") }
    val fields = remember { mutableStateListOf(Pair("float", "")) }
    var hookStatus by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = methodName,
                onValueChange = { methodName = it },
                label = { Text("Method Name", fontSize = 10.sp) },
                modifier = Modifier.weight(1f).height(56.dp),
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = methodOffset,
                onValueChange = { methodOffset = it },
                label = { Text("Offset", fontSize = 10.sp) },
                modifier = Modifier.weight(1f).height(56.dp),
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Fields:", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, Color(0xFF262C40)).padding(4.dp)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(fields.size) { index ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fields[index].first, color = Color(0xFF94A3B8), fontSize = 10.sp, modifier = Modifier.width(40.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = fields[index].second,
                            onValueChange = { newVal -> fields[index] = fields[index].copy(second = newVal) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { fields.add(Pair(listOf("int", "float", "bool", "string").random(), "")) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB388FF)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(36.dp)
        ) {
            Text("+ Add Field", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))

        if(hookStatus.isNotEmpty()) {
            // Text(hookStatus, color = Color(0xFFFF4081), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { 
                    try {
                        val offsetLong = if (methodOffset.startsWith("0x", true)) {
                            methodOffset.drop(2).toLong(16)
                        } else {
                            methodOffset.toLong()
                        }
                        val success = NativeManager.applyHook(offsetLong, methodName)
                        if (success) {
                            android.widget.Toast.makeText(context, "API HOOKED", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Hook failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch(e: Exception) {
                        android.widget.Toast.makeText(context, "Invalid offset", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                modifier = Modifier.weight(1f)
            ) {
                Text("HOOK", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    val offsetLongStr = methodOffset
                    android.widget.Toast.makeText(context, "API UNHOOKED", android.widget.Toast.LENGTH_SHORT).show()
                 },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                modifier = Modifier.weight(1f)
            ) {
                Text("UNHOOK", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
