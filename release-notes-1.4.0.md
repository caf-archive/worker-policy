!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- [CAF-3113](https://jira.autonomy.com/browse/CAF-3113): Reduced the size of the context passed from Policy Worker to a Composite Document Worker.
  With the Composite Document Handler passing all the fields of the document to the external worker it is unnecessary to duplicate those fields in the context of the sent task message. Now when a message is sent from Policy Worker to the Composite Document Worker the context only contains Policy Worker Task Data not found on the Document sent to the external worker. When the message returns to the Policy Worker the Policy Worker Task Document is reconstructed by reading from both the context and the Document Worker Document returned.

#### Known Issues
