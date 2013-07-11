package reader.connectors

import reader.data.FeedStore
import reader.data.XmlParser
import reader.network.Fetcher
import reader.network.UrlChecker

trait EventMediator {
  def mediate(fetcher: Fetcher,
              parser: XmlParser,
              store: FeedStore,
              checker: UrlChecker): Unit
}

/**
 * The CentralizedEvents class connects the fetcher, parser and the feed store,
 * as they have no knowledge about each other.
 * CentralizedEvents in specific uses the classses as follows:
 *   1. the fetcher fetches a xml feed
 *   2. the parser is connected to the rssFetched event of the fetcher and parses the data
 *   3. the store is connected to events which are triggerd after the parser has parsed channel or an item
 *   4. if the checker has a valid (checked) url then it is added to the fetcher
 */
object CentralizedEvents extends EventMediator {
  def mediate(fetcher: Fetcher, parser: XmlParser, store: FeedStore, checker: UrlChecker) {
    fetcher.rssFetched += { case (xml, url) => parser.parseRSS(xml, url) } //#HDL
  }
}

object SimpleReporter extends EventMediator {
  def mediate(fetcher: Fetcher, parser: XmlParser, store: FeedStore, checker: UrlChecker) {
    store.channels.changed += { x => println("Channels in store changed. Size: " + x.size) } //#HDL //#IF
    
    fetcher.rssFetched += { _ => println("New content fetched") } //#HDL
    
    parser.channelParsed += { _ => println("A channel was parsed") } //#HDL
    parser.itemParsed    += { _ => println("An item was parsed")   } //#HDL
    
    fetcher.state.changed += println _ //#IF //#HDL
  }
}
