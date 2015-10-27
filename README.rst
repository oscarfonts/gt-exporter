===============================
Geotools Geodata Exporting Tool
===============================

Transfers all geodata from a source database to a target database. Supported formats are:

* PostGIS
* Spatialite
* Geopackage
* H2
* Oracle Spatial (needs ojdbc7.jar driver from Oracle)
* A shapefile, or directory of shapefiles


Get the code and compile
========================

Requires git, maven and Java 7.

::

	git clone git@github.com:oscarfonts/gt-exporter.git
	cd gt-exporter
	mvn package dependency:copy-dependencies


Run from command line
=====================

::

	java -cp "*:dependency/*" co.geomati.geotools.Exporter [--list] [--layers <layer_list>] [--force] [--crs <crs_code>] <source> <target>

Or via the helper ``run.sh`` script::

	./run.sh [--list] [--layers <layer_list>] [--force] [--crs <crs_code>] <source> <target>

.. note:: The -cp classpath option indicates two directories separated by ":". On windows, this separator character is a ";".

Where:

* --list lists all the available layers. No actual processing is done when this option is set.
* --layers <layer_list> indicate a comma separated list with the layers to be exported. If not indicated, all the available layers are processed.
* --force if the target layer exist, overwrite it. Use with caution. By default, if destination layer exists no export is done.
* --crs <crs_code> is an optional CRS that will be assigned to target datasets, ignoring any detected source CRS. Please note that no reprojection is performed.
* <source> is a shapefile, a directory of shapefiles, or a properties file defining a database connection to read the data from.
* <target> is a shapefile, a directory of shapefiles, or a properties file defining a database connection copy the data to.


Examples:

List the available layers in an Oracle database::

	./run.sh --list oracle.properties ./

Export a couple of layers to a Spatialite file::

	./run.sh --layers RIVERS,PROVINCES postgis.properties spatialite.properties

Massive export from Oracle to PostGIS, overwritting any target data::

	./run.sh --force oracle.properties postgis.properties

Convert a directory of Shapefiles to an H2 database, explicitly stating their CRS::

	./run.sh --crs EPSG:23031 /a/directory/of/shapefiles/ h2.properties



Properties file connection parameters
=====================================

PostGIS
-------

============== ======================================================
Parameter      Description
============== ======================================================
"dbtype"       Must be the string "postgis"
"host"         Machine name or IP address to connect to
"port"         Port number to connect to
"database"     The database to connect to
"schema"       The database schema to access (optional if DB default)
"user"         User name
"passwd"       Password
============== ======================================================

Example ``postgis.properties``::

	dbtype=postgis
	host=localhost
	port=5432
	database=tmb
	user=tmb
	passwd=tmb


See also: http://docs.geotools.org/stable/userguide/library/jdbc/postgis.html


SpatiaLite
----------

============== ============================================
Parameter      Description
============== ============================================
"dbtype"       Must be the string "spatialite"
"database"     The database to connect to
============== ============================================

Example ``spatialite.properties``::

	dbtype=spatialite
	database=DATABASE.sqlite

.. note:: Depends on PROJ and GEOS libraries. In Linux systems, if you previously
    installed PostGIS, GDAL or SpatiaLite packages, they are probably there. But please
    refer to the GeoTools link above if you need precompiled binaries for your system.

.. note:: This plugin uses internal versions of both SQLite (3.7.2) and SpatiaLite (2.4.0).
   Therefore the plugin is only capable of accessing databases that are compatible with these 
   versions.

See also: http://docs.geotools.org/stable/userguide/library/jdbc/spatialite.html


Geopackage
----------

============== ============================================
Parameter      Description
============== ============================================
"dbtype"       Must be the string "geopkg"
"database"     The database to connect to
============== ============================================

Example ``geopkg.properties``::

	dbtype=geopkg
	database=DATABASE.gpkg

This is an unsupported geotools module, there's no further documentation.


H2
--

H2 has two connection modes: the "embedded" mode (single connection), and a "server" mode (allows multiple connections).

"Embedded" mode
...............

============== =============================================
Parameter      Description
============== =============================================
"dbtype"       Must be the string "h2"
"database"     The database (filename without .db extension)
"user"         User name (optional)
"passwd"       Password (optional)
============== =============================================

Example ``h2.properties``::

	dbtype=h2
	database=H2_DATABASE
    
"Server" mode
.............

============== ============================================
Parameter      Description
============== ============================================
"dbtype"       Must be the string "h2"
"host"         Machine name or IP address to connect to
"port"         Port number to connect to (TODO defaults?)
"database"     The database to connect to
"user"         User name (optional)
"passwd"       Password (optional)
============== ============================================

Example ``h2-server.properties``::

	dbtype=h2
	host=localhost
	port=9902
	database=H2_DATABASE
	user=geotools
	passwd=geotools

See also: http://docs.geotools.org/stable/userguide/library/jdbc/h2.html


Oracle Spatial
--------------

========================= ========================================================================
Parameter                 Description
========================= ========================================================================
"dbtype"                  Must be the string "oracle"
"host"                    Machine name or IP address to connect to
"port"                    Port number to connect to (default 1521)
"database"                The database (SID) to connect to
"schema"                  The database schema to access (use it, greatly reduces access time!)
"user"                    User name
"passwd"                  Password
"Geometry metadata table" Optional but recommended, speeds up lookups (see geotools documentation)
========================= ========================================================================

Example ``oracle.properties``::

	dbtype=oracle
	host=localhost
	port=1521
	database=sid
	schema=public
	user=geotools
	passwd=geotools
	Geometry\ metadata\ table=GEOMETRY_COLUMNS

.. note:: The propietary Oracle JDBC driver (``ojdbc7.jar``) has to be manually obtained from
	`Oracle <http://www.oracle.com/technetwork/database/features/jdbc/default-2280470.html>`_.
	You can add the jar location to the "-cp" option at run time, or include it as a maven
	dependency at build time (see comments in ``pom.xml``).

See also: http://docs.geotools.org/stable/userguide/library/jdbc/oracle.html
