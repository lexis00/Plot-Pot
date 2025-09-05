//Backend entry for API calls 
package com.plotnpot; 

import java.util.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException; 
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONObject;
import org.json.JSONArray;





public class AppServer {
    //setup http server on fixed port to accept request 
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception { //stable entry and port   
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);  //Creates server that listens to all IPs on port 
        
        /* Create a test endpoint at /hello
        server.createContext("/hello", exchange -> {  //telling server which info to display based on endpoint
            String response = "Hello Pot & Plot!";
            exchange.sendResponseHeaders(200, response.getBytes().length); // 200 = OK
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes()); // write the response
            os.close(); // close the stream
        }); */

       server.createContext("/location", exchange -> { 
            String query = exchange.getRequestURI().getQuery(); //grabs full then stores info from the url after the ?
            String response = ""; 

            if (query != null && !query.isEmpty()) {
                String[] parts = query.split("="); // split into key=value
                if (parts.length == 2 && parts[0].equals("input")) {
                    String value = parts[1]; // the actual input
                    String apiKey = "d8af05002e9727e5e22030b0f2939e50"; //openweather api
                    String fullUrl;

                    if (value.matches("\\d+")) {
                // All digits → treat as zip
                        fullUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + value + "&appid=" + apiKey + "&units=metric";
                    } else {
                // Otherwise → treat as city name
                        fullUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + value + "&appid=" + apiKey + "&units=metric";

                }

            //use HttpClient to send request to fullUrl and get weather data 
                    try { //send request to OpenWeather
                        HttpClient client = HttpClient.newHttpClient(); //Creating client to send and recieve 
                        HttpRequest request = HttpRequest.newBuilder() //prepping request to send to openweatherMap
                            .uri(URI.create(fullUrl))
                            .GET()
                            .build();
                        HttpResponse<String> apiResponse;
                            apiResponse = client.send(request, HttpResponse.BodyHandlers.ofString()); //sends the request and recieves the response as a string
                            //response = apiResponse.body(); //returns body of response

                        //Creating JSON objects to extract data from response 
                        JSONObject obj = new JSONObject(apiResponse.body()); 
                        double temp = obj.getJSONObject("main").getDouble("temp"); //getting temp
                        double tempMin = obj.getJSONObject("main").getDouble("temp_min"); //getting the lowest temperature and checking the frost prob
                        boolean frostRisk = tempMin <= 0;

                        // Convert to Fahrenheit
                        double tempF = (temp* 9/5) + 32;
                        tempF = Math.round(tempF);
                        double tempMinF = (tempMin * 9/5) + 32;

                        double precipitation = 0.0; 
                        if(obj.has("rain")) {
                            precipitation = obj.getJSONObject("rain").optDouble("1h", 0.0);

                    } else if(obj.has("snow")) {
                           precipitation = obj.getJSONObject("snow").optDouble("1h", 0.0);

                    }

                    //JSON sent to frontend 
                    JSONObject responseJson = new JSONObject(); 
                    responseJson.put("temperature in celsius", temp);
                    responseJson.put("temperature in fahrenheit", tempF);
                    responseJson.put("frostRisk", frostRisk); 
                    responseJson.put("precipitation", precipitation); 

                    response = responseJson.toString(); 

                    } catch (IOException | InterruptedException e) {
                        response = "Error fetching weather data: " + e.getMessage();
                        e.printStackTrace();
                    }

                } else {
                    response = "Invalid query format. Use ?input=London or ?input=10001";
                }
        } else {
            response = "No query provided";
    }
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
});



    server.createContext("/plants",exchange -> {
        String query = exchange.getRequestURI().getQuery(); //grabs full url then stores info from the url after the ?
        Map<String, String> params = parseQuery(query);

        String frostRisk = params.getOrDefault("frostRisk", "false"); //grabbing information from url and adding them in key value pairs
        String temp = params.getOrDefault("temperature", "60");

        //Build API url handler 
        String apiKey = "sk-Swph68bb490f86a2712224"; //stores api key
        String plantUrl =  "https://perenual.com/api/species-list?key=" + apiKey + "&frost_hardy=" + frostRisk; 

         String response = "";
         
        try { //send request to Perenual
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder() 
                .uri(URI.create(plantUrl))
                .GET()
                .build(); 
            HttpResponse<String> apiResponse = client.send(request, HttpResponse.BodyHandlers.ofString()); //sending request then storing it in response
            String plantData  = apiResponse.body(); //storing body of response

    //parse JSON from perenual response 
            JSONObject obj = new JSONObject(plantData); //storing response into json object
            JSONArray dataArray = obj.getJSONArray("data"); //returns data as an array

            JSONArray plantCards = new JSONArray(); // container for multiple flashcards

            int maxPlants = 5; 
            for (int i = 0; i < dataArray.length() && i < maxPlants; i++) {
                JSONObject plantObj = dataArray.getJSONObject(i);

                String name = plantObj.optString("common_name", "Unknown Plant");
                String watering = plantObj.optString("watering", null);
                boolean frostSensitive = !plantObj.optBoolean("frost_hardy", false);

                JSONObject card = new JSONObject();
                card.put("plantName", name);
                if (watering != null && !watering.isEmpty()) {
                    card.put("watering", watering);
                }
                
                card.put("frostSensitive", frostSensitive);
                plantCards.put(card);
        }

             response = plantCards.toString();
            
        
        } catch (Exception e) {
             response = "{\"error\":\"Error fetching plant data: " + e.getMessage() + "\"}";
        }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length); //notifying browser if fetching worked and how much data to output
            OutputStream os = exchange.getResponseBody(); //stores output from response
            os.write(response.getBytes()); //writes response body
            os.close();

    });

         server.start(); //start the server
    }

//Helper function to store query input after ? in the url
    private static Map<String, String> parseQuery(String query) {
    Map<String, String> params = new HashMap<>();
    if (query == null || query.isEmpty()) {
        return params;
    }

    String[] pairs = query.split("&"); // split by key=value pairs
    for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
            params.put(keyValue[0], keyValue[1]); // store key=value
        }
    }
    return params;
}

}