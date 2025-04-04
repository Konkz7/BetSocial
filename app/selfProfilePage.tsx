import React, { useState, useCallback,useEffect } from "react";
import { View, StyleSheet,Alert, TouchableOpacity,ScrollView , ActivityIndicator, FlatList} from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { ArrowLeft, Settings2, DollarSign, Frown, Heart, MessageCircle, Users, Trash2, Pencil } from "lucide-react-native";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { changeBio, getFriendship, getUserThreads, removeThread, sendFriendRequest, Unfriend } from "./API";
import { get } from "react-native/Libraries/TurboModule/TurboModuleRegistry";




const SelfProfileScreen = ({navigation , route}: any) => { 

    const [currentTab,setCurrentTab] = useState('Threads');
    const [threads, setThreads] = useState<any[]>([]);
    const [bioMode,setBioMode] = useState(false);
    const [bioText, setBioText] = useState("");
   
    const queryClient = useQueryClient();
    const user = queryClient.getQueryData(["user"]) as any;

   
    const { data: threadData, refetch: refetchThreads, isLoading: threadsLoading } = useQuery({
        queryKey: ["userThreads"],
        queryFn: () => getUserThreads(user.uid),
    });  
     
    useFocusEffect(
        useCallback(() => {
            console.log("Screen is focused! Refetching threads and friendship...");
            
            console.log(user);

            user.bio !== "undefined" ? setBioText(user.bio) : setBioText(""); 
            
            refetchThreads();
           
            return async () => {
                queryClient.invalidateQueries({queryKey: ["user"]});
                console.log("Screen is unfocused! Cleanup if needed.");       
            };
        }, []) // Depend on profile ID to refetch when user changes
    );
    
    const test = (text : string) => {
        setBioText(text);
        console.log(text);
        console.log(bioText);
    }

    const handleEditMode = async () => {
        await changeBio(bioText);
        setBioMode(!bioMode);
    }
    
    useEffect(() => {
        if (threadData) {

          setThreads(addUserInfo(threadData));
        }
    }, [threadData]);

  
    function addUserInfo(threads: any) {
        return threads.map((thread: any) => ({
            ...thread,
            user: user, // Attach user data to each thread
        }));
    } 
    
    function deleteThread(tid: number) {
        Alert.alert("Delete Thread", "Are you sure you want to delete this thread?", [
            {
                text: "Cancel",
                style: "cancel",
            },
            {
                text: "OK",
                onPress: () => {
                    // Call the API to delete the thread here
                    removeThread(tid);
                    setThreads((prevData) => prevData.filter(item => item.tid !== tid));   
                },
            },
        ]);
    }

    return (

        <SafeAreaView style = {styles.container}>
          <ScrollView>
            <View style = {styles.header} >
                <View style = {{ flexDirection: "row"}}>

                    <TouchableOpacity  onPress={() => navigation.goBack()}>
                        <ArrowLeft size={36} color={"#10B981"} />   
                    </TouchableOpacity>

                    <View style = {styles.nameContainer}>
                        <Text style = {styles.name}> {user.user_name} </Text>
                    </View>
                    <TouchableOpacity style = {styles.button} onPress={() => navigation.navigate("Settings_SP")}>
                        <Settings2 size={32} color={"#10B981"} />
                    </TouchableOpacity>

                </View>
                <View style = {{flexDirection: "row"}}>
                    <View style = {styles.profilePicture}>

                    </View>
                    <View style = {styles.statBlock}>
                        <Text style = {styles.stat}>Followers</Text>
                        <Text style = {styles.number}>5</Text>
                        <Text style = {styles.stat}>Following</Text>
                        <Text style = {styles.number}>5</Text>
                        <Text style = {styles.stat}>Threads</Text>
                        <Text style = {styles.number}>5</Text>
                    </View>
                </View>
         
        <TouchableOpacity style = {{flexDirection: 'row-reverse', marginRight: 20}} onPress={() => handleEditMode()}>
                    <Pencil size = {20} color={ bioMode ?  "lightgreen" : "black"  } ></Pencil>
                </TouchableOpacity>
                <View style = {{padding: 20, maxHeight: 115}}>
                    <Text style = {styles.bio}>
                        {bioMode?  
                            <TextInput
                            placeholder="Write about who you are!"
                            underlineColor="transparent"
                            activeUnderlineColor="transparent"
                            value={bioText}
                            onChangeText={test}
                            style={{ backgroundColor: "white", height: 50 }}
                            />
                            : 
                            bioText 
                        }
                    </Text>
                </View>
            </View>
            <View style = {styles.tabContainer}>
            
                <View style = {[styles.tab,{borderRightWidth: 0,}]}>
                    <TouchableOpacity  onPress={() => setCurrentTab('Threads')}>
                        <Text 
                            style={[
                            styles.tabText, 
                            currentTab === 'Threads' ? { backgroundColor: '#e6f4ea' } : {}
                            ]}
                        >
                        Threads
                        </Text>
                    </TouchableOpacity>
                </View>
        
                <View style = {styles.tab}>
                    <TouchableOpacity  onPress={() => setCurrentTab('Bets')}>
                        <Text 
                            style={[
                            styles.tabText, 
                            currentTab === 'Bets' ? { backgroundColor: '#e6f4ea' } : {}
                            ]}
                        >
                        Predictions
                        </Text>
                    </TouchableOpacity>
                </View>
            
            </View>
            
                {threadsLoading ? (
                    <ActivityIndicator size="large" color="blue" /> // Show loading spinner
                ) : (
                    <View style = {{height: 700}}>
                <FlatList
                    data={threads}
                    ListEmptyComponent={<View style = {styles.notFound}>
                    <Frown size={50} color="gray" />
                    <Text>No threads available</Text>
                    </View>}
                    keyExtractor={(item) => item.tid.toString()}
                    removeClippedSubviews={false}
                    onRefresh={() => refetchThreads} // Enable pull-to-refresh
                    refreshing={threadsLoading} // Show loading state during refresh
                    nestedScrollEnabled={true}
                    renderItem={({ item }) => (

                    <View style={styles.post}>
                        <TouchableOpacity onPress={() => navigation.navigate("Thread_S",item)}>

                            <View style={styles.postHeader}>
                               
                               <View style = {{flexDirection:"row", alignItems:"center"}}>
                                    <View style={styles.avatar} />
                                    <View>
                                        <Text style={styles.userName}>{ user.user_name }</Text>
                                        <Text style={styles.timestamp}>2h ago</Text>
                                    </View>
                                </View>

                                <TouchableOpacity onPress={() => deleteThread(item.tid)} >
                                    <Trash2 size = {20} color = {'red'}></Trash2>
                                </TouchableOpacity>
                            </View>
                            <Text style={styles.postText}>
                                {threads.at(item.index).title}
                            </Text>
                            <View style={styles.postFooter}>
                            <View style={styles.actionsLeft}>
                                <TouchableOpacity style={styles.actionButton}>
                                <Heart size={18} color="gray" />
                                <Text>24</Text>
                                </TouchableOpacity>
                                <TouchableOpacity style={styles.actionButton}>
                                <MessageCircle size={18} color="gray" />
                                <Text>12</Text>
                                </TouchableOpacity>
                            </View>
                            <View style={styles.actionsRight}>
                                <View style={styles.actionButton}>
                                <DollarSign size={18} color="green" />
                                <Text>$2.5K</Text>
                                </View>
                                <View style={styles.actionButton}>
                                <Users size={18} color="green" />
                                <Text>18</Text>
                                </View>
                            </View>
                            </View>
                        </TouchableOpacity>
                    </View>
                    )}
                />
                </View>
                )}
                    </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fcfcf7',
    },header:{
        
        backgroundColor: "white",
        borderBottomRightRadius: 40,
        paddingTop: 30,
        shadowColor: '#000',
        shadowOpacity: 0.1,
        shadowRadius: 5,
        elevation: 5,

    },profilePicture:{

        height: 240,
        width: 220,
        backgroundColor: "black",
        borderBottomRightRadius: 20,
        borderTopRightRadius: 20,
        shadowColor: '#000',
        shadowOpacity: 0.1,
        shadowRadius: 5,
        elevation: 3,

    },name:{
        fontSize: 24,
        fontWeight: "bold",
        textAlign: "left",   
    },nameContainer:{
        marginBottom: 15,
        marginLeft: 5,
        width: 300,
        flexDirection: "row",   
    },statBlock:{
        marginLeft: 55,
        marginTop: 20,
    },stat:{
        fontSize: 18,
        fontWeight: "bold",
        textAlign: "center",   
        color: "green"
    },number:{
        fontSize: 24,     
        textAlign: "center",   
        color: "green",
        marginBottom: 15,
    },button:{
        marginRight: 20,
    },bio:{
        fontSize: 15,
        textAlign: 'center',
        fontWeight: 'bold',
    },tabContainer:{
        flexDirection: 'row',
        justifyContent:'space-evenly',
        alignContent: 'center',
        paddingVertical: 10,
        paddingHorizontal: 16,
      },tab:{
        flex: 33.33,
        borderColor: '#ccc',
        borderRightWidth: 1,
        borderLeftWidth: 1,
      },tabText:{
        textAlign: 'center',
        paddingVertical: 7,
        color: '#666',
        fontSize: 16,
      },
      post: {
        
        padding: 16,
        borderWidth: 1,
        borderColor: "#ddd",
        borderTopWidth:0,
      },
      postHeader: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        marginBottom: 8,
      },
      avatar: {
        width: 40,
        height: 40,
        backgroundColor: "lightgreen",
        borderRadius: 20,
      },
      userName: {
        fontWeight: "bold",
        marginLeft: 10,
      },
      timestamp: {
        fontSize: 12,
        color: "gray",
        marginLeft: 10,
      },
      postText: {
        marginBottom: 8,
      },
      postFooter: {
        flexDirection: "row",
        justifyContent: "space-between",
      },
      actionsLeft: {
        flexDirection: "row",
      },
      actionsRight: {
        flexDirection: "row",
      },
      actionButton: {
        flexDirection: "row",
        alignItems: "center",
        marginRight: 10,
      },rowContainer:{
        flexDirection:"row",
        alignItems:"center",
      },USDC:{
        fontSize:12,
        marginLeft: 5,
        fontWeight:"bold",
      },amount:{
        marginLeft: 5,
        fontSize: 20,
      },notFound:{
        marginTop: 50,
        justifyContent:"center",
        alignItems:"center",
      }

})

export default SelfProfileScreen;