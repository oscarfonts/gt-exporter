This archive contains 64 bit Linux shared library files required by SpatiaLite.
The libraries were compiled with gcc 4.4.3. The following packages/versions are
included:

  * geos 3.1.1
  * proj 4.6.1

In order for these libraries to be picked up by SpatiaLite you must:

  * Ensure the 'lib' directory is on -Djava.library.path
  * Ensure the LD_LIBRARY_PATH environment variable is set and includes the
    'lib' directory.

Or if running on Java 6+ alternatively you can simply dump these files into a
directory that that is on the default shared library search path such as
/usr/lib.
