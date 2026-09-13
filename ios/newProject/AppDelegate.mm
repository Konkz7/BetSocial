#import "AppDelegate.h"

#import <React/RCTBundleURLProvider.h>
#import <Firebase.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  // Before React Native starts, and before any JavaScript can ask Firebase for
  // anything. react-native-firebase does not call this for you on iOS the way
  // the Android build does through its Gradle plugin - without it every Firebase
  // call fails with "Default FirebaseApp is not initialized", which covers push,
  // the phone OTP at registration, and the upload identity the chat socket needs.
  //
  // It reads GoogleService-Info.plist from the bundle. That file is gitignored,
  // so a fresh clone has to download it from the Firebase console first - see the
  // iOS section of the README.
  if ([FIRApp defaultApp] == nil) {
    [FIRApp configure];
  }

  self.moduleName = @"newProject";
  // You can add your custom initial props in the dictionary below.
  // They will be passed down to the ViewController used by React Native.
  self.initialProps = @{};

  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

- (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
{
  return [self bundleURL];
}

- (NSURL *)bundleURL
{
#if DEBUG
  return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index"];
#else
  return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
#endif
}

@end
