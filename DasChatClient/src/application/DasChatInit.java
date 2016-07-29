package application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import shared.GeneralSharedInformation;

/**
 * Initialisiert alle relevanten Daten.
 *
 * @author msc
 *
 */
public class DasChatInit
{
  static Logger            logger;

  private final Settings   settings                  = Settings.getSettings();

  final String             VERSION                   = GeneralSharedInformation.VERSION;

  private FileOutputStream keepHandle;

  private FileLock         fileLock;

  private final boolean    SHUTDOWNONDUALCLIENT      = true;

  private boolean          secondClientIsOpenAlready = false;

  /**
   * Initialisierungsmethode der Applikation
   *
   * @param args übergebene startparameter
   */
  public void init( final String[] args )
  {
    initializeLogger();

    shutdownSecondClient();

    if ( !secondClientIsOpenAlready )
    {
      checkMainFolder();

      checkSettings();

      initCssFontFile();

      copyKeyCombinationsBlacklistToMainFolder();

      initBlacklistedKeyCombinations();

      checkFontFolder();

      new DasChat().run( args );
    }
  }

  /**
   * Kopiert die Blacklist für Tastenkombinationen in das DasChat Nutzerverzeichniss.
   */
  private void copyKeyCombinationsBlacklistToMainFolder()
  {
    try
    {
      Files.copy( this.getClass().getResourceAsStream( "blacklistedkeycombinations.txt" ),
          Paths.get( settings.getMainFolder() + "blacklistedkeycombinations.txt" ) );
    }
    catch ( final IOException exception )
    {
      if ( exception instanceof FileAlreadyExistsException )
      {
        DasChatInit.logger.info( "Die Datei für die nicht nutzbaren Tastenkombinationen existiert bereits." );
      }
      else
      {
        DasChatInit.logger.warning( exception.fillInStackTrace().toString() );
        exception.printStackTrace();
      }
    }
  }

  /**
   * Schließt wenn der boolean {@link #SHUTDOWNONDUALCLIENT} auf true ist den Client wieder falls schon einer
   * offen ist, erkannt wird dies durch einen File Lock.
   */
  @SuppressWarnings( "unused" )
  private void shutdownSecondClient()
  {
    if ( SHUTDOWNONDUALCLIENT )
    {
      final File keepHandleOn = new File( settings.getMainFolder() + "handle.keep" );
      if ( keepHandleOn.exists() )
      {
        try
        {
          keepHandle = new FileOutputStream( keepHandleOn );
          fileLock = keepHandle.getChannel().tryLock();
          fileLock.isShared();
        }
        catch ( final Exception e )
        {
          logger.info( "Das Chat ist bereits geöffnet." );
          secondClientIsOpenAlready = true;
          //HACK JFXPanel wird zum initialisieren des FXThreads genutzt
          new JFXPanel();
          Platform.runLater( () ->
          {
            final Alert openAlrdy = new Alert( AlertType.INFORMATION );
            openAlrdy.setHeaderText( "Fehler beim Öffnen von DasChat" );
            openAlrdy.setContentText( "DasChat konnte nicht geöffnet werden da bereits eine DasChat Instanz geöffnet ist." );
            openAlrdy.showAndWait();
            Runtime.getRuntime().exit( 0 );
          } );
        }
      }
      else
      {
        try
        {
          keepHandleOn.createNewFile();
          keepHandle = new FileOutputStream( keepHandleOn );
          fileLock = keepHandle.getChannel().tryLock();
        }
        catch ( final IOException e )
        {
          logger.info( e.fillInStackTrace().toString() );
        }
      }
    }
  }

  /**
   * Erstellt gegebenfalls den Ordner in dem die Fonts abgespeichert werden.
   */
  private void checkFontFolder()
  {
    final File fontFolder = new File( settings.getFontFolder().substring( 0, settings.getFontFolder().length() - 1 ) );
    if ( fontFolder.exists() )
    {
      logger.info( "Ordner '" + settings.getFontFolder().substring( 0, settings.getFontFolder().length() - 1 ) + "' wurde gefunden." );
    }
    else
    {
      fontFolder.mkdir();
      logger.info( "Ordner '" + settings.getFontFolder().substring( 0, settings.getFontFolder().length() - 1 ) + "' wurde erstellt" );
    }
  }

  /**
   * Lädt falls eine Datei mit blacklisted Tastenkombinationen vorhanden ist die diese.
   */
  private void initBlacklistedKeyCombinations()
  {
    try
    {
      final List<String> tempLines = Files.readAllLines( Paths.get( settings.getMainFolder() + "blacklistedkeycombinations.txt" ) );
      for ( final String line : tempLines )
      {
        try
        {
          final List<Modifier> list = new ArrayList<>();
          final String keyCodeComb = line;
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
          final List<String> keys = Arrays.asList( keyCodeComb.split( "[+]" ) );
          final String key = keys.get( keys.size() - 1 );
          if ( list.contains( key ) )
          {
            list.remove( key );
          }
          final KeyCodeCombination toAdd = new KeyCodeCombination( KeyCode.getKeyCode( key ), list.toArray( new Modifier[0] ) );
          if ( !settings.getBlacklistedKeyCombinations().contains( toAdd ) )
          {
            settings.getBlacklistedKeyCombinations().add( toAdd );
          }
        }
        catch ( final IllegalArgumentException e )
        {
          logger.warning( line + " konnte nicht zur KeyCombination Blacklist hinzugefügt werden." );
        }
      }
    }
    catch ( final IOException e )
    {
      logger.warning( "Die Tastenkombinationen Blacklist konnte nicht gefunden werden." );
    }
  }

  /**
   * Lädt die Eisntellungen und erstellt gegebenfalls die Config.
   */
  private void checkSettings()
  {
    if ( !settings.loadSettings() )
    {
      settings.createSettings();
      settings.loadSettings();
    }
  }

  /**
   * Da die WebView nicht erneut das gleiche Css File ladden kann gibt es 2 verschiedene die existieren
   * können. Überprüft welche der beiden Dateien momentan existiert (fonts2.css | fonts.css) und lädt
   * anschließend alle Zeilen in eine Liste.
   */
  private void initCssFontFile()
  {
    try
    {
      final File cssFile = new File( settings.getMainFolder() + "fonts.css" );
      final File cssFile2 = new File( settings.getMainFolder() + "fonts2.css" );
      if ( !cssFile.exists() )
      {
        if ( !cssFile2.exists() )
        {
          cssFile.createNewFile();
        }
        else
        {
          settings.setFontCssFileName( "fonts2.css" );
        }
      }
    }
    catch ( final IOException e )
    {
      logger.warning( e.fillInStackTrace().toString() );
    }

    try
    {
      settings.getCssLineList()
          .addAll( Files.readAllLines( Paths.get( settings.getMainFolder() + settings.getFontCssFileName() ) ) );
      //Leere Zeile löschen, warum auch immer diese vorhanden ist
      if ( settings.getCssLineList().contains( "" ) )
      {
        settings.getCssLineList().remove( "" );
      }
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Initialisiert den Logger.
   */
  private void initializeLogger()
  {
    try
    {
      logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
      logger.setLevel( Level.FINEST );
      final FileHandler logFile = new FileHandler( settings.getLogFile(), true );
      final LogFormatter formatterTxt = new LogFormatter();

      logFile.setFormatter( formatterTxt );
      logger.addHandler( logFile );
    }
    catch ( final SecurityException | IOException e )
    {
      System.out.println( "Der Logger konnte nicht initialisiert werden." );
      System.out.println( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Überprüft ob das Hauptverzeichniss vorhanden ist, und erstellt dieses gegebenfalls.
   */
  private void checkMainFolder()
  {
    final File mainFolder = new File( settings.getMainFolder().substring( 0, settings.getMainFolder().length() - 1 ) );
    if ( !mainFolder.exists() )
    {
      mainFolder.mkdir();
    }
  }
}
