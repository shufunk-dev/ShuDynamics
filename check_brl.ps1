Add-Type -AssemblyName System.IO.Compression.FileSystem

$jar = (Get-ChildItem -Path ".gradle\loom-cache\minecraftMaven" -Recurse -Filter "*1.21.11*loom.mappings*.jar" | Select-Object -First 1).FullName
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$e = $zip.GetEntry("net/minecraft/client/render/BlockRenderLayer.class")
if ($e -ne $null) {
    Write-Host "Found BlockRenderLayer.class"
}
$zip.Dispose()
