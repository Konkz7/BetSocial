import React, { useState ,} from "react";
import { View, Text, Button, TextInput,Alert,Linking, StyleSheet , TouchableOpacity } from "react-native";
import axios from "axios";
import { IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { PiggyBank,ArrowLeft, PlusCircle } from "lucide-react-native";
import { getCircleSecret } from "./API";
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Card } from "react-native-paper";
import { Picker } from '@react-native-picker/picker';


const WalletScreen = ({navigation}:any) => {
  const [amount, setAmount] = useState("");
  

  const queryClient = useQueryClient();
  const walletData:any = queryClient.getQueryData(["wallet"]);
  const [selectedValue, setSelectedValue] = useState('option1');

  // Options list with labels and values
  const options = [
    { label: 'Option 1', value: 'option1' },
    { label: 'Option 2', value: 'option2' },
    { label: 'Option 3', value: 'option3' },
  ];


  const addWallet = async () => {
    try {
      const response = await axios.post(IP_STRING+"/circle/create-user-wallet");
      Alert.alert("Message:", "Wallet created successfully! Your wallet id: " + response.data.data.wallet.id);
      queryClient.invalidateQueries( {queryKey: ["wallet"]});
    } catch (error) {
      Alert.alert("Error", "Failed to create wallet. Please try again later.");
    }
    
  };

  
  return (
    <SafeAreaView style = {styles.container}>
      <View style = {styles.header}>
        <View>
          <TouchableOpacity style={{ flexDirection: "row", alignItems: "center" }} onPress={() => navigation.goBack()}>
            <ArrowLeft size={36} color={"green"} />
            <Text style = {styles.headerText}>Back</Text>
          </TouchableOpacity>
        </View>
      </View>
      <View>
        {walletData == null ? (
          <View style = {[styles.body,{marginTop: 200}]}>
            <PiggyBank size={150}  ></PiggyBank>
            <View style = { styles.addWallet}>
            <TouchableOpacity onPress={addWallet}>
              <Text style = {styles.addWalletText}> Add Wallet </Text>
            </TouchableOpacity>
          </View>
          <Text style = {[styles.addWalletInfo, {maxWidth: 280}]}> It seems you havent made a wallet with us. Start now! (Please know that all funds 
              are stored in USDC in order to prioritise the user experience while ensuring security.)
          </Text>
        </View>
        ) : 
        (<View style = {styles.body}>
          
          <Card style = {styles.balanceContainer}>  
            <Text style = {styles.balanceTitle}>Balance :</Text>
            <Text style = {styles.addWalletInfo} >Wallet ID : {walletData.data.wallet.id}</Text>
            <Text style ={styles.balance}>30.00</Text>
            <Text style ={[styles.currency]}>USDC</Text>
          </Card>

          <View style = {{marginTop: 20}}>
            <Text style = {styles.pmTitle}>Pick saved card: </Text>
            <View style = {styles.pickerShadow}>
              <Picker
                selectedValue={selectedValue}
                style={styles.picker}
                onValueChange={(itemValue) => setSelectedValue(itemValue)}
                placeholder="Select card..."
              >
                {options.map((option, index) => (
                  <Picker.Item key={index} label={option.label} value={option.value} />
                ))}
              </Picker>
            </View>
            <TouchableOpacity style = {styles.addCardButton} onPress={() => navigation.navigate("Card_S")}>
              <PlusCircle size = {24} ></PlusCircle>
              <Text style = {[styles.addWalletInfo, {marginTop: 0 , marginLeft: 3}]}>Add a card</Text>
            </TouchableOpacity>
          </View>
          
         
          


         </View>
        )}
        
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container:{
    flex:1,
    backgroundColor: "#fcfcf7",
  },header: {
    flexDirection:"row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
    padding: 10,
  },headerText:{
    paddingLeft:10,
    fontSize: 17,
    color: "green",
  },body:{
    
    justifyContent: "center",
    alignItems: "center",
  }
  ,addWallet:{
    height:70,
    width: 170,
    backgroundColor: "green",
    borderRadius: 10,
  },addWalletText:{
    fontSize: 20,
    color: "white",
    fontWeight: "bold",
    textAlign: "center",
    padding : 20,
  },addWalletInfo:{
    fontSize: 15,
    color: "#bbb",
    textAlign: "center",
    fontStyle: "italic",
    marginTop: 10,
  },balanceTitle:{
    fontSize: 38,
    fontWeight: "bold",
    textAlign: "center",
  },balance:{
    fontSize: 60,
    fontWeight: "bold",
    textAlign: "center",
    color: "green"
  },currency:{
    fontSize: 30,
    fontWeight: "bold",
    textAlign: "center",
  },pmTitle:{
    fontSize: 18,
    fontWeight: "bold",
    marginBottom: 5,
  },balanceContainer:{
    marginTop:30,
    padding: 20, 
    borderRadius: 10, 
    width: 380,
    backgroundColor: "white",
  },pickerShadow: {
    backgroundColor: "white", // added background color
    borderRadius: 20,         // ensure corners are rounded
    elevation: 5,             // Android shadow
    shadowColor: "#000",      // iOS shadow properties
    shadowOffset: { width: 1, height: 2 },
    shadowOpacity: 0.5,
    shadowRadius: 4,

  },picker: {
    height: 60,
    width: 380,
    // Remove backgroundColor from here if it's redundant
    // backgroundColor: "white",
    borderRadius: 20,
    color: "black",
    paddingVertical: 10,
  },addCardButton:{
    marginTop: 10 , 
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
  }
});

export default WalletScreen;
