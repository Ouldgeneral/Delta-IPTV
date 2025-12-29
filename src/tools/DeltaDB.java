package tools;
import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
/**
 * @author Ould_Hamdi
 */
public class DeltaDB {
    Connection con;
    String dataType;
    public DeltaDB(String dataType){
        this.dataType=dataType;
        try {
            con=DriverManager.getConnection("jdbc:sqlite:%ss.db".formatted(dataType.toLowerCase()));
            if(con!=null){
                String sql="CREATE TABLE IF NOT EXISTS %sS(%s TEXT NOT NULL);".formatted(dataType,dataType);
                Statement stmt=con.createStatement();
                stmt.execute(sql);
            }
        } catch (SQLException ex) {

        }
    }
    public void saveDelta(String server){
        String sql="INSERT INTO %sS(%s) VALUES(?);".formatted(dataType,dataType);
        PreparedStatement pstmt;
        try{
            pstmt=con.prepareStatement(sql);
            pstmt.setString(1, server);
            pstmt.executeUpdate();
        }catch(SQLException e){
            
        }
    }
    public void deleteDelta(){
        try {
            con.createStatement().execute("DROP TABLE IF EXISTS %sS".formatted(dataType));
        } catch (SQLException ex) {

        }
    }
    public ObservableList<String> loadDelta(){
        ObservableList<String> servers=FXCollections.observableArrayList();
        try{
            ResultSet rs=con.prepareStatement("SELECT %s FROM %sS".formatted(dataType,dataType)).executeQuery();
            while(rs.next()){
                servers.add(rs.getString("%s".formatted(dataType)));
            }
        }catch(SQLException e){
            
        }
        return servers;
    }
}
