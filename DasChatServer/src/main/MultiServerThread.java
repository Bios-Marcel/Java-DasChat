package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;

import shared.GeneralSharedInformation;
import util.Util;

/**
 *
 *
 * @author msc
 *
 */
public class MultiServerThread extends Thread
{
  /**
   * Referenz auf Instanz der Mainklasse(Singleton)
   */
  private final Main          main;

  private Socket              socket              = null;

  /**
   * Name des Clients
   */
  private String              name;

  /**
   * Liste aller Client Namen.
   */
  private static List<String> names               = new ArrayList<>();

  /**
   * Stream über welchen die Kommunikation abläuft
   */
  private DataOutputStream    out;

  /**
   * Stream über welchen die Kommunikation abläuft
   */
  private DataInputStream     in;

  /**
   * Um festzuhalten ob der Benutzer bereits disconnected ist.
   */
  private boolean             disconnected        = false;

  private boolean             nameWasInUseAlready = false;

  /**
   * Konstruktor
   *
   * @param socket von der Main Klasse übergebener Socket
   */
  public MultiServerThread( final Socket socket )
  {
    super( "MultiServerThread" + socket.getLocalAddress() );
    this.socket = socket;
    main = Main.getMain();
    main.addClient( this.socket );
  }

  /**
   * Empfängt die Nachrichten des Clients
   */
  @Override
  public void run()
  {
    try
    {
      out = new DataOutputStream( socket.getOutputStream() );
      in = new DataInputStream( socket.getInputStream() );
      String inputLine = null;
      while ( true )
      {
        final int length = in.readInt();
        if ( length != 0 )
        {
          byte[] message = new byte[length];
          in.readFully( message, 0, message.length );
          message = Util.decompress( message );
          inputLine = new String( message, StandardCharsets.UTF_8 );
          if ( !checkInput( inputLine ) )
          {
            break;
          }
        }
      }
      disconnectUser();
    }
    catch ( final IOException e )
    {
      disconnectUser();
    }
  }

  //TODOD(msc) Methoden rausfiltern
  /**
   * Entscheidet was bei der vom Client empfangenen Nachricht zu tun ist.
   *
   * @param message Eingegangene Nachricht des Clients
   */
  private boolean checkInput( final String message )
  {
    if ( message.equals( "request_clients" ) )
    {
      sendConnectedClientsToServer();
      return true;
    }
    else if ( message.equals( "request_hashtags" ) )
    {
      sendHashTagsToClient();
      return true;
    }
    else if ( message.substring( 0, 5 ).equals( "<msg>" ) )
    {
      receiveAndSendMessageToClients( message );
      return true;
    }
    else if ( message.substring( 0, 8 ).equals( "version:" ) )
    {
      sendVersionInformationToClient( message );
      return true;
    }
    else if ( message.substring( 0, 9 ).equals( "username=" ) )
    {
      name = message.substring( 9, message.length() );
      loginUser();
      return true;
    }
    else if ( message.substring( 0, 12 ).equals( "font_exists:" ) )
    {
      final String fontFile = message.substring( 12 );
      if ( !(new File( main.FONT_LOCATION + fontFile + ".ttf" ).exists()
          || new File( main.FONT_LOCATION + fontFile + ".ttc" ).exists()) )
      {
        sendStringToClient( "font_notexists" );
      }
      else
      {
        sendStringToClient( "font_exists" );
      }
    }
    else if ( message.substring( 0, 12 ).equals( "font_needed:" ) )
    {
      final String fontFile = message.substring( 12 );
      if ( !(new File( main.FONT_LOCATION + fontFile + ".ttf" ).exists()
          || new File( main.FONT_LOCATION + fontFile + ".ttc" ).exists()) )
      {
        sendStringToClient( "font_needed_true" );
      }
      else
      {
        sendStringToClient( "font_needed_false" );
      }
      return true;
    }
    else if ( message.equals( "request_disconnect_now" ) )
    {
      disconnectUser();
      return false;
    }
    else if ( message.equals( "request_feed_now" ) )
    {
      Main.logger.info( "Sende vergangene Beiträge an Client " + name );
      sendStringToClient( "requested_feed:" + main.getFeed() );
      return true;
    }
    return true;
  }

  /**
   * Überprüft ob Benutzername des Clients bereits in Benutzung ist und gibt rückmledung an den Client.
   */
  private void loginUser()
  {
    Main.logger.info( name + " versucht sich einzuloggen" );
    if ( !names.contains( name ) )
    {
      names.add( name );
      sendStringToClient( "username" + name + "=true" );
      Main.logger.info( "Username " + name + " verfügbar" );
    }
    else
    {
      nameWasInUseAlready = true;
      sendStringToClient( "username" + name + "=false" );
      Main.logger.info( "Username " + name + " nicht verfügbar" );
    }
  }

  /**
   * Sendet dem Client Informationen zum Versionsabgleich
   *
   * @param inptuVersion Version mit der verglichen wird
   */
  private void sendVersionInformationToClient( final String inptuVersion )
  {
    final String version = inptuVersion.substring( 8 );
    if ( version.equals( main.VERSION ) )
    {
      sendStringToClient( "version_same" );
    }
    else
    {
      sendStringToClient( "version_diffrent:" + main.VERSION );
    }
  }

  /**
   * Loggt empfangene Nachrichten, speichert diese, dessen HashTags und sendet die Nachricht anschließend an
   * alle Clients.
   *
   * @param message Nachricht
   */
  private void receiveAndSendMessageToClients( final String message )
  {
    Main.logger.info( "HTML: " + message );
    Main.logger.info( "Einkommender Beitrag:" + System.lineSeparator() + "Text: "
        + Jsoup.parse( message ).text() );
    main.setFeed( message + main.getFeed() );
    main.saveToFeed( main.getFeed() );
    main.saveHashTags( message );
    broadcastMessage( "new_article:" + message );
  }

  /**
   * Sendet dem Client eine Liste aller HashTags.
   */
  private void sendHashTagsToClient()
  {
    String hashTags = "";
    if ( !main.allHashTags.isEmpty() )
    {
      for ( final String hashTag : main.allHashTags )
      {
        hashTags = hashTags + "|" + hashTag;
      }
      sendStringToClient( "hashtags:" + hashTags );
    }
    else
    {
      sendStringToClient( "hashtags:thehashtagsareempty" );
    }
  }

  /**
   * Sendet dem Client eine Liste aller verbundenen Clients.
   */
  private void sendConnectedClientsToServer()
  {
    Main.logger.info( "Sende Liste der Aktiven Clients an " + name );
    String wholeNames = "";
    for ( final String singleName : names )
    {
      wholeNames = wholeNames + "|SPLIT|" + singleName;
    }
    wholeNames = wholeNames.substring( 7 );
    sendStringToClient( "active_clients:" + wholeNames );
  }

  /**
   * Sendet eine Nachricht an alle verbundenen Clients
   *
   * @param message Nachricht welche zu senden ist
   */
  void broadcastMessage( final String message )
  {
    final byte[] b = Util.compress( message.getBytes( GeneralSharedInformation.CHARSET ) );
    if ( b != null )
    {
      for ( final Socket socketToSendTo : main.sockets )
      {
        DataOutputStream sendMessage;
        try
        {
          sendMessage = new DataOutputStream( socketToSendTo.getOutputStream() );
          sendMessage.writeInt( b.length );
          sendMessage.write( b );
        }
        catch ( final IOException e )
        {
          Main.logger.info( e.fillInStackTrace().toString() );
        }
      }
    }
    else
    {
      Main.logger.info( "Nachricht konnte nicht versendet werden da sie null entspricht." );
    }
  }

  /**
   * Sendet einen String als UTF-8 Encoded Byte Array an den Client.
   *
   * @param toSend String welcher zu senden ist
   */
  void sendStringToClient( final String toSend )
  {
    try
    {
      final byte[] b = Util.compress( toSend.getBytes( "utf-8" ) );
      try
      {
        if ( b != null )
        {
          out.writeInt( b.length );
          out.write( b );
        }
      }
      catch ( final IOException e )
      {
        Main.logger.info( e.fillInStackTrace().toString() );
      }
    }
    catch ( final UnsupportedEncodingException e )
    {
      Main.logger.info( e.fillInStackTrace().toString() );
    }
  }

  /**
   * Trennt die Verbindung zum Socket und entfernt den Client aus dem SocketArray
   */
  private void disconnectUser()
  {
    if ( !disconnected )
    {
      disconnected = true;
      Main.logger.info( "Verbindung zu '" + name + "' unterbrochen." );
      try
      {
        this.socket.close();
        Main.logger.info( "Socket von '" + name + "' wurde erfolgreich geschlosen." );
      }
      catch ( final IOException e )
      {
        Main.logger.info( "Socket von '" + name + "' konnte nicht geschlossen werden oder ist bereits geschlossen." );
      }
      main.removeClient( this.socket );
      if ( !nameWasInUseAlready )
      {
        names.remove( name );
      }
      Main.logger.info( "Schließe Thread des Users: '" + name + "'." );
      this.interrupt();
    }
  }
}