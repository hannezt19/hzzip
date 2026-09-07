path = 'app/src/main/java/com/yohanes/filereader/ui/PdfViewerScreen.kt'
content = open(path).read()
changes = 0

old1 = """        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(0.25f)
            ) {
                IconButton(onClick = { onTextSizeChange(settings.textSizeSp - 2f) }) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "${settings.textSizeSp.toInt()} sp",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { onTextSizeChange(settings.textSizeSp + 2f) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Box(modifier = Modifier.weight(0.22f), contentAlignment = Alignment.Center) {
                ModeToggleButton("Baca", modeBacaActive) { onModeBacaChange(!modeBacaActive) }
            }
            Box(modifier = Modifier.weight(0.22f), contentAlignment = Alignment.Center) {
                ModeToggleButton("TTS", ttsActive) { onTtsActiveChange(!ttsActive) }
            }
            Box(modifier = Modifier.weight(0.22f), contentAlignment = Alignment.Center) {
                ModeToggleButton("ID", translateActive) { onTranslateChange(!translateActive) }
            }
        }"""

new1 = """        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { onTextSizeChange(settings.textSizeSp - 2f) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "${settings.textSizeSp.toInt()} sp",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = { onTextSizeChange(settings.textSizeSp + 2f) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ModeToggleButton("Baca", modeBacaActive) { onModeBacaChange(!modeBacaActive) }
            ModeToggleButton("TTS", ttsActive) { onTtsActiveChange(!ttsActive) }
            ModeToggleButton("ID", translateActive) { onTranslateChange(!translateActive) }
        }"""

if content.count(old1) == 1:
    content = content.replace(old1, new1)
    changes += 1
else:
    print("SKIP 1: tidak ketemu, count =", content.count(old1))

old2 = """        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigasiMode.values().forEach { mode ->
                val selected = settings.navMode == mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .aspectRatio(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))"""

new2 = """        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
        ) {
            NavigasiMode.values().forEach { mode ->
                val selected = settings.navMode == mode
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))"""

if content.count(old2) == 1:
    content = content.replace(old2, new2)
    changes += 1
else:
    print("SKIP 2: tidak ketemu, count =", content.count(old2))

old3 = """        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BacaWarnaLatar.values().forEach { warna ->
                val selected = settings.warnaLatar == warna
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.22f)
                        .aspectRatio(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))"""

new3 = """        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            BacaWarnaLatar.values().forEach { warna ->
                val selected = settings.warnaLatar == warna
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))"""

if content.count(old3) == 1:
    content = content.replace(old3, new3)
    changes += 1
else:
    print("SKIP 3: tidak ketemu, count =", content.count(old3))

open(path, 'w').write(content)
print(f"SELESAI: {changes}/3 perubahan berhasil")
