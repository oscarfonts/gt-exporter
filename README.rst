Shapefile Connection Parameters
-------------------------------

http://docs.geotools.org/stable/userguide/library/data/shape.html

============== =========================================================
Parameter      Description
============== =========================================================
"url"          A URL of the file ending in "shp"
"charset"      Charset used to decode strings in the DBF file (optional)
"timezone"     Timezone used to parse dates in the DBF file (optional)
============== =========================================================

Example::

    url=example.shp
	

PostGIS Connection Parameters
-----------------------------

http://docs.geotools.org/stable/userguide/library/jdbc/postgis.html

============== =============================================
Parameter      Description
============== =============================================
"dbtype"       Must be the string "postgis"
"host"         Machine name or IP address to connect to
"port"         Port number to connect to (default 5432)
"database"     The database to connect to
"schema"       The database schema to access (TODO default?)
"user"         User name
"passwd"       Password
============== =============================================

Example::

    dbtype=postgis
    host=localhost
    port=5432
    database=database
    schema=public
    user=postgres
    passwd=postgres


SpatiaLite Connection Parameters
--------------------------------

http://docs.geotools.org/stable/userguide/library/jdbc/spatialite.html

.. note:: This plugin uses internal versions of both SQLite (3.7.2) and SpatiaLite (2.4.0).
   Therefore the plugin is only capable of accessing databases that are compatible with these 
   versions.
   
 .. note:: Depends on PROJ and GEOS libraries. In Linux systems, if you previously
    installed PostGIS, GDAL or SpatiaLite packages, they are probably there. But please
    refer to the GeoTools link above if you need precompiled binaries for your system.

============== ============================================
Parameter      Description
============== ============================================
"dbtype"       Must be the string "spatialite"
"database"     The database to connect to
"user"         User name (optional)
"passwd"       Password (optional)
============== ============================================

Example::

    dbtype=spatialite
    database=geotools.db


H2 Connection Parameters
------------------------

http://docs.geotools.org/stable/userguide/library/jdbc/h2.html

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

Example::

    dbtype=h2
    database=geotools
    
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

Example::

    dbtype=h2
    host=localhost
    port=9902
    database=geotools
    user=geotools
    passwd=geotools


Oracle Connection Parameters
----------------------------

http://docs.geotools.org/stable/userguide/library/jdbc/oracle.html

.. note:: The propietary Oracle JDBC driver (``ojdbc7.jar``) has to be manually obtained from
`Oracle <http://www.oracle.com/technetwork/database/features/jdbc/default-2280470.html>`_ and
made available somewhere in the ``CLASSPATH``.

============== ==================================================================
Parameter      Description
============== ==================================================================
"dbtype"       Must be the string "oracle"
"host"         Machine name or IP address to connect to
"port"         Port number to connect to (default 1521)
"database"     The database (SID) to connect to
"schema"       The database schema to access (increases access speed if provided)
"user"         User name
"passwd"       Password
============== ==================================================================

Example::

	dbtype=oracle
	host=localhost
	port=1521
	database=database
	schema=public
	user=geotools
	passwd=geotools
