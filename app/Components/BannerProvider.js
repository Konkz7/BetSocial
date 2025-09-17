// BannerProvider.js
import React, { createContext, useState, useCallback } from 'react';
import { View } from 'react-native';
import NotificationBanner from './NotificationBanner';

export const BannerContext = createContext();

export const BannerProvider = ({ children }) => {
  const [queue, setQueue] = useState([]);
  const [current, setCurrent] = useState(null);

  const showBanner = useCallback((message) => {
    setQueue((prev) => [...prev, message]);
  }, []);

  const handleDismiss = () => {
    setCurrent(null);
    setQueue((prev) => prev.slice(1)); // remove first item
  };

  React.useEffect(() => {
    if (!current && queue.length > 0) {
      setCurrent(queue[0]);
    }
  }, [queue, current]);

  return (
    <BannerContext.Provider value={{ showBanner }}>
      <View style={{ flex: 1 }}>
        {children}
        {current && (
          <NotificationBanner message={current} onDismiss={handleDismiss} />
        )}
      </View>
    </BannerContext.Provider>
  );
};
