import React from "react";
import { View, StyleSheet } from "react-native";

const Card = ({ children, variant = "card" }) => {
  return <View style={[styles.card, styles[variant]]}>{children}</View>;
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: "white",
    padding: 20,
    borderRadius: 15,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 5,
    elevation: 5,
    margin: 10,
    minHeight: 310,
    width: 350,
    marginTop: 30,
  },
  pcard: {
    backgroundColor: "#eee",
  },
  bcard: {
    backgroundColor: "white",
    width: 345,
    borderWidth: 5,
    borderColor: "lightgreen",
  },
});

export default Card;
