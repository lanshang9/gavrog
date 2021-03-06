package buoy.event;

import buoy.widget.*;
import java.awt.*;
import java.util.*;

/**
 * A RepaintEvent is generated by certain Widgets (including CustomWidgets and many WidgetContainers)
 * whenever a portion of it needs to be repainted.
 *
 * @author Peter Eastman
 */

public class RepaintEvent extends EventObject implements WidgetEvent
{
  private Widget widget;
  private Graphics2D graphics;

  /**
   * Create a RepaintEvent.
   *
   * @param widget     the Widget which needs to be painted
   * @param graphics   a Graphics2D object which can be used to paint the Widget
   */
  
  public RepaintEvent(Widget widget, Graphics2D graphics)
  {
    super(widget);
    this.widget = widget;
    this.graphics = graphics;
  }

  /**
   * Get the Widget which generated this event.
   */
  
  public Widget getWidget()
  {
    return widget;
  }
  
  /**
   * Get a Graphics2D which can be used to paint the Widget.
   */
  
  public Graphics2D getGraphics()
  {
    return graphics;
  }
}