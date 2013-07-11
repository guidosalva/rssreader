package reader.data

import java.net.MalformedURLException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import scala.xml.Node
import scala.xml.NodeSeq

import react.events.Event
import react.events.ImperativeEvent
import react.events.Observable
import reader.common.sequence

/**
* The XMLParser is responsible for the translation of xml to the
* internal represantation of the RSS Feed
*
*/
class XmlParser {
  val explicitItemParsed = new ImperativeEvent[RSSItem] //#EVT
  
  // only for clarity in event expressions below
  private def discardArgument[A](tuple: (Any,A)): A = tuple._2
  private def parseSuccessfull[A](res: Option[A]): Boolean = res.isDefined
  
  lazy val itemParsed: Event[RSSItem] = //#EVT
    ((parseItem.after map discardArgument[Option[RSSItem]]) && //#EF //#EF
        { parseSuccessfull(_) } map { o: Option[RSSItem] => o.get }) || explicitItemParsed //#EF
  
  lazy val channelParsed: Event[RSSChannel] = //#EVT
    (parseChannel.after map discardArgument[Option[RSSChannel]]) && //#EF //#EF
        { parseSuccessfull(_) } map { o: Option[RSSChannel] => o.get }
  
  lazy val entityParsed  = channelParsed.dropParam || itemParsed.dropParam //#EVT //#EF
  
  val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
  
  /**
   * Parses a RSSChannel from the given xml NodeSeq, does NOT set the source url
   *
   * @param xmlNode - the xml data to parse
   *
   * @return
   * 	None if the xml could not be parsed
   * 	Some(RssChannel) otherwise
   */
  def parseChannelWithoutURL(xmlNode: NodeSeq): Option[RSSChannel] = {
    // version of parseChannel without URL because it is not
    // always guaranteed that we know the URL
    parseChannel(xmlNode, None)
  }
  
  /**
   * Parses a RSSChannel from the given xml NodeSeq and sets the source url
   *
   * @param xmlNode - the xml data to parse
   * @param url     - the url the channel was retrieved from
   *
   * @return
   * 	None if the xml could not be parsed
   * 	Some(RssChannel) otherwise
   */
  def parseChannelWithURL(xmlNode: NodeSeq, url: URL): Option[RSSChannel] = {
    parseChannel(xmlNode, Some(url))
  }
  
  private val parseChannel = Observable {
    (args: (NodeSeq, Option[URL])) =>
      val (xmlNode, url) = args
      
      if (xmlNode.size == 1) {
        val meta = extractInformation(xmlNode)
        val date = extractDate(xmlNode)
        val link = tryToCreateURL(meta('link))
        
        val result = RSSChannel(meta('title), link, meta('description), date, url)
        Some(result)
      }
      else
        None
  }
  
  /**
   * Parses a RSSItem from the given NodeSeq
   *
   * NOTE: does not set the sourceChannel
   *
   * @param xmlNode - xml data to parse
   *
   * @return
   * 	None if the xml could not be parsed
   * 	Some(RssItem) otherwise
   */
  val parseItem = Observable {
    (xmlNode: Node) => parseItemSilent(xmlNode)
  }
  
  // does not fire events after parsing
  private def parseItemSilent(xmlNode: Node): Option[RSSItem] = {
    if (xmlNode.size != 1)
      return None
    
    val meta = extractInformation(xmlNode)
    val date = extractDate(xmlNode)
    val link = tryToCreateURL(meta('link))
    
    val result = RSSItem(meta('title), link, meta('description), date, None)
    Some(result)
  }
  
  /**
  * Parses the given xml into the RSS Channel and RSS Item classes
  *
  * @param data - the xml data to parse
  * @param url  - the source url for the channel
  *
  * @return
  *   On success:
  *     a tuple of the channel with a sequence of its items
  *     wrapped in an option
  *   On failure:
  *     None
  */
  def parseRSS(data: NodeSeq, url: URL): Option[(RSSChannel, Seq[RSSItem])] = {
    val channelXML = data \ "channel"
    val itemXML = channelXML \ "item"
    // NOTE: we are not using parseItem
    //       because of the call to RSSItem.changeSource below
    val itemsOpt = sequence((itemXML map { parseItemSilent(_) }).toList)
    
    for {
      channel <- parseChannel(channelXML, Some(url))
      items <- itemsOpt.map { items =>
        items.map { i => RSSItem.changeSource(i, Some(channel)) } }
    }
    yield {
      items foreach { explicitItemParsed(_) }
      (channel, items)
    }
  }
  
  private def tryToCreateURL(s: String): Option[URL] = {
    try
      Some(new URL(s))
    catch {
      case _: MalformedURLException => None
    }
  }
  
  private def extractDate(xml: NodeSeq): Option[Date] = {
    val res = xml \ "pubDate"
    
    if (res.isEmpty)
      None
    else
      try
        Some(dateFormat parse res.text)
      catch {
        case _: ParseException => None
      }
  }
  
  private def extractInformation(xml: NodeSeq): Map[Symbol,String] =
    Map('title -> xml \ "title",
        'link -> xml \ "link",
        'description -> xml \ "description") mapValues { _.text }
}
