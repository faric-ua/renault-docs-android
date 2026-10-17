"""Original animated Renodocs emblem (no third-party marks). 400x400 GIF, white background."""
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

S = 4                 # supersampling
W = H = 400
N = 30                # frames
NAVY = (16, 19, 24)
NAVY2 = (34, 42, 56)
BLUE = (118, 189, 255)
YEL = (244, 197, 66)
WHITE = (255, 255, 255)
PAGE = (243, 246, 248)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def frame(t):
    big = Image.new("RGB", (W * S, H * S), WHITE)
    d = ImageDraw.Draw(big)
    cx, cy, r = 200 * S, 170 * S, 140 * S
    # emblem: soft shadow + navy disc + blue ring
    sh = Image.new("L", big.size, 0)
    ImageDraw.Draw(sh).ellipse((cx - r + 6 * S, cy - r + 12 * S, cx + r + 6 * S, cy + r + 12 * S), fill=70)
    sh = sh.filter(ImageFilter.GaussianBlur(10 * S))
    big.paste((150, 160, 175), (0, 0), sh)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=NAVY)
    # radial gradient inside
    for i in range(40):
        rr = r - 8 * S - i * 3 * S
        if rr <= 0:
            break
        k = i / 40
        col = tuple(int(NAVY[j] + (NAVY2[j] - NAVY[j]) * k) for j in range(3))
        d.ellipse((cx - rr, cy - rr - i * S, cx + rr, cy + rr - i * S), fill=col)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=BLUE, width=7 * S)
    # rotating dashes on ring (gear-like ticks)
    for k in range(24):
        a = 2 * math.pi * k / 24 + 2 * math.pi * t / 24
        r1, r2 = r - 16 * S, r - 24 * S
        d.line((cx + r1 * math.cos(a), cy + r1 * math.sin(a), cx + r2 * math.cos(a), cy + r2 * math.sin(a)),
               fill=(70, 110, 150), width=3 * S)
    # document page with folded corner
    x0, y0, x1, y1 = cx - 62 * S, cy - 82 * S, cx + 62 * S, cy + 82 * S
    fold = 30 * S
    d.polygon([(x0, y0), (x1 - fold, y0), (x1, y0 + fold), (x1, y1), (x0, y1)], fill=PAGE)
    d.polygon([(x1 - fold, y0), (x1 - fold, y0 + fold), (x1, y0 + fold)], fill=(200, 212, 224))
    # schematic traces drawing themselves
    p = (t % N) / N
    lines = [(y0 + 55 * S, 70), (y0 + 85 * S, 92), (y0 + 115 * S, 58), (y0 + 145 * S, 80)]
    for idx, (yy, length) in enumerate(lines):
        prog = max(0.0, min(1.0, p * 1.6 - idx * 0.15))
        lx0 = x0 + 18 * S
        d.line((lx0, yy, lx0 + length * S * prog, yy), fill=BLUE, width=7 * S)
        d.ellipse((lx0 - 5 * S, yy - 5 * S, lx0 + 5 * S, yy + 5 * S), fill=NAVY2)
    # pulsing yellow node
    pulse = 0.5 + 0.5 * math.sin(2 * math.pi * t / N)
    nx, ny = x1 - 22 * S, y0 + 100 * S
    gr = (10 + 8 * pulse) * S
    glow = Image.new("L", big.size, 0)
    ImageDraw.Draw(glow).ellipse((nx - gr * 2, ny - gr * 2, nx + gr * 2, ny + gr * 2), fill=int(110 * pulse))
    glow = glow.filter(ImageFilter.GaussianBlur(8 * S))
    big.paste(YEL, (0, 0), glow)
    d = ImageDraw.Draw(big)
    d.ellipse((nx - 10 * S, ny - 10 * S, nx + 10 * S, ny + 10 * S), fill=YEL)
    # light sweep across emblem
    sweep = Image.new("L", big.size, 0)
    sx = (-r + (2.6 * r) * ((t % N) / N)) + cx - r
    ImageDraw.Draw(sweep).polygon([(sx, cy - r), (sx + 40 * S, cy - r), (sx - 60 * S, cy + r), (sx - 100 * S, cy + r)], fill=80)
    sweep = sweep.filter(ImageFilter.GaussianBlur(6 * S))
    mask = Image.new("L", big.size, 0)
    ImageDraw.Draw(mask).ellipse((cx - r + 4 * S, cy - r + 4 * S, cx + r - 4 * S, cy + r - 4 * S), fill=255)
    from PIL import ImageChops
    sweep = ImageChops.multiply(sweep, mask)
    big.paste(WHITE, (0, 0), sweep)
    # wordmark
    d = ImageDraw.Draw(big)
    f = ImageFont.truetype(FONT, 46 * S)
    text = "RENODOCS"
    tw = d.textlength(text, font=f)
    tx, ty = (W * S - tw) / 2, 330 * S
    d.text((tx, ty), "RENO", font=f, fill=NAVY)
    d.text((tx + d.textlength("RENO", font=f), ty), "DOCS", font=f, fill=(40, 120, 200))
    return big.resize((W, H), Image.LANCZOS)


frames = [frame(t) for t in range(N)]
pal = [fr.convert("P", palette=Image.ADAPTIVE, colors=64) for fr in frames]
pal[0].save("RENODOCS_LOGO.gif", save_all=True, append_images=pal[1:], duration=80, loop=0, optimize=True, disposal=1)
frames[0].save("preview_frame0.png")
frames[N // 2].save("preview_frame15.png")
