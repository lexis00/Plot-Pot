//Backend entry for API calls 
package com.plotnpot; 

import java.util.*;
import java.nio.file.*;
import com.sun.net.httpserver.HttpServer;
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

        Map<String, String> apiKeys = loadKeys("keys.env");
        String openWeatherKey = apiKeys.get("OPENWEATHER_KEY");
        String perenualKey = apiKeys.get("PERENUAL_KEY");


        
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
                    String apiKey = openWeatherKey; //openweather api
                    String fullUrl;

                    if (value.matches("\\d+")) {
                // All digits → treat as zip
                     fullUrl = "http://api.openweathermap.org/data/2.5/weather?zip=" + value + ",us&appid=" + apiKey + "&units=metric";

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

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
});



   server.createContext("/plants", exchange -> {
    String query = exchange.getRequestURI().getQuery();
    Map<String, String> params = parseQuery(query);
    String locationInput = params.getOrDefault("location", "");

    JSONObject responseJson = new JSONObject();

    if (locationInput.isEmpty()) {
        responseJson.put("error", "No location provided");
    } else {
        try {
            // Step 1: Get weather data for location
            String weatherUrl;
            if (locationInput.matches("\\d+")) { // zip code
                weatherUrl = "http://api.openweathermap.org/data/2.5/weather?zip=" 
                              + locationInput + ",us&appid=" + openWeatherKey + "&units=metric";
            } else { // city name
                weatherUrl = "http://api.openweathermap.org/data/2.5/weather?q=" 
                              + locationInput + "&appid=" + openWeatherKey + "&units=metric";
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest weatherRequest = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .GET()
                    .build();
            HttpResponse<String> weatherResponse = client.send(weatherRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject weatherObj = new JSONObject(weatherResponse.body());

            double tempC = weatherObj.getJSONObject("main").getDouble("temp");
            double tempF = Math.round((tempC * 9 / 5) + 32);

            // Step 2: Call Perenual API
            String plantUrl = "https://perenual.com/api/species-list?key=" + perenualKey +
                              "&min_temp=" + (tempF - 10) +
                              "&max_temp=" + (tempF + 10) +
                              "&page=" + (new Random().nextInt(5) + 1); 


            HttpRequest plantRequest = HttpRequest.newBuilder()
                    .uri(URI.create(plantUrl))
                    .GET()
                    .build();
            HttpResponse<String> plantResponse = client.send(plantRequest, HttpResponse.BodyHandlers.ofString());

            JSONArray dataArray = new JSONObject(plantResponse.body()).getJSONArray("data");
            JSONArray plantCards = new JSONArray();
            int maxPlants = 5;

            for (int i = 0; i < dataArray.length() && i < maxPlants; i++) {
                JSONObject plantObj = dataArray.getJSONObject(i);
                JSONObject card = new JSONObject();
                card.put("plantName", plantObj.optString("common_name", "Unknown Plant"));
                card.put("watering", plantObj.optString("watering", "N/A"));
                plantCards.put(card);
            }

            responseJson.put("plants", plantCards);

        } catch (Exception e) {
            responseJson.put("error", "Error fetching plant data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    byte[] bytes = responseJson.toString().getBytes();
    exchange.sendResponseHeaders(200, bytes.length);
    OutputStream os = exchange.getResponseBody();
    os.write(bytes);
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


public static Map<String, String> loadKeys(String filePath) throws IOException {
    Map<String, String> keys = new HashMap<>();
    List<String> lines = Files.readAllLines(Paths.get(filePath));
    for (String line : lines) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            keys.put(parts[0], parts[1]);
        }
    }
    return keys;
}


}