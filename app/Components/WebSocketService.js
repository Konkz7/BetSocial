// WebSocketService.js
import { Client } from '@stomp/stompjs';
import { IP_STRING } from '../Constants';
 
class WebSocketService {
  constructor() {
    this.stompClient = null;
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
          
          if (!message.description?.trim() && message.online === undefined){return} else{console.log ("Message: ", message.description);};
          onMessageReceived(message);
        });
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

  sendMessage(messageObj) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: `/app/send`,
        body: JSON.stringify(messageObj),
      });

      console.log(JSON.stringify(messageObj));
    } else {
      console.error("Can't send message, STOMP client not connected");
    }
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