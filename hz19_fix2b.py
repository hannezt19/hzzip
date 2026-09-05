# hz19. Fase 2 fix: tambah @OptIn(ExperimentalFoundationApi::class) - HorizontalPager/rememberPagerState
# masih ditandai experimental di versi Compose proyek ini, perlu izin eksplisit biar tidak error saat compile.

path = 'app/src/main/java/com/yohanes/filereader/ui/ImagePagerScreen.kt'
content = open(path).read()

old1 = 'import androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.Image'
new1 = 'import androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.ExperimentalFoundationApi\nimport androidx.compose.foundation.Image'
assert old1 in content, "hz19: pola import tidak ditemukan, cek manual"
content = content.replace(old1, new1)

old2 = '@Composable\nfun ImagePagerScreen('
new2 = '@OptIn(ExperimentalFoundationApi::class)\n@Composable\nfun ImagePagerScreen('
assert old2 in content, "hz19: pola fungsi ImagePagerScreen tidak ditemukan, cek manual"
content = content.replace(old2, new2)

open(path, 'w').write(content)
print("OK: ImagePagerScreen.kt (tambah @OptIn ExperimentalFoundationApi)")
