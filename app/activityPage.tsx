import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native';
import { useQuery } from "@tanstack/react-query";
import { acceptFriendshipRequest, getActiveNotifications, getUsers, rejectFriendshipRequest, removeNotification} from "./API";
import { useFocusEffect } from '@react-navigation/native';
import { timeAgo } from './Constants';

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
            console.log("Screen is focused! Refetching threads...");
            return () => {
                console.log("Screen is unfocused! Cleanup if needed.");
            };
        }, [refetchUsers, refetchNotifications])
    );
        
    
    // Update filteredData when notifications change
    useEffect(() => {
        if (notifications) {
            setFilteredData(notifications);
        }
    }, [notifications]);  // Only runs when `notifications` changes


    const getUserById = (actorId: number) => users?.find((user:any) => user.uid === actorId) || console.log("User not found with ID:", actorId);

    const doNothing = (num:number) =>{};

    const action = async (nid:number , func:any , funcTarget: number) => {
        await func(funcTarget);
        setFilteredData((prevData) => prevData.filter(item => item.nid !== nid));
        removeNotification(nid); 
    };

    const notificationComponents = {
        friend_request: (item : any , user : any) => ( 
            <View>
                <View style = {styles.notificationContainer}>
                    <View style = {styles.avatar}></View>
                    <Text style={[styles.itemText, {fontWeight: 'bold' , color: 'green'}]}>{user?.user_name}</Text>
                    <Text style={styles.itemText}>has sent a friend request!</Text>
                </View>
                <View style = {styles.extrasContainer}>
                    <Text style={[styles.itemText, {color: 'gray', fontSize: 12}]}>{timeAgo(item.created_at)}</Text>
                </View>
                <View style = {styles.extrasContainer}>
                    <TouchableOpacity style = {styles.actionButton} onPress={() => action(item.nid,rejectFriendshipRequest,item.target_id)}>
                        <Text style = {{color: 'red'}} >Reject</Text>
                    </TouchableOpacity>
                    <TouchableOpacity style = {styles.actionButton} onPress={() => action(item.nid,acceptFriendshipRequest,item.target_id)}>
                        <Text style = {{color: 'green'}} >Accept</Text>
                    </TouchableOpacity>
                </View>
            </View>
        ),
        
        accepted_friend_request: (item:any, user: any) => (
            <View>
                <View style = {styles.extrasContainer}>
                    <TouchableOpacity style = {{}} onPress={() => action(item.nid,doNothing,0)}>
                        <Text style = {{color: 'red'}} >X</Text>
                    </TouchableOpacity>
                </View>
                <View style = {styles.notificationContainer}>
                    <View style = {styles.avatar}></View>
                    <Text style={[styles.itemText, {fontWeight: 'bold' , color: 'green'}]}>{user?.user_name}</Text>
                    <Text style={styles.itemText}>has accepted your friend request!</Text>
                </View>
                <View style = {styles.extrasContainer}>
                    <Text style={[styles.itemText, {color: 'gray', fontSize: 12}]}>{timeAgo(item.created_at)}</Text>
                </View>
            </View>
        ),
        
    };
    
    // Default case if notification type is not found
    const renderNotificationItem = (item : any) => {

        if(item.notification_type === "friend_request") {
            return notificationComponents.friend_request(item, getUserById(item.actor_id))
        } else if(item.notification_type === "friend_request_accepted") {
            return notificationComponents.accepted_friend_request(item, getUserById(item.actor_id))
        }else{ 
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
        fontSize: 20,
        paddingLeft: 10,
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
        maxWidth: '80%',
    },
    extrasContainer:{
        flexDirection: 'row-reverse', 
        alignItems: 'center', 
        marginRight:20,
    },
});

export default ActivityScreen;
