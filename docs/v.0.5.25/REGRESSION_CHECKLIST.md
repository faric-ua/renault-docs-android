# v0.5.25 Regression checklist

## Toolbar separation
- [ ] Toolbar is outside #pdfViewport.
- [ ] html/body do not own PDF scrolling.
- [ ] PDF viewport owns vertical/horizontal document scroll.
- [ ] Toolbar never expands with zoomed page width.
- [ ] Portrait = two toolbar rows.
- [ ] Landscape = one toolbar row.

## High-zoom scroll
- [ ] >=150% neighbor prefetch radius = 1.
- [ ] >=150% decoded retain radius = 1.
- [ ] Far page src values are evicted.
- [ ] Page placeholders preserve document geometry.
- [ ] Shared v0.5.24 compressed cache still reused.
- [ ] No OOM on 20-page 200% phone test.

## Rotation state
- [ ] Current page restored.
- [ ] Zoom restored.
- [ ] Page-relative vertical position restored.
- [ ] Proportional horizontal pan restored.
- [ ] State is short-lived (5 minutes).

## v0.5.24 regression
- [ ] PNG rendering retained.
- [ ] 2400 px max retained.
- [ ] DPR 2.5 retained.
- [ ] Shared 32 MiB cache retained.
- [ ] Save PDF works.
- [ ] Back works.

## Existing project contracts
- [ ] Classic unchanged.
- [ ] Runtime IR unchanged.
- [ ] Point 9 not required.
