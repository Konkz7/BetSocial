import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native';
import { useQuery } from "@tanstack/react-query";
import { fillReadMarkers, getActiveNotifications, getUser, getUsers, readNotifications, removeNotification, toThreadProfile} from "./API";
import { useFocusEffect } from '@react-navigation/native';
import { timeAgo } from './Constants';
import { activitySeenStore, screenStore } from './GlobalFlags';
import { eventEmitter } from './Components/EventBus';
import { X } from 'lucide-react-native';

const ActivityScreen = ({ navigation, route } : any) => {
    const [filteredData, setFilteredData] = useState<any[]>([]);
    
    // Fetch notifications
    const { data: notifications, isLoading: notificationsLoading , refetch: refetchNotifications} = useQuery({
        queryKey: ["activeNotifications"],
        queryFn: getActiveNotifications
    });

    const { data: users, isLoading: usersLoading , refetch: refetchUsers } = useQuery({ 
        queryKey: ["Users"], 
        queryFn: getUsers 
    });

    useFocusEffect(
        useCallback(() => {
            screenStore.set("Activity");
            refetchNotifications();
            readNotifications();
            activitySeenStore.set(true);
            // mark notifications as read ui-wise
            setFilteredData(notifications || []);
            const sub =  eventEmitter.addListener('notificationReceived', (data : any) => {
                refetchNotifications();
                console.log("Notification received via event bus" + data.title);
                
            });

        
            console.log("Screen is focused! Refetching threads...");
            return () => {     
                sub.remove();
                console.log("Screen is unfocused! Cleanup if needed.");
            };
        }, [refetchUsers, refetchNotifications,notifications])
    );
        
    
    // Update filteredData when notifications change
    useEffect(() => {
        if (notifications) {
            setFilteredData(notifications);
        }
    }, [notifications]);  // Only runs when `notifications` changes


    const getUserById = (actorId: number) => users?.find((user:any) => user.uid === actorId) || console.log("User not found with ID:", actorId);

  

    const remove = async (nid:number ) => { 
        setFilteredData((prevData) => prevData.filter(item => item.nid !== nid));
        removeNotification(nid); 
    };

    async function goToDMScreen(user: any, gid:number , nid:number){
    
        const recipient : any = {};
        recipient["user"] = user;
        recipient["gid"] = gid;


        fillReadMarkers(recipient["gid"]);
        removeNotification(nid);
        navigation.navigate("Messages", {
            screen: "DMScreen_M",
            params: recipient, 
        });
    
    }


    async function goToThreadScreen(tid:number , nid:number){
    
        const profile = await toThreadProfile(tid);

        removeNotification(nid);
        navigation.navigate("Home", {
            screen: "Thread_H",
            params: profile, 
        });
    
    }

    async function goToProfileScreen(actor : any , nid : number){
    
        
        removeNotification(nid);
        navigation.navigate("Search", {
            screen: "Profile_S",
            params: actor, 
        });
    
    }

    const notificationComponent = (item : any , user : any , description: string , action : () => void) => {

        return(
            <TouchableOpacity onPress={action}>
                <View style = {styles.extrasContainer}>
                    <TouchableOpacity style = {{}} onPress={() => remove(item.nid)}>
                        <X size = {16} color = {"red"}/>
                    </TouchableOpacity>
                </View>
                <View style = {styles.notificationContainer}>
                    <View style = {styles.avatar}></View>
                    <Text style={[styles.itemText, {fontWeight: 'bold' , color: 'green'}]}>{user?.user_name}</Text>
                    <Text style={styles.itemText}>{description}</Text>
                </View>
                <View style = {styles.extrasContainer}>
                    <Text style={[styles.itemText, {color: 'gray', fontSize: 12}]}>{timeAgo(item.created_at)}</Text>
                </View>
                <View style = {styles.extrasContainer}>
                    
                </View>
            </TouchableOpacity>
        )
            
    };
    
    // Default case if notification type is not found
    const renderNotificationItem = (item : any) => {

        const actor = getUserById(item.actor_id);
        //console.log(item.notification_type);
        if(item.notification_type === "follow_request") {
            return notificationComponent(item, actor," sent you a follow!", () => goToProfileScreen(actor,item.nid))    
        }else if(item.notification_type === "message") {
            return notificationComponent(item, actor, " sent you a message!", () => goToDMScreen(actor, item.target_id,item.nid))
        }else if(item.notification_type === "new_thread") {
            return notificationComponent(item, actor, " posted a new thread!",() => goToThreadScreen(item.target_id,item.nid) )
        }else if(item.notification_type === "new_comment") {
            return notificationComponent(item, actor, " commented on your post!",() => goToThreadScreen(item.target_id,item.nid) )
        }else if(item.notification_type === "thread_like") {
            return notificationComponent(item, actor, " liked your post!",() => goToThreadScreen(item.target_id,item.nid) )
        }else if(item.notification_type === "comment_like") {
            return notificationComponent(item, actor, " liked your comment!",() => goToThreadScreen(item.target_id,item.nid) )
        }
        else{ 
            return(
            <View style={styles.notificationContainer}>
                <Text style={styles.itemText}>{item.description}</Text>
            </View>
            );
        }
        
    }


    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Activity</Text>
            </View>

            {notificationsLoading ? (
                <Text style={styles.loadingText}>Loading...</Text>
            ) : (
                <FlatList
                    data={filteredData}
                    removeClippedSubviews={false}
                    keyExtractor={(item) => item.nid.toString()}
                    renderItem={({ item }) => (
                        <View style={styles.item}>
                            {renderNotificationItem(item)}
                        </View>
                    )}
                    ListEmptyComponent={<Text style={styles.noResults}>No New Activity</Text>}
                />
            )}
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fcfcf7',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 20,
        paddingHorizontal: 16,
        backgroundColor: 'white',
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '600',
        color: 'green',
    },
    itemText: {
        fontSize: 15,
        paddingLeft: 10,    
    },
    nameText: {
        fontSize: 15,
        fontWeight: 'bold' , 
        color: 'green'
    },
    noResults: {
        textAlign: 'center',
        fontSize: 16,
        color: 'gray',
        marginTop: 200,
    },
    loadingText: {
        textAlign: 'center',
        fontSize: 18,
        color: 'gray',
        marginTop: 50,
    },
    item: {
        padding: 15,
        borderBottomWidth: 1,
        borderBottomColor: '#e5e7eb',
        backgroundColor: 'white',
    },
    avatar: {
        width: 40,
        height: 40,
        backgroundColor: "lightgreen",
        borderRadius: 20,
    },
    actionButton:{
        padding: 10, 
        borderRadius: 5, 
        marginTop: 5,
    },
    notificationContainer:{
        flexDirection: 'row', 
        alignItems: 'center',
        maxWidth: '70%',
    },
    extrasContainer:{
        flexDirection: 'row-reverse', 
        alignItems: 'center', 
        marginRight:20,
    },
});

export default ActivityScreen;
