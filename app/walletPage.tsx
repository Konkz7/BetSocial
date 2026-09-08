import React, { useCallback, useState } from "react";
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { ArrowLeft } from "lucide-react-native";
import { useFocusEffect } from "@react-navigation/native";
import { getWallet } from "./API";

/**
 * The wallet: a balance, and the movements that add up to it.
 *
 * Both come from the ledger, so the number at the top can always be checked
 * against the rows beneath it. What this replaces showed a hardcoded "30.00"
 * above a button that created a crypto wallet through an endpoint deleted with
 * CircleService - every call on the screen had been 404ing for a long time.
 *
 * There is nothing to buy here and no card to add. Coins arrive as an opening
 * grant and a daily top-up, and are won or lost on bets.
 */

/** Matches LedgerReason on the server. */
const REASON_LABELS: Record<string, string> = {
  OPENING_GRANT: "Opening grant",
  DAILY_TOPUP: "Daily top-up",
  STAKE: "Stake",
  STAKE_REFUND: "Refund",
  WINNINGS: "Winnings",
  ADJUSTMENT: "Adjustment",
};

const formatWhen = (millis: number) =>
  new Date(millis).toLocaleDateString([], {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });

const WalletScreen = ({ navigation }: any) => {
  const [wallet, setWallet] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    // Opening the wallet is also what collects the daily top-up, so this is
    // refetched on focus rather than cached.
    setWallet(await getWallet());
    setLoading(false);
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <ArrowLeft size={28} color={"green"} />
          <Text style={styles.headerText}>Back</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.balanceCard}>
        <Text style={styles.balanceLabel}>Balance</Text>
        <Text style={styles.balance}>
          {wallet ? wallet.balance.toLocaleString() : "—"}
        </Text>
        <Text style={styles.balanceUnit}>coins</Text>
      </View>

      <Text style={styles.historyHeading}>Recent activity</Text>

      <FlatList
        data={wallet?.entries ?? []}
        keyExtractor={(item) => item.leid.toString()}
        removeClippedSubviews={false}
        onRefresh={load}
        refreshing={loading}
        ListEmptyComponent={
          <Text style={styles.empty}>
            {loading ? "Loading…" : "Nothing has moved yet."}
          </Text>
        }
        renderItem={({ item }) => (
          <View style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.description} numberOfLines={1}>
                {item.description}
              </Text>
              <Text style={styles.meta}>
                {REASON_LABELS[item.reason] ?? item.reason} · {formatWhen(item.created_at)}
              </Text>
            </View>
            {/* Signed, so a debit reads as one without needing a separate column. */}
            <Text style={[styles.amount, item.amount < 0 ? styles.debit : styles.credit]}>
              {item.amount > 0 ? "+" : ""}{item.amount.toLocaleString()}
            </Text>
          </View>
        )}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fcfcf7" },
  header: { paddingHorizontal: 12, paddingVertical: 10 },
  backButton: { flexDirection: "row", alignItems: "center" },
  headerText: { fontSize: 18, color: "green", marginLeft: 4 },

  balanceCard: {
    backgroundColor: "white",
    marginHorizontal: 16,
    marginTop: 8,
    paddingVertical: 28,
    borderRadius: 12,
    alignItems: "center",
  },
  balanceLabel: {
    fontSize: 13,
    color: "#6b7280",
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  balance: { fontSize: 52, fontWeight: "700", color: "#111827", marginTop: 4 },
  balanceUnit: { fontSize: 14, color: "#6b7280" },

  historyHeading: {
    marginHorizontal: 16,
    marginTop: 24,
    marginBottom: 8,
    fontSize: 15,
    fontWeight: "600",
    color: "#374151",
  },

  row: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "white",
    marginHorizontal: 16,
    marginBottom: 1,
    paddingVertical: 12,
    paddingHorizontal: 14,
  },
  rowText: { flex: 1, marginRight: 12 },
  description: { fontSize: 15, color: "#111827" },
  meta: { fontSize: 12, color: "#9CA3AF", marginTop: 2 },
  amount: { fontSize: 16, fontWeight: "600" },
  credit: { color: "#2F6B4F" },
  debit: { color: "#9E3B34" },

  empty: { textAlign: "center", marginTop: 40, color: "#6b7280" },
});

export default WalletScreen;
