import java.net.MalformedURLException;
import java.net.URL;


public class CodeBase {
	public static URL glovisURL;
	static {
		try{
			glovisURL = new URL("http://glovis.usgs.gov/ImgViewer/");
		} catch (MalformedURLException ex){
			throw new RuntimeException(ex);
		}
	}
	public static URL getGlovisURL(){
		return glovisURL;
	}
}
