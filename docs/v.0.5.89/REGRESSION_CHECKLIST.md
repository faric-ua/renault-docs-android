# v0.5.89 notification history acceptance

- [ ] Kotlin test: first 10 IDs are distinct; 11th replaces only the oldest.
- [ ] Kotlin test: summary ID and foreground IDs cannot collide.
- [ ] Python source audit: fixed foreground progress IDs remain, terminal publishes use separate history.
- [ ] Main app compiles with notification channel and per-result PendingIntent.
- [ ] Two distinct finished native tomes remain visible after second completion.
- [ ] Android groups entries (expandable); individual notification dismissal works.
- [ ] Tapping older result opens original owning project even after another project is processed.
- [ ] A run's extraction/packaging progress updates only the same foreground notification.
- [ ] A multi-volume batch yields one notification per completed tome.
- [ ] Import from existing RDPKG gets its own completed notification.
- [ ] After ten completed results the oldest slot rotates; no unbounded notification spam.
- [ ] WAITING_SELECTION does not create a false completed result.
- [ ] Redelivery/recreation does not duplicate a completed entry.
- [ ] Progress indicator removed at terminal; completed child remains visible.
- [ ] Android disabled permission cannot fail successful RDPKG operation.
- [ ] Original ZIP and existing app-private volumes unchanged.
