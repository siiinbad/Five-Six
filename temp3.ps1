Add-Type -AssemblyName System.Drawing  
$bmp=[System.Drawing.Bitmap]::FromFile('src/res/sprites/menu/menuHitbox/menu_characterselect_hitbox.png')  
$pts=@(@(120,132),@(202,132),@(287,132),@(375,132))  
foreach ($p in $pts) { $c=$bmp.GetPixel($p[0],$p[1]); Write-Output ("{0},{1} -> {2},{3},{4}" -f $p[0],$p[1],$c.R,$c.G,$c.B) }  
$bmp.Dispose()  
