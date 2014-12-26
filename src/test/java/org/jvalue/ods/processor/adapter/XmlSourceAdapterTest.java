package org.jvalue.ods.processor.adapter;


import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvalue.ods.data.DataSource;

import java.util.List;

import mockit.Mocked;
import mockit.integration.junit4.JMockit;


@RunWith(JMockit.class)
public final class XmlSourceAdapterTest extends AbstractSourceAdapterTest {

	@Mocked
	MetricRegistry registry;

	@Test
	public void testBasicAdapter(@Mocked DataSource source) throws Exception {
		String xmlContent = "<list><item><key>value1</key></item><item><key>value2</key></item></list>";
		AbstractSourceAdapter adapter = new XmlSourceAdapter(source, registry);
		List<ObjectNode> jsonResult = testAdapterWithAllProtocols(source, adapter, xmlContent);

		Assert.assertEquals(2, jsonResult.size());
		Assert.assertEquals("value1", jsonResult.get(0).get("key").asText());
		Assert.assertEquals("value2", jsonResult.get(1).get("key").asText());
	}

}
