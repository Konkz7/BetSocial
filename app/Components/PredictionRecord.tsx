import React from "react";
import { View, Text, StyleSheet, ActivityIndicator } from "react-native";
import { Target, Frown } from "lucide-react-native";

/**
 * Somebody else's track record.
 *
 * Counts, and nothing else. The full history - what was staked, on which thread,
 * for how much - is on your own profile only. A social prediction app is partly
 * about who is worth listening to, so a record earns its place here; how much
 * somebody stakes or has lost does not.
 *
 * It is also the only shape that cannot leak: the history carries thread titles,
 * and a prediction on a private thread would name one the viewer is not allowed
 * to know exists.
 */

/**
 * Correct out of settled, not out of total.
 *
 * Two kinds of prediction are neither right nor wrong and both are excluded by
 * the server: ones still running, and ones refunded because nobody backed the
 * outcome. Counting either as a loss would mark somebody down for a bet that
 * never resolved.
 */
const rateOf = (record: any) => {
  if (!record?.settled) {
    return null;
  }
  return Math.round((record.correct / record.settled) * 100);
};

const PredictionRecord = ({ record, loading, name }: any) => {
  if (loading) {
    return <ActivityIndicator size="large" color="green" style={{ marginTop: 30 }} />;
  }

  if (!record || !record.total) {
    return (
      <View style={styles.empty}>
        <Frown size={50} color="gray" />
        <Text style={styles.emptyText}>{name} has not staked on anything yet</Text>
      </View>
    );
  }

  const rate = rateOf(record);

  return (
    <View style={styles.card}>
      <View style={styles.heading}>
        <Target size={18} color="#03A65A" />
        <Text style={styles.headingText}>Track record</Text>
      </View>

      <View style={styles.figures}>
        <View style={styles.figure}>
          <Text style={styles.number}>{record.total}</Text>
          <Text style={styles.label}>predictions</Text>
        </View>

        <View style={styles.figure}>
          <Text style={styles.number}>{record.correct}</Text>
          <Text style={styles.label}>correct</Text>
        </View>

        <View style={styles.figure}>
          {/* Nothing settled yet means there is no rate to show. A "0%" would be
              read as "always wrong" rather than as "nothing has resolved". */}
          <Text style={styles.number}>{rate === null ? "—" : `${rate}%`}</Text>
          <Text style={styles.label}>success</Text>
        </View>
      </View>

      {record.total > record.settled && (
        <Text style={styles.pending}>
          {record.total - record.settled} still waiting on a result
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: "white",
    borderRadius: 12,
    padding: 16,
    marginHorizontal: 12,
    marginVertical: 10,
    shadowColor: "#000",
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  heading: { flexDirection: "row", alignItems: "center", marginBottom: 14 },
  headingText: { fontSize: 15, fontWeight: "600", color: "#111827", marginLeft: 6 },
  figures: { flexDirection: "row", justifyContent: "space-around" },
  figure: { alignItems: "center" },
  number: { fontSize: 22, fontWeight: "700", color: "#03A65A" },
  label: { fontSize: 12, color: "#6B7280", marginTop: 2 },
  pending: { fontSize: 12, color: "#6B7280", textAlign: "center", marginTop: 14 },
  empty: { alignItems: "center", justifyContent: "center", paddingVertical: 40 },
  emptyText: { color: "#6B7280", marginTop: 8 },
});

export default PredictionRecord;
