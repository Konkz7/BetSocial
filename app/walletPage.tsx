import React, { useState ,} from "react";
import { View, Text, Button, TextInput,Alert,Linking, StyleSheet , TouchableOpacity } from "react-native";
import axios from "axios";
import { IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { PiggyBank,ArrowLeft } from "lucide-react-native";
import { getCircleSecret } from "./API";
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";


const WalletScreen = ({navigation}:any) => {
  const [amount, setAmount] = useState("");
  

  const queryClient = useQueryClient();
  const walletData:any = queryClient.getQueryData(["wallet"]);

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
        (<View>
          <Text>{walletData.data.wallet.id}</Text>
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
  
  }
});

export default WalletScreen;
