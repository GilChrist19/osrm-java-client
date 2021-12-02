/**
 * Imports
 */
package testOSRM;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;





import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;



/**
 * Class for managing and contacting an OSRM instance. 
 * 
 * @author Jared Bennett (Mobius Logic, jbennett@mobiuslogic.com)
 *
 */
public class OSRM {
	/**************
	 * Useful Links
	 **************/
	// https://github.com/Project-OSRM
	// http://project-osrm.org/docs/v5.24.0/api/?language=cURL#general-options
	// https://stackoverflow.com/questions/44760360/how-to-use-osrm-properly
	// https://github.com/ATran31/osrm-java-client/blob/master/src/com/osrm/services/Nearest.java
	// https://www.baeldung.com/java-9-http-client
	// https://stackoverflow.com/questions/8997598/importing-json-into-an-eclipse-project/8997703#8997703
	
	/**************
	 * Class Constants
	 **************/
	private static int COUNTID = 0;
	private static final int CLASSHASH = OSRM.class.hashCode();
	
	private static final List<String> services = List.of("route","nearest","table","match","trip"); // not implementing "tile"
	private static final List<String> profiles = List.of("car","bike","foot");
	private static final List<String> excludes = List.of("toll","motorway","ferry"); // only things supported right now
	private static final String format = ".json"; // not trying .flatbuffers, only json
	//  flatbuffers are supposed to be faster, so maybe look at later?
	
	
	/**************
	 * Object Constants
	 **************/
	private int ID;
	private String osrm = "http://router.project-osrm.org/"; // default router
	
	
	
	/**********************************
	 * Constructors
	 *********************************/
	OSRM(){
		// set id
		this.ID = OSRM.COUNTID;
		++OSRM.COUNTID;
	}; // default
	
	OSRM(String osrmClient){
		// constructor chain
		this();
		
		// set osrm url
		this.osrm = osrmClient;
	}
	
	
	/**************************************************************************
	 * Methods
	 *************************************************************************/
	/***********************************
	 * Override Methods
	 **********************************/
	@Override
	public int hashCode() {
		// https://www.baeldung.com/java-objects-hash-vs-objects-hashcode
		//  this is pretty slow
//		return(Objects.hash(this.ID, this.getClass()));
		
		// https://www.baeldung.com/java-objects-hash-vs-objects-hashcode
		// https://stackoverflow.com/questions/113511/best-implementation-for-hashcode-method-for-a-collection
		// https://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
		// https://javarevisited.blogspot.com/2011/10/override-hashcode-in-java-example.html
		// https://vanilla-java.github.io/2018/08/12/Why-do-I-think-Stringhash-Code-is-poor.html
		// use 109 and 1 as primes
		// int result = 1;
		// result = result * 109 + this.ID;
		int result = 109 + this.ID;
		result = result * 109 + OSRM.CLASSHASH;
		return(result);
		
	}
	
	
	@Override
	public boolean equals(Object obj) {

		// if both the object references are 
		// referring to the same object.
		if(this == obj) 
			return true;

		// it checks if the argument is of the 
		// type by comparing the classes 
		// of the passed argument and this object.
		// if(!(obj instanceof Geek)) return false; ---> avoid.
		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;

		// type casting of the argument. 
		OSRM other = (OSRM) obj;

		// comparing the state of argument with 
		// the state of 'this' Object.
		return(this.ID == other.ID);
	}
	
	
	/***********************************
	 * Main Method
	 **********************************/
	/**
	 * Base Query Function
	 * 
	 * This is the only function that contacts OSRM - every other function passes 
	 * down to this one eventually. It synthesizes several parameters available from 
	 * OSRM, but not all. Options not implemented include:
	 * 	* bearings = uses default
	 * 	* radiuses = uses default
	 * 	* generate_hints = false
	 * 	* hints = uses default
	 * 	* approaches = uses default
	 * Additionally, the _exclude_ option is being ignored
	 * 
	 * @see http://project-osrm.org/docs/v5.24.0/api/?language=cURL#route-service
	 * @param String service - one of: "route","nearest","table","match","trip" (tile is not implemented)
	 * @param String profile - one of: "car","bike","foot"
	 * @param ArrayList<Double> lon - list of longitudes
	 * @param ArrayList<Double> lat - corresponding list of latitudes
	 * @param String snapping - one of: "default","any"
	 * @param boolean skipWayPt - true or false
	 * @params String serviceOptions - String of options specific to each service
	 * @return JSONObject with format specific to each service
	 */
	public JSONObject queryOSRM(String service, String profile,
								ArrayList<Double> lon, ArrayList<Double> lat,
//								ArrayList<String> exclude,
								String snapping,
								boolean skipWayPt,
								String serviceOptions) {
		
		// init url, with base osrm instance
		StringBuilder url = new StringBuilder(this.osrm);
		
		// add service and version
		//  version is always v1
		url.append(service + "/v1/");
		
		// profile specification
		url.append(profile + "/");
		
		// format points
		this.formatPoints(lon, lat, url);
		
		// pedantic, add format, setup for options
		url.append(OSRM.format + "?");
		
		// generate hints
		//  default this to no
		url.append("generate_hints=false&");
		
		// excludes
//		this.formatOptions("exclude", exclude, url);
		
		// snapping
		url.append("snapping=" + snapping + "&");
		
		// waypoints
		url.append("skip_waypoints=" + skipWayPt + "&");
		
		// append service specific options
		//  this is the last thing, so gotta make sure the string is properly formatted
		if((serviceOptions == null) || (serviceOptions.isEmpty())) {
			// remove trailing ampersand (&)
			url.deleteCharAt(url.length()-1);
		} else {
			// add options
			url.append(serviceOptions);
		}
		
		
		
		// return object
		JSONObject result = null;
		
		// call and do!
		try {
			// build request
			HttpRequest request = HttpRequest.newBuilder()
					  .uri(new URI(url.toString()))
					  .version(HttpClient.Version.HTTP_2)
					  .header("accept", "application/json")
					  .GET()
					  .build();
			
			// send/receive request
			HttpResponse<String> response = HttpClient.newBuilder()
					  .build()
					  .send(request, BodyHandlers.ofString());
			
			
			System.out.println("Full return");
			System.out.println(response);
			System.out.println();
			System.out.println("Body");
			System.out.println(response.body());
			
			
			
			
			// pull out body of response into JSON object
			result = new JSONObject(response.body());

		} catch (URISyntaxException | IOException | InterruptedException | JSONException e) {
			System.out.println("OSRM::queryOSRM() - fail.");
			e.printStackTrace();
		}
		
	
		// return JSON
		return(result);
		
	} // end base query
	
	/***********************************
	 * Derivative Methods
	 **********************************/
	/**************
	 * Routing
	 **************/
	
	public JSONObject getRoute(ArrayList<Double> lon, ArrayList<Double> lat,
			boolean skipWayPt, int alternatives, boolean steps, String annotations,
			String overview, String profile, String snapping) {
		// general route options I'm not using
		//  geometries
		//  continue_straight
		//  waypoints
		
		// setup service-specific options
		StringBuilder opts = new StringBuilder();
		
		// alternatives
		opts.append("alternatives=" + alternatives + "&");
		
		// steps
		opts.append("steps=" + steps + "&");
		
		// annotations
		opts.append("annotations=" + annotations + "&");
		
		// geometries
		//  we're using jsons only
		opts.append("geometries=geojson&");
		
		// overview
		//  last one, NO AMPERSAND!!
		opts.append("overview=" + overview);
		
		
		// pass down and return
		return(this.queryOSRM("route", profile,
				lon, lat,
				snapping,
				skipWayPt,
				opts.toString())
				);

	} // end base routing
	
	
	
	/*
	 * Gets minimalist route info
	 */
	public JSONObject getRoute(ArrayList<Double> lon, ArrayList<Double> lat) {
		// pass down and return
		return(this.getRoute(lon, lat, true,
							 0, false, "false",
							 "simplified","driving","default")
				);
		
	} // end minimal routing
	
	
	/**************
	 * Table
	 **************/
	public JSONObject getTable(ArrayList<Double> lon, ArrayList<Double> lat,
			ArrayList<Integer> sources, ArrayList<Integer> destinations, 
			String annotations,
			boolean skipWayPt,
			String profile,
			String snapping) {
		// general table options I'm not using
		//  fallback_speed
		//  fallback_coordinate
		//  scale_factor
		
		// setup service-specific options
		StringBuilder opts = new StringBuilder();
		
		// sources
		if(sources.size() == 0) {
			opts.append("sources=all&");
		} else {
			this.formatOptions("sources", sources, opts);
		}
		
		// destinations
		if(destinations.size() == 0) {
			opts.append("destinations=all&");
		} else {
			this.formatOptions("destinations", destinations, opts);
		}
		
		// annotations
	//  last one, NO AMPERSAND!!
		opts.append("annotations=" + annotations);
		
		
		// pass down and return
		return(this.queryOSRM("table", profile, lon, lat, snapping, 
							  skipWayPt, opts.toString())
			   );
	}
	
	
	public JSONObject getTableToPoint(ArrayList<Double> lon, ArrayList<Double> lat) {
		// pass down and return
		return(this.getTable(lon, lat, new ArrayList<Integer>(),
							new ArrayList<Integer>(Arrays.asList(0)),
							"duration", true, "driving", "default")
				);
	}
	
	
	
	
	
	
	
	/***********************************
	 * Aux Methods
	 **********************************/
	/**
	 * Formatter for array options provided to OSRM
	 * @author Jared Bennett (Mobius Logic, jbennett@mobiuslogic.com)
	 * @see http://project-osrm.org/docs/v5.24.0/api/?language=cURL#route-service
	 * @param String option - name of the option
	 * @param ArrayList<?> values - list of values to be combined for option
	 * @param StringBuilder url - url object to append option to
	 * @return void - it appends directly to the StringBuilder object provided
	 */
	private void formatOptions(String option, ArrayList<?> values, StringBuilder url) {
		
		// add option type
		url.append(option + "=");
		
		// add options
		for(int i=0; i<values.size(); ++i) {
			url.append(values.get(i) + ";");
		}
		
		// remove trailing semicolon (;)
		url.deleteCharAt(url.length()-1);
		
		// append trailing ampersand
		url.append("&");
	}
	
	
	/**
	 * Formatter for gps coordinates to provide to OSRM
	 * @author Jared Bennett (Mobius Logic, jbennett@mobiuslogic.com)
	 * @see http://project-osrm.org/docs/v5.24.0/api/?language=cURL#route-service
	 * @param ArrayList<Double> lon - list of longitudes
	 * @param ArrayList<Double> lat - corresponding list of latitudes
	 * @param StringBuilder url - url object to append option to
	 * @return void - it appends directly to the StringBuilder object provided
	 */
	private void formatPoints(ArrayList<Double> lon, ArrayList<Double> lat, StringBuilder url) {
		
		// make sure inputs are same length
		if(lon.size() != lat.size()) {
			System.out.println("OSRM::formatPoints() - lat/lon are different lengths.");
			System.exit(1);
		}
		
		// loop over lon/lats
		for(int i=0; i<lon.size();++i) {
			url.append(lon.get(i) + "," + lat.get(i) + ";");
		}
		
		// remove trailing semicolon (;)
		url.deleteCharAt(url.length()-1);
		
	} // end formatPoints
	
	
} // end OSRM class
