/*
 * 
 */
package integration.org.jvalue.ods.translator;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.jvalue.ods.data.DataSource;
import org.jvalue.ods.data.generic.GenericValue;
import org.jvalue.ods.data.generic.ListValue;
import org.jvalue.ods.data.generic.MapValue;
import org.jvalue.ods.db.DbAccessor;
import org.jvalue.ods.db.DbFactory;
import org.jvalue.ods.translator.OsmTranslator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The Class OsmTranslatorTest.
 */
public class OsmTranslatorTest {

	/** The translator. */
	private OsmTranslator translator;

	/** The test url. */
	private final String testUrl = "http://api.openstreetmap.org/api/0.6/map?bbox=9.382840810129357,52.78909755467678,9.392840810129357,52.79909755467678";

	/**
	 * Sets the up.
	 */
	@Before
	public void setUp() {
		translator = new OsmTranslator();
		assertNotNull(translator);
	}

	/**
	 * Test Translate.
	 */
	@Test
	public void testTranslate() {
		ListValue lv = (ListValue) translator.translate(new DataSource(testUrl,
				null));
		assertNotNull(lv);
	}

	/**
	 * Test Translate.
	 */
	@Test
	public void testTranslateOffline() {
		ListValue lv = (ListValue) translator.translate(new DataSource(
				"/nbgcity.osm", null));
		assertNotNull(lv);
		DbAccessor<JsonNode> db = DbFactory.createDbAccessor("testOsm");
		db.connect();

		List<MapValue> listMap = new LinkedList<MapValue>();

		for (GenericValue gv : lv.getList()) {
			listMap.add((MapValue) gv);
		}

		db.executeBulk(listMap, null);
		db.deleteDatabase();
	}

	/**
	 * Test Translate invalid source.
	 */
	@Test
	public void testTranslateInvalidSource() {
		ListValue lv = (ListValue) translator.translate(new DataSource(
				"invalidsource", null));
		assertNull(lv);
	}

	/**
	 * Test Translate null source.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testTranslateNullSource() {
		translator.translate(null);
	}

}