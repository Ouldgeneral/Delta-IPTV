package delta.iptv.controller;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.scene.layout.Pane;
import javafx.scene.web.*;
import tools.Internet;
import tools.DeltaDB;
/**
 * @author Ould_Hamdi
 */
public class Controller {
    String html="""
                 <html>
                 <head>
                 </head>
                 <body>
                     <h1 style='color:red'>%s</h1>
                 </body>
                 </html>""";
    String deadServer=html.formatted("Server connection Failed\n");
    String noInternet=html.formatted("No Internet Connection");
    Alert aboutDelta=new Alert(Alert.AlertType.INFORMATION);
    WebEngine engine;
    HashMap<String, String> programs,visitedPrograms;
    FilteredList<String> searchFilter;
    DeltaDB serverDB,programDB;
    @FXML
    WebView player;
    @FXML
    Pane pane;
    @FXML
    ListView<String> list;
    @FXML
    Button about,connect;
    @FXML
    TextField server, uname,pass, port,search;
    @FXML
    Label title;
    @FXML
    ChoiceBox<String> serverList,programList;
    @FXML
    ObservableList<String> servers, items,myPrograms;
    @FXML
    public void initialize() throws IOException{
        serverDB=new DeltaDB("SERVER");
        programDB=new DeltaDB("PROGRAM");
        connect.setDisable(true);
        aboutDelta.setTitle("Delta IPTV Player by Ould Hamdi");
        aboutDelta.setHeaderText("Delta IPTV Player");
        aboutDelta.setContentText("Delta IPTV Player is an m3u files player designed in javafx by Ould_Hamdi");
        about.setOnAction((e) -> aboutDelta.showAndWait());
        engine=player.getEngine();
        engine.loadContent(html.formatted("Open a file or connect to a server"));
        programs=new HashMap<>();
        visitedPrograms=new HashMap<>();
        player.prefWidthProperty().bind(pane.widthProperty().subtract(300));
        player.prefHeightProperty().bind(pane.heightProperty().subtract(150));
        list.prefHeightProperty().bind(pane.heightProperty().subtract(150));
        myPrograms=FXCollections.observableArrayList();
        programList.setItems(myPrograms);
        programList.getSelectionModel().selectedItemProperty().addListener(e->{
            if(programList.getSelectionModel().getSelectedItem()!=null && programList.getSelectionModel().getSelectedItem().equals("Delete all")){
                programDB.deleteDelta();
                myPrograms.clear();
                visitedPrograms.clear();
                myPrograms.add("Delete all");
                return;
            }
            showProgram(programList.getSelectionModel().getSelectedItem());
        });
        servers=serverDB.loadDelta();
        servers.add("Delete all");
        myPrograms.add("Delete all");
        serverList.setItems(servers);
        serverList.getSelectionModel().selectedItemProperty().addListener(e->{
            try {
                if(serverList.getSelectionModel().getSelectedItem()!=null && serverList.getSelectionModel().getSelectedItem().equals("Delete all")){
                    serverDB.deleteDelta();
                    servers.clear();
                    servers.add("Delete all");
                    return;
                }
                connectToServer(serverList.getSelectionModel().getSelectedItem());
            } catch (IOException ex) {
            }
        });
        items=FXCollections.observableArrayList();
        searchFilter=new FilteredList<>(items,s->true);
        list.setItems(searchFilter);
        list.getSelectionModel().selectedItemProperty().addListener(e->{
            showProgram(list.getSelectionModel().getSelectedItem());
        });
        Platform.runLater(()->{
            loadVisitedPrograms();
            if(servers.size()>1)try {
                connectToServer(servers.get(0));
            } catch (IOException ex) {
            }
            
        });
        
    }
    public void loadVisitedPrograms(){
        ObservableList<String> progs=programDB.loadDelta();
        if(progs.isEmpty())return;
        for(String p:progs){
            visitedPrograms.put(p.split(",")[0], p.split(",")[1]);
            myPrograms.add(p.split(",")[0]);
        }
    }
    public void showProgram(String program){
        title.setText(program);
            engine.loadContent("<h1 style='color:red;'>Loading...</h1>");
            String result;
            String link=programs.get(list.getSelectionModel().getSelectedItem());
            boolean hasInternet=Internet.hasInternet();
            boolean isValidLink=false;
            try {
                isValidLink = Internet.isValidLink(link);
            }catch(NullPointerException ex){
                
            }
            try {
                if(hasInternet && isValidLink){
                    result=loadChannel(visitedPrograms.containsKey(program)?visitedPrograms.get(program):programs.get(program));
                    if(!myPrograms.contains(program)){
                        myPrograms.add(program);
                        visitedPrograms.put(program, programs.get(program));
                        programDB.saveDelta(program+","+programs.get(program));
                    }
                }else if(hasInternet && !isValidLink){
                    result=deadServer;
                }else{
                    result=noInternet;
                }   
                engine.loadContent(result);
            } catch (IOException ex) {
            }
    }
    public void openFile() throws FileNotFoundException, IOException{
        programs.clear();
        items.clear();
        FileChooser fileChooser=new FileChooser();
        fileChooser.setTitle("Open a file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("M3U Playlist","*.m3u"));
        File file=fileChooser.showOpenDialog((Stage)pane.getScene().getWindow());
        StringBuilder fileContent;
        if(file!=null){
            fileContent=new StringBuilder();
            FileReader reader=new FileReader(file);
            int i;
            while((i=reader.read())!=-1)fileContent.append((char)i);
            addChannelsToList(fileContent.toString());
        }
    }
    public void connectToServerByButton() throws IOException{
        connectToServer(null);
    }
    public void connectToServer(String serverLink) throws FileNotFoundException, IOException{
        String link=(serverLink!=null)?serverLink:server.getText()+":"+port.getText()+"/get.php?username="+uname.getText()+"&password="+pass.getText()+"&type=m3u";
        String fileContent=Internet.fetchFile(link);
        if(fileContent.equals("Error")){
            engine.loadContent(deadServer);
            return;
        }
        if(fileContent.isEmpty()){
            engine.loadContent(noInternet);
            return;
        }
        if(!fileContent.contains("EXTINF")){
            engine.loadContent(html.formatted("Invalid playlist found on server"));
            return;
        }
        programs.clear();
        items.clear();
        addChannelsToList(fileContent);
        if(!servers.contains(link)){
            servers.add(link);
            serverDB.saveDelta(link);
        }
    }
    private void addChannelsToList(String fileContent){
        title.setText("No Program selected");
        Pattern pattern=Pattern.compile("#EXTINF:.*\\R.*");
        Matcher matcher=pattern.matcher(fileContent);
        while(matcher.find()){
            String target=matcher.group();
            String[] target2=target.split("\\R");
            String name=target2[0].split(",")[1];
            String serverLink=target2[1];
            if(serverLink.contains("/live/") && serverLink.endsWith(".ts")){
                serverLink=serverLink.replace("/live/", "/");
                serverLink=serverLink.replace(".ts", ".m3u8");
            }
            programs.put(name, serverLink);
            items.add(name);
        }
        
        if(programs.isEmpty()){
            engine.loadContent("<h1 style='color:red;'>Open a file or connect to a server</h1>");
        }else{
            engine.loadContent("<h1 style='color:red;'>Select a program to open</h1>");
        }
    }
    public String loadChannel(String serverLink) throws IOException{
            return 
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta charset="UTF-8">
                    <style>
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background: black;
                        overflow: hidden;
                    }
                    
                    #container {
                        position: fixed;
                        inset: 0;
                        width: 100vw;
                        height: 100vh;
                    }
                    
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: none;
                    }
                    </style>
                    </head>
                    <body>
                        <div id="container">
                    """ +
"        <iframe src='%s' allowfullscreen></iframe>\n".formatted(serverLink) +
"    </div>\n" +
"</body>\n" +
"</html>";
    }
    public void enableConnection(){
        if(     !uname.getText().isEmpty() &&
                !pass.getText().isEmpty() &&
                !server.getText().isEmpty() &&
                !port.getText().isEmpty() 
                )connect.setDisable(false);
        else connect.setDisable(true);
    }
    public void searchProgram(){
        searchFilter.setPredicate(program->{
            if(search.getText().isEmpty())return true;
            return program.toLowerCase().contains(search.getText().toLowerCase());
        });
    }
}
