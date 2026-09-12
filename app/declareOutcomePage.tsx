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
import { ArrowLeft } from 'lucide-react-native';
import { declareOutcome, getBetsAwaitingMyDecision } from './API';

/**
 * Your closed bets, waiting on you to say what happened.
 *
 * This is the step that had no screen at all. A bet closes, the sweep moves it
 * to PENDING, and from that moment it is invisible: off the staking screen
 * because it is not active, and out of the approval queue because that filters
 * on outcome IS NOT NULL. Its owner was the only person who could move it on and
 * nothing ever told them so, so bets closed and were never mentioned again.
 *
 * Declaring an outcome pays nobody. It hands the bet to an approver with no
 * stake in it, who signs the answer off - which is why the wording here is
 * "what happened", not "who wins".
 */
const DeclareOutcomeScreen = ({ navigation }: any) => {
  const [awaiting, setAwaiting] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    setAwaiting(await getBetsAwaitingMyDecision());
    setLoading(false);
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  const declare = (bet: any, happened: boolean) => {
    Alert.alert(
      happened ? 'It happened?' : 'It did not happen?',
      `${bet.description}\n\n` +
        `${bet.prediction_count} people staked ${bet.total_staked} coins on this. ` +
        'Your answer goes to an approver before anybody is paid, and it cannot be ' +
        'changed once sent.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: happened ? 'Yes, it happened' : 'No, it did not',
          onPress: async () => {
            setBusy(bet.bid);
            const ok = await declareOutcome(bet.bid, happened,
              happened ? 'Owner says it happened' : 'Owner says it did not happen');
            setBusy(null);

            // Drop the row rather than reloading: it has left this list for the
            // approver's either way, and the list should not jump about while
            // somebody is working through it.
            if (ok) { setAwaiting(rows => rows.filter(row => row.bid !== bet.bid)); }
          },
        },
      ],
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity style={styles.back} onPress={() => navigation.goBack()}>
          <ArrowLeft size={28} color={'green'} />
          <Text style={styles.backText}>Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Waiting on you</Text>
      </View>

      <FlatList
        data={awaiting}
        keyExtractor={item => item.bid.toString()}
        removeClippedSubviews={false}
        onRefresh={load}
        refreshing={loading}
        ListEmptyComponent={
          <Text style={styles.empty}>
            {loading ? 'Loading…' : 'Nothing is waiting on you.'}
          </Text>
        }
        ListHeaderComponent={
          awaiting.length > 0 ? (
            <Text style={styles.intro}>
              These bets have closed. Say what happened and an approver will settle
              them.
            </Text>
          ) : null
        }
        renderItem={({ item }) => (
          <View style={styles.card}>
            <Text style={styles.thread}>{item.thread_title}</Text>
            <Text style={styles.claim}>{item.description}</Text>

            <View style={styles.pools}>
              <Text style={styles.pool}>For {item.amount_for}</Text>
              <Text style={styles.pool}>Against {item.amount_against}</Text>
              <Text style={styles.staked}>
                {item.prediction_count} staked {item.total_staked}
              </Text>
            </View>

            <View style={styles.actions}>
              <TouchableOpacity
                style={[styles.button, styles.no]}
                disabled={busy === item.bid}
                onPress={() => declare(item, false)}
              >
                <Text style={styles.noText}>It did not happen</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.button, styles.yes]}
                disabled={busy === item.bid}
                onPress={() => declare(item, true)}
              >
                <Text style={styles.yesText}>
                  {busy === item.bid ? 'Working…' : 'It happened'}
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
    paddingVertical: 18,
    paddingHorizontal: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  back: { flexDirection: 'row', alignItems: 'center' },
  backText: { fontSize: 16, color: 'green', marginLeft: 4 },
  headerTitle: { fontSize: 20, fontWeight: '700', color: '#111827', marginLeft: 16 },

  intro: {
    fontSize: 13,
    color: '#6b7280',
    marginHorizontal: 12,
    marginTop: 14,
  },

  card: {
    backgroundColor: 'white',
    marginHorizontal: 12,
    marginTop: 12,
    padding: 16,
    borderRadius: 10,
  },
  thread: { fontSize: 12, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 },
  claim: { fontSize: 17, fontWeight: '600', color: '#111827', marginTop: 4 },

  pools: { flexDirection: 'row', alignItems: 'center', marginTop: 10, flexWrap: 'wrap' },
  pool: { fontSize: 14, color: '#6b7280', marginRight: 14 },
  staked: { fontSize: 13, color: '#9CA3AF' },

  actions: { flexDirection: 'row', marginTop: 16 },
  button: { flex: 1, paddingVertical: 12, borderRadius: 8, alignItems: 'center' },
  no: { backgroundColor: '#FEE2E2', marginRight: 8 },
  yes: { backgroundColor: '#10B981' },
  noText: { color: '#9E3B34', fontWeight: '600' },
  yesText: { color: 'white', fontWeight: '600' },

  empty: { textAlign: 'center', marginTop: 60, color: '#6b7280' },
});

export default DeclareOutcomeScreen;
