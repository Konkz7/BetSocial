import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  Modal,
  TextInput,
  Alert,
  Linking,
  StyleSheet,
} from 'react-native';
// Replace these with your preferred RN icon library or custom icons
import { 
  ArrowLeft,
  ChevronRight,
  User,
  Bell,
  Lock,
  Wallet,
  UserX,
  Download,
  Trash2,
  HelpCircle,
  LogOut,
  Languages,
  Moon,
  Shield,
  Bookmark,
} from 'lucide-react-native';
import { useFocusEffect } from '@react-navigation/native';
import { screenStore } from './GlobalFlags';
import { requestDataDownloadLink, deleteMyAccount } from './API';



interface SettingGroupProps {
  title: string;
  children: React.ReactNode;
}

const SettingGroup: React.FC<SettingGroupProps> = ({ title, children }) => (
  <View style={styles.groupContainer}>
    {title !== '' && <Text style={styles.groupTitle}>{title}</Text>}
    <View style={styles.groupContent}>{children}</View>
  </View>
);

interface SettingItemProps {
  icon: React.ReactNode;
  label: string;
  value?: string;
  toggle?: boolean;
  onClick?: () => void;
}

const SettingItem: React.FC<SettingItemProps> = ({
  icon,
  label,
  value,
  toggle,
  onClick,
}) => (
  <TouchableOpacity style={styles.itemContainer} onPress={onClick}>
    <View style={styles.itemLeft}>
      <View style={styles.iconWrapper}>{icon}</View>
      <Text style={styles.itemLabel}>{label}</Text>
    </View>
    <View style={styles.itemRight}>
      {value && <Text style={styles.itemValue}>{value}</Text>}
      {toggle ? (
        <View style={styles.toggleContainer}>
          <View style={styles.toggleCircle} />
        </View>
      ) : (
        <ChevronRight size={20} color="#9CA3AF" />
      )}
    </View>
  </TouchableOpacity>
);

const SettingsScreen = ({navigation}: any) => {

  const [askingPassword, setAskingPassword] = useState(false);
  const [password, setPassword] = useState('');
  const [deleting, setDeleting] = useState(false);

  useFocusEffect(
    useCallback(() => {
      screenStore.set("Settings");
      console.log("Screen is focused! Perform refresh or action here.");
      return () => {
        console.log("Screen is unfocused! Cleanup if needed.");
      };
    }, [])
  );

  /**
   * Saves the export as a file, by handing a link to the phone's browser.
   *
   * This used to put the whole export into the share sheet as a message. That
   * satisfied the right of access in the narrow sense - the data reached the
   * person - but what they received was a wall of JSON in a text field, and for
   * an account of any size the intent is too large to deliver at all.
   *
   * The app cannot write the file itself. From Android 10 the public Downloads
   * folder is closed to ordinary file writes, and React Native's Share takes a
   * string rather than a file on Android, so anything written would land where
   * its owner could not get at it. The browser can do both, so it does - with a
   * link that works once and expires in two minutes, because it has to carry its
   * own permission.
   */
  const downloadMyData = async () => {
    const url = await requestDataDownloadLink();
    if (!url) { return; }

    const opened = await Linking.openURL(url).then(() => true).catch(() => false);

    if (!opened) {
      // No browser to hand it to. Rare, but silently doing nothing after
      // somebody asks for their own data is the wrong way to fail.
      Alert.alert(
        'Couldnt open your browser',
        'Your data is ready but this device would not open the link. '
          + 'The download expires in two minutes, so try again when you can.',
      );
    }
  };

  /**
   * Two steps, then a password.
   *
   * The first alert says what happens, because "your posts stay, your name does
   * not" is not what people assume the word delete means, and finding out
   * afterwards is too late. The password is the server's requirement, not this
   * screen's - see AccountDataService.
   */
  const confirmDelete = () => {
    Alert.alert(
      'Delete your account?',
      'Your name, email, phone number, bio and picture are removed and you will '
        + 'not be able to sign in again.\n\n'
        + 'Your posts, comments and messages stay, shown as "Deleted user" - they '
        + 'are part of other people\'s threads and conversations.\n\n'
        + 'This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Continue',
          style: 'destructive',
          // A modal rather than Alert.prompt, which is iOS-only. Using it would
          // have left Android with a button that does nothing - and being able to
          // delete your account from inside the app is the requirement.
          onPress: () => setAskingPassword(true),
        },
      ],
    );
  };

  const submitDelete = async () => {
    setDeleting(true);
    const ok = await deleteMyAccount(password);
    setDeleting(false);

    if (ok) {
      setAskingPassword(false);
      setPassword('');
      Alert.alert('Account deleted', 'Your personal data has been removed.');
      navigation.navigate('Login');
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Settings</Text>
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <SettingGroup title="My Profile">
          <SettingItem
            icon={<User size={24} color="#6B7280" />}
            label="Personal Information"
            onClick={() => console.log('Navigate to profile')}
          />
          {/* "Payment Methods - Visa ****4242" stood here: a hardcoded card that
              never existed, left from the deprecated card screen. Coins cannot be
              bought, so there is no payment method to show. */}
          <SettingItem
            icon={<UserX size={24} color="#6B7280" />}
            label="Blocked Accounts"
            onClick={() => navigation.navigate('BlockedUsers_SP')}
          />
          <SettingItem icon={<Bookmark size={24} color="#6B7280" />} label="Saved Bets" />

        </SettingGroup>

        {/* UK GDPR gives both of these, and Apple requires an app that lets
            people create an account to let them delete it from inside the app -
            a link to an email address does not count. */}
        <SettingGroup title="Your Data">
          <SettingItem
            icon={<Download size={24} color="#6B7280" />}
            label="Download My Data"
            onClick={downloadMyData}
          />
          <SettingItem
            icon={<Trash2 size={24} color="#9E3B34" />}
            label="Delete My Account"
            onClick={confirmDelete}
          />
        </SettingGroup>
        <SettingGroup title="Preferences">
          <SettingItem
            icon={<Bell size={24} color="#6B7280" />}
            label="Notifications"
            toggle={true}
          />
          <SettingItem
            icon={<Languages size={24} color="#6B7280" />}
            label="Language"
            value="English"
          />
          <SettingItem
            icon={<Moon size={24} color="#6B7280" />}
            label="Dark Mode"
            toggle={false}
          />
        </SettingGroup>
        <SettingGroup title="Privacy & Security">
          <SettingItem icon={<Shield size={24} color="#6B7280" />} label="Privacy Settings" />
          <SettingItem icon={<Lock size={24} color="#6B7280" />} label="Security" />
          <SettingItem icon={<Lock size={24} color="#6B7280" />} label="Change Password" />
        </SettingGroup>
        <SettingGroup title="Support">
          <SettingItem icon={<HelpCircle size={24} color="#6B7280" />} label="Help Center" />
        </SettingGroup>
        <SettingGroup title="">
          <SettingItem
            icon={<LogOut size={24} color="#6B7280" />}
            label="Log Out"
            onClick={() => console.log('Log out')}
          />
        </SettingGroup>
      </ScrollView>

      <Modal
        visible={askingPassword}
        transparent
        animationType="fade"
        onRequestClose={() => setAskingPassword(false)}
      >
        <View style={styles.backdrop}>
          <View style={styles.sheet}>
            <Text style={styles.sheetTitle}>Enter your password</Text>
            <Text style={styles.sheetBody}>
              This confirms it is you. Deleting cannot be undone.
            </Text>

            <TextInput
              style={styles.passwordInput}
              placeholder="Password"
              placeholderTextColor="#9CA3AF"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              autoCapitalize="none"
              autoFocus
            />

            <View style={styles.sheetActions}>
              <TouchableOpacity
                style={[styles.sheetButton, styles.sheetCancel]}
                disabled={deleting}
                onPress={() => { setAskingPassword(false); setPassword(''); }}
              >
                <Text style={styles.sheetCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.sheetButton, styles.sheetDelete]}
                disabled={deleting || password.length === 0}
                onPress={submitDelete}
              >
                <Text style={styles.sheetDeleteText}>
                  {deleting ? 'Deleting…' : 'Delete'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    paddingHorizontal: 28,
  },
  sheet: {
    backgroundColor: 'white',
    borderRadius: 14,
    padding: 20,
  },
  sheetTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },
  sheetBody: { fontSize: 13, color: '#6b7280', marginTop: 6, lineHeight: 18 },
  passwordInput: {
    marginTop: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    color: '#111827',
  },
  sheetActions: { flexDirection: 'row', marginTop: 18 },
  sheetButton: { flex: 1, paddingVertical: 12, borderRadius: 10, alignItems: 'center' },
  sheetCancel: { backgroundColor: '#f3f4f6', marginRight: 8 },
  sheetDelete: { backgroundColor: '#9E3B34' },
  sheetCancelText: { color: '#374151', fontWeight: '600' },
  sheetDeleteText: { color: 'white', fontWeight: '600' },

  container: {
    flex: 1,
    backgroundColor: '#fcfcf7',
  },
  header: {
    width: '100%',
    backgroundColor: '#fff',
    borderBottomColor: '#F3F4F6',
    borderBottomWidth: 1,
    paddingHorizontal: 16,
    paddingVertical: 15,
    flexDirection: 'row',
    alignItems: 'center',
    elevation: 4, // for Android shadow
    shadowColor: '#000', // for iOS shadow
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  headerTitle: {
    marginLeft: 12,
    fontSize: 20,
    fontWeight: '600',
    color: '#111827',
  },
  content: {
    paddingTop: 16,
    paddingBottom: 16,
  },
  groupContainer: {
    marginBottom: 32,
  },
  groupTitle: {
    fontSize: 14,
    fontWeight: '500',
    color: '#6B7280',
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  groupContent: {
    backgroundColor: '#fff',
  },
  itemContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomColor: '#F3F4F6',
    borderBottomWidth: 1,
  },
  itemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconWrapper: {
    marginRight: 12,
  },
  itemLabel: {
    fontSize: 16,
    color: '#111827',
  },
  itemRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  itemValue: {
    fontSize: 14,
    color: '#6B7280',
    marginRight: 8,
  },
  toggleContainer: {
    width: 44,
    height: 24,
    backgroundColor: '#10B981',
    borderRadius: 12,
    justifyContent: 'center',
    position: 'relative',
  },
  toggleCircle: {
    width: 16,
    height: 16,
    backgroundColor: '#fff',
    borderRadius: 8,
    position: 'absolute',
    right: 4,
    top: 4,
  },
});

export default SettingsScreen;
