import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { approveBet, getPendingApprovals } from './API';

/**
 * The approval queue.
 *
 * Deliberately plain, and deliberately not the social interface: somebody signing
 * off outcomes is doing one job repeatedly, and everything that helps a person
 * browse gets in the way of that. One screen, one list, two buttons a row.
 *
 * Everything needed to judge a row arrives with it - the claim, the thread it
 * came from, which way the owner called it, and how much is riding on the answer
 * - so a decision does not require opening anything.
 */
const AdminScreen = ({ navigation }: any) => {
  const [pending, setPending] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    const data = await getPendingApprovals();
    setPending(data ?? []);
    setLoading(false);
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  const decide = (bet: any, approve: boolean) => {
    Alert.alert(
      approve ? 'Approve this outcome?' : 'Reject this outcome?',
      approve
        ? `${bet.prediction_count} people staked ${bet.total_staked} coins. Approving pays them out and cannot be undone.`
        : 'Rejecting returns every stake. Nobody wins and nobody loses.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: approve ? 'Approve' : 'Reject',
          style: approve ? 'default' : 'destructive',
          onPress: async () => {
            setBusy(bet.bid);
            const ok = await approveBet(bet.bid, approve,
              approve ? 'Outcome confirmed' : 'Outcome not accepted');
            setBusy(null);

            // Drop the row rather than reloading the list: it is settled either
            // way, and the queue should not jump about while it is being worked
            // through.
            if (ok) { setPending(rows => rows.filter(row => row.bid !== bet.bid)); }
          },
        },
      ],
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Awaiting approval</Text>
        <TouchableOpacity onPress={() => navigation.navigate('Login')}>
          <Text style={styles.signOut}>Sign out</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={pending}
        keyExtractor={item => item.bid.toString()}
        removeClippedSubviews={false}
        onRefresh={load}
        refreshing={loading}
        ListEmptyComponent={
          <Text style={styles.empty}>
            {loading ? 'Loading…' : 'Nothing waiting. Everything is settled.'}
          </Text>
        }
        renderItem={({ item }) => (
          <View style={styles.card}>
            <Text style={styles.thread}>{item.thread_title}</Text>
            <Text style={styles.claim}>{item.description}</Text>

            <View style={styles.verdictRow}>
              <Text style={styles.verdictLabel}>Owner says</Text>
              <Text style={[styles.verdict, item.outcome ? styles.yes : styles.no]}>
                {item.outcome ? 'YES' : 'NO'}
              </Text>
            </View>

            {/* What is actually at stake in the decision. The side that matches
                the owner's call is the one that gets paid. */}
            <View style={styles.pools}>
              <Text style={[styles.pool, item.outcome && styles.poolWinning]}>
                For {item.amount_for}
              </Text>
              <Text style={[styles.pool, !item.outcome && styles.poolWinning]}>
                Against {item.amount_against}
              </Text>
              <Text style={styles.staked}>
                {item.prediction_count} staked {item.total_staked}
              </Text>
            </View>

            <View style={styles.actions}>
              <TouchableOpacity
                style={[styles.button, styles.reject]}
                disabled={busy === item.bid}
                onPress={() => decide(item, false)}
              >
                <Text style={styles.rejectText}>Reject</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.button, styles.approve]}
                disabled={busy === item.bid}
                onPress={() => decide(item, true)}
              >
                <Text style={styles.approveText}>
                  {busy === item.bid ? 'Working…' : 'Approve'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 18,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerTitle: { fontSize: 20, fontWeight: '700', color: '#111827' },
  signOut: { fontSize: 15, color: '#9E3B34', fontWeight: '500' },

  card: {
    backgroundColor: 'white',
    marginHorizontal: 12,
    marginTop: 12,
    padding: 16,
    borderRadius: 10,
  },
  thread: { fontSize: 12, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 },
  claim: { fontSize: 17, fontWeight: '600', color: '#111827', marginTop: 4 },

  verdictRow: { flexDirection: 'row', alignItems: 'center', marginTop: 12 },
  verdictLabel: { fontSize: 14, color: '#6b7280', marginRight: 8 },
  verdict: { fontSize: 16, fontWeight: '700' },
  yes: { color: '#10B981' },
  no: { color: '#9E3B34' },

  pools: { flexDirection: 'row', alignItems: 'center', marginTop: 10, flexWrap: 'wrap' },
  pool: { fontSize: 14, color: '#6b7280', marginRight: 14 },
  poolWinning: { color: '#111827', fontWeight: '600' },
  staked: { fontSize: 13, color: '#9CA3AF' },

  actions: { flexDirection: 'row', marginTop: 16 },
  button: { flex: 1, paddingVertical: 12, borderRadius: 8, alignItems: 'center' },
  reject: { backgroundColor: '#FEE2E2', marginRight: 8 },
  approve: { backgroundColor: '#10B981' },
  rejectText: { color: '#9E3B34', fontWeight: '600' },
  approveText: { color: 'white', fontWeight: '600' },

  empty: { textAlign: 'center', marginTop: 60, color: '#6b7280' },
});

export default AdminScreen;
