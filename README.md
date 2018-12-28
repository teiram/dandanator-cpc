# dandanator-cpc
ROM assembler for the [CPC Dandanator Mini](http://www.dandare.es/Proyectos_Dandare/CPC_Dandanator%21_Mini.html).
An Amstrad CPC peripheral that allows for an easy and fast way to load snapshots in the machine.

This tool provides a way to create ROMSet compilations with our games of choose. Currently it supports games in SNA format.
Support for games in DSK and CDT format is also provided but still in beta stage.

##Requisites
Git, Maven and java8 are needed

##Cloning the repository
	git clone https://github.com/teiram/dandanator-cpc.git

##Building
Java8 and Maven are needed. Just execute:

	cd dandanator-cpc
	mvn install

##Executing

An executable jar with all the dependencies bundled in will be generated in the following location:

    dandanator-cpc/target/dandanator-cpc-1.0-jar-with-dependencies.jar

that can be executed with the following invocation:

	java -jar target/dandanator-cpc-1.0-jar-with-dependencies.jar

In most modern operating systems it should be also possible to execute the application by just double clicking on the jar file.
