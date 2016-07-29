package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

import util.Util;

/**
 * @author msc
 *
 */
public class FontTransferThread extends Thread
{
  private final Main       main;

  private final Socket     socket;

  private File             localOutputFile;

  private FileOutputStream localFileOutput;

  private DataOutputStream sendStream;

  private DataInputStream  receiveStream;

  private boolean          done = true;

  /**
   * @param socket this.socket
   */
  public FontTransferThread( final Socket socket )
  {
    this.socket = socket;
    main = Main.getMain();
  }

  /**
   * Regelt die Kommunikation zwischen Client und Server
   */
  private void listenToClient()
  {
    final Thread listen = new Thread( () ->
    {
      try
      {
        while ( true )
        {
          if ( done )
          {
            String inputLine = null;
            final int length = receiveStream.readInt();
            if ( length != 0 )
            {
              byte[] message = new byte[length];
              receiveStream.readFully( message, 0, length );
              message = Util.decompress( message );
              inputLine = new String( message );
              if ( inputLine.substring( 0, 12 ).equals( "font_exists:" ) )
              {
                final String fontFile = inputLine.substring( 12 );
                if ( !new File( main.getFontLocation() + fontFile + ".ttf" ).exists()
                /* || new File( main.getFontLocation() + fontFile + ".ttc" ).exists()*/ )
                {
                  sendStringToClient( "font_notexists" );
                }
                else
                {
                  sendStringToClient( "font_exists" );
                }
              }
              else if ( inputLine.substring( 0, 12 ).equals( "font_needed:" ) )
              {
                final String fontFile = inputLine.substring( 12 );
                if ( !new File( main.getFontLocation() + fontFile + ".ttf" ).exists()
                /*|| new File( main.getFontLocation() + fontFile + ".ttc" ).exists()*/ )
                {
                  sendStringToClient( "font_needed_true" );
                }
                else
                {
                  sendStringToClient( "font_needed_true" );
                }
              }
              else if ( inputLine.contains( "send_font:" ) )
              {
                localOutputFile = new File( main.getFontLocation() + inputLine.replace( "send_font:", "" ) + ".ttf" );
                done = false;
                if ( !localOutputFile.exists() )
                {
                  done = receiveFont();
                }
                else
                {
                  done = true;
                  localFileOutput.close();
                  sendStringToClient( "DontSend" );
                }
              }
              else if ( inputLine.contains( "request_font:" ) )
              {
                final String fontName = inputLine.replace( "request_font:", "" );
                final File fontFile = new File( inputLine.replace( "request_font:", "" ) );
                try (FileInputStream fileInputStream = new FileInputStream( main.getFontLocation() + fontFile );)
                {
                  done = true;
                  sendStringToClient( "sending_font:" + fontName );
                  sendBytesToClient( IOUtils.toByteArray( fileInputStream ) );
                }
              }
            }
          }
        }
      }
      catch ( final Exception e )
      {
        done = true;
      }
    } );
    listen.start();
  }

  /**
   * Gibt dem Client Bescheid das die Font gesendet werden kann und wartet auf Empfang der diesen.
   *
   * @return Erfolgreich oder nicht
   */
  private boolean receiveFont()
  {
    try
    {
      if ( sendStringToClient( "SendNow" ) )
      {
        final int length = receiveStream.readInt();
        if ( length != 0 )
        {
          byte[] message = new byte[length];
          receiveStream.readFully( message, 0, length );
          message = Util.decompress( message );
          localFileOutput = new FileOutputStream( localOutputFile );
          localFileOutput.write( message, 0, message.length );
          localFileOutput.close();
          Main.logger.info( "Font Datei " + localOutputFile.getName() + " erhalten" );
        }
        return true;
      }
      return false;
    }
    catch ( final IOException e )
    {
      Main.logger.warning( e.fillInStackTrace().toString() );
      return true;
    }
  }

  @Override
  public void run()
  {
    try
    {
      sendStream = new DataOutputStream( socket.getOutputStream() );
      receiveStream = new DataInputStream( socket.getInputStream() );
    }
    catch ( final IOException e )
    {
      Main.logger.info( "ERR" + e.getMessage() );
    }
    listenToClient();
  }

  /**
   * Wandelt gegebenen String in byte array um, komprimiert diesen und sendet ihn zum Server.
   *
   * @param message zu sendende Nachricht
   * @return Erfolgreich oder nicht
   */
  private boolean sendStringToClient( final String message )
  {
    final byte[] bytesToSend = new String( message ).getBytes();
    return sendBytesToClient( bytesToSend );
  }

  /**
   * Komprimiert gegebenen byte array und sendet diesen zum Server.
   *
   * @param bytes zu sendende bytes
   * @return Erfolgreich oder nicht
   */
  private boolean sendBytesToClient( final byte[] bytes )
  {
    byte[] bytesToSend = bytes;
    bytesToSend = Util.compress( bytesToSend );
    try
    {
      sendStream.writeInt( bytesToSend.length );
      sendStream.write( bytesToSend );
      return true;
    }
    catch ( final IOException exception )
    {
      Main.logger.warning(
          "Nachricht konnte nicht an Client gesendet werden" + System.lineSeparator() + exception.fillInStackTrace() );
      return false;
    }
  }
}
