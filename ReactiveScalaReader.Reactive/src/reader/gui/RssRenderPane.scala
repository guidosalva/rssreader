package reader.gui

import java.awt.Desktop
import java.io.IOException
import scala.swing.EditorPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import reader.data.RSSItem
import scala.events.behaviour.Signal

/**
* Displays the content of an single RSS Item
*/
class RssItemRenderPane(item: Signal[Option[RSSItem]]) extends EditorPane {
  super.editable = false
  super.contentType = "text/html"
    
  peer.addHyperlinkListener(new HyperlinkListener() {
    def hyperlinkUpdate(e: HyperlinkEvent) =
      if (e.getEventType == HyperlinkEvent.EventType.ENTERED)
        peer.setToolTipText(e.getDescription)
      else if (e.getEventType == HyperlinkEvent.EventType.EXITED)
        peer.setToolTipText(null)
      else if (e.getEventType == HyperlinkEvent.EventType.ACTIVATED)
        try
            Desktop.getDesktop.browse(e.getURL.toURI)
        catch {
          case e: IOException => e.printStackTrace
        }
  })
  
  item.changed += {
    case Some(item) =>
      text = List("<b>Title:</b>",
                  item.title,
                  "<b>Publication date:</b>",
                  item.pubDate.getOrElse("Unknown"),
                  "<b>Source:</b>",
                  item.link.map { link => "<a href="+link+">"+link+"</a>"}.getOrElse("Unknown"),
                  "<hr>",
                  item.description).mkString("<br>")
    case None =>
      text = ""
  }
}
