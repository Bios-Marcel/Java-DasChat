package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sun.javafx.font.PrismFontLoader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.stage.Modality;
import javafx.stage.Stage;
import util.Util;


/**
 * @author msc
 *
 */
@SuppressWarnings( "restriction" )
public class DasChat extends Application
{
  /**
   * Referenz auf die Settings Instanz
   */
  private final Settings   settings = Settings.getSettings();

  private DataOutputStream out      = null;
  private DataInputStream  in       = null;
  private Socket           socket;

  /**
   * Liste aller installierten / geladenen Fonts
   */
  private List<String>     fontList;
  /**
   * Beinhaltet die Namen der heruntergeladenen Fonts
   */
  ObservableList<String>   observableFontListDown;
  /**
   * Beinhaltet die Namen der ignorierten Fonts
   */
  ObservableList<String>   observableFontListIgnored;

  /**
   * Getter für die Liste alle momentan ins Programm geladenen Fonts + gedownloadete (geladen sowie nicht
   * geladen).
   *
   * @return fontList
   */
  List<String> getFonts()
  {
    return fontList;
  }

  /**
   * Initialisiert die Liste der ignorierten Fonts.
   */
  void initFontBlacklist()
  {
    observableFontListIgnored = FXCollections.observableList( new ArrayList<String>() );
    final File blacklistFile = new File( settings.getMainFolder() + "blacklistedfonts.list" );
    final Path blacklist = Paths.get( blacklistFile.getAbsolutePath() );
    final Charset charset = StandardCharsets.UTF_8;

    try
    {
      final List<String> fontNames = Files.readAllLines( blacklist, charset );
      fontNames.remove( "" );
      for ( final String fontName : fontNames )
      {
        if ( !observableFontListIgnored.contains( fontName ) )
        {
          observableFontListIgnored.add( fontName );
        }
      }
    }
    catch ( final IOException e )
    {
      try
      {
        blacklistFile.createNewFile();
      }
      catch ( final IOException e1 )
      {
        DasChatInit.logger.warning( "Schriftarten Blacklist konnte nicht erstellt werden." );
      }
    }
  }

  /**
   * Entfernt ein Font von der Liste der ignorierten Fonts
   *
   * @param fontName zu erlaubende Font
   */
  void allowFont( final String fontName )
  {
    observableFontListIgnored.remove( fontName );
  }

  void blacklistFont( final String fontName )
  {
    observableFontListIgnored.add( fontName );
  }

  /**
   * Fügt eine Font zur Liste der geladenen Fonts hinzu.
   *
   * @param fontName hinzuzufügender Name
   */
  void addToFontList( final String fontName )
  {
    fontList.add( fontName );
    observableFontListDown.add( fontName );
  }

  /**
   * Entfernt eine Font aus Liste der geladenen Fonts.
   *
   * @param fontName zu löschender Name
   */
  void removeFromFontList( final String fontName )
  {
    fontList.remove( fontName );
  }

  /**
   * Entfernt eine Font aus Liste der heruntergeladenen Fonts.
   *
   * @param fontName zu löschender Name
   */
  void removeFromFontListDown( final String fontName )
  {
    observableFontListDown.remove( fontName );
  }

  /**
   * Initialisiert die Fonts (System und heruntergeladene)
   */
  void initFontList()
  {
    fontList = new ArrayList<>();
    observableFontListDown = FXCollections.observableList( new ArrayList<>() );
    if ( settings.isShareFonts() )
    {
      for ( final String fontString : Font.getFontNames() )
      {
        fontList.add( fontString );
      }
      final File fontFolder = new File( settings.getFontFolder() );
      final File[] fontFiles = fontFolder.listFiles();
      for ( final File fontFile : fontFiles )
      {
        final String fontName = fontFile.getName().substring( 0, fontFile.getName().length() - 4 );
        try (FileInputStream inputFont = new FileInputStream( fontFile );)
        {

          final PrismFontLoader fontLoader = PrismFontLoader.getInstance();
          final Font font = Font.loadFont( inputFont, 10 );
          fontLoader.loadFont( font );
          fontList.add( fontName );
          observableFontListDown.add( fontName );
        }
        catch ( final IOException e )
        {
          DasChatInit.logger.warning( "Font " + fontName + " konnte nicht geladen werden." );
          e.printStackTrace();
        }
        catch ( final NullPointerException e )
        {
          fontFile.delete();
          DasChatInit.logger.warning( "Font " + fontName + " ist fehlerhaft und wird gelöscht." );
        }
      }
    }
  }

  /**
   * Gibt die Server Adresse zurück falls die IP Adresse auf default steht.
   *
   * @param retryAutomatically
   * @return
   */
  InetAddress getServer( final boolean retryAutomatically )
  {
    try (final DatagramSocket c = new DatagramSocket();)
    {
      c.setBroadcast( true );
      c.setSoTimeout( 2500 );
      final byte[] sendData = "DISCOVER_SERVER_REQUEST".getBytes();

      DatagramPacket sendPacket =
          new DatagramPacket( sendData, sendData.length, InetAddress.getByName( "255.255.255.255" ), settings.getDiscoveryPort() );
      c.send( sendPacket );
      DasChatInit.logger.info( "Anfrage Paket an 255.255.255.255 gesendet." );


      final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while ( interfaces.hasMoreElements() )
      {
        final NetworkInterface networkInterface = interfaces.nextElement();

        if ( networkInterface.isLoopback() || !networkInterface.isUp() )
        {
          continue;
        }

        for ( final InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses() )
        {
          final InetAddress broadcast = interfaceAddress.getBroadcast();
          if ( broadcast == null )
          {
            continue;
          }
          sendPacket = new DatagramPacket( sendData, sendData.length, broadcast, settings.getDiscoveryPort() );
          c.send( sendPacket );

          DasChatInit.logger.info( "Anfrage Paket an " + broadcast.getHostAddress() + " gesendet; Interface: "
              + networkInterface.getDisplayName() );
        }
      }

      DasChatInit.logger.info( "Senden der Anfragen beendet, warte auf Antwort." );

      final byte[] recvBuf = new byte[15000];
      final DatagramPacket receivePacket = new DatagramPacket( recvBuf, recvBuf.length );

      c.receive( receivePacket );

      DasChatInit.logger.info( "Broadcastantwort vom Server mit der IP: " + receivePacket.getAddress().getHostAddress() );

      final String message = new String( receivePacket.getData() ).trim();
      if ( message.equals( "DISCOVER_SERVER_RESPONSE" ) )
      {
        return receivePacket.getAddress();
      }
      if ( retryAutomatically )
      {
        return getServer( true );
      }
      return null;
    }
    catch ( final Exception e )
    {
      if ( retryAutomatically )
      {
        return getServer( true );
      }
      return null;
    }
  }

  /**
   * Versucht sich mit dem Server zu verbinden und versucht es bei Fehlschlag auf wunsch erneut oder schließt
   * 'DasChat'
   */
  @SuppressWarnings( "resource" )
  void tryToConnect( final boolean retryAutomatically )
  {
    OutputStream outputStream = null;
    InputStream inputStream = null;
    if ( Objects.isNull( settings.getServerIP() ) )
    {
      settings.setServerIP( getServer( false ) );
    }
    if ( !Objects.isNull( settings.getServerIP() ) )
    {
      try
      {
        setSocket( new Socket( settings.getServerIP(), settings.getServerPort() ) );
        outputStream = getSocket().getOutputStream();
        inputStream = getSocket().getInputStream();
        out = new DataOutputStream( outputStream );
        in = new DataInputStream( inputStream );
        settings.setController( new FXMLController( this ) );
      }
      catch ( final IOException e )
      {
        DasChatInit.logger.info( e.getMessage() );
        if ( retryAutomatically )
        {
          tryToConnect( true );
        }
        else
        {
          final Alert alert = new Alert( AlertType.CONFIRMATION );
          //          alert.getDialogPane().getStylesheets().add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
          alert.setTitle( "Verbindung zum Server aufbauen" );
          alert.setHeaderText( "Zeitüberschreitung bei der Serververbindung" );
          alert.setContentText(
              "Das Verbinden mit dem Server hat zu lange gedauert, stelle sicher das der Server läuft und du mit dem Netzwerk verbunden bist sowie auch das der benötigte Port(55001 falls nicht anders eingestellt) freigegeben ist. Bitte wenden sie sich an Ihren Administrator." );

          final ButtonType buttonTypeRetry = new ButtonType( "Erneut versuchen" );
          final ButtonType buttonTypeCancel = new ButtonType( "Schließen" );

          alert.getButtonTypes().setAll( buttonTypeRetry, buttonTypeCancel );

          final Optional<ButtonType> result = alert.showAndWait();
          if ( result.get() == buttonTypeRetry )
          {
            tryToConnect( false );
          }
          else
          {
            DasChatInit.logger.info( "DasChat wird nun geschlossen." );
            Runtime.getRuntime().exit( 0 );
          }
        }
      }
    }
    else
    {
      final Alert alert = new Alert( AlertType.CONFIRMATION );
      alert.setTitle( "Verbindung zum Server aufbauen" );
      alert.setHeaderText( "Zeitüberschreitung bei der Serververbindung" );
      alert.setContentText(
          "Das Verbinden mit dem Server hat zu lange gedauert, stelle sicher das der Server läuft und du mit dem Netzwerk verbunden bist sowie auch das der benötigte Port(55001 falls nicht anders eingestellt) freigegeben ist. Bitte wenden sie sich an Ihren Administrator." );

      final ButtonType buttonTypeRetry = new ButtonType( "Erneut versuchen" );
      final ButtonType buttonTypeCancel = new ButtonType( "Schließen" );

      alert.getButtonTypes().setAll( buttonTypeRetry, buttonTypeCancel );

      final Optional<ButtonType> result = alert.showAndWait();
      if ( result.get() == buttonTypeRetry )
      {
        tryToConnect( false );
      }
      else
      {
        DasChatInit.logger.info( "DasChat wird nun geschlossen." );
        Runtime.getRuntime().exit( 0 );
      }
    }
  }

  /**
   * Versucht sich mit dem Server zu verbinden und versucht es bei felhschlag auf wunsch erneut oder schließt
   * 'DasChat'
   */
  @SuppressWarnings( "resource" )
  boolean tryToReconnect()
  {
    OutputStream outputStream = null;
    InputStream inputStream = null;

    if ( Objects.isNull( settings.getServerIP() ) )
    {
      settings.setServerIP( getServer( true ) );
    }
    if ( !Objects.isNull( settings.getServerIP() ) )
    {
      try
      {
        setSocket( new Socket( settings.getServerIP(), settings.getServerPort() ) );
        outputStream = getSocket().getOutputStream();
        inputStream = getSocket().getInputStream();
        out = new DataOutputStream( outputStream );
        in = new DataInputStream( inputStream );
        settings.getController().askServerIfUsernameIsAvailable( settings.getClientName() );
        Platform.runLater( () ->
        {
          settings.getController().primaryStage.setTitle( "DasChat - Eingeloggt als: " + settings.getClientName() );
        } );
        return true;
      }
      catch ( final IOException e )
      {
        return tryToReconnect();
      }
    }
    return tryToReconnect();
  }

  /**
   * Verbindet sich mit dem Server und öffnet je nach Bedarf entweder den Login Dialog oder den Client
   */
  @Override
  public void start( final Stage primaryStage ) throws IOException
  {
    tryToConnect( false );

    settings.getController().setLoginDialogStage( primaryStage );
    if ( settings.getClientName().equals( "" ) )
    {
      initLoginDialog();
    }
    else
    {
      tryToLogin();
    }
  }

  /**
   * Initialisieren des Login Dialogs
   */
  private void initLoginDialog()
  {
    final Stage stage = settings.getController().loginDialogStage;
    final FXMLLoader loader = new FXMLLoader();
    loader.setLocation( getClass().getResource( "LoginDialog.fxml" ) );
    loader.setController( settings.getController() );
    try
    {
      final Parent root = loader.load();
      final Scene scene = new Scene( root );
      final WebEngine webEngine = settings.getController().webView.getEngine();
      webEngine.load( getClass().getResource( "text.html" ).toExternalForm() );
      scene.getStylesheets().add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
      stage.setScene( scene );
      stage.setTitle( "DasChat - Login" );
      stage.getIcons().add( new Image( DasChat.class.getResourceAsStream( "icon.png" ) ) );
      stage.show();
      stage.setIconified( false );
      stage.setMaximized( false );
      stage.setMinWidth( stage.getWidth() );
      stage.setMinHeight( stage.getHeight() );
      stage.setMaxWidth( stage.getWidth() );
      stage.setMaxHeight( stage.getHeight() );
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( e.getMessage() );
    }
  }

  /**
   * Öffnet den Client falls sollte der Benutzername nicht bereits in Nutzung sein, in diesem Fall wird der
   * Benutzer darüber benachrichtigt und kriegt die Möglichkeit einen neuen Benutzername auszuwählen.
   */
  private void tryToLogin()
  {
    if ( settings.getController().askServerIfUsernameIsAvailable( settings.getClientName() ) )
    {
      settings.getController().initClient();
    }
    else
    {
      final Alert alert = new Alert( AlertType.CONFIRMATION );
      alert.initModality( Modality.APPLICATION_MODAL );
      alert.setTitle( "Auf zu 'DasChat'" );
      alert.setHeaderText( "Benutzername bereits in Verwendung!" );
      alert.setContentText(
          "Dein Benutzername wird bereits verwendet oder du hast bereits einen Client offen. Wenn du deinen Benutzername ändern möchtest klicke auf 'Ändern', wenn nicht klicke auf 'Schließen'." );

      final ButtonType buttonTypeRetry = new ButtonType( "Erneut versuchen" );
      final ButtonType buttonTypeChange = new ButtonType( "Ändern" );
      final ButtonType buttonTypeCancel = new ButtonType( "Schließen" );

      alert.getButtonTypes().setAll( buttonTypeRetry, buttonTypeChange, buttonTypeCancel );

      final Optional<ButtonType> result = alert.showAndWait();
      if ( result.get() == buttonTypeRetry )
      {
        tryToLogin();
      }
      else if ( result.get() == buttonTypeChange )
      {
        initLoginDialog();
      }
      else
      {
        DasChatInit.logger.info( "DasChat wird nun geschlossen. at login" );
        settings.getController().disconnect();
        Runtime.getRuntime().exit( 0 );
      }
    }
  }

  /**
   * Sendet einen String als UTF-8 Encoded Byte Array an den Server.
   *
   * @param toSend String welcher zu senden ist
   */
  void sendStringToServer( final String toSend )
  {
    try
    {
      byte[] bytesToSend = toSend.getBytes( "utf-8" );
      bytesToSend = Util.compress( bytesToSend );
      out.writeInt( bytesToSend.length );
      out.write( bytesToSend );
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( e.getMessage() );
    }
  }

  /**
   * Empfängt String vom Server und gibt ihn zurück.
   *
   * @return den String der zurückgegeben wird
   */
  String receiveStringFromServer()
  {
    try
    {
      final int length = in.readInt();
      final byte[] compressed = new byte[length];
      in.readFully( compressed, 0, length );
      final byte[] message = Util.decompress( compressed );
      return new String( message, StandardCharsets.UTF_8 );
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( e.getMessage() );
      return null;
    }
  }

  /**
   * Gibt den Socket zurück um diesen zum Beispiel vom Controller aus schließen zu können.
   *
   * @return socket
   */
  Socket getSocket()
  {
    return socket;
  }

  /**
   * Setter für den Socket
   *
   * @param socketToSet socket
   */
  void setSocket( final Socket socketToSet )
  {
    socket = socketToSet;
  }

  /**
   * Zum starten der FX Applikation
   *
   * @param args beim Start übergebene Argumente
   */
  void run( final String[] args )
  {
    launch( args );
  }
}