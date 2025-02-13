import React, { useState } from "react";
import { View, TouchableOpacity, Animated } from "react-native";
import { Crown,Percent } from "lucide-react-native"; // Importing Lucide icons


interface ToggleSwitchProps {
  onClick?: () => void; // Accepts an optional onClick function
}

const ToggleSwitch: React.FC<ToggleSwitchProps> = ({ onClick }) => {
  const [isOn, setIsOn] = useState(false);
  const translateX = new Animated.Value(isOn ? 20 : -20); // Animation for moving the circle

  

  const toggleSwitch = () => {
    setIsOn(!isOn);
    Animated.timing(translateX, {
      toValue: isOn ? -20 : 20, // Moves the toggle circle
      duration: 200,
      useNativeDriver: true,
    }).start();

    if (onClick) {
      onClick();
    }
   
  };

  return (
    <TouchableOpacity
      onPress={toggleSwitch}
      style={{
        width: 75,
        height: 35,
        borderRadius: 15,
        backgroundColor: isOn ? "#4CAF50" : "#ccc",
        justifyContent: "center",
        paddingHorizontal: 3,
        flexDirection: "row",
        alignItems: "center",
        marginTop: 20,
      }}
    >
      <Animated.View
        style={{
          width: 30,
          height: 30,
          borderRadius: 10,
          backgroundColor: "white",
          transform: [{ translateX }],
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        {isOn ? (
          <Crown size={14} color="gold" />
        ) : (
          <Percent size={14} color="black" />
        )}
      </Animated.View>
    </TouchableOpacity>
  );
};



export default ToggleSwitch;
