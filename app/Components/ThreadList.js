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
  Trash2,
  Frown,
} from "lucide-react-native";
import Video from "react-native-video";

 
import { removeThread, registerThreadLike} from "../API";
import { timeAgo } from "../Constants";


const threadList =  (threads, getthreads ,loading ,navigation ,nav ,setThreads,selfProfile) =>  {
    

    const threadLikeAction = async (tid, likeResult) => {
        await registerThreadLike(tid, !likeResult);
    
        const updatedThreads = threads.map(thread =>
          thread.tid === tid
            ? { 
                ...thread,   
                liked: !likeResult,
                likes: likeResult ? thread.likes - 1 : thread.likes + 1 
              }
            : thread
        );
    
        setThreads(updatedThreads);
    };

    function deleteThread(tid) {
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
        loading ? (
            <ActivityIndicator size="large" color="blue" /> // Show loading spinner
        ) : (

        <View style = {{flex: 1}}>
        <FlatList
            data={threads}
            ListEmptyComponent={<View style = {styles.notFound}>
            <Frown size={50} color="gray" />
            <Text>No threads available</Text>
            </View>}
            keyExtractor={(item) => item.tid.toString()}
            removeClippedSubviews={false}
            onRefresh={getthreads} // Enable pull-to-refresh
            refreshing={loading} // Show loading state during refresh
            renderItem={({ item }) => (

            <View style={styles.post}>
                <TouchableOpacity onPress={() => navigation.navigate(nav,item)} onLongPress={selfProfile ? () => deleteThread(item.tid) : null}>
                    <View style = {{flexDirection:"row", alignItems:"center",justifyContent:"space-between", marginBottom:10}}>
                        <View >
                            <View style={styles.postHeader}>
                                {/* Left side (avatar + name + timestamp) */}
                                <View style={{ flexDirection: "row", alignItems: "center" }}>
                                    <Image
                                        source={{ uri: item.user.profile_picture }}
                                        style={styles.avatar}
                                    />
                                    <View>
                                        <Text style={styles.userName}>{item.user.user_name}</Text>
                                        <Text style={styles.timestamp}>{timeAgo(item.created_at)}</Text>
                                    </View>
                                </View>

                                
                            </View>

                        
                            <Text style={styles.postText}>
                                {item.title}
                            </Text>

                            

                        </View>

                        {item.media &&
                            <View style = {{ marginRight:17 , borderRadius:10, overflow:"hidden" , borderWidth:1, borderColor:"#10B981"}}>
                                {item.media_type === 1 ? (
                                <Image
                                    source = {{ uri : item.media}}
                                    style = {{ width: 80, height: 80}}
                                />
                                ) : item.media_type === 2 ? (
                                <Video
                                    source = {{ uri : item.media}}
                                    style = {{ width: 80, height: 80}}
                                    repeat = {true}
                                    muted = {true}
                                    />
                                ) : null}
                            </View>
                        }

                    </View>

                        <View style={styles.postFooter}>
                            <View style={styles.actionsLeft}>
                                <TouchableOpacity style={styles.actionButton} onPress={() => threadLikeAction(item.tid,item.liked)}>
                                    <Heart size={18} color={item.liked ? "red":"gray"} fill={item.liked ? "red":"transparent"} style = {{marginRight:5}}  />
                                    <Text>{item.likes}</Text>
                                </TouchableOpacity>
                                <TouchableOpacity style={styles.actionButton}>
                                    <MessageCircle size={18} color="gray" style = {{marginRight:5}} />
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
    ))
}



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
    justifyContent: "space-between", 
    marginBottom: 8,
    width: "100%",
    
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

export default threadList