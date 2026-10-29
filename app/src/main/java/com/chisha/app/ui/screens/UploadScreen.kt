package com.chisha.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.content.FileProvider
import com.chisha.app.domain.PhotoStore
import com.chisha.app.domain.Recipe
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre

@Composable
fun UploadScreen(
    recipe: Recipe,
    onComplete: (photoName: String?, source: String?) -> Unit
) {
    val context = LocalContext.current
    var photoName by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var pendingCameraName by remember { mutableStateOf<String?>(null) }

    fun loadPreview(name: String) {
        preview = runCatching {
            BitmapFactory.decodeFile(PhotoStore.fileFor(context, name).absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val name = pendingCameraName
        if (ok && name != null) {
            PhotoStore.compressInPlace(context, name)
            photoName = name
            source = "camera"
            loadPreview(name)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = PhotoStore.importFromUri(context, uri)
            if (name != null) {
                photoName = name
                source = "gallery"
                loadPreview(name)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("拍一张成品", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(
            "做完「${recipe.name}」了吧？留张照片，这道菜就正式进图鉴了",
            fontSize = 13.sp,
            color = InkTertiary
        )
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF7EFE4)),
            contentAlignment = Alignment.Center
        ) {
            val img = preview
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = "成品图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    "还没有照片\n传图才能拿到经验",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = InkTertiary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val name = PhotoStore.newFileName()
                    pendingCameraName = name
                    val file = PhotoStore.cameraTarget(context, name)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("拍照", color = InkSecondary) }

            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("从相册选", color = InkSecondary) }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onComplete(photoName, source) },
            enabled = photoName != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre)
        ) {
            Text(
                if (photoName == null) "先传一张成品图" else "结算经验",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { onComplete(null, null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("不传图，只记一条「已完成（无图）」", fontSize = 12.sp, color = InkTertiary)
        }
        Spacer(Modifier.height(16.dp))
    }
}
