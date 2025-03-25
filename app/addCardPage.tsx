import React, { useState } from 'react';
import 'text-encoding-polyfill';
import { Buffer } from 'buffer'; // Import Buffer polyfill
import { View, Text, TextInput, Button, StyleSheet, ScrollView,TouchableOpacity,Alert } from 'react-native';
import { ArrowLeft } from 'lucide-react-native';
import OpenPGP from "react-native-fast-openpgp";
import CountryDropdown from './Components/CountryPicker';
import { IP_STRING, pgpPublicKey } from './Constants';
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import axios from 'axios';



const AddCardScreen = ({navigation}:any) => {
  // Billing Details
  const [fullName, setFullName] = useState('');
  const [city, setCity] = useState('');
  const [country, setCountry] = useState('');
  const [line1, setLine1] = useState('');
  const [line2, setLine2] = useState('');
  const [postalCode, setPostalCode] = useState('');

  // Card Details
  const [cardNumber, setCardNumber] = useState('');
  const [cvv, setCvv] = useState('');
  const [expMonth, setExpMonth] = useState('');
  const [expYear, setExpYear] = useState('');

  const queryClient = useQueryClient();
  queryClient.invalidateQueries({queryKey: ["ipAddress"]});
  const ipAddress:any = queryClient.getQueryData(["ipAddress"]);


  const encryptCardData = async ({ cardNumber, cvv }: any) => {
    try {
      const cardData = `${cardNumber}|${cvv}`;
  
      // Ensure you have a valid PGP public key
      if (!pgpPublicKey) {
        throw new Error("PGP public key is missing.");
      }
  
      // Encrypt using react-native-fast-openpgp
      const encrypted = await OpenPGP.encrypt(cardData,pgpPublicKey);
  
      // Convert encrypted PGP message to Base64
      const base64Encrypted = Buffer.from(encrypted, 'utf-8').toString('base64');
  
      return base64Encrypted;
    } catch (error) {
      console.error("Encryption failed:", error);
      throw error;
    }
  };

  const handleSubmit = async () => {
    // Validate inputs as needed
    const billingDetails = {
      fullName,
      city,
      country,
      line1,
      line2,
      postalCode,
      expMonth,
      expYear,
    };
    const cardDetails = {
      cardNumber,
      cvv,
    };

    console.log('Billing Details:', billingDetails);
    console.log('Card Details:', cardDetails);
    console.log("Ipaddress: ", ipAddress);

   

    // Here you would trigger any encryption/tokenization before sending to your backend
    const secret =await encryptCardData({cardNumber, cvv});
    console.log("secret: ", secret);
    
    // and then call your API endpoint that creates the card.
    try {
      const response = await axios.post(
        IP_STRING + "/circle/create-card", 
        billingDetails, // Send billing details in the request body
        {
          params: { 
            encryptedData: secret, 
            ipAddress: ipAddress.ip 
          }
        }
      );
      console.log(response.data);
      navigation.pop(null);
    } catch (error) {
      Alert.alert("Error :", "Failed to add card. Please try again later.");
      console.log("Error :" + (error as Error).message );
    }

  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style = {styles.head}>
        <View>
          <TouchableOpacity style={{ flexDirection: "row", alignItems: "center" }} onPress={() => navigation.goBack()}>
            <ArrowLeft size={36} color={"green"} />
            <Text style = {styles.headText}>Back</Text>
          </TouchableOpacity>
        </View>
      </View>
      <Text style={styles.header}>Billing Details</Text>
      
      <Text style={styles.label}>Full Name</Text>
      <TextInput
        style={styles.input}
        placeholder="First / Last name"
        placeholderTextColor={"#ccc"}
        value={fullName}
        onChangeText={setFullName}
      />

      <Text style={styles.label}>City</Text>
      <TextInput
        style={styles.input}
        placeholder="City"
        placeholderTextColor={"#ccc"}
        value={city}
        onChangeText={setCity}
      />

      <CountryDropdown
        selectedCountry = {country}
        setSelectedCountry = {setCountry}
      />

      <Text style={styles.label}>Address Line 1</Text>
      <TextInput
        style={styles.input}
        placeholder="Address Line 1"
        placeholderTextColor={"#ccc"}
        value={line1}
        onChangeText={setLine1}
      />

      <Text style={styles.label}>Address Line 2</Text>
      <TextInput
        style={styles.input}
        placeholder="Address Line 2"
        placeholderTextColor={"#ccc"}
        value={line2}
        onChangeText={setLine2}
      />

      <Text style={styles.label}>Postal Code</Text>
      <TextInput
        style={styles.input}
        placeholder="Postal Code"
        placeholderTextColor={"#ccc"}
        value={postalCode}
        onChangeText={setPostalCode}
      />

      <Text style={styles.header}>Card Details</Text>

      <Text style={styles.label}>Card Number</Text>
      <TextInput
        style={styles.input}
        placeholder="Card Number"
        placeholderTextColor={"#ccc"}
        keyboardType="numeric"
        value={cardNumber}
        onChangeText={setCardNumber}
      />

      <Text style={styles.label}>CVV</Text>
      <TextInput
        style={[styles.input]}
        placeholder="CVV"
        placeholderTextColor={"#ccc"}
        keyboardType="numeric"
        secureTextEntry
        value={cvv}
        onChangeText={setCvv}
      />

      <Text style={styles.label}>Expiration Month</Text>
      <TextInput
        style={styles.input}
        placeholder="MM"
        placeholderTextColor={"#ccc"}
        keyboardType="numeric"
        value={expMonth}
        maxLength={2}
        onChangeText={setExpMonth}
      />

      <Text style={styles.label}>Expiration Year</Text>
      <TextInput
        style={styles.input}
        placeholder="YYYY"
        placeholderTextColor={"#ccc"}
        keyboardType="numeric"
        value={expYear}
        maxLength={4}
        onChangeText={setExpYear}
      />
    
      <View style = {{marginTop: 15}}>
        <Button title="Submit" onPress={handleSubmit} color={"green"}  />
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  head: {
    flexDirection:"row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
    padding: 10,
  },headText:{
    paddingLeft:10,
    fontSize: 17,
    color: "green",
  },
  container: {
    padding: 20,
    backgroundColor: '#fff',
  },
  header: {
    fontSize: 20,
    fontWeight: 'bold',
    marginVertical: 15,
    color: "green",
  },
  label: {
    marginTop: 10,
  },
  input: {
    height: 40,
    borderColor: '#ccc',
    borderWidth: 1,
    marginVertical: 5,
    paddingHorizontal: 10,
    borderRadius: 5,
    color: "black",
  },
});

export default AddCardScreen;
