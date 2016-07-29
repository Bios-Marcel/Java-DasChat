package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import shared.GeneralSharedInformation;
import util.Util;

/**
 * Hauptklasse des Servers, zuständig für Laden des Feeds sowie Herstellung der Verbindung mit den Clients.
 *
 * @author msc
 *
 */
public class Main
{
  /**
   * Logger für alles
   */
  static Logger        logger;

  /**
   * Versionsnummer des Servers
   */
  final String         VERSION           = GeneralSharedInformation.VERSION;

  /**
   * Über diesen Port werden eingehende Verbindungen angenommen und die Kommunikation geregelt.
   */
  private int          serverPort;

  /**
   * Über diesen Port lässt sich der Server im Netzwerk lokalisieren.
   */
  int                  discoveryPort;

  /**
   * Über diesen Port werden Fonts gesendet.
   */
  int                  fontTransferPort;

  /**
   * Bestimmt wieviele Clients maximal zur gleichen Zeit verbunden seien können
   */
  private int          maxClients;

  /**
   * Konfigurationsdateipfad des Servers
   */
  private final String CONFIG_PATH       = GeneralSharedInformation.SERVER_CONFIG_PATH;

  /**
   * Hier speichert der Server die Fonts welche er gesendet bekommen hat.
   */
  final String         FONT_LOCATION     = GeneralSharedInformation.SERVER_FONT_LOCATION;

  /**
   * Hier werden die einzelnen Hash Tags gespeichert.
   */
  final String         HASH_TAG_LOCATION = GeneralSharedInformation.SERVER_HASH_TAG_LOCATION;

  /**
   * Referenz auf die einzelnen Clients
   */
  List<Socket>         sockets           = new ArrayList<>();

  //TODO feed lediglich als komprimierten byte array im speicher behalten
  /**
   * String zum speichern des Feeds um permanentes Lesen aus der Datei zu verhindern
   */
  private String       feed              = "";

  /**
   * Liste welche alle HashTags enthalten (die seit je benutzt wurden)
   */
  List<String>         allHashTags       = new ArrayList<>();

  /**
   * Instanz der Mainklasse.
   */
  private static Main  instance;

  /**
   * @return gibt die Instanz von Settings zurück
   */
  public static Main getMain()
  {
    return instance;
  }

  /**
   * Akzeptiert die Client verbindungen und erstellt einen Thread für diese.
   *
   * @param args unused
   */
  public static void main( final String[] args )
  {
    new Main().launch();
  }

  /**
   * Konstruktor
   */
  public Main()
  {
    instance = this;
  }

  private void launch()
  {
    try
    {
      initializeLogger();
      logger.info( "Server gestartet (Version: " + VERSION + ")" );
    }
    catch ( final IOException | SecurityException e )
    {
      System.out.println( "Der Logger konnte nicht initialisiert werden" );
    }

    if ( !loadSettings() )
    {
      createSettings();
      loadSettings();
    }
    createFontFolderIfNeccessary();
    loadFeed();
    loadHashTags();
    startDiscoveryThread();
    startFontAcceptThread();
  }

  /**
   * Startet den Thread welcher die eingehenden Verbindungen für den Font Port behandelt.
   */
  private void startFontAcceptThread()
  {
    new FontTransferAcceptThread().start();

    final Thread thread = new Thread( () ->
    {
      try (ServerSocket serverSocket = new ServerSocket( serverPort ))
      {
        while ( true )
        {
          new MultiServerThread( serverSocket.accept() ).start();
        }
      }
      catch ( final IOException e )
      {
        logger.severe( "Port " + serverPort + " ist bereits in Benutzung!" );
        Runtime.getRuntime().exit( 0 );
      }
    } );
    thread.start();
  }

  /**
   * Startet den Thread welcher dafür sorgt das der Server im Netzwerk gefunden werden kann.
   */
  private void startDiscoveryThread()
  {
    final Thread discoveryThread = new Thread( DiscoveryThread.getInstance() );
    logger.info( "Starte Discovery Thread" );
    discoveryThread.start();
  }


  /**
   * Erstellt den Font Ordner (@link {@link #FONT_LOCATION}) falls dieser noch nicht vorhanden ist.
   */
  private void createFontFolderIfNeccessary()
  {
    final File folder = new File( FONT_LOCATION.substring( 0, FONT_LOCATION.length() - 1 ) );
    if ( folder.exists() )
    {
      logger.info( "Ordner '" + FONT_LOCATION.substring( 0, FONT_LOCATION.length() - 1 ) + "' wurde gefunden." );
    }
    else
    {
      folder.mkdir();
      logger.info( "Ordner '" + FONT_LOCATION.substring( 0, FONT_LOCATION.length() - 1 ) + "' wurde erstellt" );
    }
  }


  /**
   * Initialisiert den Logger
   *
   * @throws IOException falls der logFile bereits in Benutzung ist oder nicht darauf zugegriffen werden kann
   * @throws SecurityException
   */
  private void initializeLogger() throws IOException, SecurityException
  {
    logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    logger.setLevel( Level.ALL );
    final FileHandler logFile = new FileHandler( "Log.txt", true );
    final LogFormatter formatterTxt = new LogFormatter();

    logFile.setFormatter( formatterTxt );
    logger.addHandler( logFile );
  }

  /**
   * Wandelt portsToSet in Integer um und setzt diesen in {@link #fontTransferPort}.
   *
   * @param portToSet zu setzender Port
   */
  private void setFontTransferPort( final String portToSet )
  {
    if ( portToSet.equalsIgnoreCase( "default" ) )
    {
      fontTransferPort = GeneralSharedInformation.FONT_TRANSFER_PORT;
    }
    else
    {
      try
      {
        fontTransferPort = Integer.parseInt( portToSet );

      }
      catch ( final Exception e )
      {
        fontTransferPort = GeneralSharedInformation.FONT_TRANSFER_PORT;
        logger
            .warning(
                "Der Font Transfer Port wurde automatisch auf " + GeneralSharedInformation.FONT_TRANSFER_PORT
                    + " gesetzt da die Config fehlerhaft ist." );
      }
    }
  }

  /**
   * Wandelt portsToSet in Integer um und setzt diesen in {@link #serverPort}.
   *
   * @param portToSet zu setzender Port
   */
  private void setServerPort( final String portToSet )
  {
    if ( portToSet.equalsIgnoreCase( "default" ) )
    {
      serverPort = GeneralSharedInformation.SERVER_PORT;
    }
    else
    {
      try
      {
        serverPort = Integer.parseInt( portToSet );
      }
      catch ( final Exception e )
      {
        serverPort = GeneralSharedInformation.SERVER_PORT;
        logger
            .warning(
                "Der Server Port wurde automatisch auf " + GeneralSharedInformation.SERVER_PORT
                    + " gesetzt da die Config fehlerhaft ist." );

      }
    }
  }

  /**
   * Wandelt portsToSet in Integer um und setzt diesen in {@link #discoveryPort}.
   *
   * @param portToSet zu setzender Port
   */
  private void setDiscoveryPort( final String portToSet )
  {
    if ( portToSet.equalsIgnoreCase( "default" ) )
    {
      discoveryPort = GeneralSharedInformation.DISCOVERY_PORT;
    }
    else
    {
      try
      {
        discoveryPort = Integer.parseInt( portToSet );
      }
      catch ( final Exception e )
      {
        discoveryPort = GeneralSharedInformation.DISCOVERY_PORT;
        logger
            .warning( "Der Discovery Port wurde automatisch auf " + GeneralSharedInformation.DISCOVERY_PORT
                + " gesetzt da die Config fehlerhaft ist." );
      }
    }
  }

  /**
   * Wandelt maxClientString in Integer um und setzt diesen in {@link #maxClients}.
   *
   * @param maxClientString zu setzende maxClients
   */
  private void setMaxClients( final String maxClientString )
  {
    try
    {
      maxClients = Integer.parseInt( maxClientString );
    }
    catch ( final Exception e )
    {
      maxClients = 0;
    }
  }

  /**
   * Getter für {@link #maxClients}
   *
   * @return {@link #maxClients}
   */
  int getMaxClients()
  {
    return maxClients;
  }

  /**
   * Lädt die Einstellungen.
   */
  private boolean loadSettings()
  {
    final Properties toLoad = new Properties();
    try (InputStreamReader input = new InputStreamReader( new FileInputStream( CONFIG_PATH ), GeneralSharedInformation.CHARSET );)
    {
      toLoad.load( input );
      setServerPort( toLoad.getProperty( "serverport" ) );
      setDiscoveryPort( toLoad.getProperty( "discoveryport" ) );
      setFontTransferPort( toLoad.getProperty( "fonttransferport" ) );
      setMaxClients( toLoad.getProperty( "maxclients" ) );
      return true;
    }
    catch ( final Exception e )
    {
      logger.info( "Konfiguration konnte nicht geladen werden." );
      return false;
    }
  }

  /**
   * Erstellt die Konfigurationsdatei mit den standard Einstellungen
   */
  private void createSettings()
  {
    final Properties toSave = new Properties();
    try (
        OutputStreamWriter output = new OutputStreamWriter( new FileOutputStream( CONFIG_PATH, false ), GeneralSharedInformation.CHARSET );)
    {
      toSave.setProperty( "serverport", "" + "default" );
      toSave.setProperty( "discoveryport", "" + "default" );
      toSave.setProperty( "fonttransferport", "" + "default" );
      toSave.setProperty( "maxclients", "0" );
      toSave.store( output, "Löschen / Bearbeiten der Datei auf eigene Gefahr." );
      logger.info( "Konfigurationsdatei wurde erstellt." );
    }
    catch ( final FileNotFoundException e )
    {
      logger.warning( "Exception: " + e.fillInStackTrace() );
    }
    catch ( final IOException e )
    {
      logger.warning( "Exception: " + e.fillInStackTrace() );
    }
  }

  /**
   * Speichert den aktuellen Feed
   *
   * @param toSave zu speichernder feed
   * @return Erfolgreich oder nicht
   */
  void saveToFeed( final String toSave )
  {
    try (final FileOutputStream fileOutputStream = new FileOutputStream( "feed.html" );)
    {
      final byte[] bytesToSave = Util.compress( toSave.getBytes() );
      IOUtils.write( bytesToSave, fileOutputStream );
    }
    catch ( final Exception e )
    {
      logger.warning( "Exception: " + e.fillInStackTrace() );
      logger.warning( "Beitrag konnte nicht zum Feed hinzugefügt werden." );
    }
  }

  /**
   * Lädt die HashTags aus der HashTag Datei in die {@link #allHashTags} Liste.
   */
  void loadHashTags()
  {
    final File hashTagFile = new File( HASH_TAG_LOCATION );
    final Path hashTagPath = Paths.get( hashTagFile.getAbsolutePath() );

    if ( !hashTagFile.exists() )
    {
      try
      {
        hashTagFile.createNewFile();
      }
      catch ( final IOException e1 )
      {
        logger.warning( "HashTag Liste existiert nicht." );
      }
    }
    else
    {
      try
      {
        final List<String> hashTags = Files.readAllLines( hashTagPath, GeneralSharedInformation.CHARSET );

        //Weil erste Zeile leer ist
        hashTags.remove( "" );

        for ( final String hashTag : hashTags )
        {
          if ( !getAllHashTags().contains( hashTag ) )
          {
            getAllHashTags().add( hashTag );
          }
        }
      }
      catch ( final IOException e )
      {
        logger.warning( "Es trat ein Fehler beim Laden der HashTag Liste auf." );
      }
    }
  }

  /**
   * Getter für die {@link #allHashTags} Liste.
   *
   * @return {@link allHashTags}
   */
  private List<String> getAllHashTags()
  {
    return allHashTags;
  }

  /**
   * @return the {@link #FONT_LOCATION}
   */
  public String getFontLocation()
  {
    return FONT_LOCATION;
  }


  /**
   * Speichert gegebenfalls die vorhandenen HashTags.
   *
   * @param message nachricht welche zu speichernde HashTags enthalten könnte
   */
  void saveHashTags( final String message )
  {
    final List<String> hashTags = getHashTags( message );
    for ( final String hashTag : hashTags )
    {
      if ( !getAllHashTags().contains( hashTag ) )
      {
        getAllHashTags().add( hashTag );
        final File hashTagFile = new File( HASH_TAG_LOCATION );
        try
        {
          FileUtils.writeStringToFile( hashTagFile, new String( System.lineSeparator() + hashTag ), GeneralSharedInformation.CHARSET,
              true );
        }
        catch ( final IOException e )
        {
          logger.warning( e.fillInStackTrace().toString() );
        }

      }
    }
  }

  /**
   * Durchsucht String nach HashTags.
   *
   * @param htmlText zu durchsuchender String
   * @return List aller gefundenen HashTags
   */
  private List<String> getHashTags( final String htmlText )
  {
    final Pattern hashTagPattern = Pattern.compile( "(?:^|[^\\w])(#[\\wÜÄÖüäöß]+)" );
    final Matcher hashTagMatcher = hashTagPattern.matcher( Jsoup.parse( htmlText ).text() );
    final List<String> hashTags = new ArrayList<>();
    while ( hashTagMatcher.find() )
    {
      final String hashTag = hashTagMatcher.group( 1 );
      hashTags.add( hashTag );
    }
    return hashTags;
  }

  /**
   * Dekomprimiert den feed und lädt diesen.
   */
  private void loadFeed()
  {
    final File file = new File( "feed.html" );
    if ( file.exists() )
    {
      try (FileInputStream fileInputStream = new FileInputStream( file );)
      {
        final byte[] compressedFeed = IOUtils.toByteArray( fileInputStream );
        final byte[] uncompressedFeed = Util.decompress( compressedFeed );
        feed = new String( uncompressedFeed, GeneralSharedInformation.CHARSET );
        logger.info(
            "Feed wurde erfolgreich geladen; Größe komprimiert: " + compressedFeed.length + ";Größe unkomprimiert: "
                + uncompressedFeed.length );
      }
      catch ( final IOException | NullPointerException exception )
      {
        logger.warning(
            "Feed konnte nicht geladen werden, es wird nun versucht die Datei ohne Dekomprimierung zu laden und diese anschließend zu komprimieren und abzusüeichern." );


        try (final FileInputStream fileInputStream = new FileInputStream( file );)
        {
          final byte[] uncompressedFeed = IOUtils.toByteArray( fileInputStream );
          feed = new String( uncompressedFeed, GeneralSharedInformation.CHARSET );
          file.delete();
          file.createNewFile();
          FileUtils.writeByteArrayToFile( file, Util.compress( uncompressedFeed ) );
          logger.info( "Feed wurde erfolgreich geladen." );
        }
        catch ( final IOException exception2 )
        {
          logger.warning( "Feed konnte nicht geladen werden." );
        }
      }
    }
    else
    {
      try
      {
        file.createNewFile();
        logger.info( "Feed.html wurde erstellt, da sie nicht existierte." );
      }
      catch ( final IOException e )
      {
        logger.warning( e.fillInStackTrace().toString() );
      }
    }
  }

  /**
   * Fügt der Socket List einen Socket(Client) hinzu
   *
   * @param socketToAdd hinzuzufügender Socket(Client)
   */
  void addClient( final Socket socketToAdd )
  {
    sockets.add( socketToAdd );
  }

  /**
   * Entfernt einen Socket(Client) aus der Socket List
   *
   * @param socketToRemove zu entfernender Socket(Client)
   */
  void removeClient( final Socket socketToRemove )
  {
    sockets.remove( socketToRemove );
  }

  /**
   * Setter für den Feed
   *
   * @param feedToSet wird in den Feed String gesetzt
   */
  void setFeed( final String feedToSet )
  {
    feed = feedToSet;
  }

  /**
   * Gibt den feed (Beiträge) zurück
   *
   * @return feed
   */
  String getFeed()
  {
    return feed;
  }

}