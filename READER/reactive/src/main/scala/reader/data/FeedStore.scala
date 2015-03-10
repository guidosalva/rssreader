package reader.data

import rescala.events.ImperativeEvent
import rescala.Signal
import rescala.Var
import makro.SignalMacro.{ SignalM => Signal }

/**
 * The FeedStore stores RSSChannels and RSSItems.
 * More specifically, it stores the relation between an RSS Item and its channel
 * to enable clients to ask e.g. for all items stored related to a specific channel.
 */
class FeedStore {
  private val channelToItems = Var(Map.empty[RSSChannel, Var[Set[RSSItem]]])
  
  val channels = Signal {
    channelToItems() map { case (channel, items) => (channel, Signal { items() }) } }
  
  final val itemAdded = new ImperativeEvent[RSSItem]
  
  def addChannel(channel: RSSChannel) =
    channelToItems() = channelToItems.get + (channel -> Var(Set.empty))
  
  /*
   * Check whether the item:
   *   - has a source channel
   *   - the channel of the item is being tracked (in the map)
   *   - the item is not yet stored
   * if all of these hold, return true
   */
  private def addItemAllowed(item: RSSItem): Boolean = {
    val res = for { channel <- item.srcChannel
                    items   <- channelToItems.get get channel
                    if (!(items.get contains item))
                  } yield Some(true)
    res.isDefined
  }
  
  def addItem(item: RSSItem) =
    if (addItemAllowed(item)) {
      val channel = item.srcChannel.get
      channelToItems.get(channel)() += item
      itemAdded(item)
    }
}
