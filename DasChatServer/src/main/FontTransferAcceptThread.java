package main;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Verwaltet die Threads für das senden von Fonts
 *
 * @author msc
 *
 */
public class FontTransferAcceptThread extends Thread
{
  @Override
  public void run()
  {
    try (ServerSocket serverSocket = new ServerSocket( Main.getMain().fontTransferPort ))
    {
      while ( true )
      {
        new FontTransferThread( serverSocket.accept() ).start();
      }
    }
    catch ( final IOException e )
    {
      //TODO Tell Server that there is a problem?
      Main.logger.warning( e.fillInStackTrace().toString() );
    }
  }
}
