import React, { useState, useCallback } from "react";
import { View, StyleSheet,Alert } from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";



const AddThreadScreen = ({navigation}:any) => {

  return (
   <SafeAreaView style = {styles.container}>
      <View style = {styles.header}>

      </View>
   </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container:{
    flex: 1,
    backgroundColor: "white",
  },header: {
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
    paddingTop: 40,
    paddingBottom: 10,
  }
});

export default AddThreadScreen;
