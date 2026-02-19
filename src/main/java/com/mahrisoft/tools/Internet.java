package com.mahrisoft.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

/**
 * @author Ould_Hamdi
 */
public class Internet {
    static HttpURLConnection conn;
    public static boolean isConnected(String server) throws IOException{
        InetAddress address=InetAddress.getByName(server);
        return address.isReachable(3000);
    }
    public static boolean hasInternet(){
        try(Socket s=new Socket("8.8.8.8",53)){
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
    public static boolean isValidLink(String link){
        if(!hasInternet() ||!link.contains("http"))return false;
        try{    
            URL u=new URL(link);
            conn=(HttpURLConnection)u.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            return (conn.getResponseCode()==200);
        }catch(IOException e){
            return false;
        }
    }
    public static String fetchFile(String link) throws MalformedURLException, IOException{
        if(!hasInternet())return "";
        if(!isValidLink(link))return "Error";
        URL u=new URL(link);
        StringBuilder content=new StringBuilder();
        try(BufferedReader r=new BufferedReader(new InputStreamReader(u.openStream()))){
            String line;
            while((line=r.readLine())!=null){
                content.append(line).append("\n");
            }
        }catch(FileNotFoundException e){
            return "Error";
        }catch(IOException e){
            return "Error";
        }
        return content.toString();
    }
}
