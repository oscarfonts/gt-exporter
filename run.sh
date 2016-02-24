export LD_LIBRARY_PATH="spatialite-libs-linux-x86_64/lib/"
java -Djava.library.path="spatialite-libs-linux-x86_64/lib/" -Djava.util.logging.config.file="src/test/resources/logging.properties" -cp "target/*:target/dependency/*" co.geomati.geotools.Exporter "$@"
