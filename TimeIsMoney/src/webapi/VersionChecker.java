package webapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import de.Linus122.TimeIsMoney.Main;

public class VersionChecker {
	public static int getVersion(){
        URL oracle;
		try {
			oracle = new URL("http://backend.avendria.de/api/version.html");
	        BufferedReader in = new BufferedReader(
	        new InputStreamReader(oracle.openStream()));
	
	        String inputLine;
	        while ((inputLine = in.readLine()) != null)
	            return Integer.parseInt(inputLine);
	        in.close();
		} catch (Exception e) {
			return 9999;
		}
		return 9999;
	}
	public static void register(){
		URL url;
		try {
			url = new URL("http://backend.avendria.de/api/post.php?info=" + "v-" + Main.version + "-");
			url.openConnection().getInputStream();
		} catch (Exception e) {

		}
	}
	public static void unregister(){
		
	}
}
