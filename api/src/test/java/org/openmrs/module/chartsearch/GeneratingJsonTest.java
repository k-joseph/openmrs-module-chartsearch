/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.chartsearch;

import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.chartsearch.solr.SolrSingleton;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

/**
 * Test for {@link GeneratingJson}
 */
public class GeneratingJsonTest extends BaseModuleContextSensitiveTest {
	
	public SolrServer getSolrServer() {
		return SolrSingleton.getInstance().getServer();
	}
	
	@Test
	public void solrServerShouldNotReturnNull() {
		Assert.assertNotNull(getSolrServer());
	}
	
	@Test
	@Verifies(value = "should generate names and counts for each Count", method = "generateFacetsJson(Count)")
	public void generateFacetsJson_generateNamesAndCountsForEachCount() {
		String count1 = "count1";
		String count2 = "count2";
		FacetField facetField = new FacetField(count1, count2, null);
		List<Count> count = facetField.getValues();
		for (int i = 0; i < count.size(); i++) {
			count.get(1).setName("name_" + i);
			System.out.println(count.get(1).getName());
			count.get(i).setCount(5 + i);
			System.out.println(count.get(1).getCount());
		}
	}
	
}
