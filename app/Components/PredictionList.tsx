import React from "react";
import { View, Text, FlatList, StyleSheet, ActivityIndicator } from "react-native";
import { Frown, TrendingUp, TrendingDown, Clock, RotateCcw } from "lucide-react-native";
import { timeAgo } from "../Constants";

/**
 * Your wagers, and how they went.
 *
 * The Predictions tab set a state nobody read - the threads list rendered
 * underneath it either way, so the tab did nothing at all.
 *
 * amount_won is already the net result, so nothing here does arithmetic on it:
 *
 *   null      still running, or closed and waiting on its owner to declare
 *   negative  lost, and that is what it cost
 *   zero      refunded - nobody backed the outcome, so every stake went back
 *   positive  won, and that is the profit on top of the stake returned
 */

const outcomeOf = (amountWon: number | null) => {
  if (amountWon === null || amountWon === undefined) {
    return { label: "Waiting", colour: "#6B7280", Icon: Clock };
  }
  if (amountWon > 0) {
    return { label: "Won", colour: "#03A65A", Icon: TrendingUp };
  }
  if (amountWon < 0) {
    return { label: "Lost", colour: "#9E3B34", Icon: TrendingDown };
  }
  // Zero is not a loss. Nobody backed the outcome, so every stake was returned -
  // showing it as "Lost 0" would be wrong twice over.
  return { label: "Refunded", colour: "#6B7280", Icon: RotateCcw };
};

/** +40 / -100 / — , so the sign carries the meaning rather than the colour alone. */
const netOf = (amountWon: number | null) => {
  if (amountWon === null || amountWon === undefined) {
    return "—";
  }
  if (amountWon > 0) {
    return `+${amountWon}`;
  }
  return String(amountWon);
};

const PredictionList = ({ predictions, loading, onRefresh }: any) => {
  if (loading) {
    return <ActivityIndicator size="large" color="green" style={{ marginTop: 30 }} />;
  }

  return (
    <FlatList
      data={predictions}
      scrollEnabled={false}
      removeClippedSubviews={false}
      keyExtractor={(item: any) => String(item.pid)}
      onRefresh={onRefresh}
      refreshing={false}
      ListEmptyComponent={
        <View style={styles.empty}>
          <Frown size={50} color="gray" />
          <Text style={styles.emptyText}>You have not staked on anything yet</Text>
        </View>
      }
      renderItem={({ item }: any) => {
        const outcome = outcomeOf(item.amount_won);
        const Icon = outcome.Icon;

        return (
          <View style={styles.row}>
            <View style={styles.rowTop}>
              <Text style={styles.title} numberOfLines={1}>{item.thread_title}</Text>
              <Text style={[styles.net, { color: outcome.colour }]}>{netOf(item.amount_won)}</Text>
            </View>

            <Text style={styles.description} numberOfLines={2}>{item.bet_description}</Text>

            <View style={styles.rowBottom}>
              <View style={styles.badge}>
                {/* Which side was taken. The bet's own wording is above, so this
                    only has to say which way. */}
                <Text style={[styles.side, { color: item.prediction ? "#03A65A" : "#9E3B34" }]}>
                  {item.prediction ? "For" : "Against"}
                </Text>
                <Text style={styles.stake}>· staked {item.amount_bet}</Text>
              </View>

              <View style={styles.badge}>
                <Icon size={14} color={outcome.colour} />
                <Text style={[styles.outcome, { color: outcome.colour }]}>{outcome.label}</Text>
                <Text style={styles.stake}>· {timeAgo(item.created_at)}</Text>
              </View>
            </View>
          </View>
        );
      }}
    />
  );
};

const styles = StyleSheet.create({
  row: {
    backgroundColor: "white",
    borderRadius: 12,
    padding: 14,
    marginHorizontal: 12,
    marginVertical: 6,
    shadowColor: "#000",
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  rowTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  title: { fontSize: 15, fontWeight: "600", color: "#111827", flex: 1, marginRight: 10 },
  net: { fontSize: 16, fontWeight: "700" },
  description: { fontSize: 13, color: "#4B5563", marginTop: 4 },
  rowBottom: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 10,
  },
  badge: { flexDirection: "row", alignItems: "center" },
  side: { fontSize: 13, fontWeight: "600" },
  outcome: { fontSize: 13, fontWeight: "600", marginLeft: 4 },
  stake: { fontSize: 12, color: "#6B7280", marginLeft: 4 },
  empty: { alignItems: "center", justifyContent: "center", paddingVertical: 40 },
  emptyText: { color: "#6B7280", marginTop: 8 },
});

export default PredictionList;
