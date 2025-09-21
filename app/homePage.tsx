import React, { useState, useCallback, useEffect } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  ScrollView,
  StyleSheet,
  SafeAreaView,
  Alert,
  ActivityIndicator,
  Image,
} from "react-native";
import {
  MessageCircle,
  Heart,
  DollarSign,
  Users,
  Home,
  Search,
  Bell,
  Mail,
  Menu,
  CirclePlus,
  Wallet,
  Frown,
} from "lucide-react-native";
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {getBalance, getCircleSecret,getIpAddress,getProfile,getWallet,getGroupProfiles, getThreadLikes, registerThreadLike, getThreads, getActiveNotifications, getConversations} from "./API";
import { useFocusEffect ,} from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { IP_STRING } from "./Constants";
import Video from "react-native-video";
import {useNotificationListener } from "./Components/FBCloudMessagingService";
import threadList from "./Components/ThreadList";
import { activitySeenStore, messageSeenStore, screenStore } from "./GlobalFlags";

const categories = [
  "All",
  "Sports",
  "Politics",
  "Entertainment",
  "Tech",
  "Gaming",
];


const HomeScreen = ({navigation,route}:any) => {
  const [activeCategory, setActiveCategory] = useState("All");
  const [threads, setActiveThreads] = useState<any[]>([]);
  const [loading, setLoading] = useState(true); // Show loading indicator
  const [trueThreads , setTrueThreads] = useState<any[]>([]);

  
  // Fetch data using React Query
  // ✅ Fetching profile, circle secret, and wallet using useQuery

  // consider this in backend
  const { data: profile, isLoading: profileLoading , refetch: refetchProfile } = useQuery({ queryKey: ["user"], queryFn: getProfile });
  const { data: notifications, isLoading: notificationsLoading } = useQuery({
    queryKey: ["activeNotifications"],
    queryFn: getActiveNotifications
  });

  const { data: threadData, isLoading, refetch: refetchThreads } = useQuery({
    queryKey: ["threads"],
    queryFn: getThreads,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  

  //const { data: groupProfiles, isLoading: groupProfilesLoading } = useQuery({ queryKey: ["groupProfiles"], queryFn: getGroupProfiles});


  /*
  const { data: circleSecret, isLoading: circleLoading } = useQuery({ queryKey: ["circle-secret"], queryFn: getCircleSecret });
  const { data: wallet, isLoading: walletLoading } = useQuery({ queryKey: ["wallet"], queryFn: getWallet });
  const { data: balance, isLoading: balanceLoading } = useQuery({ queryKey: ["balance"], queryFn: getBalance });
  const { data: ipAddress, isLoading: ipLoading } = useQuery({ queryKey: ["ipAddress"], queryFn: getIpAddress});
  */

  useNotificationListener();
  
  /*
  const getAllthreads = async () => {
    try {
      const threadResponse = await refetchThreads(); // use react-query’s refetch
      if (!threadResponse.data) return;

      
      setActiveThreads(threadResponse.data);

    } catch (error) {
      Alert.alert("Error:", "Failed to fetch threads");
    } finally {
      setLoading(false);
    }
  };
  */

  const updateThreadCat = (category:string, threads : any[] , resetCat:boolean) => {
    if(resetCat){
      setActiveCategory(category);
    }
    if (category === "All") {
      setActiveThreads(threads);
    } else {
      setActiveThreads(threads.filter(thread => thread.category === category));
    }  

  }

  const updateThreadLikes = async (baseThreads: any[]) => {
    try {
      const likedThreads = await getThreadLikes();
      const likedTids = likedThreads.map((l: any) => l.tid);

      return baseThreads.map(thread => {
        const newLiked = likedTids.includes(thread.tid);
        if (newLiked === thread.liked) return thread;
        return {
          ...thread,
          liked: newLiked,
          likes: newLiked ? thread.likes + 1 : thread.likes - 1,
        };
      });
    } catch (err) {
      Alert.alert("Error", "Failed to update likes");
      return baseThreads;
    }
  };

  const is_Unread_Conversations = async () => {
    try {
      const data = await getConversations();
      for (const conversation of data) {
        if (!conversation.lastMessage.is_read) {
          messageSeenStore.set(false);
          break;
        }
      }
    } catch (error) {
      console.error("Error fetching conversations:", error);
    }
  };

  const is_Unread_Activity = async () => {
    try {
      const data = await getActiveNotifications();
      
      if (!data[0].is_read) {
        activitySeenStore.set(false);   
      }
      
    } catch (error) {
      console.error("Error fetching notifications:", error);
    }
  };

  useEffect(() => {
    (async () => {
      is_Unread_Activity();
      is_Unread_Conversations();
    })();
  }, []);

  useEffect(() => {
    if (!threadData) return;
    (async () => {
      const updated = await updateThreadLikes(threadData);    
      setTrueThreads(updated);
      setLoading(false);

    })();
  }, [threadData]);

  useFocusEffect(
    useCallback(() => {
      console.log("Screen focused → refresh threads" + route.params?.params);
      screenStore.set("Home");
      
      (async () => {
        if (route.params?.refresh) {
          console.log("Refetching because refetch flag is true");
          refetchThreads();
          // clear the flag so it doesn’t loop forever
          navigation.setParams({ refresh: false });
          return;
        }

        if (threadData) {     
          const updated = await updateThreadLikes(threadData);          
          setTrueThreads(updated);    
          updateThreadCat(activeCategory,updated,false)   
        } else { 
          await refetchThreads();
        }
      })();
      return () => {
        console.log("Screen unfocused");
      };
    }, [threadData,route.params,activeCategory])
  );

  return (
    <SafeAreaView style={styles.container}  >
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          
          <Text style={styles.title}>BetSocial</Text>

          <View style = {styles.rowContainer}>
            <TouchableOpacity style= {[styles.rowContainer,{marginRight:25}]} onPress={() => null /*wallet == undefined
              ? null :navigation.navigate("Wallet_H")*/}>
              <Wallet  color={"green"} size={20}></Wallet> 
              {/*<Text style = {styles.amount}>{balance && balance.data.tokenBalances.length > 0? "temp" : "0.00"}</Text>*/}
              <Text style = {styles.USDC}>USDC</Text>
            </TouchableOpacity>

            <TouchableOpacity style = {{marginRight:15}}>
             <Menu size={24} color="green" />
            </TouchableOpacity>

            <TouchableOpacity onPress={() => navigation.navigate("SelfProfile_H")}>
              <Image
                source={{ uri: profile?.profile_picture }}
                style={styles.avatar}
              />
            </TouchableOpacity>
          </View>
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryScroll}>
          {categories.map((category) => (
            <TouchableOpacity
              key={category}
              onPress={() =>updateThreadCat(category,trueThreads,true)}
              style={[
                styles.categoryButton,
                activeCategory === category && styles.categoryActive,
              ]}
            >
              <Text
                style={[
                  styles.categoryText,
                  activeCategory === category && styles.categoryActiveText,
                ]}
              >
                {category}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>


      {/* Main Content */}  
      {threadList(threads, refetchThreads, loading, navigation, "Thread_H", setActiveThreads,"non")}
      
     
    </SafeAreaView>
  );
}

// Styles
const styles = StyleSheet.create({
  container: {
    flex: 1.0,
    backgroundColor: "#fcfcf7",
  },
  header: {
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
    paddingTop: 30,
    paddingBottom: 10,
  },
  headerContent: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingBottom: 10,
  },
  title: {
    fontSize: 30,
    fontWeight: "bold",
    color: "green",
  },
  profileIcon: {
    width: 32,
    height: 32,
    backgroundColor: "lightgreen",
    borderRadius: 16,
  },
  categoryScroll: {
    paddingHorizontal: 16,
  },
  categoryButton: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 20,
    backgroundColor: "#e6f4ea",
    marginRight: 8,
  },
  categoryActive: {
    backgroundColor: "green",
  },
  categoryText: {
    fontSize: 14,
    color: "green",
  },
  categoryActiveText: {
    color: "white",
  },
  post: {
    backgroundColor: "white",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
  },
  postHeader: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 8,
  },
  avatar: {
    width:40,
    height:40,
    borderRadius: 15,
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
});

export default HomeScreen;