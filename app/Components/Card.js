import React from "react";
import { View, Text, StyleSheet } from "react-native";

const Card = ({ children }) => {
  return <View style={styles.card}>{children}</View>;
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: "white", // Card background color
    padding: 20, // Inner spacing
    borderRadius: 15, // Rounded edges
    shadowColor: "#000", // Shadow color
    shadowOffset: { width: 0, height: 4 }, // Shadow position
    shadowOpacity: 0.2, // Shadow transparency
    shadowRadius: 5, // Shadow blur
    elevation: 5, // Shadow for Android
    margin: 10, // Space around the card
    height:310,
    width: 350,
    marginTop:30,
  },
});

export default Card;
