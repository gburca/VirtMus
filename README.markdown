
Summary
=======

VirtMus (virtual music) is a free application that allows the user to display
sheet music and turn pages without removing the hands (or feet) from the
instrument the music is performed on. This feature is very useful during
concerts and practice sessions as it allows the musician to focus on performing
the music without interruption. The software also allows the users to store and
organize their entire sheet music collection on a laptop, making it fully
portable and available at a click of a button.

Details
=======

See the http://virtmus.com/ website for more details and full documentation.

TODO
====
Java 8 - Broken!!! Weird exceptions and the display is all messed up (nodes don't redraw properly, etc...)
git submodule init
update

- Allow the user to actually delete a song file from disk. Don't make them go
  out and use a file editor for it.
- Revisit the selection logic in IcePdfImg and foogly()
- Crop bounds seem to be ignored by PDFRenderer (Our Father)
- When running from NetBeans, the build/public-package-jars doesn't contain the
  windows JAI jar file.
- It's possible to add a pdf file as a song, since we don't check if the "new
  song" file is a real song file. To test, try to add a "new song" to a
  playlist, and when the dialog box comes up, select an existing pdf file
  instead of providing the new song name. What happens is that a new song file
  is created with .song.xml appended. That might be OK, but it should finish
  off by adding all the PDF pages to that song file.
- IcePdfImg and PdfViewImg@getDimension() must return the same value,
  regardless of screen size, otherwise the annotations are all messed up when
  the user changes screens. Need to be able to paint WRT a coordinate system
  that doesn't change.
