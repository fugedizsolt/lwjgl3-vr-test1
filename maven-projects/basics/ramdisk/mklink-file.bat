rem powershell.exe -Command "Start-Process cmd -ArgumentList \"/C\",\"mklink\",\"%1\",\"%2\" -Verb RunAs"
mklink "%1" "%2"