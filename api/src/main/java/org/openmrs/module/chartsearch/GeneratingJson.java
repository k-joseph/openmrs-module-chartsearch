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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.chartsearch.api.ChartSearchService;
import org.openmrs.module.chartsearch.solr.ChartSearchSearcher;

/**
 * Responsible for generating the JSON object to be returned to the view(s)
 */
public class GeneratingJson {
	
	final String DATEFORMAT = "dd/MM/yyyy HH:mm:ss";
	
	private static ChartSearchService chartSearchService;
	
	public static ChartSearchService getChartSearchService() {
		try {
			chartSearchService = Context.getService(ChartSearchService.class);
		}
		catch (APIAuthenticationException e) {
			System.out.println("Not Authenticated!!!");
		}
		return chartSearchService;
	}
	
	public static String generateJson() {
		
		JSONObject jsonToReturn = new JSONObject(); //returning this object
		List returnedResults = SearchAPI.getInstance().getResults();
		boolean foundNoResults = false;
		JSONObject noResults = new JSONObject();
		String searchPhrase = SearchAPI.getInstance().getSearchPhrase().getPhrase();
		
		//if (!searchPhrase.equals("") && searchPhrase != null) {
		if (returnedResults == null || returnedResults.isEmpty()) {
			foundNoResults = true;
			noResults.put("foundNoResults", foundNoResults);
			noResults.put("foundNoResultsMessage",
			    Context.getMessageSourceService().getMessage("chartsearch.results.foundNoResults"));
		} else {
			JSONArray arr_of_groups = new JSONArray();
			
			JSONArray arr_of_locations = new JSONArray();
			JSONArray arr_of_providers = new JSONArray();
			JSONArray arr_of_datatypes = new JSONArray();
			JSONObject failedPrivilegeMessages = new JSONObject();
			
			noResults.put("foundNoResults", foundNoResults);
			try {
				getChartSearchService().addLocationsToJSONToReturn(jsonToReturn, arr_of_locations);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noLocations"));
			}
			
			try {
				getChartSearchService().addProvidersToJSONToReturn(jsonToReturn, arr_of_providers);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noProviders"));
			}
			
			try {
				getChartSearchService().addDatatypesToJSONToReturn(jsonToReturn, arr_of_datatypes);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noDatatypes"));
			}
			
			try {
				getChartSearchService().addObsGroupsToJSONToReturn(jsonToReturn, arr_of_groups);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noObsGroups"));
			}
			
			JSONObject jsonObs = null;
			JSONArray arr_of_obs = new JSONArray();
			try {
				getChartSearchService().addSingleObsToJSONToReturn(jsonToReturn, jsonObs, arr_of_obs);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noSingleObs"));
			}
			
			JSONObject jsonForms = null;
			JSONArray arr_of_forms = new JSONArray();
			try {
				getChartSearchService().addFormsToJSONToReturn(jsonToReturn, jsonForms, arr_of_forms);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noForms"));
			}
			
			JSONObject jsonEncounters = null;
			JSONArray arr_of_encounters = new JSONArray();
			try {
				getChartSearchService().addEncountersToJSONToReturn(jsonToReturn, jsonEncounters, arr_of_encounters);
			}
			catch (APIAuthenticationException e) {
				failedPrivilegeMessages.put("message",
				    Context.getMessageSourceService().getMessage("chartsearch.privileges.failedPrivileges.noEncounters"));
			}
			
			jsonToReturn.put("search_phrase", searchPhrase);
			
			addFacetsToJsonToReturn(jsonToReturn);
			
			//add failed privileges to json to be returned to the view
			jsonToReturn.put("failedPrivileges", failedPrivilegeMessages);
		}
		jsonToReturn.put("noResults", noResults);
		/*} else {
			jsonToReturn.put("noSearch", "");
		}*/
		
		return jsonToReturn.toString();
	}
	
	/**
	 * Adds Facets to be used to drill down/filter results, A Facet has a name and counts returned
	 * in the results
	 * 
	 * @should
	 * @param jsonToReturn
	 */
	private static void addFacetsToJsonToReturn(JSONObject jsonToReturn) {
		//adding facets to the JSON results object
		JSONArray arr_of_facets = new JSONArray();
		JSONObject facet = new JSONObject();
		LinkedList<Count> facets = new LinkedList<Count>();
		
		facets.addAll(ChartSearchSearcher.getFacetFieldValueNamesAndCounts());
		for (int i = facets.indexOf(facets.getFirst()); i <= facets.indexOf(facets.getLast()); i++) {
			facet.put("facet", generateFacetsJson(facets.get(i)));
			arr_of_facets.add(facet);
		}
		jsonToReturn.put("facets", arr_of_facets);
	}
	
	public static JSONObject createJsonObservation(Obs obs) {
		JSONObject jsonObs = new JSONObject();
		jsonObs.put("observation_id", obs.getObsId());
		jsonObs.put("concept_name", obs.getConcept().getDisplayString());
		
		Date obsDate = obs.getObsDatetime() == null ? new Date() : obs.getObsDatetime();
		
		SimpleDateFormat formatDateJava = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		/*String dateStr = formatDateJava.format(obsDate);*/
		String dateStr = obsDate.getTime() + "";
		
		jsonObs.put("date", dateStr);
		
		if (obs.getConcept().getDatatype().isNumeric()) { // ADD MORE DATATYPES
		
			ConceptNumeric conceptNumeric = Context.getConceptService().getConceptNumeric(obs.getConcept().getId());
			jsonObs.put("units_of_measurement", conceptNumeric.getUnits());
			jsonObs.put("absolute_high", conceptNumeric.getHiAbsolute());
			jsonObs.put("absolute_low", conceptNumeric.getLowAbsolute());
			jsonObs.put("critical_high", conceptNumeric.getHiCritical());
			jsonObs.put("critical_low", conceptNumeric.getLowCritical());
			jsonObs.put("normal_high", conceptNumeric.getHiNormal());
			jsonObs.put("normal_low", conceptNumeric.getLowNormal());
		}
		jsonObs.put("value_type", obs.getConcept().getDatatype().getName());
		
		jsonObs.put("value", obs.getValueAsString(Context.getLocale()));
		jsonObs.put("location", obs.getLocation().getDisplayString());
		jsonObs.put("creator", obs.getCreator().getDisplayString());
		Set<EncounterProvider> encounterProviders = obs.getEncounter().getEncounterProviders();
		
		if (encounterProviders != null && encounterProviders.iterator().hasNext()) {
			EncounterProvider provider = encounterProviders.iterator().next();
			if (provider.getProvider() != null) {
				jsonObs.put("provider", provider.getProvider().getName());
			}
		}
		
		SearchAPI searchAPI = SearchAPI.getInstance();
		if (!searchAPI.getSearchPhrase().getPhrase().equals("") && !searchAPI.getSearchPhrase().getPhrase().equals("*")) {
			for (ChartListItem item : searchAPI.getResults()) {
				if (item != null && item instanceof ObsItem && ((ObsItem) item).getObsId() != null) {
					if (((ObsItem) item).getObsId() == obs.getObsId()) {
						jsonObs.put("chosen", "true");
					}
				}
			}
		}
		
		return jsonObs;
	}
	
	public static JSONObject createJsonForm(Form form) {
		JSONObject jsonForm = new JSONObject();
		jsonForm.put("form_id", form.getFormId());
		
		Date formDate = form.getDateCreated() == null ? new Date() : form.getDateCreated();
		
		SimpleDateFormat formatDateJava = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		/*String dateStr = formatDateJava.format(formDate);*/
		String dateStr = formDate.getTime() + "";
		jsonForm.put("date", dateStr);
		jsonForm.put("encounter_type", form.getEncounterType().getName());
		jsonForm.put("creator", form.getCreator().getName());
		
		formDate = form.getDateChanged() == null ? new Date() : form.getDateChanged();
		dateStr = formatDateJava.format(formDate);
		jsonForm.put("last_changed_date", dateStr);
		
		/* for(FormField formField : form.getOrderedFormFields()){
		     jsonForm.put(formField.getName(),formField.getDescription());

		 }*/
		
		return jsonForm;
	}
	
	public static JSONObject createJsonEncounter(Encounter encounter) {
		JSONObject jsonEncounter = new JSONObject();
		jsonEncounter.put("encounter_id", encounter.getEncounterId());
		
		return jsonEncounter;
	}
	
	public static Set<Set<Obs>> generateObsGroupFromSearchResults() {
		Set<Set<Obs>> obsGroups = new HashSet<Set<Obs>>();
		
		SearchAPI searchAPI = SearchAPI.getInstance();
		List<ChartListItem> searchResultsList = searchAPI.getResults();
		for (ChartListItem item : searchResultsList) { //for each item in results we classify it by its obsGroup, and add all of the group.
			if (item != null && item instanceof ObsItem && ((ObsItem) item).getObsId() != null) {
				int itemObsId = ((ObsItem) item).getObsId();
				Obs obsGrp = Context.getObsService().getObs(itemObsId).getObsGroup();
				if (obsGrp != null) {
					int groupId = obsGrp.getId();
					Set<Obs> obsGroup = obsGrp.getGroupMembers();
					boolean found = false; //if found == ture then we don't need to add the group.
					for (Set<Obs> grp : obsGroups) {
						Obs ob = new Obs(-1);
						if (grp.iterator().hasNext()) {
							ob = grp.iterator().next();
						}
						if (ob.getObsGroup() != null && ob.getObsGroup().getId() != null) {
							if (ob.getObsGroup().getId() == groupId) {
								found = true;
							}
						}
					}
					if (!found) {
						obsGroups.add(obsGroup);
					}
				}
			}
		}
		return obsGroups;
	}
	
	public static Set<Obs> generateObsSinglesFromSearchResults() {
		SearchAPI searchAPI = SearchAPI.getInstance();
		Set<Obs> obsSingles = new HashSet<Obs>();
		for (ChartListItem item : searchAPI.getResults()) {
			if (item != null && item instanceof ObsItem && ((ObsItem) item).getObsId() != null) {
				int itemObsId = ((ObsItem) item).getObsId();
				
				Obs obs = Context.getObsService().getObs(itemObsId);
				if (obs != null && obs.getObsGroup() == null && !obs.isObsGrouping()) {
					obsSingles.add(Context.getObsService().getObs(itemObsId));
				}
			}
		}
		return obsSingles;
	}
	
	public static JSONObject generateLocationJson(String location) {
		JSONObject jsonLocation = new JSONObject();
		jsonLocation.put("location", location);
		return jsonLocation;
	}
	
	public static JSONObject generateProviderJson(String provider) {
		JSONObject jsonProvider = new JSONObject();
		jsonProvider.put("provider", provider);
		return jsonProvider;
	}
	
	public static JSONObject generateDatatypeJson(String datatype) {
		JSONObject jsonDatatype = new JSONObject();
		jsonDatatype.put("datatype", datatype);
		return jsonDatatype;
	}
	
	public static Set<String> generateLocationsFromResults() {
		Set<String> res = new HashSet<String>();
		SearchAPI searchAPI = SearchAPI.getInstance();
		
		for (ChartListItem item : searchAPI.getResults()) {
			if (item != null && item instanceof ObsItem && ((ObsItem) item).getObsId() != null) {
				int itemObsId = ((ObsItem) item).getObsId();
				
				Obs obs = Context.getObsService().getObs(itemObsId);
				if (obs != null) {
					res.add(obs.getLocation().getDisplayString());
				}
			}
		}
		return res;
	}
	
	public static Set<String> generateProvidersFromResults() {
		Set<String> res = new HashSet<String>();
		SearchAPI searchAPI = SearchAPI.getInstance();
		
		for (ChartListItem item : searchAPI.getResults()) {
			if (item != null && item instanceof ObsItem && ((ObsItem) item).getObsId() != null) {
				int itemObsId = ((ObsItem) item).getObsId();
				
				Obs obs = Context.getObsService().getObs(itemObsId);
				if (obs != null) {
					Set<EncounterProvider> encounterProviders = obs.getEncounter().getEncounterProviders();
					
					if (encounterProviders != null && encounterProviders.iterator().hasNext()) {
						EncounterProvider provider = encounterProviders.iterator().next();
						if (provider.getProvider() != null) {
							res.add(provider.getProvider().getName());
						}
					}
				}
			}
		}
		return res;
	}
	
	public static Set<String> generateDatatypesFromResults() {
		Set<String> res = new HashSet<String>();
		SearchAPI searchAPI = SearchAPI.getInstance();
		
		for (ChartListItem item : searchAPI.getResults()) {
			if (item != null && item instanceof ObsItem && ((ObsItem) item).getObsId() != null) {
				int itemObsId = ((ObsItem) item).getObsId();
				
				Obs obs = Context.getObsService().getObs(itemObsId);
				if (obs != null) {
					res.add(obs.getConcept().getDatatype().getName());
				}
			}
		}
		return res;
	}
	
	public static Set<Form> generateFormsFromSearchResults() {
		SearchAPI searchAPI = SearchAPI.getInstance();
		Set<Form> forms = new HashSet<Form>();
		List<ChartListItem> searchResultsList = searchAPI.getResults();
		
		for (ChartListItem item : searchResultsList) {
			
			if (item != null && item instanceof FormItem) {
				
				if (((FormItem) item).getFormId() != null) {
					int itemFormId = ((FormItem) item).getFormId();
					Form form = Context.getFormService().getForm(itemFormId);
					if (form != null) {
						forms.add(Context.getFormService().getForm(itemFormId));
					}
				}
			}
		}
		return forms;
	}
	
	public static Set<Encounter> generateEncountersFromSearchResults() {
		SearchAPI searchAPI = SearchAPI.getInstance();
		Set<Encounter> encounters = new HashSet<Encounter>();
		List<ChartListItem> searchResultsList = searchAPI.getResults();
		for (ChartListItem item : searchResultsList) {
			if (item != null && item instanceof EncounterItem && ((EncounterItem) item).getEncounterId() != null) {
				int itemEncounterId = ((EncounterItem) item).getEncounterId();
				
				Encounter encounter = Context.getEncounterService().getEncounter(itemEncounterId);
				if (encounter != null) {
					encounters.add(Context.getEncounterService().getEncounter(itemEncounterId));
				}
			}
		}
		return encounters;
	}
	
	/**
	 * @should generate names and counts for each Count
	 * @should return a valid FacetField.Count object
	 * @param facet
	 * @return counts
	 */
	private static JSONObject generateFacetsJson(Count facet) {
		JSONObject counts = new JSONObject();
		counts.put("name", facet.getName());
		counts.put("count", facet.getCount());
		return counts;
	}
}
