//Backend entry for API calls 
package com.plotnpot; 

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;


public class AppServer {
    //setup http server on fixed port to accept request 
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception { //stable entry and port   
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);  //Creates server that listens to all IPs on port 
        // Create a test endpoint at /hello
        server.createContext("/hello", exchange -> {  //telling server which info to display based on endpoint
            String response = "Hello Pot & Plot!";
            exchange.sendResponseHeaders(200, response.getBytes().length); // 200 = OK
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes()); // write the response
            os.close(); // close the stream
        });

        server.createContext("/location", exchange -> { //telling server which info to display based on endpoint
            String query = exchange.getRequestURI().getQuery(); //retrieve the location or zip from the query request
            String response = ""; 
            if(query != null && !query.isEmpty()) {
                String[] parts = query.split("="); //splitting query to extract values
                if(parts.length == 2) {
                    String key = parts[0]; //zip or city
                    String value = parts[1]; //input
                    response = "Recieved " + key + ": " + value; 
                } else {
                    response = "Invalid query format";
            }
             } else {
                   response = "No query provided";
            }
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });



        server.start(); //start the server
    }

}