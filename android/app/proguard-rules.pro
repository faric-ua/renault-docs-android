# Renault Docs v0.1.0 does not require custom keep rules yet.

# 0.6.5: Credential Manager finds its Play services provider by reflection (rule from the androidx.credentials docs).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
