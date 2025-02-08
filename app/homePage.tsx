import React, { useState} from "react";
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  ScrollView,
  StyleSheet,
  SafeAreaView,
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
} from "lucide-react-native";

const categories = [
  "All",
  "Sports",
  "Politics",
  "Entertainment",
  "Tech",
  "Gaming",
];


const HomeScreen = ({navigation}:any) => {
  const [activeCategory, setActiveCategory] = useState("All");
  const [activePage , setActivePage] = useState("Home");

  return (
    <SafeAreaView style={styles.container} >
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <TouchableOpacity>
            <Menu size={24} color="green" />
          </TouchableOpacity>
          <Text style={styles.title}>BetSocial</Text>
          <View style={styles.profileIcon} />
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryScroll}>
          {categories.map((category) => (
            <TouchableOpacity
              key={category}
              onPress={() => setActiveCategory(category)}
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
      <FlatList
        data={[1, 2, 3, 4,5,6]}
        keyExtractor={(item) => item.toString()}
        removeClippedSubviews={false}
        renderItem={() => (
          <View style={styles.post}>
            <View style={styles.postHeader}>
              <View style={styles.avatar} />
              <View>
                <Text style={styles.userName}>User Name</Text>
                <Text style={styles.timestamp}>2h ago</Text>
              </View>
            </View>
            <Text style={styles.postText}>
              Who's betting on the championship game tonight? I've got a good
              feeling about the underdogs! 🏆
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
          </View>
        )}
      />

      
    </SafeAreaView>
  );
}

// Styles
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fcfcf7",
  },
  header: {
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#ddd",
    paddingTop: 40,
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
    fontSize: 20,
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
  },
  bottomNav: {
    flexDirection: "row",
    justifyContent: "space-around",
    backgroundColor: "white",
    borderTopWidth: 1,
    borderTopColor: "#ddd",
    paddingVertical: 10,
  },
});

export default HomeScreen;