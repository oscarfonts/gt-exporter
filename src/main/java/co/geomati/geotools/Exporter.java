package co.geomati.geotools;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Copies data from a GeoTools DataStore to another GeoTools DataStore.
 * Supported DataStore formats are: Shapefile, directory of spatial files,
 * PostGIS, Spatialite, H2 and Oracle Spatial.
 * 
 * It can be run from command line or instantiated as a class.
 * 
 * @author Oscar Fonts <oscar.fonts@geomati.co>
 */
public class Exporter implements Closeable {

	DataStore src, dst;
	CoordinateReferenceSystem forcedCRS;

	/**
	 * The CLI entry point.
	 * 
	 * Accepts a source and a destination location (properties file, shapefile,
	 * or directory of shapefiles). The -crs option lests assign an explict CRS
	 * to the target data (no reprojection implied, only assignment).
	 * 
	 * @param args
	 *            Source and target locations, and, optionaly, a CRS code in the
	 *            form "Authority:Code". For instance, "EPSG:4326".
	 * @throws IOException
	 *             Something happened accessing the data
	 * @throws FactoryException
	 *             A DataStore or CRS definitions don't correspond to an
	 *             available GeoTools resource.
	 */
	public static void main(String[] args) throws IOException, FactoryException {
		// OptionParser helps define and parse the command interface (parameters
		// and options)
		OptionParser parser = new OptionParser() {
			{
				accepts("help", "Print this help note").forHelp();
				accepts("crs", "Force an output CRS (no reprojection)")
						.withRequiredArg().describedAs("srs_def");
				nonOptions("source and destination datasources").ofType(
						File.class).describedAs("src dst");
			}
		};

		// Read the CLI arguments
		OptionSet options = parser.parse(args);

		// 2 arguments needed: A source and a destination
		if (options.nonOptionArguments().size() != 2) {
			parser.printHelpOn(System.out);
			System.exit(0);
		}

		// Get the options
		File srcFile = (File) options.nonOptionArguments().get(0);
		File dstFile = (File) options.nonOptionArguments().get(1);
		String crsName = (String) options.valueOf("crs");

		// Instantiate FormatConverter
		Exporter converter = new Exporter(getConfig(srcFile),
				getConfig(dstFile));

		// Optionally pass the CRS on
		if (crsName != null) {
			converter.forceCRS(crsName);
		}

		// Ta-daaa!
		converter.run();

		// Need to explicitly free resources
		converter.close();
	}

	/**
	 * Instantiate the FormatConverter. If the given configuration(s) don't
	 * correspond to any available DataStore, a FactoryException is thrown.
	 * 
	 * @param srcConfig
	 *            Source DataStore configuration parameters
	 * @param dstConfig
	 *            Target DataStore configuration parameters
	 * @throws IOException
	 *             Something happened accessing the data
	 * @throws FactoryException
	 *             A DataStore or CRS definitions don't correspond to an
	 *             available GeoTools resource.
	 */
	public Exporter(Map<String, Object> srcConfig,
			Map<String, Object> dstConfig) throws IOException, FactoryException {
		src = DataStoreFinder.getDataStore(srcConfig);
		if (src == null) {
			throw new FactoryException(
					"Error creating source datastore, please review its configuration parameters");
		}

		dst = DataStoreFinder.getDataStore(dstConfig);
		if (dst == null) {
			throw new FactoryException(
					"Error creating target datastore, please review its configuration parameters");
		}
	}

	/**
	 * Assign the given CRS to the target data, ignoring any source CRS
	 * definition. No reprojection is performed.
	 * 
	 * @param crs
	 *            The CRS code in the form "Authority:Code". For instance,
	 *            "EPSG:4326".
	 * @throws FactoryException
	 *             Code couldn't be located in the embedded EPSG database.
	 */
	public void forceCRS(String crs) throws FactoryException {
		forcedCRS = CRS.decode(crs);
	}

	/**
	 * Copy the data from SRC dataStore to DST dataStore.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		// Iterate over available types (DB tables or SHP files)
		String[] typeNames = src.getTypeNames();
		for (int i = 0; i < typeNames.length; i++) {
			copyLayer(typeNames[i]);
		}
	}

	/**
	 * Frees resources (datastores have to be disposed when done)
	 */
	@Override
	public void close() {
		if (dst != null) {
			dst.dispose();
			dst = null;
		}
		if (src != null) {
			src.dispose();
			src = null;
		}
		forcedCRS = null;
	}

	/**
	 * Returns DataStore configuration parameters based on a file, that could
	 * be: a) A shapefile, or a directory of shapefiles. b) A properties file
	 * containing a datastore configuration.
	 * 
	 * @param file
	 *            The file to read.
	 * @return A Map with the configuration parameters.
	 */
	protected static Map<String, Object> getConfig(File file) {
		Map<String, Object> config = new HashMap<String, Object>();

		if (isShapeFile(file) || file.isDirectory()) {
			// A shapefile or directory of spatial files
			config.put("url", DataUtilities.fileToURL(file));
		} else if (file.isFile()) {
			// Another kind of file, try to parse as a properties file");
			config = readProperties(file);
		} else {
			// Not a file, not a directory...
			System.err.println("File " + file.getName() + " does not exist");
			System.exit(1);
		}
		return config;
	}

	/**
	 * Determines if a given file name has the "shp" extension.
	 * 
	 * @param file
	 *            The file to check
	 */
	protected static boolean isShapeFile(File file) {
		String extension = "";
		int dot = file.getName().lastIndexOf('.');
		if (dot > 0) {
			extension = file.getName().substring(dot + 1);
		}
		return file.isFile() && extension.toLowerCase().equals("shp");
	}

	/**
	 * Given a properties file, returns a Map with the property collection
	 * 
	 * @param file
	 *            The file to read
	 * @return A key-value pair Map
	 */
	protected static Map<String, Object> readProperties(File file) {
		Map<String, Object> map = new HashMap<String, Object>();
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(input);
			for (final String name : properties.stringPropertyNames()) {
				map.put(name, properties.getProperty(name));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	/**
	 * Do the actual data copy, using low-level feature iterators, for a given
	 * FeatureType (aka layer, file, table). Will use transactions to write the
	 * data in chunks of 1000 features (balance between memory and IO usage).
	 * 
	 * @param typeName
	 *            The type name to be copied.
	 * @throws IOException
	 */
	protected void copyLayer(String typeName) throws IOException {
		Transaction t = new DefaultTransaction(typeName);
		// Inform about the dataset to be copied, and its feature count.
		SimpleFeatureSource featureSource = src.getFeatureSource(typeName);
		System.out.println("Processing " + typeName + " ("
				+ count(featureSource) + " features).");

		// Use source schema in destination, overriding CRS if needed.
		SimpleFeatureType srcFeatureType = featureSource.getSchema();
		SimpleFeatureType dstFeatureType = srcFeatureType;
		if (forcedCRS != null) {
			try {
				dstFeatureType = DataUtilities.createSubType(srcFeatureType,
						null, forcedCRS);
				System.out.println("Assigned CRS " + forcedCRS.getName().toString());
			} catch (SchemaException e) {
				System.err
						.println("Warning: Couldn't assign the forced CRS to output");
				dstFeatureType = srcFeatureType;
			}
		}

		// Will throw exception if the FeatureType (table, filename) already
		// exists in target datastore.
		dst.createSchema(dstFeatureType);

		// Get src & dst feature iterators
		SimpleFeatureIterator reader = featureSource.getFeatures().features();
		FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dst
				.getFeatureWriter(typeName, t);

		try {
			int c = 0;
			while (reader.hasNext()) {
				c++;

				// Get source and target features
				SimpleFeature srcFeature = reader.next();
				writer.hasNext();
				SimpleFeature dstFeature = writer.next();

				// Copy attributes and write
				dstFeature.setAttributes(srcFeature.getAttributes());
				writer.write();

				// Commit every 1000 features and inform about the progress
				if (c % 1000 == 0) {
					t.commit();
					System.out.print("\r  Processed " + String.valueOf(c)
							+ " features");
				}
			}
			// Done. Final commit.
			t.commit();
			System.out.print("\r  Processed " + String.valueOf(c)
					+ " features.");
			System.out.println("  Done.");
		} catch (IOException e) {
			e.printStackTrace();
			try {
				t.rollback();
			} catch (IOException ee) {
				// rollback failed?
				ee.printStackTrace();
			}
		} finally {
			t.close();
			writer.close();
			reader.close();
		}
	}

	/**
	 * Counts the number of features in a feature source
	 */
	private int count(SimpleFeatureSource fs) throws IOException {
		int count = fs.getCount(Query.ALL);
		if (count == -1) {
			count = fs.getFeatures().size();
		}
		return count;
	}

}
