$DevicePath = "/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset"
$LocalPath = if ($args.Count -ge 1) { $args[0] } else { "./data" }

adb devices
New-Item -ItemType Directory -Force -Path $LocalPath | Out-Null
adb pull $DevicePath $LocalPath
