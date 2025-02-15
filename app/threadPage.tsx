import React, { useState, useCallback,useEffect } from "react";
import { View, StyleSheet,Alert, TouchableOpacity,ScrollView , ActivityIndicator} from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { errorHandler, IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { SquarePlus, ArrowLeft,HandCoins, ShieldCheck, Heart, Crown, Percent } from "lucide-react-native";
import Card from "./Components/Card"; 
import ToggleSwitch from "./Components/ToggleSwitch"; 
import DatePickerButton from "./Components/DatePicker"; 




const ThreadScreen = ({navigation,route}:any) => {

  const [bets, setBets] = useState<any>([]);
  const [loading, setLoading] = useState(true); // Show loading indicator

  const threadObject = route.params;


  const getBets = async () => {
    try {
        const betResponse = await axios.get(IP_STRING + "/api/bets/find-by-thread/"+threadObject.tid);
        setBets(betResponse.data);
    } catch (error) {
        Alert.alert("Error:", "Unable to generate bets.")
    }finally {
        setLoading(false); // Hide loading indicator
    }
  }
    

  useFocusEffect(
      useCallback(() => {
        console.log("ThreadScreen is focused! Perform refresh or action here.");  
        getBets();
        return () => {
          console.log("ThreadScreen is unfocused! Cleanup if needed.");
        };
      }, [])
    );
  

    
  return (
   <SafeAreaView style = {styles.container}>
    <ScrollView>
      <View style = {styles.header}>
        <View >
          <TouchableOpacity style={{ flexDirection: "row", alignItems: "center" }} onPress={() => navigation.goBack()}>
            <ArrowLeft size={36} color={"green"} />
            <Text style = {styles.headerText}>Back</Text>
          </TouchableOpacity>
        </View>

    
      </View >
      <View style = {styles.body}>

        <View style = {{justifyContent: "center", alignItems: "center"}}>
          <View style = { styles.threadShadow }>
            <View style = {styles.postHeader}>

                <View style = {{flexDirection: "row", alignItems: "center"}}>
                  <View style={styles.avatar} />
                  <Text style={styles.userName}>{threadObject.user.user_name}</Text>
                </View>

                <View style = {{ marginRight:10}}>
                  <View style = {{}}>
                    <Text style = {{fontWeight: "bold",fontStyle: "italic",fontSize: 18, color: "green"}}>
                        {threadObject.is_private ? "Private" : "Public"}
                    </Text> 
                  </View>
                </View>
    
            </View>
            <View style = {styles.inputContainer}>
              <Text style = {styles.inputBox}>
                {threadObject.title}
              </Text>  
            </View>
            <View style = {styles.categoryContainer}>
              
                <Text style = {styles.categoryText}>
                    {threadObject.category}
                </Text> 

                <TouchableOpacity style={styles.actionButton}>
                  <Heart size={24} color="gray" />
                  <Text style = {{fontSize: 18,marginLeft:5}}>24</Text>
                </TouchableOpacity>
            
            </View>
          </View>
                
        {loading ? (
                <ActivityIndicator size="large" color="blue" /> // Show loading spinner
              ) : (
          <View>
            {bets.map((bet:any, index:any) => (
              <Card key={index}>
                <View style = {{borderBottomWidth: 1, borderBottomColor: "#ccc",paddingBottom: 5,}}>
                    <Text style = {[styles.title,{fontSize: 20 , paddingVertical: 0}]}>Bet {index+1}: </Text>
                </View>
                <View style = {styles.betTextContainer}>
                    <Text style = {styles.betText}>
                        {bet.description}
                    </Text>
                </View>
                <View style = {{flexDirection: "row"}}>
                  <View style = {{borderRightWidth: 1, borderRightColor: "#ddd"}}>
                    <TouchableOpacity style = {[styles.betButtonContainer, {marginTop: 10},
                        bet.is_verified ? {backgroundColor: "#4CAF50" } : {backgroundColor: "#ccc"}]}>
                        <ShieldCheck  size={36} color={bet.is_verified ? "white" : "black"}></ShieldCheck>
                      </TouchableOpacity>
                    
                    <TouchableOpacity style = {[styles.betButtonContainer, 
                      bet.profit_mode ? {backgroundColor: "#4CAF50" } : {backgroundColor: "#ccc"}]}>
                      <HandCoins  size={36} color={bet.profit_mode ? "white" : "black"}></HandCoins>
                    </TouchableOpacity>
                  </View>

                  <View style = {{}}>
                    <View style = {{marginHorizontal: 20, marginTop:35 , flexDirection: "row" , 
                       width: 208}}>
  
                      <View style = {styles.kingIcon}>
                        {bet.king_mode? 
                        <Crown size = {36} style = {{backgroundColor: "yellow", borderRadius: 30 , padding:20}}></Crown> : 
                        <Percent size = {36} style = {{backgroundColor: "dodgerblue", borderRadius: 30 , padding:20}}></Percent>
                        }
                      </View>

                      <Text style = {[styles.maxInputBox, bet.king_mode ? {backgroundColor: "gray", opacity: 0.2} : 
                        {backgroundColor: "white", opacity: 1}]}>
                            Max Bet: {bet.max_amount}
                      </Text>
                        
               
                    </View>

                    

                    <View style = {{flexDirection: "row", alignItems: "center", marginLeft:20, marginTop:5}}>
                        <Text style = {{marginRight: 20}}>Ends at:</Text>
                        <View style = {styles.dateBox}>
                            <Text style = {styles.dateBoxText}> {bet.ends_at} </Text>
                        </View>
                    </View>
                  <View>

                 </View>
                    
                  </View>

                </View>
                
              </Card>
            ))}
          </View>
              
           )}



         <View>

      </View>
     </View>

        
       
    </View>
    </ScrollView>
   </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container:{
    flex: 1,
    backgroundColor: "#f6f2e6",
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
  },postbutton:{
    backgroundColor: "green",
  }
  ,body:{
    justifyContent: "center",
    alignItems: "center",
  },inputContainer:{
    maxWidth: 380,
    padding: 20,
    paddingVertical:40,
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
  },categoryContainer:{
    padding:20,
    maxWidth:380,
    backgroundColor: "white",
    justifyContent:"space-between",
    alignItems: "center",
    flexDirection: "row",
    borderBottomLeftRadius: 10,
    borderBottomRightRadius: 10,
  },betTextContainer:{
    padding:20,
    minHeight: 100,
    width: 310,
    borderBottomWidth: 1,
    borderBottomColor: "#ccc",
    alignItems:"center",
    justifyContent:"center",
  },betText:{
    textAlign:"center",
    fontSize: 24,
  },betButtonContainer:{
    height:50,
    width:50,
    borderRadius: 15,
    padding:7,
    margin:10,
  },maxInputBox:{
    height:40,
    width: 120,
    fontWeight: "bold",
    marginLeft: 43,
    textAlign: "center",
    
  },inputBox:{
    fontWeight: "bold",
    fontSize: 18,
    backgroundColor:"#eee",
    borderRadius: 20,
    padding:20,
    textAlign: "center",
  },betContainer:{
    width:300,
    height:105,
    backgroundColor: "white",

  },title:{
    fontSize: 35,
    fontWeight: "bold",
    textDecorationStyle:"double",
    paddingVertical: 20,
    paddingLeft: "2%",
    alignSelf:"flex-start",
    textAlign:"left",
  },avatar: {
    width: 40,
    height: 40,
    backgroundColor: "lightgreen",
    borderRadius: 20,
  },
  userName: {
    fontWeight: "bold",
    marginLeft: 10,
  },
  postHeader: {
    width:380,
    backgroundColor: "white",
    justifyContent:"space-between",
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: 2,
    borderBottomColor: "#ddd",
    padding: 10,
    borderTopRightRadius: 10,
    borderTopLeftRadius: 10,
  },categoryText: {
    fontSize: 18,
    fontWeight: "bold",
    fontStyle:"italic",
    color: "green"
  },betButtons:{
    color: "green",
    marginTop: 20,
  },threadShadow:{
    shadowColor: "#000", // Shadow color
    shadowOffset: { width: 0, height: 4 }, // Shadow position
    shadowOpacity: 0.2, // Shadow transparency
    shadowRadius: 5, // Shadow blur
    elevation: 7,
    marginTop:40,

  },kingIcon:{
    width: 30,
    height: 30,
    borderRadius: 10,
    backgroundColor: "white",
    marginLeft: 7,
    alignItems: "center",
    justifyContent: "center",
},dateBox: {
    backgroundColor: "green",
    padding: 10,
    borderRadius: 5,
    alignItems: "center",
    maxWidth: 160,
  },
  dateBoxText: {
    color: "white",
    fontSize: 16,
  },
  actionButton: {
    flexDirection: "row",
    alignItems: "center",
    marginRight: 10,
  }

});

export default ThreadScreen;
