import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
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
  HelpCircle,
  LogOut,
  Languages,
  Moon,
  Shield,
  Bookmark,
} from 'lucide-react-native';



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
          <SettingItem
            icon={<Wallet size={24} color="#6B7280" />}
            label="Payment Methods"
            value="Visa ****4242"
          />
          <SettingItem icon={<Bookmark size={24} color="#6B7280" />} label="Saved Bets" />

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
    </View>
  );
}

const styles = StyleSheet.create({
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
