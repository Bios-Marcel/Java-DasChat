package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import shared.GeneralSharedInformation;

// FIXME(msc) Setter fixen, momentan gibt es Exceptions wenn diese falsch benutzt werden.
/**
 * Beinhaltet Variablen wie z.b. die Ports die genutzt werden sowie deren Setter und Getter und die Methoden
 * zum Laden , Speichern und erstellen der Einstellungen
 *
 * @author msc
 *
 */
public class Settings
{
  /**
   * Referenz auf die Settings Instanz
   */
  private static Settings                instance;

  /**
   * Referenz auf FXMLController
   */
  private FXMLController                 controller;

  /**
   * IP des Servers
   */
  private InetAddress                    serverIP;

  private int                            discoveryPort;
  private int                            serverPort;
  private int                            fontTransferPort;
  private int                            maxMessages;

  private String                         clientName                 = "";
  private String                         theme                      = "default";
  private String                         chatTheme                  = "default";
  private String                         ipSetting                  = "";
  private String                         fontCssFileName            = "fonts.css";

  private final String                   MAIN_FOLDER                = System.getProperty( "user.home" ) + File.separator
      + ".DasChat" + File.separator;
  private final String                   FONT_FOLDER                = MAIN_FOLDER + "Fonts" + File.separator;
  private final String                   LOG_FILE                   = MAIN_FOLDER + "Log.txt";
  private final String                   CONFIG_FILE                = MAIN_FOLDER + "config2.cfg";
  private final String                   DASCHAT_COLOR              = "#25b7d3";

  private boolean                        announceNewMsg;
  private boolean                        notDisturb                 = false;
  private boolean                        shareFonts                 = false;
  private boolean                        shareFontsAtBeginning      = false;
  private boolean                        dockFilterStage;

  private KeyCodeCombination             keyCodeMessageSend;
  private KeyCodeCombination             keyCodeOpenManual;
  private KeyCodeCombination             keyCodeActiveClients;
  //  private KeyCodeCombination             keyCodeLineBreak;

  ObservableList<String>                 availableMessageFilters    = FXCollections.observableList( new ArrayList<String>() );
  SortedList<String>                     sortedList                 = new SortedList<>( availableMessageFilters );


  /**
   * Enthält die nicht setzbaren Tastenkombinationen.
   */
  private final List<KeyCodeCombination> blacklistedKeyCombinations = new ArrayList<>();;

  /**
   * Beinhaltet alle Zeilen der cssDatei damit diese keine Zeilen Duplikate bekommt.
   */
  private final List<String>             cssLineList                = new ArrayList<>();;

  /**
   * @return gibt die Instanz von Settings zurück
   */
  public static Settings getSettings()
  {
    if ( Objects.isNull( instance ) )
    {
      instance = new Settings();
    }
    return instance;
  }

  /**
   * @return {@link #blacklistedKeyCombinations}
   */
  public List<KeyCodeCombination> getBlacklistedKeyCombinations()
  {
    return blacklistedKeyCombinations;
  }

  /**
   * @return {@link #cssLineList}
   */
  public List<String> getCssLineList()
  {
    return cssLineList;
  }

  /**
   * @return the dockFilterStage
   */
  public boolean isDockFilterStage()
  {
    return dockFilterStage;
  }

  /**
   * @param dockFilterStageToSet the dockFilterStage to set
   */
  public void setDockFilterStage( final boolean dockFilterStageToSet )
  {
    dockFilterStage = dockFilterStageToSet;
  }

  private void setTheme( final String themeToSet )
  {
    theme = themeToSet;
  }

  void setChatTheme( final String themeToSet )
  {
    chatTheme = themeToSet;
  }

  /**
   * Gibt den boolean Wert zurück der entscheidet ob eine neue Nachricht angekündigt werden soll.
   *
   * @return announcNewMsg
   */
  boolean isAnnounceNewMsg()
  {
    return announceNewMsg;
  }

  private void setAnnounceNewMsg( final boolean announceNewMsgToSet )
  {
    announceNewMsg = announceNewMsgToSet;
  }

  void setFontTransferPort( final String portToSet )
  {
    if ( portToSet.equalsIgnoreCase( "default" ) )
    {
      setFontTransferPort( GeneralSharedInformation.FONT_TRANSFER_PORT );
    }
    else
    {
      setFontTransferPort( Integer.parseInt( portToSet ) );
    }
  }

  void setServerPort( final String portToSet )
  {
    if ( portToSet.equalsIgnoreCase( "default" ) )
    {
      setServerPort( GeneralSharedInformation.SERVER_PORT );
    }
    else
    {
      setServerPort( Integer.parseInt( portToSet ) );
    }
  }

  void setDiscoveryPort( final String portToSet )
  {
    if ( portToSet.equalsIgnoreCase( "default" ) )
    {
      setDiscoveryPort( GeneralSharedInformation.DISCOVERY_PORT );
    }
    else
    {
      setDiscoveryPort( Integer.parseInt( portToSet ) );
    }
  }

  /**
   * Getter für notDisturb
   *
   * @return notDisturb
   */
  boolean isNotDisturb()
  {
    return notDisturb;
  }

  /**
   * Getter für das Chat Theme
   *
   * @return theme
   */
  String getChatTheme()
  {
    return chatTheme;
  }

  /**
   * Getter für das Theme der Applikation
   *
   * @return theme
   */
  String getTheme()
  {
    return theme;
  }

  
  //TODO Default properties überall setzen wo es gut möglich ist
  /**
   * Lädt die Einstellungen
   */
  boolean loadSettings()
  {
    final Properties toLoad = new Properties();
    try (InputStreamReader input = new InputStreamReader( new FileInputStream( CONFIG_FILE ), "UTF-8" );)
    {
      toLoad.load( input );
      setClientName( toLoad.getProperty( "name", "" ) );

      setTheme( toLoad.getProperty( "theme", "default" ) );
      if ( !(getTheme().equalsIgnoreCase( "dark" )
          || getTheme().equalsIgnoreCase( "default" ) || getTheme().equalsIgnoreCase( "daschat" )) )
      {
        setTheme( "default" );
      }

      setChatTheme( toLoad.getProperty( "chattheme", "default" ) );

      setServerIP( toLoad.getProperty( "ip", "default" ) );
      setIpSetting( toLoad.getProperty( "ip", "default" ) );

      setServerPort( toLoad.getProperty( "serverport", "default" ) );

      setDiscoveryPort( toLoad.getProperty( "discoveryport", "default" ) );

      setFontTransferPort( toLoad.getProperty( "fonttransferport", "default" ) );

      setMaxMessages( Integer.parseInt( toLoad.getProperty( "maxmessages", "200" ) ) );

      setNotDisturb( getBoolean( toLoad, "notdisturb", false ) );

      setAnnounceNewMsg( getBoolean( toLoad, "announcenewmsg", true ) );

      setShareFonts( getBoolean( toLoad, "sharefonts", false ) );

      setShareFontsAtBeginning( getBoolean( toLoad, "sharefontsatbeginning", false ) );
      
	  try
      {
        final List<Modifier> list = new ArrayList<>();

        final String keyCodeComb = toLoad.getProperty( "keycodemessagesend" );
        if ( keyCodeComb.contains( "Ctrl" ) )
        {
          list.add( KeyCombination.CONTROL_DOWN );
        }
        if ( keyCodeComb.contains( "Alt" ) )
        {
          list.add( KeyCombination.ALT_DOWN );
        }
        if ( keyCodeComb.contains( "Shift" ) )
        {
          list.add( KeyCombination.SHIFT_DOWN );
        }
        final String[] keys = keyCodeComb.split( "[+]" );
        final String key = keys[ keys.length - 1 ];
        setKeyCodeMessageSend( new KeyCodeCombination( KeyCode.getKeyCode( key ), list.toArray( new Modifier[0] ) ) );
      }
      catch ( final NullPointerException e )
      {
        setKeyCodeMessageSend( new KeyCodeCombination( KeyCode.ENTER ) );
      }
      
	  try
      {
        final List<Modifier> list = new ArrayList<>();

        final String keyCodeComb = toLoad.getProperty( "keycodeopenmanual" );
        if ( keyCodeComb.contains( "Ctrl" ) )
        {
          list.add( KeyCombination.CONTROL_DOWN );
        }
        if ( keyCodeComb.contains( "Alt" ) )
        {
          list.add( KeyCombination.ALT_DOWN );
        }
        if ( keyCodeComb.contains( "Shift" ) )
        {
          list.add( KeyCombination.SHIFT_DOWN );
        }
        final String[] keys = keyCodeComb.split( "[+]" );
        final String key = keys[ keys.length - 1 ];
        setKeyCodeOpenManual( new KeyCodeCombination( KeyCode.getKeyCode( key ), list.toArray( new Modifier[0] ) ) );
      }
      catch ( final NullPointerException e )
      {
        setKeyCodeOpenManual( new KeyCodeCombination( KeyCode.F1 ) );
      }
      try
      {
        final List<Modifier> list = new ArrayList<>();

        final String keyCodeComb = toLoad.getProperty( "keycodeactiveclients" );
        if ( keyCodeComb.contains( "Ctrl" ) )
        {
          list.add( KeyCombination.CONTROL_DOWN );
        }
        if ( keyCodeComb.contains( "Alt" ) )
        {
          list.add( KeyCombination.ALT_DOWN );
        }
        if ( keyCodeComb.contains( "Shift" ) )
        {
          list.add( KeyCombination.SHIFT_DOWN );
        }
        final String[] keys = keyCodeComb.split( "[+]" );
        final String key = keys[ keys.length - 1 ];
        setKeyCodeActiveClients( new KeyCodeCombination( KeyCode.getKeyCode( key ), list.toArray( new Modifier[0] ) ) );
      }
      catch ( final NullPointerException e )
      {
        setKeyCodeActiveClients( new KeyCodeCombination( KeyCode.F2 ) );
      }
      DasChatInit.logger.info( "Konfigurationsdatei wurde geladen." );
      return true;
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( "Konfigurationsdatei konnte nicht geladen werden." );
      return false;
    }
  }

  void setMaxMessages( final int maxMsg )
  {
    maxMessages = maxMsg;
  }

  int getMaxMessages()
  {
    return maxMessages;
  }

  void setKeyCodeMessageSend( final KeyCodeCombination combination )
  {
    keyCodeMessageSend = combination;
  }

  void setKeyCodeOpenManual( final KeyCodeCombination combination )
  {
    keyCodeOpenManual = combination;
  }

  void setKeyCodeActiveClients( final KeyCodeCombination combination )
  {
    keyCodeActiveClients = combination;
  }

  //  void setKeyCodeLineBreak( final KeyCodeCombination combination )
  //  {
  //    keyCodeMessageSend = combination;
  //  }

  KeyCodeCombination getKeyCodeMessageSend()
  {
    return keyCodeMessageSend;
  }

  KeyCodeCombination getKeyCodeOpenManual()
  {
    return keyCodeOpenManual;
  }

  KeyCodeCombination getKeyCodeActiveClients()
  {
    return keyCodeActiveClients;
  }

  //  KeyCodeCombination getKeyCodeLineBreak()
  //  {
  //    return keyCodeLineBreak;
  //  }

  /**
   * Wandelt die Strings "false" und "true" (ignorecase) in booleans um und nimmt einen Boolean als Ersatz an,
   * der stattdessen verwendet wird, falls es keiner dieser Strings ist
   *
   * @param properties die Properties in welchen sich der Key befindet
   * @param keyName der Name des Keys
   * @param instead Ersatz Boolean
   */
  boolean getBoolean( final Properties properties, final String keyName, final boolean instead )
  {
    final String variable = properties.getProperty( keyName, "invalid" );
    if ( variable.equalsIgnoreCase( "true" ) )
    {
      return true;
    }
    else if ( variable.equalsIgnoreCase( "false" ) )
    {
      return false;
    }
    return instead;
  }

  void setNotDisturb( final boolean notDisturbToSet )
  {
    notDisturb = notDisturbToSet;
  }

  void setShareFonts( final boolean shareFontsToSet )
  {
    shareFonts = shareFontsToSet;
  }

  void setShareFontsAtBeginning( final boolean shareFontsAtBeginningToSet )
  {
    shareFontsAtBeginning = shareFontsAtBeginningToSet;
  }

  boolean isShareFonts()
  {
    return shareFonts;
  }

  boolean isShareFontsAtBeginning()
  {
    return shareFontsAtBeginning;
  }

  /**
   * Erstellt die Konfigurationsdatei und setzt die standard Einstellungen
   */
  void createSettings()
  {
    final Properties toSave = new Properties();
    try (OutputStreamWriter output = new OutputStreamWriter( new FileOutputStream( CONFIG_FILE, false ), "UTF-8" );)
    {
      toSave.setProperty( "theme", "default" );
      toSave.setProperty( "chattheme", "default" );
      toSave.setProperty( "ip", "default" );
      toSave.setProperty( "notdisturb", "false" );
      toSave.setProperty( "serverport", "default" );
      toSave.setProperty( "discoveryport", "default" );
      toSave.setProperty( "fonttransferport", "default" );
      toSave.setProperty( "announcenewmsg", "true" );
      toSave.setProperty( "sharefonts", "false" );
      toSave.setProperty( "sharefontsatbeginning", "false" );
      toSave.setProperty( "maxmessages", "200" );
      toSave.setProperty( "keycodemessagesend", "Enter" );
      toSave.setProperty( "keycodeopenmanual", "F1" );
      toSave.setProperty( "keycodeactiveclients", "F2" );
      toSave.setProperty( "name", "" );
      toSave.store( output,
          "Löschen / Bearbeiten der Datei auf eigene Gefahr." + System.lineSeparator()
              + "announcenewmsg kann nur auf 'true' oder 'false' gesetzt werden." + System.lineSeparator()
              + "Mögliche Themes sind: Default und Dark" + System.lineSeparator() );
      DasChatInit.logger.info( "'config.cfg' wurde in 'Nutzerverzeichniss\\.DasChat\\' angelegt" );
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( "'config.cfg' konnte nicht angelegt werden" );
      DasChatInit.logger.info( e.getMessage() );
    }
  }

  /**
   * Speichert die Einstellungen
   */
  void saveSettings()
  {
    final Properties toSave = new Properties();
    try (OutputStreamWriter output = new OutputStreamWriter( new FileOutputStream( CONFIG_FILE + ".new", true ),
        "UTF-8" ); InputStreamReader input = new InputStreamReader( new FileInputStream( CONFIG_FILE ), "UTF-8" );)
    {
      final Properties toLoad = new Properties();
      toLoad.load( input );
      toSave.setProperty( "theme", getTheme() );
      toSave.setProperty( "chattheme", getChatTheme() );
      toSave.setProperty( "ip", toLoad.getProperty( "ip" ) );
      toSave.setProperty( "notdisturb", "" + isNotDisturb() );
      toSave.setProperty( "serverport", toLoad.getProperty( "serverport" ) );
      toSave.setProperty( "discoveryport", toLoad.getProperty( "discoveryport" ) );
      toSave.setProperty( "fonttransferport", toLoad.getProperty( "fonttransferport" ) );
      toSave.setProperty( "announcenewmsg", toLoad.getProperty( "announcenewmsg" ) );
      toSave.setProperty( "sharefonts", "" + isShareFonts() );
      toSave.setProperty( "sharefontsatbeginning", "" + isShareFontsAtBeginning() );
      toSave.setProperty( "maxmessages", "" + getMaxMessages() );
      try
      {
        toSave.setProperty( "keycodemessagesend", getKeyCodeMessageSend().toString() );
      }
      catch ( final NullPointerException e )
      {
        // Do nothing and keep it as it is
      }
      try
      {
        toSave.setProperty( "keycodeactiveclients", getKeyCodeActiveClients().toString() );
      }
      catch ( final NullPointerException e )
      {
        // Do nothing and keep it as it is
      }
      try
      {
        toSave.setProperty( "keycodeopenmanual", getKeyCodeOpenManual().toString() );
      }
      catch ( final NullPointerException e )
      {
        // Do nothing and keep it as it is
      }
      toSave.setProperty( "name", getClientName() );
      toSave.store( output,
          "Löschen / Bearbeiten der Datei auf eigene Gefahr." + System.lineSeparator()
              + "announcenewmsg kann nur auf 'true' oder 'false' gesetzt werden" + System.lineSeparator()
              + "Mögliche Themes sind: Default" );
      DasChatInit.logger.info( "Einstellungen wurden gespeichert." );
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( "Einstellungen konnten nicht gespeichert werden." );
      DasChatInit.logger.info( e.getMessage() );
    }
    try
    {
      Files.copy( Paths.get( CONFIG_FILE + ".new" ), Paths.get( CONFIG_FILE ), StandardCopyOption.REPLACE_EXISTING );
      new File( CONFIG_FILE + ".new" ).delete();
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.info( "Einstellungen konnten nicht gespeichert werden." );
      DasChatInit.logger.info( e.getMessage() );
    }
  }

  /**
   * Setzt den Name des Clients, wird grundsätzlich nur beim Start des Programms aufgerufen.
   *
   * @param name der zukünftige Client Name
   */
  void setClientName( final String name )
  {
    clientName = name;
  }

  /**
   * Gibt den Name des Clients zurück (Benutzername)
   *
   * @return clientName
   */
  String getClientName()
  {
    return clientName;
  }

  /**
   * Getter für die Server IP
   *
   * @return serverIP
   */
  InetAddress getServerIP()
  {
    return serverIP;
  }

  /**
   * Setter für die IP
   *
   * @param ipToSet IP welche gesetzt werden soll
   */
  void setServerIP( final String ipToSet )
  {
    if ( ipToSet.equalsIgnoreCase( "default" ) )
    {
      serverIP = null;
    }
    else
    {
      try
      {
        serverIP = InetAddress.getByName( ipToSet );
      }
      catch ( final UnknownHostException e )
      {
        DasChatInit.logger.info( e.getMessage() );
      }
    }
  }

  /**
   * Setter für die IP
   *
   * @param ipToSet IP welche gesetzt werden soll
   */
  void setServerIP( final InetAddress ipToSet )
  {
    try
    {
      serverIP = InetAddress.getByAddress( ipToSet.getAddress() );
    }
    catch ( final UnknownHostException | NullPointerException e )
    {
      serverIP = null;
    }
  }

  /**
   * @return serverPort
   */
  int getServerPort()
  {
    return serverPort;
  }

  /**
   * Setter für den Server Port
   *
   * @param serverPortToSet Port welcher gesetzt wird
   */
  void setServerPort( final int serverPortToSet )
  {
    serverPort = serverPortToSet;
  }

  /**
   * @return discoveryPort
   */
  int getDiscoveryPort()
  {
    return discoveryPort;
  }

  /**
   * Setter für den Discovery Port
   *
   * @param discoveryPortToSet Port welcher gesetzt wird
   */
  void setDiscoveryPort( final int discoveryPortToSet )
  {
    discoveryPort = discoveryPortToSet;
  }

  /**
   * @return fontTransferPort
   */
  int getFontTransferPort()
  {
    return fontTransferPort;
  }

  /**
   * Setter für den Font Transfer port
   *
   * @param fontTransferPortToSet Port welcher gesetzt wird
   */
  void setFontTransferPort( final int fontTransferPortToSet )
  {
    fontTransferPort = fontTransferPortToSet;
  }

  /**
   * @return den String aus der Config der hinter "ip=" steht
   */
  String getIpSetting()
  {
    return ipSetting;
  }

  /**
   * Setzt den String welcher in den Settings hinter "ip" steht
   *
   * @param ipSettingToSet String der gesetzt wird
   */
  void setIpSetting( final String ipSettingToSet )
  {
    ipSetting = ipSettingToSet;
  }

  /**
   * @return FXML Controller
   */
  FXMLController getController()
  {
    return controller;
  }

  /**
   * @param fxmlController FXML Controller
   */
  void setController( final FXMLController fxmlController )
  {
    controller = fxmlController;
  }

  /**
   * Pfad von .DasChat (Hauptordner)
   *
   * @return MAIN_PATH
   */
  String getMainFolder()
  {
    return MAIN_FOLDER;
  }

  /**
   * Pfad der Fonts
   *
   * @return FONT_PATH
   */
  String getFontFolder()
  {
    return FONT_FOLDER;
  }

  /**
   * Pfad des Logs
   *
   * @return {@link Settings#LOG_FILE}
   */
  String getLogFile()
  {
    return LOG_FILE;
  }

  /**
   * @return cssFileName
   */
  String getFontCssFileName()
  {
    return fontCssFileName;
  }

  /**
   *
   * @param fontCssFileNameToSet name der zu setzen ist (in der regel webviewstyle.css oder webviewstyle2.css)
   */
  void setFontCssFileName( final String fontCssFileNameToSet )
  {
    fontCssFileName = fontCssFileNameToSet;
  }

  String getDasChatColor()
  {
    return DASCHAT_COLOR;
  }
}
