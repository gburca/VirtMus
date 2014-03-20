- The icepdf directory comes from the ICDpdf-x.x.x-src.zip package. All other
  directories and files are created by NetBeans. When upgrading to a new
  version, only the icepdf directory needs to be erased and replaed with the
  new upstream source code.

- In ICEpdf 5.0.5, NetBeans shows an error in ImageStream.java for:
    import com.sun.image.codec.jpeg.JPEGCodec;
    import com.sun.image.codec.jpeg.JPEGImageDecoder;
  In spite of the error, the code compiles properly and the jar file is in dist/ICDpdf.jar

- Use -XDignore.symbol.file when compiling the library to work around the problem with com.sun.image.*:
  See: https://netbeans.org/bugzilla/show_bug.cgi?id=206774
