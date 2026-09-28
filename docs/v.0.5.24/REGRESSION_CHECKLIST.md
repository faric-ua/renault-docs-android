# v0.5.24 Regression checklist

## Quality
- [ ] PdfRenderer still used.
- [ ] PNG lossless page encoding.
- [ ] 2400 px max render width.
- [ ] DPR cap 2.5.
- [ ] 100/150/200% zoom remains usable.

## Cache / rotation
- [ ] Shared cache survives Activity recreation.
- [ ] Cache is dataset namespaced.
- [ ] Cache limited to 32 MiB compressed bytes.
- [ ] Same PDF remains open through rotation.
- [ ] Second rotation reuses cached page variants where possible.

## Scheduling
- [ ] Current page requested before neighbors.
- [ ] Neighbor prefetch delayed.
- [ ] Existing bitmap not intentionally blanked during resolution upgrade.
- [ ] Resize re-render debounced.
- [ ] Lazy prefetch reduced to 700 px.

## Stability
- [ ] Continuous scroll still works.
- [ ] Page counter still tracks current page.
- [ ] Back works.
- [ ] Save PDF works.
- [ ] No OOM in real-phone multi-page test.

## Existing contracts
- [ ] Classic unchanged.
- [ ] Runtime IR unchanged.
- [ ] Point 9 not required.
