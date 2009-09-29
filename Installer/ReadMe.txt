- Set the version in the following places:
	- Installer/VirtMus.nsi
	- nbproject/project.properties
	- VirtMus/src/com/ebixio/virtmus/MainApp.java

- Set the MainApp.RELEASED to true

- Update the documentation ChangeLog

- If needed, change the copyright year range on the splash screen
	- branding/core/core.jar/org/netbeans/core/startup/...
	- Docs/index.html VirtMus tiddler

- Run the following targets:
	- clean
	- update-version-string
	- #build-zip (is run automatically by target below)
        - create-installers

- Distribute:
	- Installer/VirtMus-v.vv.exe
	- Installer/VirtMus-v.vv.zip

- Update virtmus.com
