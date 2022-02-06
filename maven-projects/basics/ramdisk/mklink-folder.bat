rem powershell.exe -Command "Start-Process cmd -ArgumentList \"/C\",\"mklink\",\"/D\",\"%1\",\"%2\" -Verb RunAs"
mklink /D "%1" "%2"
