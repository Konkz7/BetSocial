export const IP_STRING = "http://192.168.1.60:8080"; 

export const errorHandler = (error) => {
    
    if (axios.isAxiosError(error)) {
        console.error("Axios error:", error.response?.data);
        Alert.alert("Operation Failed", JSON.stringify(error.response?.data) || "Invalid credentials.");
      } else {
        console.error("Unexpected error:", error.message);
        Alert.alert("Error", "Something went wrong.");
      }
}

export const timeAgo = (timestamp) => {
  const now = Date.now();
  const diffMs = now - timestamp; // Difference in milliseconds

  const seconds = Math.floor(diffMs / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const weeks = Math.floor(days / 7);
  const months = Math.floor(days / 30);
  const years = Math.floor(days / 365);

  if (seconds < 60) return `${seconds}s`;
  if (minutes < 60) return `${minutes}m`;
  if (hours < 24) return `${hours}h`;
  if (days < 7) return `${days}d`;
  if (weeks < 4) return `${weeks}w`;
  if (months < 12) return `${months}M`;
  return `${years}y`;
};

export const pgpPublicKey = `-----BEGIN PGP PUBLIC KEY BLOCK-----
Version: Keybase OpenPGP v1.0.0
Comment: https://keybase.io/crypto

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
GSE+TFsaxPokFQeFyXY5KxxT
=zXMG
-----END PGP PUBLIC KEY BLOCK-----
`;