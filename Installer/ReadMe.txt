- Set the version in the following places:
	- Installer/VirtMus.nsi
	- nbproject/project.properties
	- VirtMus/src/com/ebixio/virtmus/MainApp.java

- Run the "update-version-string" ant target

- Change the copyright year range on the splash screen
	- branding/core/core.jar/org/netbeans/core/startup/...

- Run the following targets:
	- clean
	- update-version-string
	- build-zip
	- create-installer

- Rename dist/virtmus.zip to VirtMus_v.vv.zip
- Rename the "virtmus" directory inside the zip to VirtMus_v.vv

- Distribute:
	- Installer/VirtMus_v.vv.exe
	- dist/VirtMus_v.vv.zip
