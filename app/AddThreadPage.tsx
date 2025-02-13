import React, { useState, useCallback,useEffect } from "react";
import { View, StyleSheet,Alert, TouchableOpacity,ScrollView } from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { errorHandler, IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { SquarePlus, ArrowLeft,HandCoins, ShieldCheck, CircleX } from "lucide-react-native";
import Card from "./Components/Card"; 
import ToggleSwitch from "./Components/ToggleSwitch"; 
import DatePickerButton from "./Components/DatePicker"; 




const AddThreadScreen = ({navigation}:any) => {
  const [threadText,setThreadText] = useState("");
  const [category, setCategory] = useState("");
  const [username, setUsername] = useState("Loading...");
  const [bets, setBets]= useState <any>([]);
  const maxCharacters = 280;
  const maxBetCharacters = 50;
  const maxBets = 4;
  const addBetOptions = () => {
    if(bets.length >= maxBets){
      return false;
    }
    return true;
  }
  const [canAddBet, setCanAddBet] = useState(addBetOptions());


  const categories = [
    "All",
    "Sports",
    "Politics",
    "Entertainment",
    "Tech",
    "Gaming",
  ];

  const thread = {
    "title": threadText,
    "description": "",
    "category": category,
  }

  const getUsername = async() =>{
    try {
        const profile = await axios.get(IP_STRING + "/req/profile");
        setUsername(profile.data.user_name);
    } catch (error) {
      errorHandler(error);
    }
  }

  const post = async() =>{
    try {
       const threadResponse = await axios.post(IP_STRING + "/api/threads/make", thread); 
       Alert.alert("Well done!", "Thread successfully made!");
       navigation.navigate("Home");
    } catch (error) {
      Alert.alert("Error:", "Make sure all thread fields are filled out correctly.");
    }
  }

  const deleteTempBetAt = (index: number) => {
    setBets(bets.filter((_:any, i:number) => i !== index)); // Removes the item at the given index
  };

  const addTempBet = () =>{
    console.log(addBetOptions());
    setBets((prevBets:any) => [...prevBets, { 
      verified: false,
      kingMode: false, 
      description: "", 
      profitMode: false , 
      maxBet: 0 , 
      ends_at: 0}]);
  }

  const handleBetProfitChange = ( index:number) => {
    const updatedBets = [...bets];
    updatedBets[index].profitMode = !updatedBets[index].profitMode;
    setBets(updatedBets); // Assuming you have setBets as a state updater
  };

  const handleBetKingChange = ( index:number) => {
    const updatedBets = [...bets];
    updatedBets[index].kingMode = !updatedBets[index].kingMode;
    setBets(updatedBets); // Assuming you have setBets as a state updater
  };

  const handleBetVerifiedChange = ( index:number) => {
    const updatedBets = [...bets];
    updatedBets[index].verified = !updatedBets[index].verified;
    setBets(updatedBets); // Assuming you have setBets as a state updater
  };

  const handleBetTextChange = (text:String, index:number) => {
    const updatedBets = [...bets];
    updatedBets[index].description = text;
    setBets(updatedBets); // Assuming you have setBets as a state updater
  };

  const handleBetMaxChange = ( text:number ,index:number) => {
    const updatedBets = [...bets];
    updatedBets[index].maxBet = text;
    setBets(updatedBets); // Assuming you have setBets as a state updater
  };

  const handleBetEndChange = ( text:number ,index:number) => {
    const updatedBets = [...bets];
    updatedBets[index].maxBet = text;
    setBets(updatedBets); // Assuming you have setBets as a state updater
  };

  useEffect(() => {
    setCanAddBet(addBetOptions());
  }, [bets]);

  useFocusEffect(
      useCallback(() => {
        console.log("Screen is focused! Perform refresh or action here.");  
        getUsername();
        return () => {
          console.log("Screen is unfocused! Cleanup if needed.");
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

          <Button style = {styles.postbutton} onPress={post}>
            <Text style = {{color: "white"}}>Post</Text>
          </Button>
      </View >
      <View style = {styles.body}>
        <Text style = {styles.title}> Create Thread: </Text>

        <View style = {{justifyContent: "center", alignItems: "center"}}>
          <View style = { styles.threadShadow }>
            <View style = {styles.postHeader}>
              <View style={styles.avatar} />
                <View>
                  <Text style={styles.userName}>{username}</Text>
                </View>
            </View>
            <View style = {styles.inputContainer}>
              <TextInput style = {styles.inputBox}
                value = {threadText}
                onChangeText={setThreadText}
                multiline
                maxLength={maxCharacters}
                placeholder="What are we writing about?" 
              />  
            </View>
            <View style = {styles.categoryContainer}>
              <TextInput style = {styles.inputBox}
                value = {category}
                onChangeText={setCategory}
                multiline
                maxLength={maxCharacters}
                placeholder= "Category:" 
              />  
            </View>
            <View style = {styles.scrollContainer}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryScroll}>
                {categories.map((category) => (
                  <TouchableOpacity
                    key={category}
                    onPress={() => setCategory(category)}
                    style={[
                      styles.categoryButton,
                    ]}
                  >
                    <Text style={[styles.categoryText,]}>
                      {category}
                    </Text>
                  </TouchableOpacity>
                ))}
            </ScrollView>
           </View>
          </View>


          <View>
            {bets.map((bet:any, index:any) => (
              <Card key={index}>
                <Text style = {[styles.title,{fontSize: 20 , paddingVertical: 0}]}>Bet {index+1}: </Text>
                <View style={styles.betContainer}>
                  <TouchableOpacity style = {styles.fab} onPress={() => deleteTempBetAt(index)}>
                      <CircleX size={24}></CircleX>
                  </TouchableOpacity>
                  <TextInput
                    style={styles.inputBox}
                    value={bet.description}
                    onChangeText={(text) => handleBetTextChange(text, index)}
                    multiline
                    maxLength={maxBetCharacters}
                    placeholder="What's the bet today?"
                  />
                </View>

                <View style = {{flexDirection: "row"}}>
                  <View>
                    <ToggleSwitch onClick={() => handleBetKingChange(index)}>

                    </ToggleSwitch>
                    
                    <TouchableOpacity style = {[styles.betButtonContainer, 
                      bet.profitMode ? {backgroundColor: "#4CAF50" } : {backgroundColor: "#ccc"}]}
                      onPress={() => handleBetProfitChange(index)}>
                      <HandCoins  size={36} color={bet.profitMode ? "white" : "black"}></HandCoins>
                    </TouchableOpacity>
                  </View>

                  <View style = {{}}>
                    <View style = {{marginHorizontal: 20, marginVertical:5 , flexDirection: "row" , justifyContent:"space-between", width: 220}}>
                      <TextInput style = {[styles.maxInputBox, bet.kingMode ? {backgroundColor: "gray", opacity: 0.2} : 
                      {backgroundColor: "white", opacity: 1}]}
                      disabled = {bet.kingMode}
                      value={bet.maxBet}
                      onChangeText={(text) => handleBetMaxChange(Number.parseFloat(text), index)}
                      inputMode="numeric"
                      maxLength={6}
                      placeholder="Max Bet:">

                      </TextInput>
                      
                      <TouchableOpacity style = {[styles.betButtonContainer, {marginTop: 10,padding:7},
                        bet.verified ? {backgroundColor: "#4CAF50" } : {backgroundColor: "#ccc"}]}
                        onPress={() => handleBetVerifiedChange(index)}>
                        <ShieldCheck  size={36} color={bet.verified ? "white" : "black"}></ShieldCheck>
                      </TouchableOpacity>
                      
                    </View>

                    

                    <View style = {{flexDirection: "row", alignItems: "center", marginLeft:20}}>
                        <Text style = {{marginRight: 20}}>Ends at:</Text>
                        <DatePickerButton onDateSelect={(number) => handleBetEndChange(number,index)}/>
                      </View>
                    <View>

                 </View>
                    
                  </View>

                </View>
                
              </Card>
            ))}
          </View>

          
          <View style = {{marginTop: 40}}>
            <TouchableOpacity style = {{alignItems: "center", opacity: canAddBet ? 1 : 0.5 }} 
            onPress= {canAddBet ? addTempBet : undefined} 
            disabled={!canAddBet}>
              <SquarePlus size={50} color={"green"}></SquarePlus>
              <Text style = {{fontSize: 28, fontWeight: "bold", color: "green"}}>
              Add Bet
              </Text>
            </TouchableOpacity>
            
          </View>


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
    height:220,
    width:380,
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
  },categoryContainer:{
    height:60,
    width:380,
    backgroundColor: "white",
  },betButtonContainer:{
    height:50,
    width:50,
    borderRadius: 15,
    padding:5,
    margin:10,
    marginTop: 20,
  },maxInputBox:{
    marginVertical: 10,
    height:40,
    width: 130,
  },inputBox:{
    flex:1,
    backgroundColor: "white",

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
    justifyContent:"flex-start",
    alignSelf:"flex-start",
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: 2,
    borderBottomColor: "#ddd",
    padding: 10,
    borderTopRightRadius: 10,
    borderTopLeftRadius: 10,
  },categoryScroll: {
    paddingHorizontal: 16,
  },scrollContainer:{
    height: 60, 
    width: 380,
    backgroundColor: "white",
    borderBottomRightRadius: 10,
    borderBottomLeftRadius: 10,
  },
  categoryButton: {
    paddingVertical: 9,
    paddingHorizontal: 12,
    borderRadius: 20,
    backgroundColor: "#e6f4ea",
    marginRight: 8,
    marginVertical:10,
  },categoryText: {
    fontSize: 14,
    color: "green",
  },betButtons:{
    color: "green",
    marginTop: 20,
  },threadShadow:{
    shadowColor: "#000", // Shadow color
    shadowOffset: { width: 0, height: 4 }, // Shadow position
    shadowOpacity: 0.2, // Shadow transparency
    shadowRadius: 5, // Shadow blur
    elevation: 7, 
    
  },fab: {
    position: "absolute", // Ensures it floats
    bottom: 120, // Distance from bottom
    right: -35, // Distance from right
    backgroundColor: "red",
    width: 40,
    height: 40,
    borderRadius: 30,
    alignItems: "center",
    justifyContent: "center",
    elevation: 5, // Shadow for Android
    shadowColor: "#000", // Shadow for iOS
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },

});

export default AddThreadScreen;
