josm-jogl
==================

Provides the JOGL (OpenGl) library for other JOSM plugins.


*** License

This software is provided "AS IS" without a warranty of any kind.  You use it on your own risk and responsibility!!!

This program is shared on license BSDv3. More information in file BSD3.
Some parts of program as source, images, models may be shared on different licenses. In case of doubt ask.

* Release process

 - mvn -DignoreSnapshots=true release:clean release:prepare 
 
 - mvn -Darguments="-Dmaven.deploy.skip=true"  release:perform
 
 - update url in:
https://josm.openstreetmap.de/wiki/PluginsSource

