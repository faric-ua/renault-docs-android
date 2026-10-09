# Progress regression checklist

- [ ] Kotlin tests for stage / count formatting and measured normalization.
- [ ] Python source contract checks where available.
- [ ] Android PR Check compiles exact feature head.
- [ ] Project status (ZIP/raw) has fixed stage + counter slots; counters do not blink.
- [ ] .rdpkg import/export/share and .rdproject preparation/share use same component.
- [ ] Drive/catalog import card uses same component.
- [ ] Converter thin bar and compact stage/count visually match the status contract.
- [ ] Home/Status menu card survives collapse/expand with live progress visible.
- [ ] Portrait/landscape/rotation keep useful text without jumping or restarting work.
- [ ] Indeterminate/unknown total has no bouncing fake progress and does not retain a previous phase's completed 100%.
- [ ] COMPLETE / FAILED / CANCELLED continue to work; copy status retains full message.
- [ ] Original ZIP and installed Megane II / Kangoo II volumes remain unchanged.
