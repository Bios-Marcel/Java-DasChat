package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Sorgt dafür vom Client gefunden zu werden, so das eine Angabe der Server IP beim Client nicht benötigt
 * wird.
 *
 * @author msc
 *
 */
public class DiscoveryThread implements Runnable
{

  /**
   * Getter für die DiscoveryThread Instanz
   *
   * @return Instanz des DiscoveryThreads
   */
  public static DiscoveryThread getInstance()
  {
    return DiscoveryThreadHolder.INSTANCE;
  }

  private static class DiscoveryThreadHolder
  {

    static final DiscoveryThread INSTANCE = new DiscoveryThread();
  }

  DatagramSocket socket;

  @Override
  public void run()
  {
    try
    {
      socket = new DatagramSocket( Main.getMain().discoveryPort, InetAddress.getByName( "0.0.0.0" ) );
      socket.setBroadcast( true );

      while ( true )
      {
        final byte[] recvBuf = new byte[15000];
        final DatagramPacket packet = new DatagramPacket( recvBuf, recvBuf.length );
        socket.receive( packet );

        Main.logger.info( "Discovery Paket von " + packet.getAddress().getHostAddress() + " empfangen." );
        Main.logger.info( "Daten: " + new String( packet.getData() ).trim() );

        final String message = new String( packet.getData() ).trim();
        if ( message.equals( "DISCOVER_SERVER_REQUEST" ) )
        {
          final byte[] sendData = "DISCOVER_SERVER_RESPONSE".getBytes();

          final DatagramPacket sendPacket = new DatagramPacket( sendData, sendData.length, packet.getAddress(), packet.getPort() );
          socket.send( sendPacket );

          Main.logger.info( "Paket an " + sendPacket.getAddress().getHostAddress() + " gesendet." );
        }
      }
    }
    catch ( final IOException ex )
    {
      Main.logger.info( "Discovery Thread konnte nicht gestartet werden." + System.lineSeparator() + ex.fillInStackTrace().toString() );
    }
  }

}