import React, { useState } from "react";
import { View, TouchableOpacity, Text, StyleSheet, Platform } from "react-native";
import DateTimePickerModal from "react-native-modal-datetime-picker";

interface DatePickerButtonProps {
  onDateSelect?: (timestamp: number) => void; // Callback function to send selected date
}

const DatePickerButton: React.FC<DatePickerButtonProps> = ({ onDateSelect }) => {
  const [isDatePickerVisible, setDatePickerVisible] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);

  const showDatePicker = () => {
    setDatePickerVisible(true);
  };

  const hideDatePicker = () => {
    setDatePickerVisible(false);
  };

  const handleConfirm = (date: Date) => {

    const now = new Date();
    if(date.getTime() <= now.getTime()){
      hideDatePicker();
      return;
    }

    setSelectedDate(date); // Save the selected date
    hideDatePicker(); // Close the picker

    if (onDateSelect) {
      onDateSelect(date.getTime()); // Convert to timestamp and pass it to parent component
    }
  };

  return (
    <View>
      {/* Button to open date picker */}
      <TouchableOpacity style={styles.button} onPress={showDatePicker}>
        <Text style={styles.buttonText} >
          {selectedDate ? selectedDate.toLocaleString(): "Pick a Date"}
        </Text>
      </TouchableOpacity>

      {/* Date Picker Modal */}
      <DateTimePickerModal
        isVisible={isDatePickerVisible}
        mode="datetime"
        display={Platform.OS === "ios" ? "spinner" : "default"}
        onConfirm={handleConfirm}
        onCancel={hideDatePicker}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  button: {
    backgroundColor: "green",
    padding: 10,
    borderRadius: 5,
    alignItems: "center",
    maxWidth: 160,
  },
  buttonText: {
    color: "white",
    fontSize: 16,
  },
});

export default DatePickerButton;
