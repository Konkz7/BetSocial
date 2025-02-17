export const IP_STRING = "http://192.168.0.104:8080" ; 

export const errorHandler = (error) => {
    
    if (axios.isAxiosError(error)) {
        console.error("Axios error:", error.response?.data);
        Alert.alert("Operation Failed", JSON.stringify(error.response?.data) || "Invalid credentials.");
      } else {
        console.error("Unexpected error:", error.message);
        Alert.alert("Error", "Something went wrong.");
      }
}