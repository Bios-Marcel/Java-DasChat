package main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatiert die Log Ausgaben des Servers.
 *
 * @author msc
 *
 */
public class LogFormatter extends Formatter
{

  @Override
  public String format( LogRecord arg0 )
  {
    return "[" + getFormattedDateAsString( new Date() ) + "] " + arg0.getMessage() + System.lineSeparator();
  }

  private static String getFormattedDateAsString( Date date )
  {
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "HH:mm:ss dd.MM.yy" );
    return dateFormat.format( date );
  }
}
