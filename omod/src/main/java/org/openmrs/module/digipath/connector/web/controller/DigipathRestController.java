package org.openmrs.module.digipath.connector.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.digipath.connector.api.DigipathRestService;
import org.openmrs.module.digipath.connector.proforma.DigipathConnector;
import org.openmrs.module.digipath.connector.proforma.DpAlerts;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/rest/v1/digipath-connector")
public class DigipathRestController extends MainResourceController {
	
	@RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, String>> getDigipath() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, String> hm = new HashMap<String, String>();
		hm.put("date", String.valueOf(new Date()));
		hm.put("action", "Hello World");
		Map<String, String> hm2 = new HashMap<String, String>();
		hm2.put("date", String.valueOf(new Date()));
		hm2.put("action", "Second action");
		list.add(hm);
		list.add(hm2);
		return list;
		//		return "HELLO WORLD";
	}
	
	@RequestMapping(value = "/get-recommendations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, Object>> getDigipathRecommendations(@RequestParam String patientUuid) {


		System.out.println("HEREEEEEE GET RECOMMENDATIONS =>>>>>>>>>>>>>>>>>");

		User authenticatedUser = Context.getAuthenticatedUser();
		DigipathRestService digipathRestService = Context.getService(DigipathRestService.class);
		ExecutorService executor = Executors.newFixedThreadPool(5);

		UserContext authContext = Context.getUserContext();

		try {

			List<DigipathConnector> digipathConnectorList = digipathRestService.getAllDigipathConnectorData();
			List<CompletableFuture<List<Map<String,Object>>>> futures = digipathConnectorList.stream()
					.map(digipathConnector -> CompletableFuture.supplyAsync(() -> {

						try {
							Context.openSession();
							Context.setUserContext(authContext);
							String response = digipathConnector.getProtocol();
							return executeProtocol(patientUuid, response);
						} finally {
							Context.closeSession();
						}


					}, executor))
					.collect(Collectors.toList());

			List<Map<String,Object>> finalList = futures.stream()
					.map(CompletableFuture::join)
					.flatMap(Collection::stream)
					.collect(Collectors.toList());




			return finalList;

			} finally {
				executor.shutdown();
			}

		
//		String json = fetchDataFromExternalApi();
//		ObjectMapper objectMapper = new ObjectMapper();
//
//		try {
//			DpAlerts dpAlerts = objectMapper.readValue(json, DpAlerts.class);
//			JsonNode root = objectMapper.readTree(json);
//			JsonNode dataNode = root.get("data");
//			String flattenedJson = objectMapper.writeValueAsString(dataNode);
//
//			if (dpAlerts == null || dpAlerts.getData() == null)
//				throw new IllegalArgumentException();
//
//
//			return digipathRestService.performProforma(dpAlerts.getData(), flattenedJson, patientUuid);
//		}
//		catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		
	}
	
	private List<Map<String, Object>> executeProtocol(String patientUuid, String json) {
		DigipathRestService digipathRestService = Context.getService(DigipathRestService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			DpAlerts dpAlerts = objectMapper.readValue(json, DpAlerts.class);
			JsonNode root = objectMapper.readTree(json);
			JsonNode dataNode = root.get("data");
			String flattenedJson = objectMapper.writeValueAsString(dataNode);
			
			if (dpAlerts == null || dpAlerts.getData() == null)
				throw new IllegalArgumentException();
			return digipathRestService.performProforma(dpAlerts.getData(), flattenedJson, patientUuid);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@RequestMapping(value = "/get-fhir-data", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getFhirFormattedData(@RequestParam String patientUuid, @RequestParam String url) {
		
		String json = fetchDataFromExternalApi(url);
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			DpAlerts dpAlerts = objectMapper.readValue(json, DpAlerts.class);
			JsonNode root = objectMapper.readTree(json);
			JsonNode dataNode = root.get("data");
			String flattenedJson = objectMapper.writeValueAsString(dataNode);
			
			if (dpAlerts == null || dpAlerts.getData() == null)
				throw new IllegalArgumentException();
			
			DigipathRestService digipathRestService = Context.getService(DigipathRestService.class);
			return digipathRestService.getFhirFormattedData(dpAlerts.getData(), flattenedJson, patientUuid);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@RequestMapping(value = "/fetch-data", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String fetchDataFromExternalApi(String url) {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		String json = response.getBody();
		
		System.out.println("json fetched " + json);
		
		return json;
	}
	
	@RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DigipathConnector saveDigipathConnectorData(@RequestBody DigipathConnector digipathConnector) {
		
		DigipathRestService digipathRestService = Context.getService(DigipathRestService.class);
		
		return digipathRestService.saveDigipathConnectorData(digipathConnector);
	}
	
}
