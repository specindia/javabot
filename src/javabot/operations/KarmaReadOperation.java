package javabot.operations;

import java.util.ArrayList;
import java.util.List;

import javabot.BotEvent;
import javabot.Javabot;
import javabot.Message;

import com.rickyclarkson.java.util.TypeSafeList;

/**
 * @author ricky_clarkson
 */
public class KarmaReadOperation implements BotOperation {
    /**
     * @see javabot.operations.BotOperation#handleMessage(javabot.BotEvent)
     */
    public List handleMessage(BotEvent event) {
        List messages = new TypeSafeList(new ArrayList(), Message.class);

        String message = event.getMessage();
        String channel = event.getChannel();
        String sender = event.getSender();

        Javabot bot = event.getBot();

        if (!message.startsWith("karma "))
            return messages;

        String nick = message.substring("karma ".length());

        nick = nick.toLowerCase();

        if (nick.indexOf(" ") != -1) {
            messages
                .add(new Message(channel,
                    "I've never seen a nick with a space " + "in, " + sender,
                    false));

            return messages;
        }

        int karma;

        try {
            karma = Integer.parseInt(bot.getFactoid("karma " + nick));
        } catch (Exception exception) {
            karma = 0;
        }

        if (nick.equals(sender)) {
            messages.add(new Message(channel, sender
                + ", you have a karma level of " + karma + ".", false));
        } else {
            messages.add(new Message(channel, nick + " has a karma level of "
                + karma + ", " + sender, false));
        }

        return messages;
    }

    public List handleChannelMessage(BotEvent event)
    {
	    	return new TypeSafeList(new ArrayList(),Message.class);
    }
}
