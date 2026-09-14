import React, { useCallback, useState } from 'react';

import { View, Text, FlatList, TouchableOpacity, Image, StyleSheet,SafeAreaView } from 'react-native';
import { Search ,X} from 'lucide-react-native';
import { TextInput,Searchbar } from 'react-native-paper';
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {getThreadLikes, searchUsers, searchThreads} from "./API";
import { useFocusEffect } from '@react-navigation/native';
import threadList from './Components/ThreadList';
import { screenStore } from './GlobalFlags';
import { getProfilePictureUrl } from './Constants';

// One shared empty array. A fresh [] on every render is exactly what made the
// results loop when they were copied into state, and a stable reference costs
// nothing to keep.
const NOTHING: any[] = [];


const SearchScreen = ({ navigation} : any) => {
    const [search,setSearch] = useState('');
    const [currentTab,setCurrentTab] = useState('People');


    // Both are searched on the server. Threads used to be filtered here from the
    // cached feed, which searched one page of however many exist - and returned
    // an empty list for everything else, which is indistinguishable from "no
    // matches". It also called getThreads as the queryFn directly, so React Query
    // passed its context object where a cursor was expected.
    const { data: people, isLoading: usersLoading } = useQuery({
      queryKey: ["userSearch", search],
      queryFn: () => searchUsers(search),
      // Only ask once something has been typed: this tab shows nothing until
      // then, so the first request would be for a list nobody is looking at.
      enabled: search.trim() !== "" && currentTab === "People",
      placeholderData: (previous: any) => previous,
    });
    const { data: threads, isLoading: threadsLoading } = useQuery({
      queryKey: ["threadSearch", search],
      queryFn: () => searchThreads(search),
      // Same as People: nothing is shown until something is typed, so the first
      // request would be for a list nobody is looking at.
      enabled: search.trim() !== "" && currentTab === "Threads",
      placeholderData: (previous: any) => previous,
    });


    // Derived rather than held in state. Copying the results into state through
    // an effect meant the effect depended on the query data, set state, and so
    // ran again - and because both queries defaulted to a fresh [], every render
    // looked like new data. That is the loop React reports as "maximum update
    // depth exceeded", and it started the moment this screen opened, since an
    // empty search box takes the branch that sets a new [] every time.
    const filteredData =
      search.trim() === ''
        ? NOTHING
        : (currentTab === 'People' ? people : threads) ?? NOTHING;

    useFocusEffect(
      useCallback(() => {
        screenStore.set("Search");
      }, [])
    );
    

  // Only records what was typed. Both queries re-run on the new term and the
  // effect above copies whichever tab's result into filteredData.
  //
  // What was here filtered `threads` in place - and `threads` is a FeedPage
  // object, not an array, so `threads.filter` threw on every keystroke in the
  // Threads tab. That is why searching threads did nothing at all.
  const handleSearch = (text: string) => {
    setSearch(text);
  };


  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Searchbar     
          value = {search}
          onChangeText={handleSearch}
          autoCapitalize='none'
          style = {styles.searchBar}
          icon={() => <Search size={20} />}
          clearIcon={() => <X></X>}
          elevation={2}    
        />
      </View>
      <View style = {styles.tabContainer}>

        <View style = {[styles.tab,{borderRightWidth: 0,}]}>
            <TouchableOpacity onPress={() => setCurrentTab('People')}>
                <Text 
                    style={[
                    styles.tabText, 
                    currentTab === 'People' ? { backgroundColor: '#e6f4ea' } : {}
                    ]}
                >
                People
                </Text>
            </TouchableOpacity>
        </View>

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

      </View>



    {currentTab === 'Threads' ? threadList(filteredData,() =>{} , threadsLoading,navigation,"Thread_S",filteredData,"search") :
    
      usersLoading ? (<Text style={styles.loadingText}>Loading...</Text>) :
        <FlatList
          data={filteredData}
          removeClippedSubviews={false}
          keyExtractor={(item) => item.uid.toString() }
          renderItem={( {item }) => (

            <TouchableOpacity style={styles.item} onPress={() => navigation.navigate("Profile_S",item)}>
              <Image
                  source={getProfilePictureUrl(item?.profile_picture)}
                  style={styles.avatar}
              /> 
              <Text style={styles.itemText}>{item.user_name}</Text>
            </TouchableOpacity>
          )}
          ListEmptyComponent={<Text style={styles.noResults}>No results found</Text>}
        />
    }
     
    </SafeAreaView> 
  );
}

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
  },searchBar:{
    borderWidth: 0 ,
    backgroundColor: '#aff589', 
    width: '100%', 
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
  itemText: {
    fontSize: 20,
    paddingLeft: 10,
    

  },
  noResults: {
    textAlign: 'center',
    fontSize: 16,
    color: 'gray',
    marginTop: 20,
  },item: {
    padding: 15,
    flexDirection: 'row',
    alignItems: 'center',
    /*
    backgroundColor: '#fff',
    marginBottom: 8,
    borderRadius: 5,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 5,
    elevation: 2,
    */
   borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  loadingText: {
      textAlign: 'center',
      fontSize: 18,
      color: 'gray',
      marginTop: 50,
  },
  avatar: {
    width: 40,
    height: 40,
    backgroundColor: "lightgreen",
    borderRadius: 20,
  }
  
});

export default SearchScreen;