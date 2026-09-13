import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { ArrowLeft, Frown, Bookmark, Clock, CheckCircle2 } from 'lucide-react-native';
import { getSavedBets } from './API';
import { screenStore } from './GlobalFlags';

/**
 * The bets you bookmarked.
 *
 * The bookmark itself has worked for as long as the thread screen has had the
 * icon - there was a toggle and a per-bet lookup. What was missing was any way
 * to ask for the list, so the Settings row that should have shown them did
 * nothing at all.
 */

const stateOf = (bet: any) => {
  // outcome is null until somebody declares it, whatever the status says.
  if (bet.outcome !== null && bet.outcome !== undefined) {
    return { label: bet.outcome ? 'Settled · For won' : 'Settled · Against won',
             colour: '#03A65A', Icon: CheckCircle2 };
  }
  if (bet.ends_at && bet.ends_at < Date.now()) {
    return { label: 'Closed · awaiting a result', colour: '#6B7280', Icon: Clock };
  }
  return { label: 'Open', colour: '#03A65A', Icon: Clock };
};

const SavedBetsScreen = ({ navigation }: any) => {
  const [saved, setSaved] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    setSaved(await getSavedBets());
    setLoading(false);
  };

  useFocusEffect(
    useCallback(() => {
      screenStore.set('SavedBets');
      load();
    }, []),
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <ArrowLeft size={24} color="#10B981" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Saved Bets</Text>
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="green" style={{ marginTop: 30 }} />
      ) : (
        <FlatList
          data={saved}
          removeClippedSubviews={false}
          keyExtractor={(item: any) => String(item.bsid)}
          onRefresh={load}
          refreshing={false}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Frown size={50} color="gray" />
              <Text style={styles.emptyText}>
                Nothing saved yet. The bookmark on a bet keeps it here.
              </Text>
            </View>
          }
          renderItem={({ item }: any) => {
            const state = stateOf(item);
            const Icon = state.Icon;

            return (
              <View style={styles.row}>
                <View style={styles.rowTop}>
                  <Bookmark size={16} color="#10B981" />
                  <Text style={styles.title} numberOfLines={1}>{item.thread_title}</Text>
                </View>

                <Text style={styles.description} numberOfLines={2}>
                  {item.bet_description}
                </Text>

                <View style={styles.rowBottom}>
                  <Icon size={14} color={state.colour} />
                  <Text style={[styles.state, { color: state.colour }]}>{state.label}</Text>
                </View>
              </View>
            );
          }}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fcfcf7' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    backgroundColor: 'white',
  },
  headerTitle: { fontSize: 18, fontWeight: '600', color: '#111827', marginLeft: 12 },
  row: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 14,
    marginHorizontal: 12,
    marginVertical: 6,
    shadowColor: '#000',
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  rowTop: { flexDirection: 'row', alignItems: 'center' },
  title: { fontSize: 15, fontWeight: '600', color: '#111827', marginLeft: 6, flex: 1 },
  description: { fontSize: 13, color: '#4B5563', marginTop: 6 },
  rowBottom: { flexDirection: 'row', alignItems: 'center', marginTop: 10 },
  state: { fontSize: 12, fontWeight: '600', marginLeft: 5 },
  empty: { alignItems: 'center', justifyContent: 'center', paddingVertical: 60 },
  emptyText: { color: '#6B7280', marginTop: 8, textAlign: 'center', paddingHorizontal: 40 },
});

export default SavedBetsScreen;
