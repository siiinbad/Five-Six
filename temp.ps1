Add-Type -AssemblyName System.Drawing  
$bmp=[System.Drawing.Bitmap]::FromFile('src/res/sprites/menu/menuHitbox/menu_characterselect_hitbox.png')  
$c=$bmp.GetPixel(120,132)  
Write-Output "120,132 -> $($c.R),$($c.G),$($c.B)"  
$bmp.Dispose()  
