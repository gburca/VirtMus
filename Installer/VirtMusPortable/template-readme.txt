This is a (the?) PortableApps.com Application Template. You should probably
start a new package from this. There are a few things which need doing to make
your application package from this:

* Update help.html, replacing strings in between double asterisks (like ``**App
  Name**``) with the appropriate details of the app. Also app-specific links
  may be added.

* Put the app in the right subdirectory in App.

* Create everything in App\AppInfo and App\AppInfo\Launcher (the Development
  Test splash screen has been put in for convenience).

* Run the PortableApps.com Launcher Generator over the directory to build the
  launcher (make sure you've done the AppInfo stuff first, or at least the app
  name in appinfo.ini and appicon.ico).

Additionally, you should not change anything in the Other directory.
