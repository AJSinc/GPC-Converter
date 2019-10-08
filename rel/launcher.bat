
for /f "tokens=1,* delims= " %%a in ("%*") do set ALL_BUT_FIRST=%%b
start javaw -jar GPC-Converter.jar "%*"