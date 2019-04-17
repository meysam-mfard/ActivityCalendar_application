package se.meysam.activitycalendarApplication.authorization;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Component
public class Authorize {
    private static final String APPLICATION_NAME = "ActivityCalendarAPI";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.activityCalendar/resources/tokens";
    private static final String CREDENTIALS_FILE_PATH = "/client_secret.json";
    //private static final String CREDENTIALS_FILE_PATH = "classpath:client_secret.json";

    //If scopes are modified, delete the previously saved "tokens" folder.
    private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR_EVENTS);


    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, String user) throws IOException {

        // Loading client secrets
        InputStream in = Authorize.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        //File file = ResourceUtils.getFile(CREDENTIALS_FILE_PATH);
        //GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(file));

        // Building flow and triggering user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();



        //String redirectUris = String.join( "+", clientSecrets.getDetails().getRedirectUris());
        String url = clientSecrets.getDetails().getAuthUri()+"?access_type=offline&client_id="+
                clientSecrets.getDetails().getClientId()
                +"&redirect_uri=http://localhost:8888/Callback&response_type=code&scope=https://www.googleapis.com/auth/calendar.readonly%20https://www.googleapis.com/auth/calendar.events";
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = flow.loadCredential(user);
        if (credential!=null)
            return credential;
        else
        {

            if(Desktop.isDesktopSupported()){
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    Runtime.getRuntime().exec(new String[] { "firefox", url });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return new AuthorizationCodeInstalledApp(flow, receiver).authorize(user);
        }


        /*LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(user);*/
    }

    public Calendar getCalendar(String userEmail) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar calendar = new com.google.api.services.calendar.Calendar
                .Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT, userEmail))
                .setApplicationName(APPLICATION_NAME)
                .build();

        return calendar;
    }
}