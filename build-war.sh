#!/bin/bash
if [ ! -d misc ]; then
  mkdir misc
  if [ $? -ne 0 ] ; then
    echo "couldn't create misc directory"
    exit
  fi
fi
if [ ! -d misc/WEB-INF ]; then
  mkdir misc/WEB-INF
  if [ $? -ne 0 ] ; then
    echo "couldn't create misc/WEB-INF directory"
    exit
  fi
fi
if [ ! -d misc/WEB-INF/lib ]; then
  mkdir misc/WEB-INF/lib
  if [ $? -ne 0 ] ; then
    echo "couldn't create misc/WEB-INF/lib directory"
    exit
  fi
fi
rm -f misc/WEB-INF/lib/*.jar
cp dist/Misc.jar misc/WEB-INF/lib/
cp lib/markdown4j*.jar misc/WEB-INF/lib/
cp web.xml misc/WEB-INF/
jar cf misc.war -C misc WEB-INF 
echo "NB: you MUST copy the contents of tomcat-bin to \$tomcat_home/bin"
