package org.jvalue.ods.filter;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.name.Named;

import org.jvalue.ods.data.DataSource;
import org.jvalue.ods.db.SourceDataRepository;

public interface FilterFactory {

	static final String
			NAME_DB_INSERTION_FILTER = "DbInsertionFilter",
			NAME_NOTIFICATION_FILTER = "NotificationFilter";

	public @Named(NAME_NOTIFICATION_FILTER) Filter<ArrayNode, ArrayNode> createNotificationFilter(DataSource source);
	public @Named(NAME_DB_INSERTION_FILTER) Filter<ArrayNode, ArrayNode> createDbInsertionFilter(
			DataSource source,
			SourceDataRepository dataRepository);

}
