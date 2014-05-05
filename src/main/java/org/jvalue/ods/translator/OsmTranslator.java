/*  Open Data Service
    Copyright (C) 2013  Tsysin Konstantin, Reischl Patrick

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
 */
package org.jvalue.ods.translator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jvalue.ods.data.DataSource;
import org.jvalue.ods.data.generic.GenericValue;
import org.jvalue.ods.data.generic.ListValue;
import org.jvalue.ods.data.generic.MapValue;
import org.jvalue.ods.data.generic.NumberValue;
import org.jvalue.ods.data.generic.StringValue;
import org.jvalue.ods.grabber.Translator;
import org.jvalue.ods.logger.Logging;
import org.jvalue.ods.schema.SchemaManager;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

/**
 * The Class OsmTranslator.
 */
public class OsmTranslator implements Translator {

	/** The lv. */
	private ListValue lv = new ListValue();

	/**
	 * Translate.
	 * 
	 * @param source
	 *            the source
	 * @return the osm data
	 */
	@Override
	public GenericValue translate(DataSource source) {
		if (source == null) {
			throw new IllegalArgumentException("source is null");
		}

		String url = source.getUrl();

		if (url == null || url.length() == 0) {
			throw new IllegalArgumentException("source is empty");
		}

		File file = null;

		if (!url.startsWith("http")) {
			URL sourceUrl = getClass().getResource(url);
			if (sourceUrl == null)
				return null;
			try {
				file = new File(sourceUrl.toURI());
			} catch (URISyntaxException e) {
				Logging.error(this.getClass(), e.getMessage());
				e.printStackTrace();
				return null;
			}
		} else {

			PrintWriter out = null;

			try {
				Logging.info(this.getClass(), "Opening: " + url);
				HttpReader reader = new HttpReader(url);
				String data = reader.read("UTF-8");

				// ToDo: Nicht thread-sicher, Problem bei 2 parallelen Anfragen
				// Schreiben in Dateien nicht ohne weiteres m�glich in tomcat
				File tmpFile = new File("tmp.txt");
				if (tmpFile.exists()) {
					tmpFile.delete();
				}
				out = new PrintWriter("tmp.txt");
				out.println(data);
				file = new File("tmp.txt");
			} catch (IOException e) {
				Logging.error(this.getClass(), e.getMessage());
				// maybe throw another exception here instead of returning null
				return null;
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}

		Sink sinkImplementation = new Sink() {
			public void process(EntityContainer entityContainer) {
				Entity entity = entityContainer.getEntity();
				List<GenericValue> list = lv.getList();

				if (entity instanceof Node) {
					list.add(convertNodeToGenericValue((Node) entity));
				} else if (entity instanceof Way) {
					list.add(convertWayToGenericValue((Way) entity));
				} else if (entity instanceof Relation) {
					list.add(convertRelationToGenericValue((Relation) entity));
				}
				// else if (entity instanceof Bound) {
				//
				// }
			}

			public void release() {
			}

			public void complete() {
			}

			public void initialize(Map<String, Object> metaData) {

			}
		};

		RunnableSource reader = new XmlReader(file, false,
				CompressionMethod.None);

		reader.setSink(sinkImplementation);

		Thread readerThread = new Thread(reader);
		readerThread.start();

		while (readerThread.isAlive()) {
			try {
				readerThread.join();
			} catch (InterruptedException e) {
				// maybe throw another exception here instead of returning null
				return null;
			}
		}

		if (file.exists() && url.startsWith("http")) {
			file.delete();
		}

		if (source.getDataSourceSchema() != null) {
			if (!SchemaManager.validateGenericValusFitsSchema(lv,
					source.getDataSourceSchema()))
				return null;
		}

		return lv;
	}

	/**
	 * Convert relation to generic value.
	 * 
	 * @param relation
	 *            the relation
	 * @return the map value
	 */
	private MapValue convertRelationToGenericValue(Relation relation) {
		MapValue mv = new MapValue();
		Map<String, GenericValue> map = mv.getMap();
		map.put("type", new StringValue("Relation"));

		map.put("relationId", new StringValue("" + relation.getId()));
		map.put("timestamp",
				new StringValue(relation.getTimestamp().toString()));
		map.put("uid", new NumberValue(relation.getUser().getId()));
		map.put("user", new StringValue(relation.getUser().getName()));
		map.put("version", new NumberValue(relation.getVersion()));
		map.put("changeset", new NumberValue(relation.getChangesetId()));

		MapValue tagsMapValue = new MapValue();
		Map<String, GenericValue> tagsMap = tagsMapValue.getMap();
		Collection<Tag> coll = relation.getTags();
		for (Tag tag : coll) {
			tagsMap.put(tag.getKey(), new StringValue(tag.getValue()));
		}
		map.put("tags", tagsMapValue);

		ListValue memberList = new ListValue();
		for (RelationMember rm : relation.getMembers()) {
			MapValue membersMapValue = new MapValue();
			Map<String, GenericValue> membersMap = membersMapValue.getMap();

			membersMap.put("type", new StringValue(rm.getMemberType()
					.toString()));
			membersMap.put("ref", new NumberValue(rm.getMemberId()));
			membersMap.put("role", new StringValue(rm.getMemberRole()));
			memberList.getList().add(membersMapValue);
		}

		map.put("members", memberList);

		return mv;
	}

	/**
	 * Convert way to generic value.
	 * 
	 * @param w
	 *            the w
	 * @return the map value
	 */
	private MapValue convertWayToGenericValue(Way w) {
		MapValue mv = new MapValue();
		Map<String, GenericValue> map = mv.getMap();
		map.put("type", new StringValue("Way"));

		map.put("wayId", new StringValue("" + w.getId()));
		map.put("timestamp", new StringValue(w.getTimestamp().toString()));
		map.put("uid", new NumberValue(w.getUser().getId()));
		map.put("user", new StringValue(w.getUser().getName()));
		map.put("changeset", new NumberValue(w.getChangesetId()));
		map.put("version", new NumberValue(w.getVersion()));
		MapValue tagsMapValue = new MapValue();
		Map<String, GenericValue> tagsMap = tagsMapValue.getMap();
		Collection<Tag> coll = w.getTags();
		for (Tag tag : coll) {
			tagsMap.put(tag.getKey(), new StringValue(tag.getValue()));
		}
		map.put("tags", tagsMapValue);

		ListValue lv = new ListValue();
		for (WayNode wn : w.getWayNodes()) {
			lv.getList().add(new NumberValue(wn.getNodeId()));
		}
		map.put("nd", lv);

		return mv;
	}

	/**
	 * Convert node to generic value.
	 * 
	 * @param n
	 *            the n
	 * @return the map value
	 */
	private MapValue convertNodeToGenericValue(Node n) {
		MapValue mv = new MapValue();
		Map<String, GenericValue> map = mv.getMap();
		map.put("type", new StringValue("Node"));

		map.put("nodeId", new StringValue("" + n.getId()));
		map.put("timestamp", new StringValue(n.getTimestamp().toString()));
		map.put("uid", new NumberValue(n.getUser().getId()));
		map.put("user", new StringValue(n.getUser().getName()));
		map.put("changeset", new NumberValue(n.getChangesetId()));
		map.put("latitude", new NumberValue(n.getLatitude()));
		map.put("longitude", new NumberValue(n.getLongitude()));

		MapValue tagsMapValue = new MapValue();
		Map<String, GenericValue> tagsMap = tagsMapValue.getMap();
		Collection<Tag> coll = n.getTags();
		for (Tag tag : coll) {
			tagsMap.put(tag.getKey(), new StringValue(tag.getValue()));
		}
		map.put("tags", tagsMapValue);

		return mv;
	}
}