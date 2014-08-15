package org.openmrs.module.chartsearch;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.chartsearch.solr.SolrUtils;

public class EmbeddedSolrServerTest {
	
	private static final int SUCCESS = 0;
	
	private final String solrHome = SolrUtils.getEmbeddedSolrProperties().getSolrHome();
	
	private EmbeddedSolrServer server;
	
	@Before
	public void setUp() throws Exception {
		System.out.println(solrHome);
		System.setProperty("solr.solr.home", solrHome);
		CoreContainer coreContainer = new CoreContainer.Initializer().initialize();
		server = new EmbeddedSolrServer(coreContainer, "collection1");
	}
	
	@After
	public void tearDown() throws Exception {
		server.shutdown();
		//removeIndexDirectory();
	}
	
	@Test
	public void testSolrSchema() throws Exception {
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("test_id", "123");
		doc1.addField("test_desc", "some text");
		UpdateResponse ur = server.add(doc1);
		Assert.assertEquals(ur.getStatus(), SUCCESS);
		server.commit();
		
		QueryResponse response1 = server.query(new SolrQuery("*:*"));
		Assert.assertEquals(response1.getResults().getNumFound(), 1L);
		
		QueryResponse response2 = server.query(new SolrQuery("test_desc:*text"));
		Assert.assertEquals(response2.getResults().getNumFound(), 1L);
	}
	
	@SuppressWarnings("unused")
    private void removeIndexDirectory() throws IOException {
		File indexDir = new File(solrHome, "data/index");
		FileUtils.deleteDirectory(indexDir);
	}
}
