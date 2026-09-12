import { Alert } from 'react-native';
import { reportContent } from '../API';

/**
 * The reason list, in one place.
 *
 * Every screen that can report something offers the same reasons in the same
 * order, and they have to match ReportReason on the server - a mismatch is a 400
 * the user cannot do anything about. Keeping them here means adding a reason is
 * one edit rather than three.
 */
export const REPORT_REASONS: { label: string; value: string }[] = [
  { label: 'Spam',                 value: 'SPAM' },
  { label: 'Abuse',                value: 'ABUSE' },
  { label: 'Harassment',           value: 'HARASSMENT' },
  { label: 'Sexual content',       value: 'SEXUAL_CONTENT' },
  { label: 'Violence',             value: 'VIOLENCE' },
  { label: 'Self-harm',            value: 'SELF_HARM' },
  { label: 'Something else',       value: 'OTHER' },
];

/**
 * Asks why, then reports.
 *
 * Two taps rather than a screen: reporting competes with scrolling past, and a
 * form is enough friction that people choose the scroll. Alert is used instead
 * of a custom sheet because it is what every other confirmation in the app uses
 * and it is native on both platforms.
 */
export const promptReport = (
  targetType: 'THREAD' | 'COMMENT' | 'USER',
  targetId: number,
  what: string,
  onReported?: () => void,
) => {
  Alert.alert(
    `Report this ${what}?`,
    'A moderator will review it. Reporting is not the same as blocking - if you '
      + 'also want to stop seeing this person, block them from their profile.',
    [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Choose a reason',
        onPress: () =>
          Alert.alert(
            'Why are you reporting this?',
            undefined,
            [
              ...REPORT_REASONS.map(reason => ({
                text: reason.label,
                onPress: async () => {
                  const ok = await reportContent(targetType, targetId, reason.value, null);
                  if (ok) { onReported?.(); }
                },
              })),
              { text: 'Cancel', style: 'cancel' as const },
            ],
          ),
      },
    ],
  );
};
