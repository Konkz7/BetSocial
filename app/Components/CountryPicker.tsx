import React, { useState } from "react";
import { View, StyleSheet, Text } from "react-native";
import RNPickerSelect from "react-native-picker-select";

const countries = [
  { label: "United States", value: "US" },
  { label: "Canada", value: "CA" },
  { label: "United Kingdom", value: "GB" },
  { label: "Germany", value: "DE" },
  { label: "France", value: "FR" },
  { label: "India", value: "IN" },
  { label: "Australia", value: "AU" },
  { label: "Japan", value: "JP" },
  { label: "Brazil", value: "BR" },
  { label: "South Africa", value: "ZA" },
];

const CountryDropdown = () => {
  const [selectedCountry, setSelectedCountry] = useState(null);

  return (
    <View style={{marginTop: 10}}>
      <Text style={styles.label}>Country:</Text>
      <View style={styles.container}>
        <RNPickerSelect
          onValueChange={(value) => setSelectedCountry(value)}
          items={countries}
          placeholder={{ label: "Select a country...", value: null }}
          style={pickerSelectStyles}
        />
      </View>

    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginTop:5,
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 5,
  },
  label: {
    fontSize: 16,
  },
});

const pickerSelectStyles = {
  inputIOS: {
    fontSize: 16,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: "gray",
    borderRadius: 4,
    color: "black",
    paddingRight: 30,
  },
  inputAndroid: {
    fontSize: 16,  
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: "gray",
    borderRadius: 4,
    color: "black",
    paddingRight: 30,
  },
};

export default CountryDropdown;
