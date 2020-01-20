# TripBuilderRDF

TripBuilderRDF is a semantic trajectory dataset constructed from user-generated content obtained from Flickr, combined with data from Wikipedia. The dataset contains user trajectories in 3 different Italian cities: Pisa, Rome, and Firenze.

File structure of the project:
* src/main/resources/TripBuilderRawData -  Raw data containing original dataset (https://github.com/igobrilhante/TripBuilder)
* src/main/resources/trajectory_ijgis.ttl.gz - RDF version of the raw data
* src/main/java - Java program to convert the raw data.
