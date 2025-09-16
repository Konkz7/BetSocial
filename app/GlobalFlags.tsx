let currentScreen : string | undefined;


export const screenStore = {
  get: () => currentScreen,
  set: (name: string | undefined) => {
    currentScreen = name;
  }
};

let seenActivity: boolean = false;


export const activitySeenStore = {
  get: () => seenActivity, 
  set: (value:boolean) => {
    seenActivity = value;
    console.log("Activity seen status:", value);
  }
};