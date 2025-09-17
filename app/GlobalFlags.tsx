let currentScreen : string | undefined;


export const screenStore = {
  get: () => currentScreen,
  set: (name: string | undefined) => {
    currentScreen = name;
  }
};

let seenActivity: boolean = true;


export const activitySeenStore = {
  get: () => seenActivity, 
  set: (value:boolean) => {
    seenActivity = value;
    console.log("Activity seen status:", value);
  }
};

let seenMessages: boolean = true;


export const messageSeenStore = {
  get: () => seenMessages, 
  set: (value:boolean) => {
    seenMessages = value;
    console.log("Message seen status:", value);
  }
};