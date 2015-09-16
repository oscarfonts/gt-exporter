package co.geomati.geotools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

public class FormatConverter {

	public static void main(String[] args) {

		Map<String, Object> srcParams = new HashMap<String, Object>();
		String fileName = "/home/oscar/Projects/tmb/h2/docker/gs-h2/data_dir/TMB/AREA_TMB.shp";
		srcParams.put("url", DataUtilities.fileToURL(new File(fileName)));

		Map<String, Object> dstParams = new HashMap<String, Object>();
		dstParams.put("dbtype", "h2");
		dstParams.put("database", "H2_AREA_TMB");

		try {
			DataStore src = DataStoreFinder.getDataStore(srcParams);
			DataStore dst = DataStoreFinder.getDataStore(dstParams);

			copy(src, dst);

			System.out.println("Done!");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void copy(DataStore src, DataStore dst) throws IOException {
		Transaction t = new DefaultTransaction();
		try {
			String typeName = src.getTypeNames()[0];
			SimpleFeatureSource featureSource = src.getFeatureSource(typeName);

			SimpleFeatureType srcFt = featureSource.getSchema();
			SimpleFeatureType dstFt = srcFt;
			if (srcFt.getCoordinateReferenceSystem() == null) { // TODO why not detected?
				try {
					dstFt = DataUtilities.createSubType(srcFt, null, CRS.decode("EPSG:23031"));
				} catch (SchemaException | FactoryException e) {
					e.printStackTrace();
				}
			}
			dst.createSchema(dstFt);
			SimpleFeatureStore featureStore = (SimpleFeatureStore) dst.getFeatureSource(typeName);
			SimpleFeatureCollection collection = featureSource.getFeatures();
			featureStore.setTransaction(t);
			featureStore.addFeatures(collection);
			t.commit();
		} catch (IOException e) {
			t.rollback();
		} finally {
			t.close();
			dst.dispose();
			src.dispose();
		}
	}

}
