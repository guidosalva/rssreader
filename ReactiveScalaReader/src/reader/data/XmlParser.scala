package reader.data

import scala.xml._
import java.text._
import scala.events._
import java.util.Date
import java.net._
import reader.common._
import java.util.Locale

/**
* The XMLParser is responsible for the translation of xml to the
* internal represantation of the RSS Feed
*
*/
class XmlParser {
  val explicitItemParsed = new ImperativeEvent[RSSItem]

  // only for clarity in event expressions below
  private def discardArgument[A](tuple: (Any,A)): A = tuple._2
  private def parseSuccessfull[A](res: Option[A]): Boolean = res.isDefined

  lazy val itemParsed: Event[RSSItem] = {
    ( ( parseItemObservable.after map discardArgument[Option[RSSItem]] ) &&
      { parseSuccessfull(_) } map { o: Option[RSSItem] => o.get } ) || explicitItemParsed
  }

  lazy val channelParsed: Event[RSSChannel] = {
    ( parseChannelObservable.after map discardArgument[Option[RSSChannel]] ) &&
      { parseSuccessfull(_) } map { o: Option[RSSChannel] => o.get }
  }

  lazy val entityParsed  = channelParsed.dropParam || itemParsed.dropParam

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
    parseChannelObservable(xmlNode,None)
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
  def parseChannelWithURL(xmlNode: NodeSeq,url: URL): Option[RSSChannel] = {
    parseChannelObservable(xmlNode,Some(url))
  }

  private val parseChannelObservable = Observable {
    (xml_and_url: (NodeSeq,Option[URL])) => parseChannelSilent(xml_and_url._1,xml_and_url._2)
  }

  // does not fire events
  private def parseChannelSilent(xmlNode: NodeSeq, url: Option[URL]): Option[RSSChannel] = {
    if (xmlNode.size != 1) return None

    val meta = extractInformation(xmlNode)
    val date = extractDate(xmlNode)
    val link = tryToCreateURL(meta('link))

    val result = RSSChannel( meta('title) , link
                           , meta('description) , date , url )

    Some(result)
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
  def parseItem(xmlNode: Node): Option[RSSItem] = {
    parseItemObservable(xmlNode)
  }

  private val parseItemObservable = Observable {
    (xmlNode: Node) => parseItemSilent(xmlNode)
  }

  // does not fire events after parsing
  private def parseItemSilent(xmlNode: Node): Option[RSSItem] = {
    if (xmlNode.size != 1) return None

    val meta = extractInformation(xmlNode)
    val date = extractDate(xmlNode)
    val link = tryToCreateURL(meta('link))

    val result = RSSItem( meta('title), link
                 , meta('description),date,None)

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
  def parseRSS(data: NodeSeq,url: URL): Option[(RSSChannel,Seq[RSSItem])] = {
    val channelXML = data \ "channel"
    val itemXML = channelXML \ "item"
    // NOTE: we are not using parseItemObservable
    //       because of the call to RSSItem.changeSource below
    val itemsOpt = sequence( itemXML map { parseItemSilent(_) } toList )

    for {
      channel <- parseChannelObservable(channelXML,Some(url))
      items <- itemsOpt.map { items =>
        items.map { i => RSSItem.changeSource(i, Some(channel)) }
      }

    } yield {
      items.foreach { explicitItemParsed(_) }
      (channel,items)
    }

  }

  private def tryToCreateURL(s: String): Option[URL] = {
    try {
      Some(new URL(s))
    } catch {
      case _:MalformedURLException => None
    }
  }

  private def extractDate(xml: NodeSeq): Option[Date] = {
    val res = xml \ "pubDate"

    if (!(res isEmpty)) {
      try {
        Some(dateFormat.parse(res.text))
      } catch {
        case _:ParseException => None
      }
    } else {
     None
    }
  }

  private def extractInformation(xml: NodeSeq): Map[Symbol,String] =
    Map( 'title -> xml \ "title",
         'link -> xml \ "link",
         'description -> xml \ "description"
       ) mapValues { _.text }
}