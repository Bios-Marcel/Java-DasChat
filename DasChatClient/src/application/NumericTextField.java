package application;

import javafx.scene.control.TextField;

/**
 * TextFeld was nur zahlen zulässt
 *
 * @author msc
 *
 */
public class NumericTextField extends TextField
{

  @Override
  public void replaceText( int start, int end, String text )
  {
    if ( text.matches( "[0-9]" ) || text == "" )
    {
      super.replaceText( start, end, text );
    }
  }

  @Override
  public void replaceSelection( String text )
  {
    if ( text.matches( "[0-9]" ) || text == "" )
    {
      super.replaceSelection( text );
    }
  }

}