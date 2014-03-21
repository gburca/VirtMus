;NSIS Modern User Interface

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"
  !include "WinMessages.nsh"
  !include "LogicLib.nsh"

;--------------------------------
;General
        ; Name and file.
	!define VERSION "3.20"

	; This ${PRODUCT} !define is used throughout this intaller for a lot of
	; things including install directory names and links. It should probably
	; never be changed.
	!define PRODUCT "VirtMus"
	!define WEBSITE "http://virtmus.com/"
	!define UNINSTALLER_NAME "VirtMus-Uninstall.exe"
	!define REG_UNINSTALL "Software\Microsoft\Windows\CurrentVersion\Uninstall\VirtMus"

	Name "${PRODUCT} ${VERSION}"

	OutFile "${PRODUCT}-${VERSION}.exe"

	; Default installation folder
	InstallDir "$PROGRAMFILES\${PRODUCT}-${VERSION}"

	;Request application privileges for Windows
	RequestExecutionLevel admin

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

Function .onInit
	UserInfo::GetAccountType
	pop $0
	${If} $0 != "admin" ; Require admin rights on NT4+
		MessageBox mb_iconstop "Administrator rights required!"
		SetErrorLevel 740 ; ERROR_ELEVATION_REQUIRED
		Quit
	${EndIf}
FunctionEnd

;--------------------------------
;Installer Sections

Section "VirtMus" SVirtMus

  SetOutPath "$INSTDIR"

  File /r ..\dist\virtmus\*.*

  ;Create uninstaller
  WriteUninstaller "$INSTDIR\${UNINSTALLER_NAME}"

  !insertmacro MUI_STARTMENU_WRITE_BEGIN Application

  ;Register uninstaller into Add/Remove panel
  ;KHCU = for local user only, HKLM = local machine
  WriteRegStr   HKLM "${REG_UNINSTALL}" "DisplayName" "${PRODUCT}-${VERSION}"
  WriteRegStr   HKLM "${REG_UNINSTALL}" "DisplayIcon" "$\"$INSTDIR\bin\virtmus.exe$\""
  WriteRegStr   HKLM "${REG_UNINSTALL}" "Publisher" "Ebixio"
  WriteRegStr   HKLM "${REG_UNINSTALL}" "DisplayVersion" "${VERSION}"
  WriteRegDWord HKLM "${REG_UNINSTALL}" "EstimatedSize" 50000 ;KB
  WriteRegStr   HKLM "${REG_UNINSTALL}" "HelpLink" "${WEBSITE}"
  WriteRegStr   HKLM "${REG_UNINSTALL}" "URLInfoAbout" "${WEBSITE}"
  WriteRegStr   HKLM "${REG_UNINSTALL}" "InstallLocation" "$\"$INSTDIR$\""
  WriteRegStr   HKLM "${REG_UNINSTALL}" "InstallSource" "$\"$EXEDIR$\""
  WriteRegDWord HKLM "${REG_UNINSTALL}" "NoModify" 1
  WriteRegDWord HKLM "${REG_UNINSTALL}" "NoRepair" 1
  WriteRegStr   HKLM "${REG_UNINSTALL}" "UninstallString" "$\"$INSTDIR\${UNINSTALLER_NAME}$\""
  WriteRegStr   HKLM "${REG_UNINSTALL}" "Comments" "Uninstalls ${PRODUCT}-${VERSION}"

  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\${PRODUCT}-${VERSION}.lnk" "$INSTDIR\bin\VirtMus.exe"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\${PRODUCT}-${VERSION} Docs.lnk" "$INSTDIR\Docs\index.html"
  CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall-${VERSION}.lnk" "$INSTDIR\${UNINSTALLER_NAME}"
  CreateShortCut "$DESKTOP\${PRODUCT}-${VERSION}.lnk" "$INSTDIR\bin\VirtMus.exe"

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

  RMDir /r /REBOOTOK "$INSTDIR"

  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder

  Delete "$SMPROGRAMS\$StartMenuFolder\${PRODUCT}-${VERSION}.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\${PRODUCT}-${VERSION} Docs.lnk"
  Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall-${VERSION}.lnk"
  RMDir "$SMPROGRAMS\$StartMenuFolder"
  Delete "$DESKTOP\${PRODUCT}-${VERSION}.lnk"

  ;Deregister uninstaller from Add/Remove panel
  DeleteRegKey HKLM "${REG_UNINSTALL}"

SectionEnd

; vim: filetype=nsis
