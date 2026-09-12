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
import { approveBet, getPendingApprovals, getOpenReports, decideReport } from './API';

/**
 * The two queues a privileged account works through: bet outcomes waiting to be
 * signed off, and reported content waiting to be judged.
 *
 * Deliberately plain, and deliberately not the social interface: somebody
 * working a queue is doing one job repeatedly, and everything that helps a
 * person browse gets in the way of that. Two tabs, one list each, two buttons a
 * row.
 *
 * Everything needed to judge a row arrives with it - for a bet, the claim, its
 * thread, which way the owner called it and how much is riding on the answer;
 * for a report, the reported text itself, who wrote it, and how many people
 * reported the same thing. Neither decision requires opening anything.
 */
const AdminScreen = ({ navigation }: any) => {
  const [pending, setPending] = useState<any[]>([]);
  const [reports, setReports] = useState<any[]>([]);
  // Two queues, one screen. A tab rather than a second screen because it is the
  // same job either way and the person doing it should not have to navigate
  // between them.
  const [tab, setTab] = useState<'bets' | 'reports'>('bets');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    const [betData, reportData] = await Promise.all([
      getPendingApprovals(),
      getOpenReports(),
    ]);
    setPending(betData ?? []);
    setReports(reportData ?? []);
    setLoading(false);
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  /**
   * Acts on a report. REMOVED takes the content down, SUSPENDED stops an
   * account, DISMISSED says there was nothing wrong with it.
   *
   * The confirmation spells out what happens because none of the three is
   * obvious from a button, and removing a thread also refunds everybody who had
   * staked on its bets - which is a surprise if nobody says so.
   */
  const decideOnReport = (report: any, action: string) => {
    const isUser = report.target_type === 'USER';
    const wording: Record<string, string> = {
      REMOVED: 'The content is taken down for everybody. Anyone who staked on its '
        + 'bets is refunded. This can be undone in the database, not from here.',
      SUSPENDED: `${report.author_name} will not be able to sign in, and their posts `
        + 'are taken down. Stakes on their bets are refunded.',
      DISMISSED: 'The content stays where it is and the report is closed.',
    };

    Alert.alert(
      action === 'DISMISSED' ? 'Dismiss this report?'
        : isUser ? 'Suspend this account?' : 'Remove this content?',
      wording[action],
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: action === 'DISMISSED' ? 'Dismiss' : 'Confirm',
          style: action === 'DISMISSED' ? 'default' : 'destructive',
          onPress: async () => {
            setBusy(report.rid);
            const ok = await decideReport(report.rid, action);
            setBusy(null);
            // Every report about the same thing is closed server-side, so drop
            // them all rather than just this row.
            if (ok) {
              setReports(rows => rows.filter(row =>
                !(row.target_type === report.target_type
                  && row.target_id === report.target_id)));
            }
          },
        },
      ],
    );
  };

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
        <Text style={styles.headerTitle}>Moderation</Text>
        <TouchableOpacity onPress={() => navigation.navigate('Login')}>
          <Text style={styles.signOut}>Sign out</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.tabs}>
        <TouchableOpacity
          style={[styles.tab, tab === 'bets' && styles.tabActive]}
          onPress={() => setTab('bets')}
        >
          <Text style={[styles.tabText, tab === 'bets' && styles.tabTextActive]}>
            Bets ({pending.length})
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, tab === 'reports' && styles.tabActive]}
          onPress={() => setTab('reports')}
        >
          <Text style={[styles.tabText, tab === 'reports' && styles.tabTextActive]}>
            Reports ({reports.length})
          </Text>
        </TouchableOpacity>
      </View>

      {tab === 'reports' ? (
        <FlatList
          data={reports}
          keyExtractor={item => item.rid.toString()}
          removeClippedSubviews={false}
          onRefresh={load}
          refreshing={loading}
          ListEmptyComponent={
            <Text style={styles.empty}>
              {loading ? 'Loading…' : 'Nothing reported.'}
            </Text>
          }
          renderItem={({ item }) => (
            <View style={styles.card}>
              <Text style={styles.thread}>
                {item.target_type} · {item.reason}
                {item.report_count > 1 ? ` · ${item.report_count} reports` : ''}
              </Text>
              <Text style={styles.claim}>{item.content}</Text>
              <Text style={styles.reportMeta}>
                by {item.author_name}
                {item.already_removed ? ' · already removed' : ''}
              </Text>
              {item.detail ? (
                <Text style={styles.reportDetail}>“{item.detail}”</Text>
              ) : null}

              <View style={styles.actions}>
                <TouchableOpacity
                  style={[styles.button, styles.reject]}
                  disabled={busy === item.rid}
                  onPress={() => decideOnReport(item, 'DISMISSED')}
                >
                  <Text style={styles.rejectText}>Dismiss</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.button, styles.takedown]}
                  disabled={busy === item.rid}
                  onPress={() => decideOnReport(item,
                    item.target_type === 'USER' ? 'SUSPENDED' : 'REMOVED')}
                >
                  <Text style={styles.approveText}>
                    {busy === item.rid ? 'Working…'
                      : item.target_type === 'USER' ? 'Suspend' : 'Remove'}
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        />
      ) : (
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
      )}
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

  tabs: { flexDirection: 'row', backgroundColor: 'white', borderBottomWidth: 1, borderBottomColor: '#e5e7eb' },
  tab: { flex: 1, paddingVertical: 12, alignItems: 'center', borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabActive: { borderBottomColor: '#10B981' },
  tabText: { fontSize: 15, color: '#6b7280', fontWeight: '500' },
  tabTextActive: { color: '#111827', fontWeight: '700' },

  reportMeta: { fontSize: 13, color: '#6b7280', marginTop: 8 },
  reportDetail: { fontSize: 14, color: '#374151', marginTop: 8, fontStyle: 'italic' },
  takedown: { backgroundColor: '#9E3B34' },

  empty: { textAlign: 'center', marginTop: 60, color: '#6b7280' },
});

export default AdminScreen;
