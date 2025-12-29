package delta.iptv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Ould_Hamdi
 */
public class DeltaIPTV extends Application{

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root=FXMLLoader.load(getClass().getResource("/GUI/GUI.fxml"));
        Scene scene=new Scene(root);
        stage.setTitle("Delta IPTV");
        stage.setScene(scene);
        stage.show();
    }

}
