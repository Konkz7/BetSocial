// WebSocketService.js
import { Client } from '@stomp/stompjs';
import { IP_STRING } from '../Constants';
 
/**
 * The most messages held while the socket is down.
 *
 * A cap rather than an unbounded list: somebody typing into a chat with no
 * connection should not be able to grow this until the app runs out of memory.
 * Fifty is far more than the few seconds of connecting this exists to cover.
 */
const MAX_QUEUED = 50;

class WebSocketService {
  constructor() {
    this.stompClient = null;

    /**
     * Messages typed before the socket was ready.
     *
     * Opening a chat starts connecting and returns immediately, so the first
     * message is regularly typed and sent before onConnect has fired. Sending
     * used to log "STOMP client not connected" and drop it - while the chat
     * screen cleared the input either way, so the text vanished exactly as if it
     * had been delivered. The same happens for the reconnect window after a
     * network drop, which is up to reconnectDelay long.
     */
    this.outbox = [];
  }

  connect(uid,gid, onMessageReceived) {
    this.stompClient = new Client({
      brokerURL: `ws://${IP_STRING.replace(/^http:\/\//, '')}/ws`, // Use ws:// not http://

      connectHeaders: {
        userId: String(uid), // Send userId as a native STOMP header
        chatId: String(gid),
      },
      
      debug: (str) => {
        console.log(str);
        //console.log(`ws://${IP_STRING.replace(/^http:\/\//, '')}/ws`);
      },
      reconnectDelay: 5000, // Optional: retry on disconnect
      onConnect: () => {
        console.log('Connected');
        this.stompClient.subscribe(`/topic/chat/${gid}`, (frame) => {
          const message = JSON.parse(frame.body);
          console.log(message.online);

          onMessageReceived(message);
        });

        // After subscribing, so anything that was waiting comes back on the
        // topic like a normal message rather than being sent into a chat this
        // client is not listening to yet.
        this.flush();
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
      forceBinaryWSFrames: true,
      appendMissingNULLonIncoming: true,
    });

    this.stompClient.activate();
  }

  /**
   * Sends a message, or holds on to it until the socket is up.
   *
   * @return whether it went out now. Queued is not failed - the chat screen is
   *         right to clear the input either way, because the message is still
   *         going to be delivered.
   */
  sendMessage(messageObj) {
    if (!messageObj.description?.trim()){return false} else{console.log ("Message: ", messageObj.description);};

    if (this.stompClient && this.stompClient.connected) {
      this.publish(messageObj);
      console.log(JSON.stringify(messageObj));
      return true;
    }

    if (this.outbox.length >= MAX_QUEUED) {
      // Oldest first. If the connection has been gone long enough to fill this,
      // what somebody typed a moment ago is worth more than what they typed
      // several minutes before it.
      this.outbox.shift();
      console.warn(`Holding more than ${MAX_QUEUED} messages; dropped the oldest`);
    }

    this.outbox.push(messageObj);
    console.log(`Socket is not up yet; holding this message (${this.outbox.length} waiting)`);
    return false;
  }

  publish(messageObj) {
    this.stompClient.publish({
      destination: `/app/send`,
      body: JSON.stringify(messageObj),
    });
  }

  /**
   * Sends everything that was waiting, in the order it was typed.
   *
   * Emptied before publishing rather than after: publish can throw, and a list
   * that is cleared only on success would send the whole backlog again on the
   * next reconnect.
   */
  flush() {
    if (this.outbox.length === 0) {
      return;
    }

    const waiting = this.outbox;
    this.outbox = [];

    waiting.forEach((messageObj) => this.publish(messageObj));
    console.log(`Sent ${waiting.length} message(s) that were waiting for the socket`);
  }

  addUser(gid) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: `/app/api/messages/add-user/${gid}`,
      });
    } else {
      console.error("Can't send message, STOMP client not connected");
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }
}

export default new WebSocketService();