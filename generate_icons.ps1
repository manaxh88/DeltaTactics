Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\XXH\.gemini\antigravity\brain\530b2e22-56da-4c44-a3e6-be1c8ef90c03\.user_uploaded\media_1789973228732.png"
$resDir = "c:\Users\XXH\Documents\antigravity\keen-faraday\app\src\main\res"

$srcBmp = [System.Drawing.Bitmap]::new($srcPath)
$bgColor = [System.Drawing.Color]::FromArgb(222, 220, 207)

$densities = @(
    @{ name = "mipmap-mdpi";    launcher = 48;  fg = 108 },
    @{ name = "mipmap-hdpi";    launcher = 72;  fg = 162 },
    @{ name = "mipmap-xhdpi";   launcher = 96;  fg = 216 },
    @{ name = "mipmap-xxhdpi";  launcher = 144; fg = 324 },
    @{ name = "mipmap-xxxhdpi"; launcher = 192; fg = 432 }
)

foreach ($d in $densities) {
    $targetDir = Join-Path $resDir $d.name
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }

    # 1. ic_launcher.png (square)
    $lSize = $d.launcher
    $lBmp = [System.Drawing.Bitmap]::new($lSize, $lSize)
    $g = [System.Drawing.Graphics]::FromImage($lBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($srcBmp, 0, 0, $lSize, $lSize)
    $g.Dispose()
    $lBmp.Save((Join-Path $targetDir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $lBmp.Dispose()

    # 2. ic_launcher_round.png (circular clipped)
    $rBmp = [System.Drawing.Bitmap]::new($lSize, $lSize)
    $g = [System.Drawing.Graphics]::FromImage($rBmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddEllipse(0, 0, $lSize, $lSize)
    $g.SetClip($path)
    $g.DrawImage($srcBmp, 0, 0, $lSize, $lSize)
    $path.Dispose()
    $g.Dispose()
    $rBmp.Save((Join-Path $targetDir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $rBmp.Dispose()

    # 3. ic_launcher_foreground.png (for adaptive icon)
    $fgSize = $d.fg
    $fgBmp = [System.Drawing.Bitmap]::new($fgSize, $fgSize)
    $g = [System.Drawing.Graphics]::FromImage($fgBmp)
    $g.Clear($bgColor)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    # Cat face scaled to ~80% of canvas and centered
    $catSize = [int]($fgSize * 0.82)
    $catOffset = [int](($fgSize - $catSize) / 2)
    $g.DrawImage($srcBmp, $catOffset, $catOffset, $catSize, $catSize)
    $g.Dispose()
    $fgBmp.Save((Join-Path $targetDir "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $fgBmp.Dispose()
}

# Also save to drawable/cat_avatar.png for in-app use
$drawableDir = Join-Path $resDir "drawable"
if (-not (Test-Path $drawableDir)) {
    New-Item -ItemType Directory -Path $drawableDir -Force | Out-Null
}
$srcBmp.Save((Join-Path $drawableDir "cat_avatar.png"), [System.Drawing.Imaging.ImageFormat]::Png)

$srcBmp.Dispose()
Write-Host "All icons and avatars generated successfully!"
