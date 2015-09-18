package co.geomati.geotools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

public class FormatConverter {

	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser() {
			{
				accepts( "help", "Print this help note" ).forHelp();
				accepts( "crs", "Force an output CRS (no reprojection)" )
		    		.withRequiredArg()
		    		.describedAs("srs_def");
				nonOptions("source and destination datasources")
		    		.ofType(File.class)
		    		.describedAs("src dst");
			}
		};
	    OptionSet options = parser.parse(args);
	    if (options.nonOptionArguments().size() != 2) {
		    parser.printHelpOn(System.out);
		    System.exit(0);
	    }
	    File srcFile = (File) options.nonOptionArguments().get(0);
	    File dstFile = (File) options.nonOptionArguments().get(1);	    
	    String crsName = (String) options.valueOf("crs");
	    
	    Map<String, Object> srcParams = new HashMap<String, Object>();
	    if (srcFile.isDirectory()) {
	    	System.out.println("SRC is a directory");
	    	srcParams.put("url", DataUtilities.fileToURL(srcFile));
	    } else if (srcFile.isFile()) {
			//String fileName = "/home/oscar/Projects/tmb/h2/docker/gs-h2/data_dir/TMB/AREA_TMB.shp";
	    	String extension = "";
	    	int i = srcFile.getName().lastIndexOf('.');
	    	if (i > 0) {
	    	    extension = srcFile.getName().substring(i+1);
	    	}
	    	if (extension.equals("shp")) {
	    		System.out.println("SRC is a shapefile");
	    		srcParams.put("url", DataUtilities.fileToURL(srcFile));
	    	} else {
	    		System.out.println("SRC should be a properties file");
	    		srcParams = getProperties(srcFile);
	    	}
	    } else {
	    	System.err.println("Source location does not exist");
	    	System.exit(1);
	    }
	
		Map<String, Object> dstParams = new HashMap<String, Object>();
		dstParams.put("dbtype", "spatialite");
		dstParams.put("database", "OUTPUT.sqlite");
		dstParams.put( "validating connections", false);

		try {
			DataStore src = DataStoreFinder.getDataStore(srcParams);
			DataStore dst = DataStoreFinder.getDataStore(dstParams);
			
			if (src == null) {
				System.err.println("Error creating source datastore");	
				System.exit(2);
			} else if (dst == null) {
				System.err.println("Error creating destination datastore");
				System.exit(2);
			} else {
				copy(src, dst);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	
	private static Map<String, Object> getProperties(File file) {
		Map<String, Object> map = new HashMap<String, Object>();
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(input);
			for (final String name: properties.stringPropertyNames()) {
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

	private static void copy(DataStore src, DataStore dst) throws IOException {
		Transaction t = new DefaultTransaction("handle");
		try {
			System.out.println("Typenames: " + src.getTypeNames());
			String typeName = src.getTypeNames()[0];
			System.out.println("  Processing " + typeName);
			SimpleFeatureSource featureSource = src.getFeatureSource(typeName);
		    int count = featureSource.getCount( Query.ALL );
		    if( count == -1 ){
		        count = featureSource.getFeatures().size();
		    }
			System.out.println("    Feature count: " + count);

			SimpleFeatureType srcFt = featureSource.getSchema();
			SimpleFeatureType dstFt = srcFt;
			if (srcFt.getCoordinateReferenceSystem() == null) { // TODO force as an option
				try {
					dstFt = DataUtilities.createSubType(srcFt, null, CRS.decode("EPSG:23031"));
				} catch (SchemaException | FactoryException e) {
					e.printStackTrace();
				}
			}
			dst.createSchema(dstFt);
			SimpleFeatureStore featureStore = (SimpleFeatureStore) dst.getFeatureSource(typeName);
			SimpleFeatureCollection collection = featureSource.getFeatures();
			SimpleFeatureCollection memory = DataUtilities.collection(collection);
			System.out.println("Fetched into memory!");
			featureStore.setTransaction(t);
			featureStore.addFeatures(memory);
			// Terrible performance here
			// will need to use low level FeatureWriter instead
			// See: http://sourceforge.net/p/geotools/mailman/message/24229107/
			// See also: http://osgeo-org.1560.x6.nabble.com/Re-Performance-writing-SHP-files-for-the-record-td4321719.html
			t.commit();
			System.out.println("Done!");
		} catch (IOException | IllegalArgumentException e) {
			t.rollback();
			System.err.print("Error: " + e.getMessage());
		} finally {
			t.close();
			src.dispose();
			dst.dispose();
		}
	}

}
