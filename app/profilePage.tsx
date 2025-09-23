import React, { useState, useCallback,useEffect } from "react";
import { View, StyleSheet,Alert, TouchableOpacity,ScrollView , ActivityIndicator, FlatList} from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { IP_STRING } from "./Constants";
import { SafeAreaView } from "react-native-safe-area-context";
import { ArrowLeft, Bold, DollarSign, Frown, Heart, MessageCircle, Users } from "lucide-react-native";
import { 
    UserRoundCheck,
    UserRoundPlus,
    UserRoundCog,
    Send,
    CircleAlert,
  } 
    from "lucide-react-native";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getFollow, getUserThreads, follow, unfollow , DMCheck , makePrivateGroup, fillReadMarkers, getThreadLikes, registerThreadLike, getOtherFollow, getFollowsByID, getFollowersByID} from "./API";
import { get } from "react-native/Libraries/TurboModule/TurboModuleRegistry";
import threadList from "./Components/ThreadList";
import { screenStore } from "./GlobalFlags";




const ProfileScreen = ({navigation , route}: any) => {

    const [currentTab,setCurrentTab] = useState('Threads');
    const [threads, setThreads] = useState<any[]>([]);
    const [threadsLoading, setThreadsLoading] = useState(true); // Show loading indicator
    const [following, setFollowing] = useState(false);
    const [followed , setFollowed] = useState(false);

    const[follows , setFollows] = useState(0);
    const[followers , setFollowers] = useState(0);

    const queryClient = useQueryClient();
    const profile = queryClient.getQueryData(["user"]) as any;
    const groupProfiles = queryClient.getQueryData(["groupProfiles"]) as any;

    var user = route.params;
   
    const { data: threadData, isLoading: threadDataLoading, refetch: refetchThreads } = useQuery({
        queryKey: ["userThreads" + user.uid],
        queryFn: () => getUserThreads(user.uid),
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
    });
       
    const { data: followData, refetch: refetchFollow, isLoading: followLoading } = useQuery({ 
        queryKey: ["follow"], 
        queryFn: () => getFollow(user.uid),
    });

    const { data: otherFollowData, refetch: refetchOtherFollow, isLoading: otherFollowLoading } = useQuery({ 
        queryKey: ["otherFollow"], 
        queryFn: () => getOtherFollow(user.uid),
    });

    const { data: followsData, refetch: refetchFollows, isLoading: followsLoading } = useQuery({ 
        queryKey: ["follows" + user.uid], 
        queryFn: () => getFollowsByID(user.uid),
    });

    const { data: followersData, refetch: refetchFollowers, isLoading: followersLoading } = useQuery({ 
        queryKey: ["followers" + user.uid], 
        queryFn: () => getFollowersByID(user.uid),
    });


   
    const getAllthreads = async () => {
        try {
            const threadResponse = await refetchThreads(); // use react-query’s refetch
            if (!threadResponse.data) return;

            
            setThreads(threadResponse.data);

        } catch (error) {
            Alert.alert("Error:", "Failed to fetch threads");
        } finally {
            setThreadsLoading(false);
        }
    };
    
     
    useFocusEffect(
        useCallback(() => {
            console.log("Screen is focused! Refetching threads and friendship...");
            screenStore.set("Profile");
            refetchThreads();
            refetchFollow(); // Ensure this refetches correctly
            refetchOtherFollow();
            refetchFollowers();
            refetchFollows();
             
            console.log("Friendship data:", followData);

            return async () => {

                console.log("Screen is unfocused! Cleanup if needed.");       
            };
        }, []) 
    );
    
    
    useEffect(() => {
            getAllthreads();
    }, []);

    useEffect(() => {
        if (followData) {
            console.log("Friendship data:", followData);
            setFollowing(true)
        }else{
            setFollowing(false);
        }
    }, [followData]);

    useEffect(() => {
        if (otherFollowData) {
            console.log("Other Friendship data:", otherFollowData);
            setFollowed(true)
        }else{
            setFollowed(false);
        }
    }, [otherFollowData]);

    useEffect(() => {
        if (followsData) {
            setFollows(followsData.length)
        }
    }, [followsData]);

    useEffect(() => {
        if (followersData) {
            setFollowers(followersData.length)
        }
    }, [followersData]);




    async function goToDMScreen(){

        // could be optimized by caching group data and checking against that
        const recipient : any = {};
        recipient["user"] = user;
        const gid = await DMCheck(user.uid);
        console.log("Gid:" + gid);

        if( gid === undefined || gid === null || gid === ""){
            const newGroup = await makePrivateGroup(user.uid);
            recipient["gid"] = newGroup.gid;
            console.log("New group" + newGroup);
        }else{ 
            recipient["gid"] = gid;
        }

        //fillReadMarkers(recipient["gid"]);

        navigation.navigate("DMScreen_S",recipient); 
    }
    
    async function handleFollow() {
        if (!following) {
            
            try{
                await follow(user.uid);
                //Alert.alert("Message:", "Friend request sent to " + user.user_name);
                setFollowing(true);    
            }catch (error) {
                //Alert.alert("Error:", "Failed to send friend request");
            }

        } else  {          
            try{
                await unfollow(user.uid);
                //Alert.alert("Message:", "Friend request cancelled for " + user.user_name);
                setFollowing(false); 
            }catch (error) {
                //Alert.alert("Error:", "Failed to send friend request");
            }
        } 
        
        queryClient.invalidateQueries({queryKey: ["follow"]});
        
    }


    return (

        <SafeAreaView style = {styles.container}>
          <ScrollView>
            <View style = {styles.header} >
                <View style = {{ flexDirection: "row"}}>

                    <TouchableOpacity  onPress={() => navigation.goBack()}>
                        <ArrowLeft size={36} color={"#10B981"} />   
                    </TouchableOpacity>

                    <Text style = {[styles.name , followed ? {color: "blue"} : null]}> {user.user_name} </Text>
            
                    {followLoading ? 
                    <ActivityIndicator size = "small" color = " green"/>
                    :
                    <TouchableOpacity style = {styles.button} onPress={() => handleFollow()}> 
                        {
                        following === false ? <UserRoundPlus color={"green"}></UserRoundPlus> :
                        <UserRoundCheck color={"green"}></UserRoundCheck> 
                        }
                    </TouchableOpacity>
                    }

                    <TouchableOpacity style = {styles.button} onPress={() => goToDMScreen()}>
                        <Send color={"green"}></Send>
                    </TouchableOpacity>

                    <TouchableOpacity style = {styles.button}>
                        <CircleAlert color={"red"}></CircleAlert>
                    </TouchableOpacity>

                </View>
                <View style = {{flexDirection: "row"}}>
                
                    
                    <TouchableOpacity >
                        <View style = {styles.profilePicture}>

                        </View>
                    </TouchableOpacity>

                    <View style = {styles.statBlock}>
                        <Text style = {styles.stat}>Followers</Text>
                        <Text style = {styles.number}>{followers}</Text>
                        <Text style = {styles.stat}>Following</Text>
                        <Text style = {styles.number}>{follows}</Text>
                        <Text style = {styles.stat}>Threads</Text>
                        <Text style = {styles.number}>{threadsLoading? 0 : threadData?.length}</Text>
                    </View>
                </View>
                <View style = {{padding: 20, maxHeight: 115}}>
                    <Text style = {styles.bio}>
                        {user.bio}
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

            {threadList(threads, refetchThreads, threadsLoading, navigation, "Thread_S", setThreads,"non")}

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
        marginBottom: 15,
        marginLeft: 5,
        width: 215,
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

export default ProfileScreen;