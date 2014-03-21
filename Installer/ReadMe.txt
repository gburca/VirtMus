Directories:
============
- BradedExe
	= A version of the launcher with the icon changed. The build script
	will replace the default one with this launcher when creating the zip
	and exe installers.

- SF-ReadMe
	= Some ReadMe files for sourceforge.net

- VirtMusPortable
	= Files required to create the PortablsApps.com version of VirtMus

To release:
===========
- Set the version in the following places:
	- Installer/VirtMus.nsi
	- nbproject/project.properties
	- VirtMus/src/com/ebixio/virtmus/MainApp.java
	- Docs: VirtMus and ChangeLog tiddlers
	- Installer/VirtMusPortable/App/AppInfo/appinfo.ini

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
	- From the DocMaintenance tiddler, click on "publish" and "generate SEO files"
	- Copy the entire Docs directory to virtmus.com
	- Follow the "Step-by-step guide" section

- To create the PortableApps version for the USB drive
	- Unzip the Installer/VirtMus-v.vv.zip into Installer/VirtMusPortable/App/VirtMus
		- The executable should be in .../App/VirtMus/bin/virtmus.exe
	- Run "PortableApps.com Launcher" from PortableApps on USB drive
		- Point it to .../Installer/VirtMusPortable
		- This will create Installer/VirtMusPortable/VirtMusPortable.exe
	- Run "PortableApps.com Installer" from PortableApps on USB drive
		- Point it to .../Installer/VirtMusPortable
		- This will create Installer/VirtMusPortable_v.vv.paf.exe
	- Distribute
		- Installer/VirtMusPortable_v.vv.paf.exe

