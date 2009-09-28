- Set the version in the following places:
	- Installer/VirtMus.nsi
	- nbproject/project.properties
	- VirtMus/src/com/ebixio/virtmus/MainApp.java

- Set the MainApp.RELEASED to true

- Update the documentation ChangeLog

- If needed, change the copyright year range on the splash screen
	- branding/core/core.jar/org/netbeans/core/startup/...

- Run the following targets:
	- clean
	- update-version-string
	- build-zip

- Rename dist/virtmus.zip to VirtMus-v.vv.zip
- Rename the "virtmus" directory inside the zip to VirtMus-v.vv
- Unzip the VirtMus-v.vv in the "dist" directory
- Run the create-installer target

- Distribute:
	- Installer/VirtMus-v.vv.exe
	- dist/VirtMus-v.vv.zip

- Update virtmus.com
