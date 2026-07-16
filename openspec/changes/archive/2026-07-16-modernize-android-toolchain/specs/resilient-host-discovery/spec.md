## MODIFIED Requirements

<!-- No requirement text changes. This change makes the implementation actually conform to this
     already-existing requirement (the current source tree still persists the discovery response
     verbatim with no validation); the requirement text itself is carried over unchanged. -->

### Requirement: Malformed or empty discovery responses do not overwrite a known-good host
The system SHALL validate the fetched host value before persisting it, and SHALL leave the previously cached host untouched if the discovery request fails or returns a value that is blank or not a well-formed HTTPS URL.

Note: host discovery already used HTTPS (`https://sowbreira-26fe1.firebaseapp.com/f1mane/host`) before this change — there was no HTTP→HTTPS migration to make. The gap this requirement closes is that the pre-existing code persisted whatever the response contained verbatim, including blank/malformed values, with no validation.

#### Scenario: Discovery response is empty
- **WHEN** the discovery request succeeds but returns an empty body
- **THEN** the app does not overwrite the previously cached host value

#### Scenario: Discovery response is not a well-formed HTTPS URL
- **WHEN** the discovery request succeeds but the response body is not parseable as an HTTPS URL
- **THEN** the app does not overwrite the previously cached host value

#### Scenario: Discovery request fails
- **WHEN** the discovery request fails due to a network error or non-success response
- **THEN** the app leaves the previously cached host value untouched, allowing sign-in to proceed using the last known-good host
