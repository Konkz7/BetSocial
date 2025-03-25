import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, Image, StyleSheet,SafeAreaView } from 'react-native';
import { Search ,X} from 'lucide-react-native';
import { TextInput,Searchbar } from 'react-native-paper';
import { QueryClient, QueryClientProvider,useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {getUsers} from "./API";
import { useFocusEffect } from '@react-navigation/native';


const SearchScreen = ({ navigation , route } : any) => {
    const [search,setSearch] = useState('');
    const [currentTab,setCurrentTab] = useState('People');
    const [filteredData, setFilteredData] = useState<any>([]);
    const [list, setList] = useState<any>([]);

    const { data: users, isLoading: usersLoading } = useQuery({ queryKey: ["Users"], queryFn: getUsers });


    useFocusEffect(
          useCallback(() => {
            console.log("Screen is focused! Perform refresh or action here.");  
            setSearch('');
            return () => {
              console.log("Screen is unfocused! Cleanup if needed."); 
            };
          }, [])
      );
    

  const handleSearch = (text: string) => {
    setSearch(text);
    
    if (text.trim() === '') {
      setFilteredData(null);
    } else {
      setFilteredData(
        users.filter((item: any) => 
          item.user_name.toLowerCase().includes(text.toLowerCase())
        )
      );
    }
  };



  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Searchbar     
          value = {search}
          onChangeText={handleSearch}
          style = {styles.searchBar}
          icon={() => <Search size={20}  />}
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

        <View style = {styles.tab}>
            <TouchableOpacity  onPress={() => setCurrentTab('Categories')}>
                <Text 
                    style={[
                    styles.tabText, 
                    currentTab === 'Categories' ? { backgroundColor: '#e6f4ea' } : {}
                    ]}
                >
                Categories
                </Text>
            </TouchableOpacity>
        </View>

      </View>

      <FlatList
        data={filteredData}
        keyExtractor={(item) => item.uid.toString()}
        renderItem={({ item }) => (

          <TouchableOpacity style={styles.item}>
            <Text style={styles.itemText}>{item.user_name}</Text>
          </TouchableOpacity>
        )}
        ListEmptyComponent={<Text style={styles.noResults}>No results found</Text>}
      />
     
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
    backgroundColor: '#fff',
    marginBottom: 8,
    borderRadius: 5,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 5,
    elevation: 2,
  },
  
});

export default SearchScreen;