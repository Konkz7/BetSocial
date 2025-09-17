// NotificationBanner.js
import React, { useEffect, useRef } from 'react';
import { Animated, TouchableOpacity, Text, StyleSheet, Dimensions } from 'react-native';

const SCREEN_WIDTH = Dimensions.get('window').width;

const NotificationBanner = ({ message, onDismiss, timeout = 4000 }) => {
  const slideAnim = useRef(new Animated.Value(-100)).current; // Start above screen

  useEffect(() => {
    // Slide down
    Animated.spring(slideAnim, {
      toValue: 0,
      useNativeDriver: true,
    }).start();

    // Auto-dismiss after timeout
    const timer = setTimeout(() => {
      hideBanner();
    }, timeout);

    return () => clearTimeout(timer);
  }, []);

  const hideBanner = () => {
    Animated.timing(slideAnim, {
      toValue: -100,
      duration: 300,
      useNativeDriver: true,
    }).start(() => onDismiss?.());
  };

  return (
    <Animated.View
      style={[
        styles.banner,
        { transform: [{ translateY: slideAnim }] },
      ]}
    >
      <TouchableOpacity onPress={hideBanner} style={styles.touchArea}>
        <Text style={styles.title}>{message?.title || 'New Notification'}</Text>
        <Text style={styles.body}>{message?.body || ''}</Text>
      </TouchableOpacity>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  banner: {
    position: 'absolute',
    top: 0,
    width: SCREEN_WIDTH - 20,
    marginHorizontal: 10,
    backgroundColor: '#4aaa0aff',
    borderRadius: 12,
    padding: 12,
    shadowColor: '#000',
    shadowOpacity: 0.3,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
    elevation: 5,
    zIndex: 9999,
  },
  touchArea: { flex: 1 },
  title: { color: 'white', fontWeight: 'bold', fontSize: 16 },
  body: { color: 'white', marginTop: 4 },
});

export default NotificationBanner;
