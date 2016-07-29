package application;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.HyperlinkEvent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codefx.libfx.control.webview.WebViewHyperlinkListener;
import org.codefx.libfx.control.webview.WebViews;
import org.controlsfx.control.CheckListView;
import org.jsoup.Jsoup;

import com.sun.javafx.font.PrismFontLoader;
import com.sun.javafx.scene.web.skin.HTMLEditorSkin;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import shared.GeneralSharedInformation;
import tray.animations.AnimationType;
import tray.notification.NotificationType;
import tray.notification.TrayNotification;
import util.Util;

/**
 * @author msc
 */
@SuppressWarnings( "restriction" )
public class FXMLController
{
  /**
   * Referenz auf die Settings Instanz
   */
  private final Settings                settings           = Settings.getSettings();

  // LoginDialog.fxml
  @FXML
  private Button                        confirm;

  @FXML
  private TextField                     usernameField;

  @FXML
  private CheckMenuItem                 notDisturbCheck;

  @FXML
  WebView                               webView;

  //Client.fxml

  @FXML
  private Button                        sendButton;

  /**
   * HTMLEditor der zum schreiben von Nachrichten genutzt wird.
   */
  @FXML
  HTMLEditor                            editor;

  @FXML
  private WebView                       beitragsWebView;

  @FXML
  Stage                                 loginDialogStage;

  @FXML
  private VBox                          rightVBox;

  @FXML
  private VBox                          tagBox;

  @FXML
  private HBox                          activeTagsPane;

  @FXML
  private ScrollPane                    scrollPaneTags;

  Stage                                 primaryStage;

  private MouseEvent                    lastEvent;

  private String                        feed               = "";

  private String                        incommingName      = "";

  private final DasChat                 dasChat;

  private boolean                       notSending         = true;

  private int                           fontExistantInt    = -1;

  private Thread                        listenThread;
  private Thread                        fontThread;

  private Socket                        fontFileSocket;
  private DataInputStream               receiveStream;
  private DataOutputStream              sendStream;

  // settings.fxml
  private Stage                         settingsStage;

  @FXML
  private CheckBox                      buttonFonts;

  @FXML
  private CheckBox                      buttonFontsAtBeginning;

  @FXML
  private ListView<String>              fontListView;

  @FXML
  private ListView<String>              fontListViewIgnored;

  @FXML
  private TabPane                       settingsTabPane;

  @FXML
  private Tab                           fontTab;

  @FXML
  private CheckBox                      dontDisturbCheckBox;

  @FXML
  private NumericTextField              textFieldMaxMessages;

  @FXML
  private Button                        keyLayoutButtonMessageSend;

  @FXML
  private Button                        keyLayoutButtonOpenManual;

  @FXML
  private Button                        keyLayoutButtonActiveClients;

  @FXML
  private WebView                       cssPreviewWebView;

  @FXML
  private ComboBox<String>              cssPatternComboBox;

  // @FXML
  // private Button keyLayoutButtonLineBreak;

  private KeyCodeCombination            tempKeyCodeOpenManual;
  private KeyCodeCombination            tempKeyCodeMessageSend;
  private KeyCodeCombination            tempKeyCodeActiveClients;
  // private KeyCodeCombination tempKeyCodeLineBreak;

  private Stage                         keyInputStage;

  private boolean                       disconnected       = false;

  // AboutDasChat.fxml
  @FXML
  private Label                         versionLabel;

  private Stage                         dasChatAboutStage;

  // Manual.fxml
  @FXML
  private ListView<String>              manualListView;

  @FXML
  private TextField                     searchField;

  @FXML
  private WebView                       manualTextArea;

  private final HashMap<String, String> manualTopics       = new HashMap<>();

  private Stage                         manualStage;

  // FilterSet.fxml
  private Stage                         filterStage;

  @FXML
  private CheckListView<String>         availableFiltersCheckListView;

  @FXML
  private TextField                     filterField;

  @FXML
  private CheckBox                      showUntaggedCheckBox;

  private ContextMenu                   hashTagContextMenu = null;

  /**
   * Verbindet sich mit dem Font Transfer Socket des Servers und nimmt die Referenz der DasChat Instanz
   * entgegen
   *
   * @param dasChat Da DasChat keine statische Klasse ist wird im Konstruktor die referenz übergeben
   */
  public FXMLController( final DasChat dasChat )
  {
    this.dasChat = dasChat;
    try
    {
      fontFileSocket = new Socket( settings.getServerIP(), settings.getFontTransferPort() );
      receiveStream = new DataInputStream( fontFileSocket.getInputStream() );
      sendStream = new DataOutputStream( fontFileSocket.getOutputStream() );
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }

  }

  /**
   * Hört dem Server zu um Nachrichten(Befehle) von den Benutzern zu empfangen
   */
  void listenToIncommingMessages()
  {
    Platform.runLater( () ->
    {
      editor.setDisable( false );
    } );
    final Runnable run = () ->
    {
      try
      {
        askServerForHashTags();
        String input;
        while ( (input = dasChat.receiveStringFromServer()) != null )
        {
          if ( input.substring( 0, 4 ).equals( "hash" ) )
          {
            initializeHashTags( input );
            break;
          }
        }
        dasChat.sendStringToServer( "request_feed_now" );
      }
      catch ( final Exception e )
      {
        DasChatInit.logger.info( e.fillInStackTrace().toString() );
      }
      String input;
      while ( (input = dasChat.receiveStringFromServer()) != null )
      {
        try
        {
          if ( input.contains( "requested_feed:" ) )
          {
            useRequestedFeed( input );
          }
          else if ( input.substring( 0, 9 ).equals( "hashtags:" ) )
          {
            initializeHashTags( input );
          }
          else if ( input.substring( 0, 12 ).equals( "new_article:" ) )
          {
            input = input.replace( "new_article:", "" )
                .replace( "<div class='bywho'><div class='bywhoin'>" + settings.getClientName() + "<",
                    "<div class='byme'><div class='bywhoin'>" + settings.getClientName() + "<" )
                .replace( "<user=" + settings.getClientName() + "><div class='messageblob'>",
                    "<user=" + settings.getClientName() + "><div class='messageblobme'>" );
            final Pattern userPattern = Pattern.compile( "<user=(.*?)>" );
            final Matcher userMatcher = userPattern.matcher( input );
            while ( userMatcher.find() )
            {
              incommingName = userMatcher.group( 1 );
              incommingName = incommingName.replace( "<user=", "" ).substring( 0, incommingName.length() );
            }

            input = input.replaceAll( "\\r|\\n", "" );
            final String tempFeed = input + feed;
            feed = "";
            final Pattern msgPattern = Pattern.compile( "<msg>(.*?)</msg>" );
            final Matcher msgMatcher = msgPattern.matcher( tempFeed );
            int numberOfMessages = 0;
            while ( msgMatcher.find() )
            {
              if ( numberOfMessages != settings.getMaxMessages() )
              {
                String message = msgMatcher.group( 0 );
                final Pattern datePattern = Pattern.compile( "<date (.*?)></date>" );
                final Matcher dateMatcher = datePattern.matcher( message );
                while ( dateMatcher.find() )
                {
                  final String date = dateMatcher.group();
                  final String replaceWith = "<div class='day'>"
                      + date.replace( "<date ", "" ).replace( "></date>", "" ) + "</div></div><msg>";
                  if ( !feed.contains( replaceWith ) )
                  {
                    message = message.replace( "<msg>", replaceWith );
                  }
                }
                if ( availableFiltersCheckListView.getCheckModel().getCheckedItems().size() != 0 )
                {
                  if ( getHashTags( message ).isEmpty() )
                  {
                    if ( showUntaggedCheckBox.isSelected() )
                    {
                      feed = feed + message;
                      numberOfMessages++;
                    }
                  }
                  else
                  {
                    for ( final String hashTag : getHashTags( message ) )
                    {
                      if ( availableFiltersCheckListView.getCheckModel().getCheckedItems()
                          .contains( hashTag ) )
                      {
                        if ( !settings.availableMessageFilters.contains( hashTag ) )
                        {
                          settings.availableMessageFilters.add( hashTag );
                        }
                        feed = feed + message;
                        numberOfMessages++;
                        break;
                      }
                    }
                  }
                }
                else
                {
                  feed = feed + message;
                  numberOfMessages++;
                }
              }
              else
              {
                break;
              }
            }

            final String toFilter = input;
            Platform.runLater( () ->
            {

              if ( settings.isShareFonts() )
              {
                final Timer timer = new Timer();
                timer.schedule( new TimerTask()
                {
                  @Override
                  public void run()
                  {
                    try
                    {
                      downloadFonts( toFilter );
                      refreshWebView();
                    }
                    catch ( final Exception e )
                    {
                      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
                    }
                  }
                }, 450 );
              }
              else
              {
                refreshWebView();
              }
              announceMessage( incommingName );
              if ( incommingName.equals( settings.getClientName() ) )
              {
                final Timer timer = new Timer();
                timer.schedule( new TimerTask()
                {
                  @Override
                  public void run()
                  {
                    Platform.runLater( () ->
                    {
                      editorWebViewRequestFocus();
                    } );
                  }
                }, 100 );
              }

            } );
          }
          else if ( input.substring( 0, 15 ).equals( "active_clients:" ) )
          {
            input = input.replace( "active_clients:", "" );
            final String clients = input.replace( "|SPLIT|", System.lineSeparator() );
            Platform.runLater( () ->
            {
              final Alert alert = new Alert( AlertType.INFORMATION );
              alert.initModality( Modality.APPLICATION_MODAL );
              alert.initOwner( primaryStage );
              alert.getDialogPane().getStylesheets().add( getClass()
                  .getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
              alert.setTitle( "Verbundene Clients" );
              alert.setHeaderText( "Verbundene Clients" );
              alert.setContentText( clients );
              alert.show();
            } );
          }
          else if ( input.substring( 0, 18 ).equals( "version_diffrent:" ) )
          {
            alertUser( AlertType.ERROR, "Versionswarnung", "Dein Client ist veraltet",
                "Die Version deines Clients stimmt nicht mit der des Servers überein, bitte wende dich an einen Administrator.",
                false );
          }
        }
        catch ( final Exception e )
        {
          DasChatInit.logger.warning( e.fillInStackTrace().toString() );
          break;
        }
      }
      try
      {
        dasChat.getSocket().close();
      }
      catch ( final Exception e )
      {
        DasChatInit.logger.info( e.fillInStackTrace().toString() );
      }
      Platform.runLater( () ->
      {
        alertUser( AlertType.ERROR, "Verbindung zum Server verloren", "Verbindung zum Server verloren!",
            "Es besteht keine Verbindung mehr zum Server, aber Sie können trotzdem noch bereits gesendete Nachrichten lesen. Überprüfen sie Ihre Netzwerkverbindung, stellen sie sicher das der Server läuft und überprüfen sie die Netzwerkverbindung des Servers.",
            true );
        primaryStage.setTitle( "DasChat - Nicht verbunden" );
        editor.setDisable( true );
        sendButton.setDisable( true );
        tryToReconnect();
      } );
    };
    listenThread = new Thread( run );
    listenThread.start();
  }

  public static int getActiveStageLocation( final Scene scene )
  {
    final List interScreens = Screen.getScreensForRectangle( scene.getWindow().getX(),
        scene.getWindow().getY(),
        scene.getWindow().getWidth(),
        scene.getWindow().getHeight() );
    final Screen activeScreen = (Screen) interScreens.get( 0 );
    final Rectangle2D r = activeScreen.getBounds();
    final double position = r.getMinX();
    return (int) position;
  }

  private void useRequestedFeed( final String inputOriginal )
  {
    feed = "";
    final String inputEdit = inputOriginal.replace( "requested_feed:", "" )
        .replace( "<div class='bywho'><div class='bywhoin'>" + settings.getClientName() + "<",
            "<div class='byme'><div class='bywhoin'>" + settings.getClientName() + "<" )
        .replace( "<user=" + settings.getClientName() + "><div class='messageblob'>",
            "<user=" + settings.getClientName() + "><div class='messageblobme'>" );
    final Pattern userPattern = Pattern.compile( "<msg>(.*?)</msg>" );
    final Matcher userMatcher = userPattern.matcher( inputEdit );
    int numberOfMessages = 0;
    while ( userMatcher.find() )
    {
      if ( numberOfMessages != settings.getMaxMessages() )
      {
        String message = userMatcher.group( 0 );
        final Pattern datePattern = Pattern.compile( "<date (.*?)></date>" );
        final Matcher dateMatcher = datePattern.matcher( message );
        while ( dateMatcher.find() )
        {
          final String date = dateMatcher.group();
          final String replaceWith = "<div class='day'>"
              + date.replace( "<date ", "" ).replace( "></date>", "" ) + "</div><msg>";
          if ( !feed.contains( replaceWith ) )
          {
            message = message.replace( "<msg>", replaceWith );
          }
        }
        if ( availableFiltersCheckListView.getCheckModel().getCheckedItems().size() != 0 )
        {
          if ( getHashTags( message ).isEmpty() )
          {
            if ( showUntaggedCheckBox.isSelected() )
            {
              feed = feed + message;
              numberOfMessages++;
            }
          }
          else
          {
            for ( final String hashTag : getHashTags( message ) )
            {
              if ( availableFiltersCheckListView.getCheckModel().getCheckedItems()
                  .contains( hashTag ) )
              {
                if ( !settings.availableMessageFilters.contains( hashTag ) )
                {
                  settings.availableMessageFilters.add( hashTag );
                }
                feed = feed + message;
                numberOfMessages++;
                break;
              }
            }
          }
        }
        else
        {
          feed = feed + message;
          numberOfMessages++;
        }
      }
    }
    try
    {
      if ( settings.isShareFonts() && settings.isShareFontsAtBeginning() )
      {
        downloadFonts( feed );
        refreshWebView();
      }
      else
      {
        refreshWebView();
      }
      askServerForHashTags();
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Verarbeitet die vom Server gesendete HashTag Liste und lädt diese in eine Liste.
   *
   * @param hashTagsInput vom Server empfangene HashTags
   */
  private void initializeHashTags( final String hashTagsInput )
  {
    final String inputEdit = hashTagsInput.replace( "hashtags:", "" );
    if ( !inputEdit.equals( "thehashtagsareempty" ) )
    {
      final List<String> hashTags = Arrays.asList( inputEdit.split( "[|]" ) );
      for ( final String hashTag : hashTags )
      {
        if ( !(Objects.isNull( hashTag ) || hashTag.equals( "" )) )
        {
          if ( !settings.availableMessageFilters.contains( hashTag ) )
          {
            settings.availableMessageFilters.add( hashTag );
          }
        }
      }
    }
  }

  /**
   * Fragt den Server nach der Liste der aktuell existenten HashTags.
   */
  private void askServerForHashTags()
  {
    dasChat.sendStringToServer( "request_hashtags" );
  }

  /**
   * Download die in den Nachrichten verwendeten Fonts vom Server sofern vorhanden.
   *
   * @param inputString zu überprüfende Nachricht
   * @throws IOException
   */
  void downloadFonts( final String inputString ) throws IOException
  {
    boolean changes = false;
    String input = inputString;
    final Pattern fontPattern = Pattern.compile( "face=\"(.*?)\"" );
    final Matcher matcher = fontPattern.matcher( input );
    final List<String> fonts = new ArrayList<>();
    while ( matcher.find() )
    {
      final String fontResult = matcher.group( 1 );
      if ( !fonts.contains( fontResult ) )
      {
        fonts.add( fontResult );
      }
    }
    for ( final String font : fonts )
    {
      if ( !dasChat.getFonts().contains( font ) && !dasChat.observableFontListIgnored.contains( font ) )
      {
        byte[] bytesToSend = new String( "font_exists:" + font ).getBytes();
        bytesToSend = Util.compress( bytesToSend );
        sendStream.writeInt( bytesToSend.length );
        sendStream.write( bytesToSend );

        int length = receiveStream.readInt();
        if ( length != 0 )
        {
          byte[] message = new byte[length];
          receiveStream.readFully( message, 0, length );
          message = Util.decompress( message );
          input = new String( message );

          if ( input.equals( "font_notexists" ) )
          {
            fontExistantInt = 0;
            DasChatInit.logger.info( "Font " + font + " existiert nicht." );
          }
          else if ( input.equals( "font_exists" ) )
          {
            fontExistantInt = 1;
            DasChatInit.logger.info( "Font " + font + " existiert und wird angefordert werden." );
          }
          while ( true )
          {
            if ( notSending )
            {
              if ( fontExistantInt == 1 )
              {
                bytesToSend = new String( "request_font:" + font + ".ttf" ).getBytes();
                bytesToSend = Util.compress( bytesToSend );
                sendStream.writeInt( bytesToSend.length );
                sendStream.write( bytesToSend );

                length = receiveStream.readInt();
                if ( length != 0 )
                {
                  message = new byte[length];
                  receiveStream.readFully( message, 0, length );
                  message = Util.decompress( message );
                  final String inputLine = new String( message );
                  if ( inputLine.contains( "sending_font:" ) )
                  {
                    length = receiveStream.readInt();
                    if ( length != 0 )
                    {
                      message = new byte[length];
                      receiveStream.readFully( message, 0, length );
                      message = Util.decompress( message );
                      final File localOutputFile = new File(
                          settings.getFontFolder() + font + ".ttf" );
                      try (FileOutputStream localFileOutput = new FileOutputStream(
                          localOutputFile );)
                      {
                        localFileOutput.write( message, 0, message.length );
                        localFileOutput.close();
                        dasChat.addToFontList( font );
                        fontExistantInt = -1;
                        DasChatInit.logger.info( "Font " + font + " wurde heruntergeladen." );

                        String fontStr = "@font-face { font-family: '" + font
                            + "'; src: url('file:///" + settings.getFontFolder() + font
                            + ".ttf') format('truetype'); }";

                        fontStr = fontStr.replace( "\\", "/" );

                        if ( !settings.getCssLineList().contains( fontStr ) )
                        {
                          try (BufferedWriter output = new BufferedWriter( new FileWriter(
                              settings.getMainFolder() + settings.getFontCssFileName(),
                              true ) );)
                          {
                            output.newLine();
                            output.write( fontStr );
                            output.flush();
                            settings.getCssLineList().add( fontStr );
                            changes = true;
                          }
                        }
                        try
                        {
                          final PrismFontLoader fontLoader = PrismFontLoader.getInstance();

                          try (FileInputStream fontStream = new FileInputStream(
                              localOutputFile );)
                          {
                            final Font fontToLoad = Font.loadFont( fontStream, 10 );
                            fontLoader.loadFont( fontToLoad );
                          }
                        }
                        catch ( final NullPointerException fontCouldntBeLoaded )
                        {
                          DasChatInit.logger.warning( "Font konnte nicht geladen werden." );
                        }
                        break;
                      }
                    }
                  }
                  else
                  {
                    DasChatInit.logger
                        .info( "Font " + font + " konnte nicht heruntergeladen werden." );
                    fontExistantInt = -1;
                    break;
                  }
                }
              }
              else if ( fontExistantInt == 0 )
              {
                break;
              }
            }
          }
        }
        else
        {
          //Platzhalter für etwas sinnvolleres
          DasChatInit.logger.warning( "SOMETHING WENT TERRIBLY WRONG!!!" );
        }
      }
      else
      {
        fontExistantInt = -1;
      }
    }

    if ( changes )
    {
      recreateCss();
    }
  }

  /**
   * Lädt die {@link #beitragsWebView} neu.
   */
  void refreshWebView()
  {
    Platform.runLater( () ->
    {
      final String fontCssPath = "file:///" + settings.getMainFolder().replace( "\\", "/" )
          + settings.getFontCssFileName();
      beitragsWebView.getEngine().load( "" );
      beitragsWebView.getEngine().reload();
      beitragsWebView.getEngine()
          .loadContent( "<html>" + "<head>" + "<meta charset='UTF-8'>" + "<link rel='stylesheet' href='"
              + fontCssPath + "'>" + "</head>" + "<body>" + feed + "</body>" + "</html>" );
      editor.setDisable( false );
      final Timer timer2 = new Timer();
      timer2.schedule( new TimerTask()
      {
        @Override
        public void run()
        {
          Platform.runLater( () ->
          {
            editorWebViewRequestFocus();
          } );
        }
      }, 400 );
    } );
  }

  void recreateCss()
  {
    Path source = null;
    Path newFile = null;
    if ( settings.getFontCssFileName().equals( "fonts.css" ) )
    {
      source = Paths.get( settings.getMainFolder() + "fonts.css" );
      newFile = Paths.get( settings.getMainFolder() + "fonts2.css" );
      settings.setFontCssFileName( "fonts2.css" );
    }
    else
    {
      source = Paths.get( settings.getMainFolder() + "fonts2.css" );
      newFile = Paths.get( settings.getMainFolder() + "fonts.css" );
      settings.setFontCssFileName( "fonts.css" );
    }
    try
    {
      Files.move( source, newFile, StandardCopyOption.REPLACE_EXISTING );
      new File( source.toUri() ).delete();
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Öffnet eine TrayNotification welche dem Nutzer mitteilt das er eine Nachricht bekommen hat.
   *
   * @param msgFrom Nutzer der die Nachricht gesendet hat
   */
  private void announceMessage( final String msgFrom )
  {
    if ( settings.isAnnounceNewMsg() )
    {
      if ( !primaryStage.focusedProperty().get() )
      {
        if ( !settings.isNotDisturb() )
        {
          if ( !msgFrom.equals( settings.getClientName() ) )
          {
            final String title = "Neue Nachricht";
            final String message = "Nachricht von " + msgFrom + System.lineSeparator()
                + "Zum Anzeigen klicken.";
            final NotificationType notification = NotificationType.CUSTOM;

            final TrayNotification tray = new TrayNotification();
            tray.setStylesheet( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
            tray.setTitle( title );
            tray.setAnimationType( AnimationType.POPUP );
            tray.setImage( new Image( getClass().getResource( "newmessage.png" ) + "" ) );
            tray.setRectangleFill( Paint.valueOf( settings.getDasChatColor() ) );
            tray.setMessage( message );
            final Runnable toFrontRunnable = () ->
            {
              primaryStage.toFront();
            };
            tray.setOnAction( toFrontRunnable );
            tray.setNotificationType( notification );
            tray.showAndDismiss( Duration.millis( 5000 ) );
          }
        }
      }
    }
  }

  @SuppressWarnings( "synthetic-access" )
  private void tryToReconnect()
  {
    listenThread.interrupt();

    final Thread thread = new Thread( () ->
    {
      final Timer timer = new Timer();
      final TimerTask task = new TimerTask()
      {
        @Override
        public void run()
        {
          if ( dasChat.tryToReconnect() )
          {
            Platform.runLater( () ->
            {
              editor.setDisable( false );
            } );
            listenToIncommingMessages();
            timer.cancel();
          }
        }
      };

      timer.schedule( task, new Date(), 3000 );
    } );
    thread.start();
  }

  /**
   * Um dem Controller mitzuteilen welche Stage er schließen muss beim betätigen des "Auf zu "DasChat""
   * Buttons Wird einmal in der startMethode aufgerufen und danach nie wieder.
   *
   * @param loginStage Die übergebene Login Dialog Stage
   */
  void setLoginDialogStage( final Stage loginStage )
  {
    loginDialogStage = loginStage;
  }

  /**
   * Überprüft ob Benutzername bereits in Nutzung ist oder nicht
   *
   * @param username Zu überprüfender Benutzername
   * @return Ja oder Nein (Vergeben oder nicht vergeben)
   */
  boolean askServerIfUsernameIsAvailable( final String username )
  {
    dasChat.sendStringToServer( "username=" + username );
    String answer = dasChat.receiveStringFromServer();

    if ( answer.equals( "username" + username + "=true" ) || answer.equals( "username" + username + "=false" ) )
    {
      answer = answer.replace( "username" + username + "=", "" );
      return answer.equals( "true" );
    }
    return askServerIfUsernameIsAvailable( username );
  }

  /**
   * Wechselt die Vorschau für das ChatTheme
   */
  @FXML
  private void changeCssPatternPreview()
  {
    final String chosenPattern = cssPatternComboBox.getSelectionModel().getSelectedItem();
    cssPreviewWebView.getEngine().setUserStyleSheetLocation(
        getClass().getResource( "chatthemes/" + chosenPattern.toLowerCase().replaceAll( "\\s", "" ) + ".css" )
            .toExternalForm() );
  }

  /**
   * Entfernt alle Filter
   */
  @FXML
  private void resetFilters()
  {
    toggleTagBox();
    if ( availableFiltersCheckListView.getCheckModel().getCheckedItems().size() != 0 )
    {
      availableFiltersCheckListView.getCheckModel().clearChecks();
      dasChat.sendStringToServer( "request_feed_now" );
    }
    showUntaggedCheckBox.setSelected( false );
  }

  /**
   * Selektiert alle Filter
   */
  @FXML
  private void selectAllFilters()
  {
    availableFiltersCheckListView.getCheckModel().checkAll();
  }

  /**
   * Deselektiert alle Filter
   */
  @FXML
  private void deselectAllFilters()
  {
    availableFiltersCheckListView.getCheckModel().clearChecks();
  }

  @FXML
  private void applyFilters()
  {
    dasChat.sendStringToServer( "request_feed_now" );
    activeTagsPane.getChildren().clear();
    for ( final String tag : availableFiltersCheckListView.getCheckModel().getCheckedItems() )
    {
      final Hyperlink tempHyperLink = new Hyperlink( tag );
      tempHyperLink.setOnAction( event ->
      {
        try
        {
          hashTagContextMenu.hide();
        }
        catch ( final Exception e )
        {
          //Alles gut :)
        }

        hashTagContextMenu = new ContextMenu();

        final MenuItem cmItem1 = new MenuItem( "Nur nach diesem Tag filtern" );
        final MenuItem cmItem2 = new MenuItem( "Diesen Tag aus den Filtern entfernen" );

        cmItem1.setOnAction( e ->
        {
          availableFiltersCheckListView.getCheckModel().clearChecks();
          availableFiltersCheckListView.getCheckModel().check( tag );
          applyFilters();
        } );

        cmItem2.setOnAction( e ->
        {
          availableFiltersCheckListView.getCheckModel().clearCheck( tag );
          applyFilters();
        } );

        hashTagContextMenu.getItems().add( cmItem1 );
        hashTagContextMenu.getItems().add( cmItem2 );

        final java.awt.Point p = java.awt.MouseInfo.getPointerInfo().getLocation();
        hashTagContextMenu.show( primaryStage, p.getX(), p.getY() );
      } );
      activeTagsPane.getChildren().add( tempHyperLink );
    }
    toggleTagBox();
  }

  private void toggleTagBox()
  {
    if ( activeTagsPane.getChildrenUnmodifiable().isEmpty() )
    {
      rightVBox.getChildren().remove( tagBox );
    }
    else
    {
      rightVBox.getChildren().add( 0, tagBox );
    }
  }

  @SuppressWarnings( "synthetic-access" )
  @FXML
  private void chooseMessageFilters()
  {
    try
    {
      filterStage.show();
      filterStage.toFront();

      final Thread filterDockThread = new Thread( () ->
      {
        final Timer timer = new Timer();
        final TimerTask task = new TimerTask()
        {
          @Override
          public void run()
          {
            if ( settings.isDockFilterStage() )
            {
              Platform.runLater( () ->
              {
                filterStage.setX( primaryStage.getX() + primaryStage.getScene().getWidth() );
                filterStage.setY( primaryStage.getY() );

              } );
            }
          }
        };

        timer.schedule( task, new Date(), 5 );
      } );
      filterDockThread.start();
    }
    catch ( final NullPointerException e )
    {
      buildFilterStage();
    }
  }

  private void buildFilterStage()
  {
    final FXMLLoader loader = new FXMLLoader();
    loader.setLocation( getClass().getResource( "FilterSet.fxml" ) );
    loader.setController( this );

    try
    {
      final Parent root = loader.load();
      final Scene scene = new Scene( root );
      scene.getStylesheets()
          .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
      filterStage = new Stage();
      filterStage.initOwner( primaryStage );
      filterStage.setScene( scene );
      filterStage.setTitle( "DasChat - Filter setzen" );
      filterStage.getIcons().add( new Image( Settings.class.getResourceAsStream( "icon.png" ) ) );
      scene.setOnKeyReleased( event ->
      {
        if ( event.isControlDown() && event.isShiftDown() && event.getCode().equals( KeyCode.D ) )
        {
          settings.setDockFilterStage( !settings.isDockFilterStage() );
          if ( !settings.isDockFilterStage() )
          {
            filterStage.centerOnScreen();
            filterStage.toFront();
          }
        }

      } );

      availableFiltersCheckListView.setOnKeyReleased( event ->
      {
        if ( !event.isControlDown() && !event.isShiftDown() && !event.isMetaDown() && !event.isAltDown()
            && event.getCode().equals( KeyCode.SPACE ) )
        {
          if ( availableFiltersCheckListView.getCheckModel()
              .isChecked( availableFiltersCheckListView.getSelectionModel().getSelectedItem() ) )
          {
            availableFiltersCheckListView.getCheckModel().clearCheck( availableFiltersCheckListView.getSelectionModel().getSelectedItem() );
          }
          else
          {
            availableFiltersCheckListView.getCheckModel().check( availableFiltersCheckListView.getSelectionModel().getSelectedItem() );
          }
        }
      } );

      Bindings.bindContent( availableFiltersCheckListView.getItems(), settings.sortedList );
      settings.sortedList.setComparator( Comparator.naturalOrder() );

      filterField.textProperty().addListener( obs ->
      {
        final String text = filterField.getText();

        // Sortiert nach dem Eingabetext (Groß-/Kleinschreibung ignorierend) , reverse sorgt dafür das die Sachen nach denen gesucht wird nicht unten angezeigt werden
        final Comparator<String> comparator =
            Comparator.<String, Boolean>comparing( item -> StringUtils.containsIgnoreCase( item, text ) )
                .reversed().thenComparing( Comparator.naturalOrder() );

        final List<String> checkedItems = new ArrayList<>(
            availableFiltersCheckListView.getCheckModel().getCheckedItems() );
        settings.sortedList.setComparator( comparator );
        availableFiltersCheckListView.getCheckModel().clearChecks();
        checkedItems.forEach( availableFiltersCheckListView.getCheckModel()::check );

      } );

    }
    catch ( final IOException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Schließt den login dialog und öffnet das fenster zum schreiben von Nachrichten
   */
  @FXML
  void loginButtonClicked()
  {
    final String username = usernameField.getText();

    if ( !username.trim().equals( "" ) )
    {
      if ( username.length() < 30 )
      {
        if ( askServerIfUsernameIsAvailable( username ) )
        {
          settings.setClientName( username );
          if ( settings.getTheme().equalsIgnoreCase( "Default" ) )
          {
            settings.saveSettings();
          }
          else
          {
            settings.saveSettings();
          }
          loginDialogStage.close();
          initClient();
        }
        else
        {
          alertUser( AlertType.ERROR, "Auf zu 'DasChat'", "Ungültiger Benutzername!",
              "Der Benutzername '" + username
                  + "' wird bereits von einem anderem Benutzer verwendet, bitte wähle einen Anderen noch freien Benutzername.",
              false );
        }
      }
      else
      {
        alertUser( AlertType.ERROR, "Auf zu 'DasChat'", "Ungültiger Benutzername!",
            "Du kannst keinen Benutzername haben welcher mehr als 30 Zeichen hat.", false );
      }
    }
    else
    {
      alertUser( AlertType.ERROR, "Auf zu 'DasChat'", "Ungültiger Benutzername!",
          "Du kannst keinen leeren Benutzername haben.", false );
    }
  }

  /**
   * Überprüft ob die übergebene KeyCombination auf der Blacklist steht.
   *
   * @param combination zu überprüfende KeyCombination
   * @return true falls sie auf der Blacklist steht und false falls nicht
   */
  private boolean checkKeyCodeCombination( final KeyCodeCombination combination )
  {
    if ( settings.getBlacklistedKeyCombinations().contains( combination ) )
    {
      return false;
    }
    return true;
  }

  private void changeStageShow( final String shortcutFor )
  {
    try
    {
      keyInputStage.show();
      setProperListener( shortcutFor );
    }
    catch ( final NullPointerException e )
    {
      keyInputStage = new Stage();
      final Pane root = new Pane();
      final Scene scene = new Scene( root );
      keyInputStage.initOwner( settingsStage );
      keyInputStage.setScene( scene );
      keyInputStage.initStyle( StageStyle.UNDECORATED );
      keyInputStage.initModality( Modality.APPLICATION_MODAL );
      root.getChildren().add( new Label( "Drücke die gewünschte Tastenkombination" ) );
      root.setStyle( "-fx-background-color: red; padding: 5 5 5 5;" );
      setProperListener( shortcutFor );
      keyInputStage.show();
    }
  }

  //  @FXML
  //  private void changeLineBreakKey()
  //  {
  //  }

  @FXML
  private void changeActiveClientsKey()
  {
    changeStageShow( "ActiveClients" );
  }

  @FXML
  private void changeMessageSendKey()
  {
    changeStageShow( "MessageSend" );
  }

  @FXML
  private void changeOpenManualKey()
  {
    changeStageShow( "OpenManual" );
  }

  private void setProperListener( final String shortcutFor )
  {
    if ( shortcutFor.equals( "ActiveClients" ) )
    {
      keyInputStage.getScene().setOnKeyReleased( keyRelease ->
      {
        final List<Modifier> list = new ArrayList<>();

        if ( keyRelease.isControlDown() )
        {
          list.add( KeyCombination.CONTROL_DOWN );
        }
        if ( keyRelease.isAltDown() )
        {
          list.add( KeyCombination.ALT_DOWN );
        }
        if ( keyRelease.isShiftDown() )
        {
          list.add( KeyCombination.SHIFT_DOWN );
        }

        final KeyCode code = keyRelease.getCode();
        if ( !code.equals( KeyCode.UNDEFINED ) && !code.equals( KeyCode.SHIFT ) && !code.equals( KeyCode.CONTROL )
            && !code.equals( KeyCode.ALT ) && !code.equals( KeyCode.META ) )
        {
          tempKeyCodeActiveClients = new KeyCodeCombination( keyRelease.getCode(),
              list.toArray( new Modifier[0] ) );

          if ( checkKeyCodeCombination( tempKeyCodeActiveClients ) )
          {
            keyLayoutButtonActiveClients.setText( tempKeyCodeActiveClients.toString() );
          }
          else
          {
            alertUser( AlertType.ERROR, "Tastenkombination setzen",
                "Tastenkombination konnte nicht gesetzt werden",
                "Die von dir verwendete Tastenkombination befindet sich auf der schwarzen Liste da sie Systemreserviert ist.",
                true );
          }
        }
        else
        {
          alertUser( AlertType.ERROR, "Tastenkombination setzen",
              "Tastenkombination konnte nicht gesetzt werden",
              "Eine der von dir benutzen Tasten ist nicht gültig.", true );
        }
        keyInputStage.hide();
      } );
    }
    else if ( shortcutFor.equals( "MessageSend" ) )
    {
      keyInputStage.getScene().setOnKeyReleased( keyRelease ->
      {
        final List<Modifier> list = new ArrayList<>();

        if ( keyRelease.isControlDown() )
        {
          list.add( KeyCombination.CONTROL_DOWN );
        }
        if ( keyRelease.isAltDown() )
        {
          list.add( KeyCombination.ALT_DOWN );
        }
        if ( keyRelease.isShiftDown() )
        {
          list.add( KeyCombination.SHIFT_DOWN );
        }

        final KeyCode code = keyRelease.getCode();
        if ( !code.equals( KeyCode.UNDEFINED ) && !code.equals( KeyCode.SHIFT ) && !code.equals( KeyCode.CONTROL )
            && !code.equals( KeyCode.ALT ) && !code.equals( KeyCode.META ) )
        {
          tempKeyCodeMessageSend = new KeyCodeCombination( keyRelease.getCode(),
              list.toArray( new Modifier[0] ) );

          if ( checkKeyCodeCombination( tempKeyCodeMessageSend ) )
          {
            keyLayoutButtonMessageSend.setText( tempKeyCodeMessageSend.toString() );
          }
          else
          {
            alertUser( AlertType.ERROR, "Tastenkombination setzen",
                "Tastenkombination konnte nicht gesetzt werden",
                "Die von dir verwendete Tastenkombination befindet sich auf der schwarzen Liste da sie Systemreserviert ist.",
                true );
          }
        }
        else
        {
          alertUser( AlertType.ERROR, "Tastenkombination setzen",
              "Tastenkombination konnte nicht gesetzt werden",
              "Eine der von dir benutzen Tasten ist nicht gültig.", true );
        }
        keyInputStage.hide();
      } );
    }
    else if ( shortcutFor.equals( "OpenManual" ) )
    {
      keyInputStage.getScene().setOnKeyReleased( keyRelease ->
      {
        final List<Modifier> list = new ArrayList<>();

        if ( keyRelease.isControlDown() )
        {
          list.add( KeyCombination.CONTROL_DOWN );
        }
        if ( keyRelease.isAltDown() )
        {
          list.add( KeyCombination.ALT_DOWN );
        }
        if ( keyRelease.isShiftDown() )
        {
          list.add( KeyCombination.SHIFT_DOWN );
        }

        final KeyCode code = keyRelease.getCode();
        if ( !code.equals( KeyCode.UNDEFINED ) && !code.equals( KeyCode.SHIFT ) && !code.equals( KeyCode.CONTROL )
            && !code.equals( KeyCode.ALT ) && !code.equals( KeyCode.META ) )
        {
          tempKeyCodeOpenManual = new KeyCodeCombination( keyRelease.getCode(), list.toArray( new Modifier[0] ) );

          if ( checkKeyCodeCombination( tempKeyCodeOpenManual ) )
          {
            keyLayoutButtonOpenManual.setText( tempKeyCodeOpenManual.toString() );
          }
          else
          {
            alertUser( AlertType.ERROR, "Tastenkombination setzen",
                "Tastenkombination konnte nicht gesetzt werden",
                "Die von dir verwendete Tastenkombination befindet sich auf der schwarzen Liste da sie Systemreserviert ist.",
                true );
          }
        }
        else
        {
          alertUser( AlertType.ERROR, "Tastenkombination setzen",
              "Tastenkombination konnte nicht gesetzt werden",
              "Eine der von dir benutzen Tasten ist nicht gültig.", true );
        }
        keyInputStage.hide();
      } );
    }
  }

  /**
   * Bereit alles vor um die vorgenommenen Einstellungen abzuspeichern.
   */
  @FXML
  private void prepareSavingSettings()
  {
    settings.setShareFonts( buttonFonts.isSelected() );
    settings.setShareFontsAtBeginning( buttonFontsAtBeginning.isSelected() );
    settings.setMaxMessages( Integer.parseInt( textFieldMaxMessages.getText() ) );
    settings.setNotDisturb( dontDisturbCheckBox.isSelected() );
    settings.setChatTheme( cssPatternComboBox.getSelectionModel().getSelectedItem() );
    beitragsWebView.getEngine().setUserStyleSheetLocation(
        getClass().getResource(
            "chatthemes/" +
                cssPatternComboBox.getSelectionModel().getSelectedItem().toLowerCase().replaceAll( "\\s", "" ) + ".css" )
            .toExternalForm() );
    notDisturbCheck.setSelected( dontDisturbCheckBox.isSelected() );
    List<Modifier> list = new ArrayList<>();

    String keyCodeComb = keyLayoutButtonMessageSend.getText();
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
    final String[] keys2 = keyCodeComb.split( "[+]" );
    final String key2 = keys2[ keys2.length - 1 ];
    settings.setKeyCodeMessageSend( new KeyCodeCombination( KeyCode.getKeyCode( key2 ), list.toArray( new Modifier[0] ) ) );

    list = new ArrayList<>();
    keyCodeComb = keyLayoutButtonActiveClients.getText();
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
    final String[] keys3 = keyCodeComb.split( "[+]" );
    final String key3 = keys3[ keys3.length - 1 ];
    settings.setKeyCodeActiveClients(
        new KeyCodeCombination( KeyCode.getKeyCode( key3 ), list.toArray( new Modifier[0] ) ) );

    list = new ArrayList<>();
    keyCodeComb = keyLayoutButtonOpenManual.getText();
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
    settings.setKeyCodeOpenManual( new KeyCodeCombination( KeyCode.getKeyCode( key ), list.toArray( new Modifier[0] ) ) );

    settings.saveSettings();
    settingsStage.hide();
  }

  /**
   * Entfernt die in der {@link #fontListViewIgnored} ausgewählten Fonts evon der Ignorieren Liste.
   */
  @FXML
  private void allowFont()
  {
    final File blacklistFile = new File( settings.getMainFolder() + "blacklistedfonts.list" );
    final Path blacklist = Paths.get( blacklistFile.getAbsolutePath() );
    final Charset charset = StandardCharsets.UTF_8;

    try
    {
      final List<String> lines = Files.readAllLines( blacklist, charset );
      //Kopieren der Liste um zu verhindern das nicht alle der markierten Items erkannt werden
      final List<String> tempNames = new ArrayList<>( fontListViewIgnored.getSelectionModel().getSelectedItems() );
      for ( final String fontName : tempNames )
      {
        if ( fontName != null )
        {
          dasChat.allowFont( fontName );
          lines.remove( fontName );
        }
      }

      try (BufferedWriter output = new BufferedWriter( new FileWriter( blacklistFile, false ) );)
      {
        boolean addNewLine = false;
        for ( final String line : lines )
        {
          if ( !line.equals( "" ) )
          {
            if ( !addNewLine )
            {
              output.newLine();
              addNewLine = true;
            }
            output.write( line );
          }
        }
        output.flush();
      }
      catch ( final Exception e )
      {
        DasChatInit.logger.warning( "Fehler beim schreiben der Font blacklist" );
      }
    }
    catch ( final IOException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Löscht die in der {@link #fontListView} ausgewählten Fonts und verschiebt diese auf die Ignorieren Liste.
   */
  @FXML
  private void deleteFont()
  {
    final File blacklist = new File( settings.getMainFolder() + "blacklistedfonts.list" );
    //Kopieren der Liste um zu verhindern das nicht alle der markierten Items erkannt werden
    final List<String> tempNames = new ArrayList<>( fontListView.getSelectionModel().getSelectedItems() );
    for ( final String fontName : tempNames )
    {
      if ( fontName != null )
      {
        dasChat.removeFromFontList( fontName );
        dasChat.removeFromFontListDown( fontName );
        new File( settings.getFontFolder() + fontName + ".ttf" ).delete();

        try
        {
          FileUtils.writeStringToFile( blacklist, new String( System.lineSeparator() + fontName ), "UTF-8", true );
          dasChat.blacklistFont( fontName );
        }
        catch ( final IOException e )
        {
          DasChatInit.logger.warning( e.fillInStackTrace().toString() );
        }
      }
    }

    new Timer().schedule( new TimerTask()
    {
      @SuppressWarnings( "synthetic-access" )
      @Override
      public void run()
      {
        Platform.runLater( () ->
        {
          recreateCss();
          beitragsWebView.getEngine().load( "" );
          beitragsWebView.getEngine().reload();
          beitragsWebView.getEngine()
              .loadContent( "<html>" + "<head>" + "<meta charset='UTF-8'>"
                  + "<link rel='stylesheet' href='file:///"
                  + settings.getMainFolder().replace( "\\", "/" ) + settings.getFontCssFileName()
                  + "'>" + "</head>" + "<body>" + feed + "</body>" + "</html>" );
        } );
      }
    }, 200 );
  }

  /**
   * Schließt den Einstellungsdialog.
   */
  @FXML
  private void closeSettingsDialog()
  {
    settingsStage.hide();
  }

  /**
   * Reagiert auf Tastenkombinationen des Users
   *
   * @param e KeyEvent
   */
  @FXML
  void editorKeyListener( final KeyEvent e )
  {
    if ( settings.getKeyCodeMessageSend().match( e ) )
    {
      sendButtonClicked();
    }
    if ( settings.getKeyCodeOpenManual().match( e ) )
    {
      openManual();
    }
    if ( settings.getKeyCodeActiveClients().match( e ) )
    {
      getClients();
    }
  }

  /**
   * Fragt beim Server einen String ab in welchem alle Clients die verbunden sind stehen bzw deren Nutzernamen
   */
  @FXML
  void getClients()
  {
    dasChat.sendStringToServer( "request_clients" );
  }

  /**
   * Extrahiert URLs aus einem String und tut diese anschließend in eine Liste
   *
   * @param text übergebener String welcher womöglich URLs enthält
   * @return Liste mit den URLs
   */
  List<String> extractUrls( final String text )
  {
    final List<String> containedUrls = new ArrayList<>();
    final String urlRegex =
        "(?:^|\\s|[;]|[ ]|[[:blank]]|[[:space]])((?:(?:https?|s?ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?)";

    final Pattern pattern = Pattern.compile( urlRegex,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE );
    final Matcher urlMatcher = pattern.matcher( text );

    while ( urlMatcher.find() )
    {
      containedUrls.add( text.substring( urlMatcher.start( 1 ), urlMatcher.end( 1 ) ) );
    }

    //TODO(msc) Entscheiden ob dies Bentuzt werden soll da es nicht nur urls wie wwww.google.com matcht sondern auch www.google.com.com.com.com.com
    //    final String urlRegex2 =
    //        "(?:^|\\s|[;]|[ ]|[[:blank]]|[[:space]])(www[.](?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?)";
    //
    //    final Pattern pattern2 = Pattern.compile( urlRegex2,
    //        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE );
    //    final Matcher urlMatcher2 = pattern2.matcher( text );
    //
    //    while ( urlMatcher2.find() )
    //    {
    //      System.out.println( text.substring( urlMatcher2.start( 1 ), urlMatcher2.end( 1 ) ) );
    //    }

    return containedUrls;
  }

  /**
   * Fokusiert den Editor bzw die WebView des HTMLEditors.
   */
  void editorWebViewRequestFocus()
  {
    Platform.runLater( () ->
    {
      editor.requestFocus();
      final WebView view = (WebView) ((GridPane) ((HTMLEditorSkin) editor.getSkin()).getChildren().get( 0 ))
          .getChildren().get( 2 );
      view.fireEvent( new MouseEvent( MouseEvent.MOUSE_PRESSED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false,
          false, false, false, false, false, false, false, false, false, null ) );
      editor.requestFocus();
      view.fireEvent( new MouseEvent( MouseEvent.MOUSE_RELEASED, 100, 100, 200, 200, MouseButton.PRIMARY, 1, false,
          false, false, false, false, false, false, false, false, false, null ) );
    } );
  }

  /**
   * Wenn der sendet Button betätigt wird dann wird diese Methode aufgerufen,
   */
  @FXML
  void sendButtonClicked()
  {
    editorWebViewRequestFocus();
    String htmlText = editor.getHtmlText();
    final int returnCode = isArticleValid( htmlText );
    if ( returnCode == 1 )
    {
      htmlText = formatMessageThatIsToSend( htmlText );

      sendArticle( htmlText );
      editor.setHtmlText( "<link rel='stylesheet' href='" + getClass().getResource( "editorstyle.css" ) + "'>"
          + "<link rel='stylesheet' href='file:///" + settings.getMainFolder().replace( "\\", "/" )
          + settings.getFontCssFileName() + "'>" );
      sendButton.setText( "Senden" );
      sendButton.setDisable( true );
    }
    else if ( returnCode == 0 )
    {
      alertUser( AlertType.ERROR, "Nachricht senden", "Nachricht zu lang!",
          "Der von dir eingegebene Nachricht ist zu lang, es dürfen höchstens 16000 Zeichen gesendet werden.",
          false );
    }
    else if ( returnCode == 2 )
    {
      alertUser( AlertType.ERROR, "Nachricht senden", "Nachricht leer!",
          "Du kannst keine leeren Nachrichten senden.", false );
    }
  }

  /**
   * Durchsucht einen String nach HashTags und gibt diese in einer Liste zurück.
   *
   * @param htmlText zu überprüfender Texz
   * @return Liste der gefundenen HashTags
   */
  private List<String> getHashTags( final String htmlText )
  {
    String tempHtmlText = Jsoup.parse( htmlText ).text();
    final Pattern hashTagPattern = Pattern.compile( "(?:^|[^\\w])(#[\\wÜÄÖüäöß]+)" );
    Matcher hashTagMatcher = hashTagPattern.matcher( tempHtmlText );
    final List<String> hashTags = new ArrayList<>();
    while ( hashTagMatcher.find() )
    {
      final String hashTag = hashTagMatcher.group( 1 );
      tempHtmlText = tempHtmlText.replaceFirst( hashTag, "" );
      if ( !hashTags.contains( hashTag ) )
      {
        hashTags.add( hashTag );
      }
      hashTagMatcher = hashTagPattern.matcher( tempHtmlText );
    }
    return hashTags;
  }

  //TODO(msc) Usernamenfreiheit begrenzen (0-9und buchstaben a-z/A-Z , üöäß)
  //Funktion zum erkennen von user Annotations (noch nicht implementiert)
  // private List<String> getAtTags( String htmlText )
  // {
  // Pattern atTagPattern = Pattern.compile( "(?:^|[^\\w])(@[\\wÜÄÖüäöß]+)" );
  // Matcher atTagMatcher = atTagPattern.matcher( Jsoup.parse( htmlText
  // ).text() );
  // List<String> atTags = new ArrayList<>();
  // while ( atTagMatcher.find() )
  // {
  // String atTag = atTagMatcher.group();
  // atTags.add( atTag );
  // }
  // return atTags;
  // }

  /**
   * Formatiert die übergebene Nachricht für den weiteren Verlauf.
   *
   * @param htmlText zu formatierender Textz
   * @return formatierter Text
   */
  private String formatMessageThatIsToSend( final String htmlText )
  {
    // Löschen der Tags welche nicht Teil der Nachricht sind (nicht mehr
    // benötigt werden)
    String toSend = htmlText;
    toSend = toSend.substring( toSend.indexOf( "<body" ), toSend.length() );
    toSend = toSend.replace( "<body contenteditable=\"true\">", "" ).replace( "</body></html>", "" );

    // Create Links
    for ( final String url : extractUrls( Jsoup.parse( htmlText.replace( "&nbsp;", " " ) ).text() ) )
    {
      toSend = toSend.replace( url, "<a href='" + url + "'>" + url + "</a>" );
    }

    // Create HashTags
    for ( final String hashTag : getHashTags( toSend ) )
    {
      toSend = toSend.replaceFirst( hashTag, "<a class='hashtag' href='+'>" + hashTag + "</a>" );
      if ( !settings.availableMessageFilters.contains( hashTag ) )
      {
        settings.availableMessageFilters.add( hashTag );
      }
    }

    if ( settings.isShareFonts() )
    {
      final String toFilter = toSend;
      final File windowsFolder = new File( "C:" + File.separator + "Windows" + File.separator + "fonts" );
      if ( windowsFolder.exists() )
      {
        fontThread = new Thread( () ->
        {
          filterAndSendFonts( toFilter );
        } );
        fontThread.start();
      }
    }
    return toSend;
  }

  /**
   * Überprüft den übergeben String auf {@code <font>} Tags und sendet wenn auf der Serverseite die
   * Notwendigkeit besteht die genutzten Fonts an den Server.
   *
   * @param input String welcher nach {@code <font>} Tags zu durchsuchen ist
   */
  private void filterAndSendFonts( final String input )
  {
    notSending = false;
    final Pattern fontFacePattern = Pattern.compile( "face=\"(.*?)\"" );
    final Matcher fontMatcher = fontFacePattern.matcher( input );
    final List<String> doneAlready = new ArrayList<>();
    while ( fontMatcher.find() )
    {
      final String fontResult = fontMatcher.group( 1 );
      try
      {
        if ( !doneAlready.contains( fontResult ) )
        {
          doneAlready.add( fontResult );
          byte[] bytesToSend = new String( "send_font:" + fontResult ).getBytes();
          bytesToSend = Util.compress( bytesToSend );
          sendStream.writeInt( bytesToSend.length );
          sendStream.write( bytesToSend );
          final int length = receiveStream.readInt();
          byte[] message = new byte[length];
          receiveStream.readFully( message, 0, length );
          message = Util.decompress( message );
          final String answer = new String( message );

          if ( answer.equals( "SendNow" ) )
          {
            final Font font = new Font( fontResult, 10 );
            final File windowsFolder = new File(
                "C:" + File.separator + "Windows" + File.separator + "fonts" + File.separator );
            //Font ordner auf anderen system
            // System.out.println(
            // sun.font.SunFontManager.getInstance().getPlatformFontPath(
            // true ) );
            // File osxSystemFolder = new File( "" );
            // File ubuntuSysFontFolder1 =
            // new File( File.separator + "user" + File.separator +
            // "share" + File.separator + "fonts" + File.separator
            // );
            // File ubuntuSysFontFolder2 = new File(
            // File.separator + "user" + File.separator + "local" +
            // File.separator + "share" + File.separator + "fonts" +
            // File.separator );
            // File fontFolder = new File(
            // sun.font.SunFontManager.getInstance().getPlatformFontPath(
            // true ) );

            final File[] fontFiles = windowsFolder.listFiles();
            Font fontCompare = null;
            for ( final File fontFile : fontFiles )
            {
              try (FileInputStream stream = new FileInputStream( fontFile );)
              {

                if ( FilenameUtils.getExtension( fontFile.getAbsolutePath() ).equalsIgnoreCase( "ttf" ) )
                {
                  fontCompare = Font.loadFont( stream, 10 );
                }

                if ( fontCompare != null )
                {
                  if ( font.getName().equals( fontCompare.getName() ) )
                  {
                    byte[] file = IOUtils.toByteArray( new FileInputStream( fontFile ) );
                    file = Util.compress( file );
                    sendStream.writeInt( file.length );
                    sendStream.write( file );
                    notSending = true;
                    break;
                  }
                }
              }
              catch ( final IOException e )
              {
                DasChatInit.logger.info( e.fillInStackTrace().toString() );
                notSending = true;
              }
            }
          }
          else
          {
            continue;
          }
        }
      }
      catch ( final IOException e )
      {
        DasChatInit.logger.warning( e.fillInStackTrace().toString() );
        notSending = true;
      }
    }
    notSending = true;
  }

  /**
   * Überprüft die visuelle Zeichenanzahl des Artikels und bestimmt somit ob dieser valide ist oder nicht
   *
   * @param htmlTextInput HTML Text that is to check
   * @return Gibt 1 zurück wenn der Text valide ist, 0 wenn zu lang und 2 wenn zu kurz
   */
  private int isArticleValid( final String htmlTextInput )
  {
    final String plainText = Jsoup.parse( htmlTextInput ).text();
    if ( plainText.length() >= 16001 )
    {
      return 0;
    }
    //Entfernen von Tabs, Line Breaks, Spaces und Non Break Spaces(Unicode)
    else if ( plainText.replaceAll( "\\s+", "" ).replace( " ", "" ).replace( "\u00A0", "" ).length() >= 1 )
    {
      return 1;
    }
    else
    {
      return 2;
    }
  }

  /**
   * Sendet den Article ab Wenn dieser validiert wurde
   *
   * @param htmlTextInput Text der zu senden ist
   */
  private void sendArticle( final String htmlTextInput )
  {
    final String htmlText = "<msg><user=" + settings.getClientName() + "><div class='messageblob'>" + htmlTextInput
        + "<div class='nocopy'><div class='bywho'><div class='bywhoin'>" + settings.getClientName()
        + "<span class='tstamp'> - " + getFormattedDateAsString( new Date() )
        + "</span></div></div></br></div></div></msg>";

    dasChat.sendStringToServer( htmlText );
  }

  /**
   * Gibt das übergebene Datum im vorgegebenen Format zurück
   *
   * @return Datum
   */
  String getFormattedDateAsString( final Date date )
  {
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "dd.MM.yy" );
    final SimpleDateFormat timeFormat = new SimpleDateFormat( "HH:mm:ss" );
    final String dateString = dateFormat.format( date );
    final String timeString = timeFormat.format( date );
    return "<date " + dateString + "></date> " + timeString;
  }

  /**
   * Erstellt das GUI für das Senden / Lesen von Nachrichten .
   */
  void initClient()
  {

    settings.getController().loginDialogStage.close();
    final FXMLLoader loader = new FXMLLoader();
    loader.setLocation( getClass().getResource( "Client.fxml" ) );
    loader.setController( this );
    try
    {
      dasChat.initFontList();
      dasChat.initFontBlacklist();
      final Parent root = loader.load();
      final Scene scene = new Scene( root );
      scene.getStylesheets()
          .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
      primaryStage = new Stage();
      primaryStage.setMinWidth( 800 );
      primaryStage.setMinHeight( 300 );
      primaryStage.setScene( scene );
      primaryStage.setTitle( "DasChat - Eingeloggt als: " + settings.getClientName() );
      primaryStage.getIcons().add( new Image( Settings.class.getResourceAsStream( "icon.png" ) ) );

      scene.setOnKeyReleased( event ->
      {
        if ( event.isControlDown() && event.isShiftDown() && event.getCode().equals( KeyCode.D ) )
        {
          settings.setDockFilterStage( !settings.isDockFilterStage() );
          if ( !settings.isDockFilterStage() )
          {
            filterStage.centerOnScreen();
            filterStage.toFront();
          }
        }
      } );

      primaryStage.setOnCloseRequest( close ->
      {
        disconnect();
      } );

      Runtime.getRuntime().addShutdownHook( new Thread()
      {
        @Override
        public void run()
        {
          disconnect();
        }
      } );

      primaryStage.show();

      buildFilterStage();

      //Verhindert das vertikal gescrollt wird
      scrollPaneTags.addEventFilter( ScrollEvent.SCROLL, event ->
      {
        if ( event.getDeltaY() != 0 )
        {
          event.consume();
        }
      } );

      if ( checkVersion() )
      {
        listenToIncommingMessages();
      }

      configureEditor();

      configureBeitragsWebView();

      notDisturbCheck.setSelected( settings.isNotDisturb() );
    }
    catch ( final Exception e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
      disconnect();
    }

    toggleTagBox();
  }


  /**
   * Ersetzt inhalt des Clipboards mit plainText Equivalent
   */
  private void modifyClipboard()
  {
    final Clipboard clipboard = Clipboard.getSystemClipboard();

    final String plainText = clipboard.getString();
    final ClipboardContent content = new ClipboardContent();
    content.putString( plainText );

    clipboard.setContent( content );
  }

  /**
   * Setzt alle nötigen Einstellungen und Listener für den {@link #editor}
   */
  private void configureEditor()
  {

    editor.setDisable( true );
    sendButton.setDisable( true );

    final WebView editorWebView = (WebView) editor.lookup( ".web-view" );
    //Möglichkeit das CotnextMenu loszuwerden
    //    editorWebView.setContextMenuEnabled( false );

    //Drag and drop im editor
    editorWebView.addEventHandler( DragEvent.DRAG_DROPPED, handler ->
    {
      final ClipboardContent content = new ClipboardContent();
      content.putString( handler.getDragboard().getString() );
      handler.getDragboard().setContent( content );
    } );

    primaryStage.getScene().addEventFilter( MouseEvent.ANY, e ->
    {
      if ( !Objects.isNull( lastEvent ) )
      {
        if ( Clipboard.getSystemClipboard().hasHtml() )
        {
          if ( lastEvent.getTarget().equals( editorWebView ) && lastEvent.getEventType().equals( MouseEvent.MOUSE_ENTERED_TARGET ) )
          {
            if ( e.getTarget().equals( editorWebView ) && e.getEventType().equals( MouseEvent.MOUSE_MOVED ) )
            {
              modifyClipboard();
            }
          }
        }
      }
      lastEvent = e;
    } );


    editor.addEventFilter( KeyEvent.KEY_PRESSED, e ->
    {
      if ( e.isControlDown() && e.getCode() == KeyCode.V )
      {
        modifyClipboard();
      }
    } );


    final Button button = (Button) editor.lookup( ".html-editor-paste" );
    button.addEventFilter( MouseEvent.MOUSE_PRESSED, e ->
    {
      modifyClipboard();
      new Timer().schedule( new TimerTask()
      {
        @Override
        public void run()
        {
          Platform.runLater( () ->
          {
            updateSize();
          } );
        }
      }, 200 );
    } );

    editor.setHtmlText( "<link rel='stylesheet' href='" + getClass().getResource( "editorstyle.css" ) + "'>"
        + "<link rel='stylesheet' href='file:///" + settings.getMainFolder().replace( "\\", "/" )
        + settings.getFontCssFileName() + "'>" );

    editor.setOnKeyReleased( event ->
    {
      updateSize();
    } );

    editor.setOnKeyTyped( event ->
    {
      updateSize();
    } );

    editor.setOnMouseEntered( clicked ->
    {
      updateSize();
    } );

    editor.setOnMouseExited( clicked ->
    {
      updateSize();
    } );

    editor.getStylesheets().add( getClass().getResource( "htmleditor.css" ).toExternalForm() );

    editorWebView.setPrefHeight( 2000 );
  }

  /**
   * Setzt alle nötigen Einstellungen und Listener für die {@link #beitragsWebView}
   */
  private void configureBeitragsWebView()
  {
    final WebViewHyperlinkListener openLinkInBrowser = event ->
    {
      if ( Objects.isNull( event.getURL() ) && event.getDescription().substring( 0, 1 ).equals( "#" ) )
      {
        try
        {
          hashTagContextMenu.hide();
        }
        catch ( final Exception e )
        {
          //Alles gut :)
        }

        final String hashtag = event.getDescription();

        hashTagContextMenu = new ContextMenu();

        final MenuItem cmItem1 = new MenuItem( "Nur nach diesem Tag filtern" );
        final MenuItem cmItem2 = new MenuItem( "Diesen Tag zu den Filtern hinzufügen" );
        final MenuItem cmItem3 = new MenuItem( "Diesen Tag aus den Filtern entfernen" );

        cmItem1.setOnAction( e ->
        {
          availableFiltersCheckListView.getCheckModel().clearChecks();
          availableFiltersCheckListView.getCheckModel().check( hashtag );
          applyFilters();
        } );

        cmItem2.setOnAction( e ->
        {
          availableFiltersCheckListView.getCheckModel().check( hashtag );
          applyFilters();
        } );

        cmItem3.setOnAction( e ->
        {
          availableFiltersCheckListView.getCheckModel().clearCheck( hashtag );
          applyFilters();
        } );

        hashTagContextMenu.getItems().add( cmItem1 );
        hashTagContextMenu.getItems().add( cmItem2 );
        hashTagContextMenu.getItems().add( cmItem3 );

        final java.awt.Point p = java.awt.MouseInfo.getPointerInfo().getLocation();
        hashTagContextMenu.show( primaryStage, p.getX(), p.getY() );
      }
      try
      {
        openWebpage( URI.create( event.getDescription() ) );
      }
      catch ( final Exception exc )
      {
        DasChatInit.logger.warning( "Die soeben angeklickte URL ist ungültig, " + event.getDescription() );
      }
      return true;
    };

    WebViews.addHyperlinkListener( beitragsWebView, openLinkInBrowser, HyperlinkEvent.EventType.ACTIVATED );

    //Verhindert das unerwünschter Content in die WebView gezogen wird
    beitragsWebView.setOnDragDropped( event ->
    {
      event.consume();
    } );

    beitragsWebView.getEngine().setUserStyleSheetLocation( getClass().getResource(
        "chatthemes/" + settings.getChatTheme().toLowerCase().replaceAll( "\\s", "" ) + ".css" ).toExternalForm() );
  }

  /**
   * Fragt den Server ob es einen Versionsunterschied gibt.
   *
   * @return Gibt true zurück wenn Abfrage erfolgreich war und ruft sich selber bei Fehlschlag auf.
   */
  boolean checkVersion()
  {
    dasChat.sendStringToServer( "version:" + GeneralSharedInformation.VERSION );
    final String version = dasChat.receiveStringFromServer();
    if ( version == null )
    {
      return checkVersion();
    }
    if ( version.contains( "version_diffrent:" ) )
    {
      alertUser( AlertType.ERROR, "Versionsüberprüfung", "Versionen stimmen nicht überein",
          "Die Version deines Clients(" + GeneralSharedInformation.VERSION + ") stimmt nicht mit der des Servers("
              + version.substring( 17 ) + ") überein.",
          false );
      return true;
    }
    else if ( version.equals( "version_same" ) )
    {
      return true;
    }
    else
    {
      return checkVersion();
    }
  }

  /**
   * Öffnet den About Dialog und baut diesen gegebnfalls auf
   */
  @FXML
  private void openAboutDasChat()
  {
    try
    {
      dasChatAboutStage.show();
      dasChatAboutStage.toFront();
    }
    catch ( final NullPointerException e )
    {

      final FXMLLoader loader = new FXMLLoader();
      loader.setLocation( getClass().getResource( "AboutDasChat.fxml" ) );
      loader.setController( this );
      try
      {
        final Parent root = loader.load();
        final Scene scene = new Scene( root );
        scene.getStylesheets()
            .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
        dasChatAboutStage = new Stage();
        dasChatAboutStage.setScene( scene );
        dasChatAboutStage.setTitle( "Über DasChat" );
        dasChatAboutStage.getIcons().add( new Image( Settings.class.getResourceAsStream( "icon.png" ) ) );
        dasChatAboutStage.show();
        dasChatAboutStage.initOwner( primaryStage );
        dasChatAboutStage.setMinWidth( dasChatAboutStage.getWidth() );
        dasChatAboutStage.setMinHeight( dasChatAboutStage.getHeight() );
        dasChatAboutStage.setMaxWidth( dasChatAboutStage.getWidth() );
        dasChatAboutStage.setMaxHeight( dasChatAboutStage.getHeight() );
        versionLabel.setText( "DasChat Version " + GeneralSharedInformation.VERSION );
      }
      catch ( final Exception e2 )
      {
        DasChatInit.logger.warning( e2.fillInStackTrace().toString() );
      }
    }
  }

  /**
   * Öffnet das Benutzerhandbuch und baut dieses gegebenfalls auf.
   */
  @FXML
  private void openManual()
  {
    try
    {
      manualStage.show();
      manualStage.toFront();
    }
    catch ( final NullPointerException e )
    {
      final FXMLLoader loader = new FXMLLoader();
      loader.setLocation( getClass().getResource( "Manual.fxml" ) );
      loader.setController( this );
      try
      {
        final Parent root = loader.load();
        final Scene scene = new Scene( root );
        scene.getStylesheets()
            .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
        manualStage = new Stage();
        manualStage.setScene( scene );
        manualStage.setTitle( "DasChat - Benutzerhandbuch" );
        manualStage.getIcons().add( new Image( Settings.class.getResourceAsStream( "icon.png" ) ) );
        manualStage.show();

        manualStage.initOwner( primaryStage );

        final ObservableList<String> topicsLive = FXCollections.observableList( new ArrayList<String>() );
        topicsLive.add( "Schriftarten Sharing - Aktivieren des Features" );
        topicsLive.add( "Schriftarten Sharing - Empfangen und Senden von Schriftarten" );
        topicsLive.add( "Schriftarten Sharing - Schriftarten löschen/in Zukunft ignorieren" );
        final ObservableList<String> topicsConst = FXCollections.observableList( new ArrayList<String>() );
        topicsConst.addAll( topicsLive );

        final WebViewHyperlinkListener processHelpLinks = event ->
        {
          if ( event.getDescription().equals( "Schriftarten Sharing" ) )
          {
            showSettings();
            settingsTabPane.getSelectionModel().select( fontTab );
            buttonFonts.requestFocus();
            return true;
          }
          else if ( event.getDescription().equals( "Schriftarten alter Nachrichten downloaden" ) )
          {
            showSettings();
            settingsTabPane.getSelectionModel().select( fontTab );
            buttonFontsAtBeginning.requestFocus();
            return true;
          }
          else if ( event.getDescription().equals( "Igonrierte Schriftarten" ) )
          {
            showSettings();
            settingsTabPane.getSelectionModel().select( fontTab );
            fontListViewIgnored.requestFocus();
            return true;
          }
          else if ( event.getDescription().equals( "Heruntergeladene Schriftarten" ) )
          {
            showSettings();
            settingsTabPane.getSelectionModel().select( fontTab );
            fontListView.requestFocus();
            return true;
          }
          return false;
        };

        WebViews.addHyperlinkListener( manualTextArea, processHelpLinks, HyperlinkEvent.EventType.ACTIVATED );

        setManualTopics();

        searchField.textProperty().addListener( ( observable, oldValue, newValue ) ->
        {
          final String text = newValue;
          topicsLive.clear();
          for ( final String topic : topicsConst )
          {
            if ( StringUtils.containsIgnoreCase( topic, text ) )
            {
              topicsLive.add( topic );
            }
          }
        } );
        manualListView.setItems( topicsLive );

        manualListView.setOnMouseClicked( event ->
        {
          final String topic = manualListView.getSelectionModel().getSelectedItem();
          manualTextArea.getEngine().loadContent( manualTopics.get( topic ) );
        } );

        manualListView.setOnKeyReleased( event ->
        {
          if ( event.getCode().equals( KeyCode.ENTER ) || event.getCode().equals( KeyCode.SPACE ) )
          {
            final String topic = manualListView.getSelectionModel().getSelectedItem();
            manualTextArea.getEngine().loadContent( manualTopics.get( topic ) );
          }
        } );

        manualStage.setMinWidth( manualStage.getWidth() );
        manualStage.setMinHeight( manualStage.getHeight() );
        manualStage.setWidth( 800 );
        manualStage.setHeight( 600 );
      }
      catch ( final Exception e2 )
      {
        DasChatInit.logger.warning( e2.fillInStackTrace().toString() );
      }
    }
  }

  /**
   * Setzt den Text für das Benutzerhandbuch.
   */
  private void setManualTopics()
  {
    manualTopics.put( "Schriftarten Sharing - Aktivieren des Features",
        "<h3>Aktivieren des Features</h3>"
            + "Um Schriftarten Sharing zu nutzen muss zuerst in den <b>Einstellungen</b> -> <b>Schriftarten</b> der Punkt "
            + "'<a href='+'>Schriftarten Sharing</a>' gesetzt werden." + "<br>" + "<br>" + "<img src='"
            + getClass().getResource( "ss-enable.png" ) + "'/>" );
    manualTopics.put( "Schriftarten Sharing - Empfangen und Senden von Schriftarten",
        "<h3>Empfangen und Senden von Schriftarten</h3>"
            + "Das Senden und Empfangen der Schriftarten geschieht sobald das '<a href='+'>Schriftarten Sharing</a>' Feature aktiviert wurde komplett automatisch. Nachdem eine Schriftart empfangen wurde kann diese gesehen werden jedoch nicht selber genutzt werden, dass geht erst nach einem Neustart des DasChat Clients. Möchtest du auch die Schriftarten vergangener Nachrichten herunterladen, so musst du den Punkt bei '"
            + "<a href='+'>Schriftarten alter Nachrichten downloaden</a>' setzen." + "<br>" + "<br>"
            + "<img src='" + getClass().getResource( "ss-enable-old.png" ) + "'/>" );
    manualTopics.put( "Schriftarten Sharing - Schriftarten löschen/in Zukunft ignorieren",
        "<h3>Schriftarten löschen/in Zukunft ignorieren</h3>"
            + "Wenn man eine Schriftart löschen möchte, beziehungsweise diese in Zukunft nicht mehr erhalten möchte, dann öffnet man die <b>Einstellungen</b> -> <b>Schriftarten</b> und wählt dort in der Liste '<a href='+'>Heruntergeladene Schriftarten</a>' eine Schriftart aus und betätigt den '->' Button um diese in die 'Ignorieren Schriftarten' Liste zu verschieben."
            + "<br>" + "<br>"
            + "Möchtest du die Schriftart wieder erhalten können musst du diese aus der '<a href='+'>Igonrierte Schriftarten</a>' Liste entfernen indem du sie auswählst und anschließend den '<-' Button betätigst."
            + "<br>" + "<br>" + "<img src='" + getClass().getResource( "ss-ignore-allow.png" ) + "'/>" + "" );
  }

  /**
   * Öffnet den Einstellungsdialog und baut diesen gegebenfalls auf.
   */
  @FXML
  private void showSettings()
  {
    try
    {
      settingsStage.show();
      settingsStage.toFront();
      buttonFonts.setSelected( settings.isShareFonts() );
      buttonFontsAtBeginning.setSelected( settings.isShareFontsAtBeginning() );
      keyLayoutButtonMessageSend.setText( settings.getKeyCodeMessageSend().toString() );
      keyLayoutButtonOpenManual.setText( settings.getKeyCodeOpenManual().toString() );
      keyLayoutButtonActiveClients.setText( settings.getKeyCodeActiveClients().toString() );
      textFieldMaxMessages.setText( "" + settings.getMaxMessages() );
      dontDisturbCheckBox.setSelected( settings.isNotDisturb() );
    }
    catch ( final NullPointerException e )
    {
      try
      {
        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation( getClass().getResource( "Settings.fxml" ) );
        loader.setController( this );
        final Parent root = loader.load();
        final Scene scene = new Scene( root );
        scene.getStylesheets()
            .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
        settingsStage = new Stage();
        settingsStage.setScene( scene );
        settingsStage.initOwner( primaryStage );
        settingsStage.setTitle( "DasChat - Einstellungen" );
        settingsStage.getIcons().add( new Image( Settings.class.getResourceAsStream( "icon.png" ) ) );
        fontListView.setItems( dasChat.observableFontListDown );
        fontListView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
        fontListViewIgnored.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
        fontListViewIgnored.setItems( dasChat.observableFontListIgnored );
        fontListView.setOnKeyPressed( event ->
        {
          if ( !event.isControlDown() && !event.isShiftDown() && !event.isMetaDown() && !event.isAltDown()
              && event.getCode().equals( KeyCode.SPACE ) )
          {
            deleteFont();
          }
        } );
        fontListViewIgnored.setOnKeyPressed( event ->
        {
          if ( !event.isControlDown() && !event.isShiftDown() && !event.isMetaDown() && !event.isAltDown()
              && event.getCode().equals( KeyCode.SPACE ) )
          {
            allowFont();
          }
        } );
        buttonFonts.setSelected( settings.isShareFonts() );
        buttonFontsAtBeginning.setSelected( settings.isShareFontsAtBeginning() );
        keyLayoutButtonMessageSend.setText( settings.getKeyCodeMessageSend().toString() );
        keyLayoutButtonOpenManual.setText( settings.getKeyCodeOpenManual().toString() );
        keyLayoutButtonActiveClients.setText( settings.getKeyCodeActiveClients().toString() );
        textFieldMaxMessages.setText( "" + settings.getMaxMessages() );
        dontDisturbCheckBox.setSelected( settings.isNotDisturb() );
        final ObservableList<String> patterns = FXCollections.observableArrayList( "Default", "Upholstery",
            "Carbon fiber", "Japanese cube", "Augenkrebs", "Hearts", "DasChat" );
        cssPatternComboBox.setItems( patterns );
        cssPreviewWebView.getEngine().loadContent(
            "<msg><div class='day'>11.01.2011</div><user=Donald J. Trump><div class='messageblob'><p><font face='Segoe UI'>Hey, wie geht es dir?<p>Super</p></font></p><div class='nocopy'><div class='bywho'><div class='bywhoin'>A User<span class='tstamp'> - <date 01.07.16></date> 09:17:15</span></div></div></br></div></div></msg><msg><div class='messageblobme'><user=Donald J. Trump><p><font face='Segoe UI'>Hey, wie geht es dir?</font></p><div class='nocopy'><div class='byme'><div class='bywhoin'>You<span class='tstamp'> - <date 01.07.16></date> 09:17:15</span></div></div></br></div></div></msg>" );
        String chosenPattern = null;
        try
        {
          final String toChoose;
          toChoose = settings.getChatTheme();
          cssPatternComboBox.getSelectionModel().select( toChoose );
          chosenPattern = cssPatternComboBox.getSelectionModel().getSelectedItem();
        }
        catch ( final Exception exception )
        {
          cssPreviewWebView.getEngine()
              .setUserStyleSheetLocation( getClass().getResource( "chatthemes/" + "default.css" ).toExternalForm() );
          chosenPattern = "default";
        }
        cssPreviewWebView.getEngine().setUserStyleSheetLocation( getClass()
            .getResource( "chatthemes/" + chosenPattern.toLowerCase().replaceAll( "\\s", "" ) + ".css" ).toExternalForm() );

        settingsStage.show();

        final Timer timer = new Timer();
        timer.schedule( new TimerTask()
        {

          @Override
          public void run()
          {
            Platform.runLater( () ->
            {
              //Settings Dialog wird ohne diese magischen Nummern zu klein
              getSettingsStage().setMinWidth( scene.getWidth() + 16 );
              getSettingsStage().setMinHeight( scene.getHeight() + 39 );
            } );
          }
        }, 100 );

      }
      catch ( final IOException e2 )
      {
        DasChatInit.logger.warning( e2.fillInStackTrace().toString() );
      }
    }
  }

  Stage getSettingsStage()
  {
    return settingsStage;
  }

  /**
   * Meldet den Client vom Server ab und schließt den Client
   */
  @FXML
  private void closeDasChat()
  {
    disconnect();
    DasChatInit.logger.info( "DasChat wird nun geschlossen." );
    Runtime.getRuntime().exit( 0 );
  }

  /**
   * Öffnet eine Sicherheitsabfrage um sicherzugehen, dass der Nutzer Schriftarten Sharing wirklich aktivieren
   * will.
   */
  @FXML
  private void toggleFontSharing()
  {
    if ( buttonFonts.isSelected() )
    {

      final Alert alert = new Alert( AlertType.CONFIRMATION );
      alert.getDialogPane().getStylesheets()
          .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
      alert.setTitle( "Schriftarten Sharing aktivieren" );
      alert.setHeaderText( "WARNUNG!" );
      alert.setContentText(
          "Durch das Aktivieren der Schriftarten Sharing Funktion könnten Schriftarten heruntergeladen und genutzt werden, für welche eine Lizenz erworben werden muss." );

      final ButtonType buttonTypeResume = new ButtonType( "Fortfahren" );
      final ButtonType buttonTypeCancel = new ButtonType( "Abbrechen" );

      alert.getButtonTypes().setAll( buttonTypeResume, buttonTypeCancel );

      final Optional<ButtonType> result = alert.showAndWait();
      if ( result.get() == buttonTypeResume )
      {
        buttonFonts.setSelected( true );
      }
      else
      {
        buttonFonts.setSelected( false );
      }
    }
  }

  /**
   * Wenn eine der beiden Haken bei Nicht stören gesetzt wird (Einstellungsdialog oder Menü) wird der andere
   * automatisch mit gesetzt.
   */
  @FXML
  private void toggleNotDisturb()
  {

    settings.setNotDisturb( notDisturbCheck.isSelected() );
    dontDisturbCheckBox.setSelected( notDisturbCheck.isSelected() );
    settings.saveSettings();
  }

  /**
   * Teilt dem Server mit das mit die Verbindung unterbricht , schließt den FileHandler welcher dafür sorgt
   * das nur ein Client gleichzeitig offen sein kann und schließt DasChat.
   */
  @FXML
  void disconnect()
  {
    if ( !disconnected )
    {
      disconnected = true;
      for ( final Handler handler : DasChatInit.logger.getHandlers() )
      {
        handler.close();
      }
      try
      {
        dasChat.sendStringToServer( "request_disconnect_now" );
        dasChat.getSocket().close();
      }
      catch ( final IOException e )
      {
        DasChatInit.logger.info( "DasChat wird nun geschlossen." );
        DasChatInit.logger.info( e.fillInStackTrace().toString() );
      }
      DasChatInit.logger.info( "DasChat wird nun geschlossen." );
      Runtime.getRuntime().exit( 0 );
    }
  }

  /**
   * Zeigt dem User an wie viele Zeichen er genutzt hat und warnt ihn wenn er das Limit erreicht bzw. kurz
   * davor ist.
   */
  void updateSize()
  {
    final String plaintext = Jsoup.parse( editor.getHtmlText() ).text();
    final int plainTextLength = plaintext.length();
    if ( plainTextLength != 0 )
    {
      if ( plainTextLength >= 16001 )
      {
        final ImageView error = new ImageView( new Image( getClass().getResourceAsStream( "error-small.png" ) ) );
        sendButton.setGraphic( error );
        sendButton.setTooltip( new Tooltip( "Achtung, du hast das maximale Zeichenlimit überschritten." ) );
      }
      else if ( plainTextLength >= 14000 )
      {
        final ImageView warning = new ImageView( new Image( getClass().getResourceAsStream( "warning-small.png" ) ) );
        sendButton.setGraphic( warning );
        sendButton.setTooltip( new Tooltip( "Achtung, du kannst noch " + (16000 - plainTextLength)
            + " Zeichen einfügen bis du das maximale Zeichenlimit überschreitest." ) );
      }
      else
      {
        sendButton.setDisable( false );
        sendButton.setGraphic( null );
        sendButton.setTooltip( null );
      }
      sendButton.setText( "Senden ( " + plainTextLength + " )" );
    }
    else
    {
      sendButton.setGraphic( null );
      sendButton.setTooltip( null );
      sendButton.setDisable( true );
      sendButton.setText( "Senden" );
    }
  }

  /**
   * Öffnet die angegebene Website im browser, falls es eine file URL ist im file browser
   *
   * @param uri URI welche geöffnet werden soll
   */
  void openWebpage( final URI uri )
  {
    final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
    if ( desktop != null && desktop.isSupported( Desktop.Action.BROWSE ) )
    {
      try
      {
        if ( uri.toString().contains( "file://" ) )
        {
          desktop.open( new File( uri.toString().replace( "file:", "" ) ) );
        }
        else
        {
          desktop.browse( uri );
        }
      }
      catch ( final Exception e )
      {
        DasChatInit.logger.warning( e.fillInStackTrace().toString() );
      }
    }
  }

  /**
   * Ruft openWebpage mit URI input auf
   *
   * @param url URL welche zur URI umgewandelt wird
   */
  void openWebpage( final URL url )
  {
    try
    {
      final URL urlToOpen = new URL( url.toString().trim() );
      openWebpage( urlToOpen.toURI() );
    }
    catch ( final MalformedURLException | URISyntaxException e )
    {
      DasChatInit.logger.warning( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Zeigt dem user eine Fehlermeldung mit den angegebenen Parametern. (Methode ist lediglich um Code zu
   * sparen)
   *
   * @param title Titel des Alerts
   * @param headerText headerText des Alerts
   * @param text Text des Alerts
   * @param wait Entscheidet ob .show() oder .showAndWait() benutzt wird
   */
  void alertUser( final AlertType type, final String title, final String headerText, final String text,
                  final boolean wait )
  {
    final Alert alert = new Alert( type );
    alert.initModality( Modality.APPLICATION_MODAL );
    alert.initOwner( primaryStage );
    alert.getDialogPane().getStylesheets()
        .add( getClass().getResource( settings.getTheme().toLowerCase() + ".css" ).toExternalForm() );
    alert.setTitle( title );
    alert.setHeaderText( headerText );
    alert.setContentText( text );
    if ( !wait )
    {
      alert.show();
    }
    else
    {
      alert.showAndWait();
    }
  }
}