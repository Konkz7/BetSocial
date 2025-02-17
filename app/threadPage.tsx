import React, { useState, useCallback,useEffect } from "react";
import { View, StyleSheet,Alert, TouchableOpacity,ScrollView , ActivityIndicator} from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { errorHandler, IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { 
  Bookmark,
  BookmarkCheck, 
  ArrowLeft,
  HandCoins, 
  ShieldCheck, 
  Heart, 
  Crown, 
  Percent, 
  Users, 
  DollarSign } 
  from "lucide-react-native";
import Card from "./Components/Card"; 
import { getProfile } from "./API";




const ThreadScreen = ({navigation,route}:any) => {

  const [bets, setBets] = useState<any>([]);
  const [betStats, setBetStats] = useState<boolean[]>([]);
  const [betSaves, setBetSaves] = useState<boolean[]>([]);
  const [loading, setLoading] = useState(true); // Show loading indicator
  

  const threadObject = route.params;

  const statusStrings = [
    "Active" ,
    "Pending",
    "Accepted",
    "Rejected",
    "Cancelled",
    "Approved"
  ]

  const statusColors = [
    "lightgreen",
    "gray",
    "blue",
    "black",
    "red",
    "purple",
  ]

  const { data, isLoading, error } = useQuery({ queryKey: ["user"], queryFn: getProfile });


  const toggleStat = (index: number) => {
    setBetStats((prev) =>
      prev.map((item, i) => (i === index ? !item : item))
    );
  };

  const toggleSave = (index: number) => {
    setBetSaves((prev) =>
      prev.map((item, i) => (i === index ? !item : item))
    );
  };

  const addStates = () => {
    setBetStats((prev) => [...prev, false]); // adding `false` to the list
    setBetSaves((prev) => [...prev, false]); // adding `false` to the list
  };

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

  const getSaves = async () => {
    
    for(let i = 0; i < bets.length; i++){
      try {

          const getResponse = await axios.get(IP_STRING +  "/api/bets/saved?bid="+bets[i].bid+"&uid="+data.uid);
          
          if(getResponse.data !== ""){
            toggleSave(i);
          }
      } catch (error) {
          Alert.alert("Error:", "Unable to get saved bet.")
      }
    }
  }

  
  const setBet = async (bid:number) => {
    try {
      if(data){
        const saveResponse = await axios.post(IP_STRING + "/api/bets/set-bet?bid="+bid+"&uid="+data.uid);
      }
    } catch (error) {
        Alert.alert("Error:", "Unable to save bets.")
    }
  }

  const changeSave  = (bid:number, index: number) => {
      setBet(bid);
      toggleSave(index);
      console.log(betSaves[index]);
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
  

    // Wait for `bets` to be updated before setting `betStats` and `betSaves`
  useEffect(() => {
    if (bets.length > 0) {
      console.log("Bets updated, setting initial states...");
      setBetStats(new Array(bets.length).fill(false));
      setBetSaves(new Array(bets.length).fill(false));
      getSaves();
    }
}, [bets]);

    
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
                    <Text style = {{fontSize: 18, color: "green"}}>
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
                  <Heart size={18} color="gray" />
                  <Text style = {{fontSize: 15,marginLeft:5}}>24</Text>
                </TouchableOpacity>
            
            </View>
          </View>
                
        {loading ? (
                <ActivityIndicator size="large" color="blue" /> // Show loading spinner
              ) : (
        
            <ScrollView horizontal showsHorizontalScrollIndicator={false} key={null}/*refreshControl={() => getBets()}*/ >
            {bets.map((bet:any, index:any) => (
              <View>
              <Card key={index}>
                <TouchableOpacity style ={styles.fab} onPress={() => changeSave(bet.bid,index)}>
                      {betSaves.at(index)? (<BookmarkCheck size = {32}></BookmarkCheck>) :(<Bookmark size = {32}></Bookmark>)}
                </TouchableOpacity>
                <View style = {{borderBottomWidth: 1, borderBottomColor: "#ccc",paddingBottom: 5, flexDirection: "row" , 
                    justifyContent: "space-between" , alignItems: "center"}}>

                    

                    <Text style = {[styles.title,{fontSize: 20 , paddingVertical: 0}]}>Bet {index+1}: </Text>

                    <TouchableOpacity  onPress={() => toggleStat(index)}>
                        {betStats.at(index) ? 
                        (<View style = {{flexDirection: "row"}}>
                            <Users size={18} color={"#03FB52"}></Users>
                            <Text style = {{color: "#03FB52",fontWeight: "bold"}}> for </Text> 
                            <Text style = {{color: "black",fontWeight: "bold"}}> / </Text> 
                            <Text style = {{color: "red",fontWeight: "bold"}}> against </Text> 
                        </View>) :
                        (<View style = {{flexDirection: "row"}}>
                            <DollarSign size={18} color={"#03FB52"}></DollarSign>
                            <Text style = {{color: "#03FB52",fontWeight: "bold"}}> for </Text> 
                            <Text style = {{color: "black",fontWeight: "bold"}}> / </Text> 
                            <Text style = {{color: "red",fontWeight: "bold"}}> against </Text> 
                        </View>)}      
                    </TouchableOpacity>

                    
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
                    <View style = {{marginLeft: 30 , flexDirection: "row" , width: 208}}>
  
                      <View style = {bet.king_mode ? styles.kingIcon : styles.percentIcon}>
                        {bet.king_mode? 
                        <Crown size = {64}  style = {{backgroundColor: "pink", borderRadius: 10 , padding:20 }}></Crown> : 
                        <Percent size = {64}  style = {{backgroundColor: "dodgerblue", borderRadius: 10 , padding:20}}></Percent>
                        }
                      </View>

                      {bet.king_mode ? (null) : 
                      (<View style = {{marginTop: 20}}>
                        <Text style = {styles.maxInputBox}>Max Bet: {(bet.max_amount == 0) ? "N/A" : bet.max_amount}</Text>
                        <Text style = {styles.maxInputBox}> Min Bet: {bet.min_amount}</Text>
                      </View>)}
                      
                        
                    </View>

                    

                    <View style = {{flexDirection: "row", alignItems: "center", marginLeft:30, marginTop:5}}>
                        <Text style = {{marginRight: 20}}>Ends at:</Text>
                        <View style = {styles.dateBox}>
                            <Text style = {styles.dateBoxText}> {new Date(bet.ends_at).toLocaleString()} </Text>
                        </View>
                    </View>
                  <View>

                 </View>
                    
                  </View>

                </View>
                
              </Card>

              <View style = {[styles.statusContainer,{backgroundColor: statusColors.at(bet.status)}]}>       
                  <Text style = {styles.statusText}>{statusStrings.at(bet.status)}</Text>
              </View>
              <View style = {{marginTop:10}}>

              </View>
             </View>
            ))}
            </ScrollView>
              
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
    backgroundColor: "white",
    
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
    marginLeft: 10,
    textAlign: "center",
    
  },inputBox:{
    fontWeight: "bold",
    fontSize: 20,
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

  },percentIcon:{
    borderRadius: 10,
    backgroundColor: "white",
    alignItems: "center",
    justifyContent: "center",
},kingIcon:{
    margin:10,
    marginLeft: 50,
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
    textAlign: "center",
  },
  actionButton: {
    flexDirection: "row",
    alignItems: "center",
    marginRight: 10,
  },statusContainer: {
    borderRadius:70,
    width: 180,
    height:40,
    alignSelf:"center",
    justifyContent:"center",
    backgroundColor: "blue",
    marginTop: -25,
    
  },statusText: {
    textAlign:"center",
    color: "white",
    fontWeight: "bold",
  },fab: {
    
    position: "absolute",
    left: -5,
    top: -15,
    width: 40,
    height: 40,
    borderRadius: 30,
    alignItems: "center",
    justifyContent: "center",
    
  }

});

export default ThreadScreen;
