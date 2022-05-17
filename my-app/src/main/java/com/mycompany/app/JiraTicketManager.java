package com.mycompany.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.time.LocalDate;
import java.time.ZoneId;


public class JiraTicketManager {
	
	// ------------------------------ Attributes -----------------------------

	public final String USER_DIR = "user.dir";
	public final String RELEASE_DATE = "releaseDate";
	public final String FILE_EXTENSION = ".java";
	private String projectName = "";

	// ------------------------------ Builders --------------------------------

	public JiraTicketManager( String projectName ){
		this.projectName = projectName;
	}

	// ------------------------------ Setters ---------------------------------

	public void setProjectName( String projectName ){
		this.projectName = projectName;
	}

	// ------------------------------ Getters ---------------------------------

	public String getProjectName(){
		return this.projectName;
	}

	// ------------------------------ Methods ---------------------------------


	public String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
  
   
	public JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONArray json = new JSONArray(jsonText);
			return json;
		} finally {
			is.close();
		}
	}


	public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}


	public  void retrieveTicketsID( IssueLifeCycleManager controller ) throws IOException, JSONException {
		
		Integer j = 0, i = 0, total = 1;
		//Get JSON API for closed bugs w/ AV in the project
		do {
			//Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
			j = i + 1000;
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
				+ this.projectName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
				+ "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
				+ i.toString() + "&maxResults=" + j.toString();
			JSONObject json = readJsonFromUrl(url);
			JSONArray tickets = json.getJSONArray("issues");
			total = json.getInt("total");
			for (; i < total && i < j; i++) {
				//Iterate through each bug
				String ticketID = tickets.getJSONObject(i%1000).get("key").toString().split("-")[1];
				IssueObject issue = new IssueObject( ticketID );
				controller.appendIssue( issue );
			}  
		} while (i < total);
		return;
	}


   	/*	This Method performs a rest API to Jira querying for all versions with related dates for the
	   	specified software project. */ 
	public void getVersionsWithReleaseDate( IssueLifeCycleManager controller )
			throws IOException, JSONException {

		String releaseName = null;
		Integer i;

		// Url for the GET request to get information associated to Jira project
		String url = "https://issues.apache.org/jira/rest/api/2/project/" + this.projectName;

		JSONObject json = readJsonFromUrl( url );

		// Get the JSONArray associated to project version
		JSONArray versions = json.getJSONArray( "versions" );	

		// For each version...
		for ( i = 0; i < versions.length(); i++ ) {
			// ... check if verion has release date and name, and add it to relative list
			if ( versions.getJSONObject(i).has( RELEASE_DATE ) && versions.getJSONObject(i).has("name") ) {
				releaseName = versions.getJSONObject(i).get("name").toString();
				controller.appendVersionMapEntry( LocalDate.parse( versions.getJSONObject(i).get( RELEASE_DATE ).toString() ), releaseName );
			}
		}

		// Give an index to each release in the list
		int releaseNumber = 1;
		for ( LocalDate k : controller.getVersionMapKeySet() ) {
			controller.appendVersionMapEntry( k, String.valueOf( releaseNumber ) );
			releaseNumber++;
		}

	}

	/*	This Method performs a Rest API to Jira querying for all tickets of type BUG which have been 
		closed/resolved already. By this call it is possible to get the following informations:
		 - ticket creation date -> related to OV
		 - ticket resolution date -> related to FV
		 - ticket Identifier -> related to Git commits (if any) 
		 - Affected Versions names and dates (if specified)		*/
	public void getTickets( IssueLifeCycleManager controller ) throws IOException, JSONException {

		Integer j = 0;
		Integer i = 0;
		Integer total = 1;

		// Get JSON API for closed bugs w/ AV in the project
		do {
			// Only gets a max of 1000 at a time, so must do this multiple times if bugs > 1000
			j = i + 1000;
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + this.projectName
					+ "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
					+ "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,versions,resolutiondate,created,fixVersions&startAt="
					+ i.toString() + "&maxResults=1000";
			
         	JSONObject json = readJsonFromUrl(url);
			JSONArray issues = json.getJSONArray("issues");
			total = json.getInt("total");

			// For each closed ticket...
			for (; i < total && i < j; i++) {

            // ... get the key of the ticket,
			String key = ( issues.getJSONObject(i % 1000).get("key").toString() ).split("-")[1];

			JSONObject currentIssue = (JSONObject) issues.getJSONObject(i % 1000).get("fields");

            String resolutionDate = currentIssue.getString("resolutiondate").toString().split("T")[0];

			String creationDate = currentIssue.getString("created").toString().split("T")[0];

			// , get JSONArray associated to the affected versions.
			JSONArray AVarray = currentIssue.getJSONArray("versions");
				
			// Get a Java List from the JSONArray with only dated affected versions.
			ArrayList<String> AVlist = this.getJsonAffectedVersionList( AVarray );

            IssueObject issue = new IssueObject( key, resolutionDate, creationDate, AVlist );

            controller.appendIssue( issue );
			}

		} while (i < total);

	}


	/*	This Method takes the JSON Array containing all affected versions specified for the ticket
		and returns a String ArrayList containing only those having an associated release date.	*/ 
   	public ArrayList<String> getJsonAffectedVersionList( JSONArray AVarray ) throws JSONException {

		ArrayList<String> affectedVersions = new ArrayList<>();

		if ( AVarray.length() > 0 ) {

			// For each release in the AV version...
			for (int k = 0; k < AVarray.length(); k++) {

				JSONObject singleRelease = AVarray.getJSONObject(k);

				// ... check if the single release has been released
				if ( singleRelease.has( RELEASE_DATE ) ) {
					affectedVersions.add(singleRelease.getString("name"));
				}
			}
		}

		return affectedVersions;
	}

 
}
