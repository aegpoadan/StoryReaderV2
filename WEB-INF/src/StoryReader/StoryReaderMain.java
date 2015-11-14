package StoryReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mindrot.jbcrypt.BCrypt;

import freemarker.core.ParseException;
import freemarker.template.*;

public class StoryReaderMain extends HttpServlet {
	/**
	 * 
	 */
	private final String storyLocQuery = "SELECT Path FROM Stories";
	private final String userLookUp = "SELECT Password FROM `Users` WHERE Username=";
	private static final long serialVersionUID = -7653870810021249763L;
	private Configuration cfg;
	private Log log;
	private ArrayList<Story> stories;
	private HashMap<String, Integer> storyIndecies = new HashMap<String, Integer>();
	private HashMap<HttpSession, SessionState> sessions = new HashMap<HttpSession, SessionState>();
	private StoryDBMan dbMan;
	
	public StoryReaderMain() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		log = new Log("log.out");
		/* ------------------------------------------------------------------------ */
        /* You should do this ONLY ONCE in the whole application life-cycle: */
        /* Create and adjust the configuration singleton */
        cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDirectoryForTemplateLoading(new File("/Library/Tomcat/webapps/StoryReader"));
        cfg.setDefaultEncoding("UTF-8"); //cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW);
        
        dbMan = new StoryDBMan();
        loadStories();
	}
	
	private String getPassword(String username) throws SQLException {
		ArrayList<String[]> qResult = dbMan.select(userLookUp + "'" + username + "'");
		if(qResult.size() > 0) {
			return qResult.get(0)[0];
		} else {
			return null;
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		
		String[] tokens = request.getRequestURI().split("\\s*/\\s*");
		PrintWriter out = response.getWriter();
		if(tokens[2].toLowerCase().equals("select")) {
			try {
				if(!sessions.containsKey(request.getSession())) {
					sessions.put(request.getSession(), new SessionState("", request.getRemoteAddr()));
				}
				generatePage(request, out);
			} catch (Exception e) {
				log.log("Failed to generate page: " + request.getRequestURI() + "\tfrom: " + request.getRemoteAddr());
			}
		} else if(tokens[2].toLowerCase().equals("read")) {
			Story storyToLoad = stories.get(storyIndecies.get(request.getParameter("story")));
			try {
				generateStoryPage(request, response.getWriter(), storyToLoad);
			} catch (TemplateException e) {
				log.log("Failed to generate page: " + request.getRequestURI() + "\tfrom: " + request.getRemoteAddr());
			}
		}
		
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		String remoteAddress = request.getRemoteAddr();
		String username = request.getParameter("username");
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		String storyName = request.getParameter("story");
		
		if(username == null || email == null || storyName == null) {
			try {
				sessions.get(request.getSession()).setErrorMessage("You must fill in all fields");
				generatePage(request, out);
			} catch (Exception e) {
				log.log("Failed to generate page: " + request.getRequestURI() + "\tfrom: " + sessions.get(request.getSession()).toString());
			}
		} else {
			try {
				String hashPass = getPassword(username);
				if(hashPass == null) {
					sessions.get(request.getSession()).setErrorMessage("Invalid credentials");
					generatePage(request, out);
				}
				else if(BCrypt.checkpw(password, hashPass)) {
					sessions.put(request.getSession(), new SessionState(username, email, remoteAddress, 0, stories.get(storyIndecies.get(storyName)).pages.size()-1));
					response.sendRedirect(request.getRequestURI().replace("select", "read") + "?story=" + storyName + "&p=0");
				} else {
					sessions.get(request.getSession()).setErrorMessage("Invalid credentials");
					generatePage(request, out);
				}
			} catch (SQLException e) {
				log.log("Failed to retrieve hashed password for: " + sessions.get(request.getSession()).toString());
			} catch (Exception e) {
				log.log("Failed to generate page: " + request.getRequestURI() + "\tfrom: " + sessions.get(request.getSession()).toString());
			}
		}
	}
	
	/*Used by doGet() when loading a story (/StoryReader/read)*/
	private void generateStoryPage(HttpServletRequest request, PrintWriter out, Story story) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		 Map<String, Object> root = new HashMap<String, Object>();
		 
		 SessionState ses = sessions.get(request.getSession());
		 ses.setPage(Integer.parseInt(request.getParameter("p")));
		 root.put("sessionState", ses);
		 root.put("storyName", story.name);
		 int pageNumber = Integer.parseInt(request.getParameter("p"));
		 if(pageNumber >= 0 && pageNumber < story.pages.size()) {
			 root.put("pageContent", story.pages.get(pageNumber).toString());
		 } else if(pageNumber < 0){
			 root.put("pageContent", story.pages.get(0).toString());
		 } else if(pageNumber > story.pages.size()-1) {
			 root.put("pageContent", story.pages.get(story.pages.size()-1).toString());
		 }
		 
		 Template temp = cfg.getTemplate("StoryReadTemplate.ftl");
         /* Merge data-model with template */
         temp.process(root, out);
		 
	}
	
	/*Used by doGet() when loading the home page (/StoryReader/select)*/
	private void generatePage(HttpServletRequest req, PrintWriter out) throws Exception {
          /* ------------------------------------------------------------------------ */
          /* You usually do these for MULTIPLE TIMES in the application life-cycle: */
          /* Create a data-model */
          Map<String, Object> root = new HashMap<String, Object>();
          SessionState state = sessions.get(req.getSession());
          root.put("sessionState", state);
          root.put("stories", stories);
          Template temp = cfg.getTemplate("StoryTemplate.ftl");
          /* Merge data-model with template */
          temp.process(root, out); // Note: Depending on what `out` is, you may need to call `out.close()`. 
          // This is usually the case for file output, but not for servlet output. 
	}
	
	/*Used by constructor to initially load stories from a directory. If directory is updated, server restart is required*/
	private void loadStories() throws IOException, SQLException {
		stories = new ArrayList<Story>();
		ArrayList<String[]> storyLocations = dbMan.select(storyLocQuery);
		
		for(String[] r: storyLocations) {
			String fileName = r[0];
			File f = new File(fileName);
			int index = fileName.lastIndexOf(".");
			int nameIndex = fileName.lastIndexOf("/");
			if(fileName.substring(index + 1, fileName.length()).equalsIgnoreCase("sty")) {
				String storyName = fileName.substring(nameIndex+1, index);
				stories.add(new Story(Jsoup.parse(f, "UTF-8"), storyName));
				storyIndecies.put(storyName, stories.size()-1);
			}
		}
	}
	
	/*Helper class to hold story data*/
	public class Story {
		protected Elements pages;
		protected String name;
		
		public Story(Document doc, String name) {
			pages = doc.getElementsByTag("PAGE");
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	/*Helper class to hold session state data*/
	public class SessionState {
		protected String username;
		protected String email;
		protected String remoteAddress;
		protected int page;
		protected int lastPage;
		protected String errorMessage;
		
		public SessionState(String username, String email, String remoteAddress, int page, int lastPage) {
			this.username = username;
			this.email = email;
			this.page = page;
			this.lastPage = lastPage;
			errorMessage = "";
		}
		
		public SessionState(String errorMessage, String remoteAddress) {
			this.errorMessage = errorMessage;
			this.remoteAddress = remoteAddress;
			username = "N/A";
			email = "N/A";
		}
		
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		
		public String getErrorMessage() {
			return errorMessage;
		}
		
		public String getUsername() {
			return username;
		}
		
		public String getEmail() {
			return email;
		}
		
		public int getPage() {
			return page;
		}
		
		public void setPage(int page) {
			this.page = page;
		}
		
		public int getLastPage() {
			return lastPage;
		}
		
		public String toString() {
			return "username: " + username + " email: " + email + " remoteAddress: " + remoteAddress;
		}
	}
	  
	  public static void main(String[] args) {
		  System.out.println(BCrypt.hashpw("test", BCrypt.gensalt()));
		  System.out.println(BCrypt.checkpw("test", "$2a$10$qjQGoTPYIY4to2REttx6dujM7m7WOSXEVlx4obRD0IT5GjRkzcBOy"));
		  
	  }

}
