# google-sign-in

## Purpose

Google account sign-in for Fl-Mane's Android app, using Android's Credential Manager API to
authenticate the user with Firebase and mint a backend session token via `criarSessaoGoogle`.
## Requirements
### Requirement: Google sign-in is initiated via Credential Manager
The system SHALL let a signed-out user authenticate with a Google account through Android's Credential Manager API ("Sign in with Google"), replacing the legacy `GoogleSignInClient` intent-based flow.

#### Scenario: Signed-out user starts Google sign-in
- **WHEN** a signed-out user taps the Google sign-in button on the login screen
- **THEN** the system invokes Credential Manager's Google sign-in flow and presents the user's available Google account(s)

#### Scenario: Previously authenticated user skips the sign-in prompt
- **WHEN** the app launches and Firebase Auth already has a valid session for the user
- **THEN** the system skips the sign-in button and shows the signed-in profile (name and photo) directly, without invoking Credential Manager

### Requirement: Successful Google sign-in creates a backend session
The system SHALL, after obtaining a Google ID token via Credential Manager and completing Firebase authentication with it, exchange the authenticated user's profile (Google UID, display name, email, photo URL) for a backend session token by calling `criarSessaoGoogle`, then update the UI to the signed-in profile state.

#### Scenario: Credential Manager sign-in succeeds
- **WHEN** the user selects a Google account and Credential Manager returns a valid ID token
- **THEN** the system signs in to Firebase with that token, requests a backend session via `criarSessaoGoogle` with the user's UID/name/email/photo, hides the sign-in button, and displays the user's name and photo

### Requirement: Failed Google sign-in leaves the user signed out
The system SHALL show an error message and keep the sign-in button visible if Credential Manager sign-in, Firebase authentication, or the backend session request fails.

#### Scenario: User cancels the Google account picker
- **WHEN** the user dismisses the Credential Manager account picker without selecting an account
- **THEN** the system shows an error/cancellation message and the sign-in button remains visible

#### Scenario: Firebase authentication fails after a valid credential is obtained
- **WHEN** Credential Manager returns a valid ID token but Firebase rejects the resulting credential
- **THEN** the system shows an error message and the sign-in button remains visible

### Requirement: Sign-out clears Credential Manager credential state
The system SHALL clear Credential Manager's cached credential state, in addition to signing out of Firebase Auth, when the user signs out, so that the next sign-in attempt does not automatically re-select the previously used Google account.

#### Scenario: User signs out from the profile screen
- **WHEN** the user taps "Sair" (sign out) on the profile screen
- **THEN** the system signs out of Firebase Auth, clears Credential Manager's credential state, and returns to the login screen with the sign-in button visible

