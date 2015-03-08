Directories:
============
- VirtMus.conf
	= A copy of NetBeans/harness/etc/app.conf customized for VirtMus to allow
	for more memory. nbproject/project.properties points app.conf to this
	location. A "clean" will need to be done to pick up a modified version of
	this file because the version that ends up in the dist/ installers comes
	from build/ if that's already present.
	= We need to compare this with the original on new NetBeans platforms to
	make sure we're picking up any new features or settings.

- BradedExe
	= A version of the launcher with the icon changed. The build script
	will replace the default one with this launcher when creating the zip
	and exe installers.

- GenerateMacIcons.sh
	= Script to generate .icns file for Mac / OSX

- SF-ReadMe
	= Some ReadMe files for sourceforge.net

- VirtMusPortable
	= Files required to create the PortablsApps.com version of VirtMus

To release:
===========
- See the DeveloperInfo page in the WiKi

