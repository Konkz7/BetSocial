import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  Modal,
  Pressable,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { reportContent } from '../API';

/**
 * Reporting: choosing a reason, with a way out at every step.
 *
 * This was two chained Alert.alert calls, and it was broken. React Native's
 * Alert supports at most three buttons on Android, so a list of seven reasons
 * plus a Cancel was silently truncated - the Cancel was never rendered and
 * somebody who tapped "report" and then thought better of it had no way back
 * except killing the app. A modal has no button limit and one visible Cancel.
 *
 * The reason list lives here so every screen offers the same reasons in the same
 * order, and they have to match ReportReason on the server: a mismatch is a 400
 * the user can do nothing about.
 */

export const REPORT_REASONS: { label: string; hint: string; value: string }[] = [
  { label: 'Spam',           hint: 'Repetitive, misleading or advertising',  value: 'SPAM' },
  { label: 'Abuse',          hint: 'Insults or hateful content',            value: 'ABUSE' },
  { label: 'Harassment',     hint: 'Targeting someone repeatedly',          value: 'HARASSMENT' },
  { label: 'Sexual content', hint: 'Explicit or unwanted sexual material',  value: 'SEXUAL_CONTENT' },
  { label: 'Violence',       hint: 'Threats or graphic violence',           value: 'VIOLENCE' },
  { label: 'Self-harm',      hint: 'Someone may be at risk',               value: 'SELF_HARM' },
  { label: 'Something else', hint: 'None of the above',                     value: 'OTHER' },
];

type Request = {
  targetType: 'THREAD' | 'COMMENT' | 'USER';
  targetId: number;
  what: string;
  onReported?: () => void;
};

/**
 * A module-level store rather than context, because one of the callers -
 * ThreadList - is a plain function rather than a component and cannot use a
 * hook. Same shape as the stores in GlobalFlags, with a listener added since
 * this one has to move the UI.
 */
let pending: Request | null = null;
let listener: ((request: Request | null) => void) | null = null;

const setPending = (request: Request | null) => {
  pending = request;
  listener?.(request);
};

/** Opens the sheet. Safe to call from anywhere, including outside a component. */
export const promptReport = (
  targetType: Request['targetType'],
  targetId: number,
  what: string,
  onReported?: () => void,
) => {
  setPending({ targetType, targetId, what, onReported });
};

/**
 * Rendered once, at the root of the app. Mounted in App.tsx alongside the other
 * app-wide providers so that any screen can report without each one carrying the
 * sheet itself.
 */
export const ReportSheet = () => {
  const [request, setRequest] = useState<Request | null>(pending);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    listener = setRequest;
    return () => { listener = null; };
  }, []);

  const close = () => {
    setBusy(false);
    setPending(null);
  };

  const choose = async (reason: string) => {
    if (!request || busy) { return; }
    setBusy(true);
    const ok = await reportContent(request.targetType, request.targetId, reason, null);
    const done = request.onReported;
    close();
    if (ok) { done?.(); }
  };

  return (
    <Modal
      visible={request !== null}
      transparent
      animationType="slide"
      // Android's hardware back button. Without this it closes the whole modal
      // stack rather than this sheet, which is the same trap as the missing
      // Cancel.
      onRequestClose={close}
    >
      <Pressable style={styles.backdrop} onPress={close}>
        {/* Stops a tap inside the sheet from closing it through the backdrop. */}
        <Pressable style={styles.sheet} onPress={() => {}}>
          <Text style={styles.title}>Report this {request?.what}</Text>
          <Text style={styles.subtitle}>
            A moderator will review it. Reporting is not the same as blocking — if
            you also want to stop seeing this person, block them from their
            profile.
          </Text>

          <ScrollView style={styles.reasons} bounces={false}>
            {REPORT_REASONS.map(reason => (
              <TouchableOpacity
                key={reason.value}
                style={styles.reason}
                disabled={busy}
                onPress={() => choose(reason.value)}
              >
                <Text style={styles.reasonLabel}>{reason.label}</Text>
                <Text style={styles.reasonHint}>{reason.hint}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          <TouchableOpacity style={styles.cancel} disabled={busy} onPress={close}>
            <Text style={styles.cancelText}>
              {busy ? 'Sending…' : 'Cancel'}
            </Text>
          </TouchableOpacity>
        </Pressable>
      </Pressable>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: 'white',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    paddingTop: 20,
    paddingBottom: 24,
    paddingHorizontal: 20,
    maxHeight: '85%',
  },
  title: { fontSize: 20, fontWeight: '700', color: '#111827' },
  subtitle: { fontSize: 13, color: '#6b7280', marginTop: 6, lineHeight: 18 },

  reasons: { marginTop: 14 },
  reason: {
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  reasonLabel: { fontSize: 16, color: '#111827', fontWeight: '500' },
  reasonHint: { fontSize: 12, color: '#9CA3AF', marginTop: 2 },

  cancel: {
    marginTop: 16,
    paddingVertical: 14,
    borderRadius: 10,
    backgroundColor: '#f3f4f6',
    alignItems: 'center',
  },
  cancelText: { fontSize: 16, fontWeight: '600', color: '#374151' },
});
