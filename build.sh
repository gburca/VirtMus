#!/bin/bash

# NetBeans can only be built with Java6, but NetBeans projects needs Java7 or 8.
 export JAVA_HOME=/usr/lib/jvm/java-7-oracle
 ant create-installer-zip
