package StoryReader;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Function;
import com.mysql.jdbc.Driver;


public class StoryDBMan {
	private Connection conn = null;
	private final static String localConnectionString = "jdbc:mysql://localhost:8889/StoryData";
	private final static String rootUser = "root";
	private final static String rootPass = "root";
	
	public StoryDBMan(String connectionString, String user, String pass) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager.getConnection(connectionString, user, pass);
	}
	
	public StoryDBMan() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager.getConnection(localConnectionString, rootUser, rootPass);
		conn.setAutoCommit(false);
	}
	
	public void executeTransaction(String[] updates) throws SQLException {
		for(String update : updates) {
			update(update);
		}
		conn.commit();
	}
	
	public void executeTransaction(String[] updates, Method[] functions, Object callOnMe, Object[][] params) throws Exception {
		for(String update : updates) {
			update(update);
		}
		int i=0;
		for(Method function : functions) {
			function.invoke(callOnMe, params[i]);
			i++;
		}
		conn.commit();
	}
	
	public ArrayList<String[]> select(String query) throws SQLException {
		ArrayList<String[]> result = new ArrayList<String[]>();
		Statement selectStatement = conn.createStatement();
		ResultSet rs = selectStatement.executeQuery(query);
		ResultSetMetaData rsmd = rs.getMetaData();
		int numColumns = rsmd.getColumnCount();
		
		while(rs.next()) {
			result.add(new String[numColumns]);
			for(int i=1; i<=numColumns; i++) {
				result.get(result.size()-1)[i-1] = rs.getString(i);
			}
		}
		selectStatement.close();
		return result;
	}
	
	private void update(String update) throws SQLException {
		Statement updateStatement = conn.createStatement();
		updateStatement.executeUpdate(update);
		updateStatement.close();
	}
	

	public static void main(String[] args) {
		 try {
	            Class.forName("com.mysql.jdbc.Driver").newInstance();
	            
	            StoryDBMan testDB = new StoryDBMan(localConnectionString, rootUser, rootPass);
	            testDB.select("SELECT * FROM Users");
	            
	        } catch (Exception ex) {
	            System.err.println(ex.toString());
	        }

	}

}

