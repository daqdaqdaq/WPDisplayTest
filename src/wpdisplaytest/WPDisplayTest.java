/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wpdisplaytest;

import client.Postgres;
import com.sun.javafx.css.StyleManager;
import hu.daq.UDPSender.SenderThread;
import hu.daq.fileservice.FileService;
import hu.daq.servicehandler.ServiceHandler;
import hu.daq.settings.SettingsHandler;
import hu.daq.thriftconnector.connector.ThriftConnector;
import hu.daq.thriftconnector.server.WPDisplayServer;
import hu.daq.thriftconnector.talkback.WPTalkBackClient;
import hu.daq.wp.fx.display.control.ControllerScreen;
import hu.daq.wp.fx.display.screens.ScoreBoardScreen;
import hu.daq.wp.fx.display.screens.SplashScreen;
import hu.daq.wp.horn.Horn;
import hu.daq.wp.matchorganizer.OrganizerBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONException;

/**
 *
 * @author DAQ
 */
public class WPDisplayTest extends Application {

    @Override
    public void start(Stage primaryStage) throws InterruptedException, JSONException, FileNotFoundException, IOException {
        SettingsHandler settings = ServiceHandler.getInstance().getSettings();
        settings.loadProps("display.cfg");
        
//        File f = new File("wpstyle.css");
//        System.out.println(f.getCanonicalPath()+":"+f.canRead()+":"+f.exists());
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet(Paths.get(settings.getProperty("css_path"),"wpstyle.css").toUri().toURL().toExternalForm());
        //System.out.printf(this.getClass().getClassLoader().getResource("").getPath()+":"+java.nio.file.Paths.get("display.cfg"));
        //System.out.println(new File(".").getAbsoluteFile());
        

        // Set root logger level to DEBUG and its only appender to A1.
        String jsonstr = "{\"numlegs\":2,\"legduration\":3000,\"numovertimes\":0,\"overtimeduration\":20000}";

        BasicConfigurator.configure();
        Postgres db = new Postgres();
        db.connect("jdbc:postgresql://"
                + settings.getProperty("database_url") + "/"
                + settings.getProperty("database_db") + "?ssl=true&tcpKeepAlive=true&sslfactory=org.postgresql.ssl.NonValidatingFactory",
                settings.getProperty("database_user"),
                settings.getProperty("database_pass"));
        ServiceHandler.getInstance().setDb(db);

        FileService fs = FileService.getInst();
        fs.setDb(db);

        ThriftConnector<WPDisplayServer, WPTalkBackClient> tc = new ThriftConnector<WPDisplayServer, WPTalkBackClient>();
        ServiceHandler.getInstance().setThriftconnector(tc);
        try {
            ServiceHandler.getInstance().setSenderthread(new SenderThread(
                    settings.getProperty("pyramid_ip"),
                    settings.getIntProperty("pyramid_port")));
        } catch (UnknownHostException ex) {
            Logger.getLogger(WPDisplayTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SocketException ex) {
            Logger.getLogger(WPDisplayTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        ServiceHandler.getInstance().setHorn(new Horn(ServiceHandler.getInstance().getSenderthread()));

        ControllerScreen root = new ControllerScreen(db, primaryStage);
        ScoreBoardScreen sbc = new ScoreBoardScreen(db);
        root.addScreen(sbc, "scoreboard");
        root.addScreen(new SplashScreen(db), "splash");
        ServiceHandler.getInstance().setOrganizer(OrganizerBuilder.build(jsonstr, sbc));
        root.switchScreen("splash");

        ServiceHandler.getInstance().registerCleanup(primaryStage);
        //ResultWrapper rw = root.sendCommand(new LoadTeams(1,2));
        //if (rw.isError()){
        //    System.out.println(rw.get("error"));
        //}
        //Scene scene = new Scene(root, 1024, 768);
        Scene scene = new Scene(root);        
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true);
        primaryStage.show();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
