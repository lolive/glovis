import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;


public class ProxySettings {

       public static void init() {
                                   Properties systemProperties = System.getProperties();
                                   systemProperties.setProperty("http.proxyHost","***");
                                   systemProperties.setProperty("http.proxyPort","***");
                                   systemProperties.setProperty("http.nonProxyHosts","***");
                                   Authenticator.setDefault(new Authenticator() {
                                       protected PasswordAuthentication getPasswordAuthentication() {

                                           return new PasswordAuthentication("***","***".toCharArray());
                                       }
                                   });

       }

}

