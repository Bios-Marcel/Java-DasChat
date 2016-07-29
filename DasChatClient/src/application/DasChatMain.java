package application;

/**
 * Main Klasse von das Chat, ruft lediglich die Methoden zum Laden der Einstellungen auf und Startet
 * anschließend die FX Applikation
 *
 * @author msc
 *
 */
public class DasChatMain
{
  /**
   * Lädt Einstellungen und ruft die Start Methode auf.
   *
   * @param args Übergeben Startparameter
   */
  public static void main( final String[] args )
  {
    new DasChatInit().init( args );
  }

}
