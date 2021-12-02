/**
 * 
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

import org.json.*;

/**
 * @author gilchrist
 *
 */
public class mainRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("test print from mainRunner");
		
		mainRunner first = new mainRunner("Jared");
		first.printer("stupid");
		
		
		String url = "http://router.project-osrm.org/route/v1/driving/13.388860,52.517037;13.397634,52.529407;13.428555,52.523219?overview=false";
		

		
		
		
		OSRM testme = new OSRM();
		
		
//		13.388860,52.517037;
//		13.397634,52.529407;
//		13.428555,52.523219
		
		ArrayList<Double> lon = new ArrayList<Double>(Arrays.asList(13.388860,13.397634,13.397634,13.428555));
		ArrayList<Double> lat = new ArrayList<Double>(Arrays.asList(52.517037,52.529407,52.523219,52.523219));
		
		
		
		JSONObject newTest = testme.queryOSRM("route", "driving",
												lon, lat, 
												"default", false, "");
		
		
		System.out.println("Jared Query");
		System.out.println(newTest);
		System.out.println();
		
		
		
		
		
		
		/*
		 * Get route, figure out how to get gps coords from it
		 */
		// route object, comes with return code
		newTest = testme.getRoute(lon, lat);
		
		System.out.println();
		System.out.println("Jared route");
		System.out.println();
		System.out.println(newTest);
		
		// get route object only
		System.out.println();
		System.out.println("Jared test subroute");
		try {
			System.out.println(newTest.get("routes"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get the gps coords from the route object
		System.out.println("Jared test geometry");
		try {
			
			
			// getRoute() returns a JSONObject, with 2 keys - "code" and "routes"
			// routes is a JSONArray
			//  the function guarantees it is length 1, because I only ask for 1 path
			// The JSONArray has several objects in it
			//  "legs", "weight_name", "geometry", "weight", "distance", "duration"
			//  we want the "geometry" object
			// geometry is a JSONObject with 2 keys - "coordinates" and "type"
			//  we want the "coordinates" objet
			// coordinates is a JSONArray (points) of JSONArray (length 2, has lon/lat)
			//
			// so, "test" here is a JSONArray of every point in our _simplified_ path
			//  we then cast each point as an array of length 2, lon/lats
			
			
			JSONArray test = newTest.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
			
			for(int i=0; i<test.length(); ++i) {
				System.out.println(test.get(i));
				System.out.println("lon:" + test.getJSONArray(i).getDouble(0));
				System.out.println("lat:" + test.getJSONArray(i).getDouble(1));
			}
			
			
			
			
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println();
		
		/*
		 * get times to point
		 */
		// table gets the times to all points. I set this function so the first 
		//  point is used as the destination, and all other points are timed to 
		//  this one.
		
		// getTableToPoint() returns a JSONObject with 2 keys - "code" and "durations"
		//  we want "durations"
		// durations is a JSONArray (length number of points) of JSONArrays 
		//  (length 1, just the duration)
		newTest = testme.getTableToPoint(lon, lat);
		try {
			JSONArray durations = newTest.getJSONArray("durations");
			
			for(int i=0; i<durations.length();++i) {
				System.out.println(durations.getJSONArray(i).getDouble(0));
			}
			
			
			// example of how to get closest point to destination
			//  remember, first point is the destination
			
			// prime search with first actual point
			int minIdx = 1;
			double minVal = durations.getJSONArray(minIdx).getDouble(0);
			
			// loop over rest of points
			for(int i=2; i<durations.length(); ++i) {
				
				// check current time to point against our fastest time to point
				if(durations.getJSONArray(i).getDouble(0) < minVal) {
					// new time is less, update "fastest"
					minIdx = i;
					minVal = durations.getJSONArray(i).getDouble(0);
				} // end check
				
			} // end loop
			
			// print for funsies
			System.out.println("closest point to origin is: ");
			System.out.println("\t" + minIdx);
			System.out.println("\t" + minVal);
			
			
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
		System.out.println();
		System.out.println("Jared table");
		System.out.println();
		System.out.println(newTest);
		System.out.println();
		System.out.println();
		
	}
	
	
	/*
	 * Crap methods I was playing with
	 */
	
	
	String name;
	int id = 0;
	static int idCounter = 0;
	
	mainRunner(String name){
		
		this.name = name;
		this.id = mainRunner.idCounter;
		++mainRunner.idCounter;
		
	}
	
	
	private void printer(String test) {
		
		System.out.println("in test func");
		
		System.out.println(test);
	}

}
