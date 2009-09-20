;NSIS Modern User Interface
;Start Menu Folder Selection Example Script
;Written by Joost Verburg

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"

;--------------------------------
;General
        ; Name and file.
	!define VERSION "2.50"

	; This ${PRODUCT} !define is used throughout this intaller for a lot of
	; things including install directory names and links. It should probably
	; never be changed.
	!define PRODUCT "VirtMus"

	Name "${PRODUCT} ${VERSION}"
	
	OutFile "${PRODUCT}_${VERSION}.exe"
	
	; Default installation folder
	InstallDir "$PROGRAMFILES\${PRODUCT}_${VERSION}"
  
	;Request application privileges for Windows Vista
	RequestExecutionLevel user

;--------------------------------
;Variables

	Var StartMenuFolder

;--------------------------------
;Interface Settings

	!define MUI_ABORTWARNING
	!define MUI_ICON ..\Docs\src\AppIcons\Icon48x48x256.ico

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_LICENSE "..\License.txt"
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "VirtMus" SVirtMus

  SetOutPath "$INSTDIR"

  File /r ..\dist\VirtMus\*.*
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    
  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\${PRODUCT}.lnk" "$INSTDIR\bin\VirtMus.exe"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\${PRODUCT} Docs.lnk" "$INSTDIR\Docs\index.html"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
  CreateShortCut "$DESKTOP\${PRODUCT}.lnk" "$INSTDIR\bin\VirtMus.exe"
 
  !insertmacro MUI_STARTMENU_WRITE_END

SectionEnd

;--------------------------------
;Descriptions

  ;Language strings
  LangString DESC_SVirtMus ${LANG_ENGLISH} "Main VirtMus application."

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SVirtMus} $(DESC_SVirtMus)
  !insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ; Delete VirtMus Files

  Delete "$INSTDIR\Uninstall.exe"

  RMDir /r /REBOOTOK "$INSTDIR"
  
  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
    
  Delete "$SMPROGRAMS\$StartMenuFolder\${PRODUCT}.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\${PRODUCT} Docs.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"
  Delete "$DESKTOP\${PRODUCT}.lnk"
  
SectionEnd
